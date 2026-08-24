#!/usr/bin/env bash
#
# TEST ortamini uctan uca yurutmeye yetecek hesap ve kayitlarla doldurur.
#
# Kullanim (backend ayaktayken, BOS bir veritabaninda bir kez):
#
#   chmod 600 seed.env          # icine asagidaki degiskenler yazilir
#   set -a; . ./seed.env; set +a
#   ./deploy/seed-test-data.sh
#   unset TEST_TEMP_PASSWORD TEST_USER_PASSWORD TEST_ADMIN_FINAL_PASSWORD \
#         BOOTSTRAP_ADMIN_PASSWORD
#
# Degiskenleri komut satirinda "VAR=... ./seed..." diye ONUNE YAZMAYIN: o bicim
# hem kabuk gecmisine hem de sunucudaki herkesin okuyabildigi /proc/<pid>/environ
# ve `ps e` ciktisina duser. Terminalden calistiriyorsaniz parola degiskenlerini
# hic tanimlamayip betigin sormasina birakabilirsiniz; girisi ekrana yazmaz.
#
# Zorunlu degiskenler (VARSAYILAN DEGERI YOKTUR, eksikse betik baslamaz):
#
#   BASE                      TEST API adresi, orn. https://ornek.duckdns.org
#   BOOTSTRAP_ADMIN_EMAIL     .env icindeki bootstrap Admin adresi
#   BOOTSTRAP_ADMIN_PASSWORD  .env icindeki bootstrap Admin gecici parolasi
#   TEST_TEMP_PASSWORD        createUser'in actigi gecici parola
#   TEST_USER_PASSWORD        ekiple paylasilacak Calisan/Bsk.Yrd./Baskan parolasi
#   TEST_ADMIN_FINAL_PASSWORD YALNIZ Admin'in parolasi; ekiple paylasilmaz
#
# Admin parolasinin ayri olmasinin sebebi: test hesabi parolasi ekip kanalinda
# dolasir. Admin de ayni parolayi kullansaydi, parolayi alan herkes kullanici
# olusturup rol degistirebilir ve denetim kayitlarini okuyabilirdi.
#
# Neden dogrudan SQL degil: parolalar bcrypt'li, audit satirlari yalnizca
# servis katmanindan gecince olusuyor ve durum gecisleri records ile
# audit_logs arasinda tutarli kalmali. API uzerinden gitmek bunlari bedava
# veriyor; INSERT ile gitmek yarim bir veri seti uretirdi.
#
# --- Idempotent DEGILDIR; ama korumasizca da calismaz ---
#
# Betik ilk is olarak ortamin temiz olup olmadigini kontrol eder ve seed'in
# daha once calistigini gorurse HICBIR degisiklik yapmadan durur. Yarim kalmis
# bir kosumdan sonra ne yapilacagi asagidaki TOPARLAMA bolumunde yazili.
#
# TOPARLAMA
#
# 1) Betik "Admin parolasi zaten ... degistirilmis" diyerek durduysa: ilk adim
#    gecmis, hesaplar acilmamis olabilir. Kaldigi yerden devam etmek icin:
#
#      SEED_RESUME_AFTER_ADMIN=1 ./deploy/seed-test-data.sh
#
#    Bu mod yalnizca Admin parola degisimini atlar; diger kontroller korunur.
#
# 2) Hesaplar kismen acilmissa: backend'de kullanici silme ucu YOKTUR, bu yuzden
#    temizlik veritabani seviyesinde yapilir. Asagidaki blok yalnizca seed
#    hesaplarini ve onlarin kayitlarini siler; bootstrap Admin bilerek kalir.
#    Tablo ve kolon adlarini calistirmadan once \d ile dogrulayin, sema
#    degismis olabilir.
#
#      docker compose exec -T db psql -U "$DB_USER" -d "$DB_NAME" <<'SQL'
#      BEGIN;
#      CREATE TEMP TABLE seed_users AS
#        SELECT id FROM users WHERE email LIKE '%@ebys-test.local';
#      DELETE FROM audit_logs
#        WHERE record_id IN (SELECT id FROM records
#                            WHERE created_by IN (SELECT id FROM seed_users));
#      DELETE FROM records
#        WHERE created_by IN (SELECT id FROM seed_users);
#      DELETE FROM user_audit_logs
#        WHERE user_id IN (SELECT id FROM seed_users)
#           OR actor_id IN (SELECT id FROM seed_users);
#      DELETE FROM users WHERE id IN (SELECT id FROM seed_users);
#      COMMIT;
#      SQL
#
# 3) `docker compose down -v` SON CAREDIR ve yalnizca "bu veritabanindaki her
#    sey atilabilir" diyebiliyorsaniz kullanilir: uploads volume'unu de siler,
#    yani yuklenmis butun dosyalar gider. Yarim seed'in normal cozumu 1 veya
#    2. maddedir.

set -euo pipefail
umask 077

die() { printf 'HATA: %s\n' "$*" >&2; exit 1; }

# --- Zorunlu degiskenler -----------------------------------------------------

BASE="${BASE:?BASE verilmeli, orn: https://ornek.duckdns.org}"
BASE="${BASE%/}"
ADMIN_MAIL="${BOOTSTRAP_ADMIN_EMAIL:?BOOTSTRAP_ADMIN_EMAIL verilmeli}"

# Parola degiskeni tanimli degilse ve terminalden calisiyorsak ekrana yazmadan
# sorar. Boylece parola ne kabuk gecmisine ne de surec tablosuna duser.
prompt_secret() {
	local var_name=$1 label=$2 value=''
	[ -n "${!var_name:-}" ] && return 0
	[ -t 0 ] || die "$var_name verilmeli ($label). Terminalsiz kosumda sorulamaz."
	printf '%s: ' "$label" >&2
	read -rs value
	printf '\n' >&2
	[ -n "$value" ] || die "$var_name bos birakilamaz."
	printf -v "$var_name" '%s' "$value"
}

prompt_secret BOOTSTRAP_ADMIN_PASSWORD  'Bootstrap Admin gecici parolasi'
prompt_secret TEST_TEMP_PASSWORD        'TEST_TEMP_PASSWORD (gecici parola)'
prompt_secret TEST_USER_PASSWORD        'TEST_USER_PASSWORD (ekiple paylasilacak)'
prompt_secret TEST_ADMIN_FINAL_PASSWORD 'TEST_ADMIN_FINAL_PASSWORD (yalniz Admin)'

ADMIN_PW="$BOOTSTRAP_ADMIN_PASSWORD"
RESUME_AFTER_ADMIN="${SEED_RESUME_AFTER_ADMIN:-0}"

command -v jq   >/dev/null || die 'jq gerekli: sudo apt install -y jq'
command -v curl >/dev/null || die 'curl gerekli'

# --- Parola kurallari: ilk HTTP cagrisindan ONCE dogrulanir ------------------
#
# Kurallarin kaynagi backend'dir; buradaki kopya hatayi yarim seed'li bir
# veritabani birakmadan daha ilk saniyede gostermek icindir:
#   ChangePasswordRequest: en az 8 karakter, en az bir harf, en az bir rakam
#   CreateUserRequest    : en az 6 karakter
# Bash lookahead desteklemedigi icin backend'in tek regex'i uc kontrole
# bolundu. Hicbir mesaj parolanin kendisini yazmaz, yalnizca degisken adini.

require_change_password_policy() {
	local label=$1 pw=$2
	[ "${#pw}" -ge 8 ]    || die "$label en az 8 karakter olmali (backend change-password kurali)."
	[[ $pw =~ [A-Za-z] ]] || die "$label en az bir harf icermeli (backend change-password kurali)."
	[[ $pw =~ [0-9] ]]    || die "$label en az bir rakam icermeli (backend change-password kurali)."
}

require_distinct() {
	local label_a=$1 value_a=$2 label_b=$3 value_b=$4
	[ "$value_a" != "$value_b" ] || die "$label_a ile $label_b ayni olamaz."
}

# TEST_TEMP_PASSWORD yalnizca createUser'a gider; oradaki kural daha gevsek.
[ "${#TEST_TEMP_PASSWORD}" -ge 6 ] \
	|| die 'TEST_TEMP_PASSWORD en az 6 karakter olmali (backend createUser kurali).'
require_change_password_policy TEST_USER_PASSWORD        "$TEST_USER_PASSWORD"
require_change_password_policy TEST_ADMIN_FINAL_PASSWORD "$TEST_ADMIN_FINAL_PASSWORD"

# changePassword yeni parolanin eskisiyle ayni olmasini PASSWORD_REUSED ile
# reddeder; asagidaki dortlu bu hatanin akisin ortasinda patlamasini onler.
# Ucuncu satir ayrica Admin ile test hesaplarinin parolasini ayirmayi zorunlu
# kilar: ayni olsalardi ekip kanalindaki parola Admin yetkisi verirdi.
require_distinct TEST_TEMP_PASSWORD "$TEST_TEMP_PASSWORD" TEST_USER_PASSWORD        "$TEST_USER_PASSWORD"
require_distinct TEST_TEMP_PASSWORD "$TEST_TEMP_PASSWORD" TEST_ADMIN_FINAL_PASSWORD "$TEST_ADMIN_FINAL_PASSWORD"
require_distinct TEST_USER_PASSWORD "$TEST_USER_PASSWORD" TEST_ADMIN_FINAL_PASSWORD "$TEST_ADMIN_FINAL_PASSWORD"
require_distinct BOOTSTRAP_ADMIN_PASSWORD "$ADMIN_PW"     TEST_ADMIN_FINAL_PASSWORD "$TEST_ADMIN_FINAL_PASSWORD"

# --- HTTP yardimcilari -------------------------------------------------------
#
# Govde her zaman stdin'den (--data @-) gider. `-d "$json"` bicimi govdeyi
# argumana koyardi ve parolalar `ps` ciktisinda gorunurdu. Govdeler jq -n --arg
# ile uretiliyor: parola icindeki tirnak, ters bolu veya $ karakteri JSON'i
# bozmaz.

api()     { curl -sS --fail-with-body -H 'Content-Type: application/json' --data @- "$@"; }
api_get() { curl -sS --fail-with-body "$@"; }

credentials_json() { jq -n --arg email "$1" --arg password "$2" '{email:$email,password:$password}'; }

# eposta parola -> accessToken
login() {
	local token
	token=$(credentials_json "$1" "$2" | api -X POST "$BASE/api/auth/login" | jq -r '.accessToken // empty')
	[ -n "$token" ] || { printf 'Giris basarisiz: %s\n' "$1" >&2; return 1; }
	printf '%s' "$token"
}

# eposta parola -> 0 ise giris basarili. Sessizdir ve betigi dusurmez;
# yalnizca temiz baslangic kontrolunde kullanilir.
probe_login() {
	local code
	code=$(credentials_json "$1" "$2" \
		| curl -sS -o /dev/null -w '%{http_code}' \
			-H 'Content-Type: application/json' --data @- \
			-X POST "$BASE/api/auth/login" 2>/dev/null || printf '000')
	[ "$code" = '200' ]
}

# token eskiParola yeniParola
change_pw() {
	jq -n --arg currentPassword "$2" --arg newPassword "$3" \
		'{currentPassword:$currentPassword,newPassword:$newPassword}' \
		| api -X POST "$BASE/api/auth/change-password" -H "Authorization: Bearer $1" > /dev/null
}

# --- Seed kimlikleri ---------------------------------------------------------

SEED_MAIL_DOMAIN=ebys-test.local
C1_MAIL="calisan1@$SEED_MAIL_DOMAIN"
C2_MAIL="calisan2@$SEED_MAIL_DOMAIN"
BY_MAIL="bskyrd@$SEED_MAIL_DOMAIN"
BK_MAIL="baskan@$SEED_MAIL_DOMAIN"

# --- Temiz baslangic kontrolu ------------------------------------------------

echo '==> Ortam kontrolu'
HEALTH=$(api_get "$BASE/actuator/health" | jq -r '.status // empty') \
	|| die "Backend'e ulasilamadi: $BASE"
[ "$HEALTH" = 'UP' ] || die "Backend health UP degil (gelen: ${HEALTH:-bos}). Seed baslatilmadi."

if probe_login "$C1_MAIL" "$TEST_USER_PASSWORD"; then
	die "$C1_MAIL zaten TEST_USER_PASSWORD ile giris yapiyor: seed bu ortamda daha
       once calismis. Hicbir degisiklik yapilmadi; TOPARLAMA bolumune bakin."
fi

if probe_login "$ADMIN_MAIL" "$TEST_ADMIN_FINAL_PASSWORD"; then
	if [ "$RESUME_AFTER_ADMIN" != '1' ]; then
		die "Admin parolasi zaten TEST_ADMIN_FINAL_PASSWORD; seed'in ilk adimi gecmis
       ama test hesaplari acilmamis: kosum buyuk ihtimalle YARIM kalmis.
       Kaldigi yerden devam icin: SEED_RESUME_AFTER_ADMIN=1 $0
       Hicbir degisiklik yapilmadi."
	fi
	echo '==> Bootstrap Admin: parola zaten degistirilmis, atlaniyor (resume modu)'
	ADMIN=$(login "$ADMIN_MAIL" "$TEST_ADMIN_FINAL_PASSWORD")
else
	[ "$RESUME_AFTER_ADMIN" != '1' ] \
		|| die 'SEED_RESUME_AFTER_ADMIN=1 verildi ama Admin parolasi henuz degismemis. Resume olmadan calistirin.'
	echo '==> Bootstrap Admin: zorunlu parola degisimi'
	# Bootstrap Admin de mustChangePassword=true acilir; degistirmeden
	# /api/admin/** uclarina erisemez (403 PASSWORD_CHANGE_REQUIRED).
	T=$(login "$ADMIN_MAIL" "$ADMIN_PW") \
		|| die "Bootstrap Admin girisi basarisiz. BOOTSTRAP_ADMIN_EMAIL ve
       BOOTSTRAP_ADMIN_PASSWORD backend'in .env dosyasindakiyle ayni mi?
       Hicbir degisiklik yapilmadi."
	change_pw "$T" "$ADMIN_PW" "$TEST_ADMIN_FINAL_PASSWORD"
	# changePassword aktif refresh token'lari iptal ediyor; yeniden giris sart.
	ADMIN=$(login "$ADMIN_MAIL" "$TEST_ADMIN_FINAL_PASSWORD")
fi

# Admin yetkisi elde edildi; hesap OLUSTURMADAN once kalinti var mi diye bak.
echo '==> Kalinti seed hesabi kontrolu'
EXISTING=$(api_get -H "Authorization: Bearer $ADMIN" --get \
	--data-urlencode "q=$SEED_MAIL_DOMAIN" --data-urlencode 'size=100' \
	"$BASE/api/admin/users" | jq -r '.content[].email' | sort)
if [ -n "$EXISTING" ]; then
	die "Ortamda zaten @$SEED_MAIL_DOMAIN hesaplari var:
$(printf '%s\n' "$EXISTING" | sed 's/^/       /')
       Kullanici olusturulmadan durduruldu; TOPARLAMA bolumune bakin."
fi

# --- Hesaplar ----------------------------------------------------------------

echo '==> Hesaplar olusturuluyor (hepsi CALISAN dogar)'
# ad soyad e-posta -> uuid
create_user() {
	jq -n --arg firstName "$1" --arg lastName "$2" --arg email "$3" \
		--arg password "$TEST_TEMP_PASSWORD" \
		'{firstName:$firstName,lastName:$lastName,email:$email,password:$password}' \
		| api -X POST "$BASE/api/admin/users" -H "Authorization: Bearer $ADMIN" \
		| jq -r .id
}

create_user Ahmet  Yilmaz "$C1_MAIL" > /dev/null
create_user Zeynep Demir  "$C2_MAIL" > /dev/null
BY=$(create_user Ayse   Kaya  "$BY_MAIL")
BK=$(create_user Mehmet Aydin "$BK_MAIL")

echo '==> Rol terfileri'
# BASKAN ve BASKAN_YARDIMCISI tekil roller: sistemde ayni anda YALNIZ BIR
# aktif sahip olabilir. TargetUserResolver hedefi "aktif ve rolu X olan tek
# kullanici" diye cozuyor; ikinci bir yardimci acilirsa her workflow gecisi
# WORKFLOW_ROLE_NOT_CONFIGURED ile duser. Bu yuzden her rolden tam bir tane.
#
# CALISAN'dan cikis replacementBaskanYardimcisiId istemez; ters yon ister.
promote() {
	jq -n --arg roleName "$2" '{roleName:$roleName}' \
		| api -X PATCH "$BASE/api/admin/users/$1/role" -H "Authorization: Bearer $ADMIN" > /dev/null
}
promote "$BY" BASKAN_YARDIMCISI
promote "$BK" BASKAN

echo '==> Hesaplarin zorunlu parola degisimi'
# Bu adim atlanirsa hesaplar kullanilamaz: JwtAuthenticationFilter
# mustChangePassword=true iken yalnizca change-password, logout ve
# GET /api/users/me uclarina izin verip digerlerini 403 ile keser.
# e-posta -> accessToken
activate() {
	local t
	t=$(login "$1" "$TEST_TEMP_PASSWORD")
	change_pw "$t" "$TEST_TEMP_PASSWORD" "$TEST_USER_PASSWORD"
	login "$1" "$TEST_USER_PASSWORD"
}
TC1=$(activate "$C1_MAIL")
TC2=$(activate "$C2_MAIL")
TBY=$(activate "$BY_MAIL")
TBK=$(activate "$BK_MAIL")

# --- Ornek kayitlar ----------------------------------------------------------

echo '==> Ornek kayitlar'
CAT=$(api_get "$BASE/api/categories" -H "Authorization: Bearer $TC1" | jq -r '.[0].id // empty')
[ -n "$CAT" ] || die 'Kategori bulunamadi; kategori seed migration calismis mi?'

# token baslik -> uuid
new_record() {
	jq -n --arg title "$2" --argjson categoryId "$CAT" \
		'{title:$title,description:"TEST ortami ornek kaydi.",categoryId:$categoryId}' \
		| api -X POST "$BASE/api/records" -H "Authorization: Bearer $1" \
		| jq -r .id
}

# token recordId aksiyon [aciklama]
act() {
	local token=$1 record_id=$2
	if [ "$#" -ge 4 ]; then
		jq -n --arg action "$3" --arg comment "$4" '{action:$action,comment:$comment}'
	else
		jq -n --arg action "$3" '{action:$action}'
	fi | api -X POST "$BASE/api/records/$record_id/workflow/actions" \
		-H "Authorization: Bearer $token" > /dev/null
}

# Alti durumun her biri gercek gecislerle uretiliyor; boylece bildirimler ve
# islem gecmisi satirlari da kendiliginden olusuyor.
new_record "$TC1" 'Taslak halinde dilekce' > /dev/null            # TASLAK

R=$(new_record "$TC1" 'Bsk. Yrd. incelemesinde teklif')
act "$TC1" "$R" GONDER                                            # BSK_YRD_INCELEMESINDE

R=$(new_record "$TC1" 'Baskan onayinda butce talebi')
act "$TC1" "$R" GONDER
act "$TBY" "$R" BASKANA_ILET                                      # BASKAN_INCELEMESINDE

R=$(new_record "$TC1" 'Duzenleme bekleyen izin formu')
act "$TC1" "$R" GONDER
act "$TBY" "$R" CALISANA_GERI_GONDER 'Ek belge eksik.'            # DUZENLEME_BEKLIYOR

R=$(new_record "$TC1" 'Onaylanmis satinalma talebi')
act "$TC1" "$R" GONDER
act "$TBY" "$R" BASKANA_ILET
act "$TBK" "$R" ONAYLA 'Uygundur.'                                # ONAYLANDI

R=$(new_record "$TC1" 'Reddedilmis harcama talebi')
act "$TC1" "$R" GONDER
act "$TBY" "$R" BASKANA_ILET
act "$TBK" "$R" REDDET 'Butce disi.'                              # REDDEDILDI

# Gorunurluk kapsaminin negatif tarafi: calisan1 bu kaydi gormemeli.
new_record "$TC2" 'Baska calisanin taslagi' > /dev/null

# --- Ozet --------------------------------------------------------------------
#
# Ozette bilerek PAROLA YOKTUR. Bu cikti CI artifact'ine, ekip kanalina veya
# bir issue'ya yapistirildiginda ortami ele gecirecek hicbir sey tasimamalidir.

# token -> o kullanicinin gorebildigi kayit sayisi
visible_records() {
	api_get -H "Authorization: Bearer $1" --get --data-urlencode 'size=1' \
		"$BASE/api/records" | jq -r '.totalElements'
}

C1_SEEN=$(visible_records "$TC1")
C2_SEEN=$(visible_records "$TC2")
BY_SEEN=$(visible_records "$TBY")
BK_SEEN=$(visible_records "$TBK")

printf '\nSeed tamam.\n\n'
printf '  %-34s %-18s %s\n' 'Hesap' 'Rol' 'Gorunur kayit'
printf '  %-34s %-18s %s\n' '----------------------------------' '------------------' '-------------'
printf '  %-34s %-18s %s\n' "$C1_MAIL"    CALISAN           "$C1_SEEN"
printf '  %-34s %-18s %s\n' "$C2_MAIL"    CALISAN           "$C2_SEEN"
printf '  %-34s %-18s %s\n' "$BY_MAIL"    BASKAN_YARDIMCISI "$BY_SEEN"
printf '  %-34s %-18s %s\n' "$BK_MAIL"    BASKAN            "$BK_SEEN"
printf '  %-34s %-18s %s\n' "$ADMIN_MAIL" ADMIN             '-'

# Gorunur kayit sutunu gorunurluk kapsaminin kanitidir: calisan1 yalnizca kendi
# alti kaydini, calisan2 yalnizca kendi taslagini gormeli.

cat <<BILGI

Parolalar bilerek yazilmadi.
  - TEST_USER_PASSWORD ekip kanalindan paylasilir; envantere veya repoya YAZILMAZ.
  - TEST_ADMIN_FINAL_PASSWORD paylasilmaz; yalnizca ortam sahibinde kalir.
  - .env icindeki BOOTSTRAP_ADMIN_PASSWORD satirini artik silebilirsiniz.
  - Oturumu kapatmadan once parola degiskenlerini unset edin.
BILGI
