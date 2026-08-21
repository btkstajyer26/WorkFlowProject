#!/usr/bin/env bash
#
# TEST ortamini uctan uca yurutmeye yetecek hesap ve kayitlarla doldurur.
#
# Kullanim (backend ayaktayken, BOS bir veritabaninda bir kez):
#
#   BASE=https://ornek.duckdns.org \
#   BOOTSTRAP_ADMIN_EMAIL=admin@ebys-test.local \
#   BOOTSTRAP_ADMIN_PASSWORD=gecici-bootstrap-parolasi \
#   ./deploy/seed-test-data.sh
#
# Neden dogrudan SQL degil: parolalar bcrypt'li, audit satirlari yalnizca
# servis katmanindan gecince olusuyor ve durum gecisleri records ile
# audit_logs arasinda tutarli kalmali. API uzerinden gitmek bunlari bedava
# veriyor; INSERT ile gitmek yarim bir veri seti uretirdi.
#
# Idempotent DEGILDIR. Ikinci kez calistirilirsa hesaplar zaten var diye
# CONFLICT alirsiniz. Sifirlamak icin:
#   docker compose -f docker-compose.yml -f docker-compose.test.yml down -v
#   (dikkat: -v yuklenen dosyalari da siler)

set -euo pipefail

BASE="${BASE:?BASE verilmeli, orn: https://ornek.duckdns.org}"
ADMIN_MAIL="${BOOTSTRAP_ADMIN_EMAIL:?BOOTSTRAP_ADMIN_EMAIL verilmeli}"
ADMIN_PW="${BOOTSTRAP_ADMIN_PASSWORD:?BOOTSTRAP_ADMIN_PASSWORD verilmeli}"

# createUser her hesabi mustChangePassword=true ile aciyor; bu gecici parola
# betik icinde kullanilip hemen degistiriliyor.
TEMP_PW="${TEMP_PW:-Gecici1234}"
# Ekibe verilecek nihai parola. changePassword yeni parolanin eskisiyle ayni
# olmasini PASSWORD_REUSED ile reddettigi icin TEMP_PW'den farkli olmali.
FINAL_PW="${FINAL_PW:-Ebys2026test}"

command -v jq   >/dev/null || { echo "jq gerekli: sudo apt install -y jq"; exit 1; }
command -v curl >/dev/null || { echo "curl gerekli"; exit 1; }

if [ "$TEMP_PW" = "$FINAL_PW" ]; then
	echo "TEMP_PW ile FINAL_PW ayni olamaz (PASSWORD_REUSED)."; exit 1
fi

api() { curl -sS --fail-with-body -H "Content-Type: application/json" "$@"; }

# e-posta parola -> accessToken
login() {
	local token
	token=$(api -X POST "$BASE/api/auth/login" \
		-d "{\"email\":\"$1\",\"password\":\"$2\"}" | jq -r '.accessToken // empty')
	[ -n "$token" ] || { echo "Giris basarisiz: $1" >&2; return 1; }
	printf '%s' "$token"
}

# token eskiParola yeniParola
change_pw() {
	api -X POST "$BASE/api/auth/change-password" -H "Authorization: Bearer $1" \
		-d "{\"currentPassword\":\"$2\",\"newPassword\":\"$3\"}" > /dev/null
}

echo "==> Bootstrap Admin: zorunlu parola degisimi"
# Bootstrap Admin de mustChangePassword=true acilir; degistirmeden
# /api/admin/** uclarina erisemez (403 PASSWORD_CHANGE_REQUIRED).
T=$(login "$ADMIN_MAIL" "$ADMIN_PW")
change_pw "$T" "$ADMIN_PW" "$FINAL_PW"
# changePassword aktif refresh token'lari iptal ediyor; yeniden giris sart.
ADMIN=$(login "$ADMIN_MAIL" "$FINAL_PW")

echo "==> Hesaplar olusturuluyor (hepsi CALISAN dogar)"
# ad soyad e-posta -> uuid
create_user() {
	api -X POST "$BASE/api/admin/users" -H "Authorization: Bearer $ADMIN" \
		-d "{\"firstName\":\"$1\",\"lastName\":\"$2\",\"email\":\"$3\",\"password\":\"$TEMP_PW\"}" \
		| jq -r .id
}

C1_MAIL=calisan1@ebys-test.local
C2_MAIL=calisan2@ebys-test.local
BY_MAIL=bskyrd@ebys-test.local
BK_MAIL=baskan@ebys-test.local

create_user Ahmet  Yilmaz "$C1_MAIL" > /dev/null
create_user Zeynep Demir  "$C2_MAIL" > /dev/null
BY=$(create_user Ayse   Kaya  "$BY_MAIL")
BK=$(create_user Mehmet Aydin "$BK_MAIL")

echo "==> Rol terfileri"
# BASKAN ve BASKAN_YARDIMCISI tekil roller: sistemde ayni anda YALNIZ BIR
# aktif sahip olabilir. TargetUserResolver hedefi "aktif ve rolu X olan tek
# kullanici" diye cozuyor; ikinci bir yardimci acilirsa her workflow gecisi
# WORKFLOW_ROLE_NOT_CONFIGURED ile duser. Bu yuzden her rolden tam bir tane.
#
# CALISAN'dan cikis replacementBaskanYardimcisiId istemez; ters yon ister.
promote() {
	api -X PATCH "$BASE/api/admin/users/$1/role" -H "Authorization: Bearer $ADMIN" \
		-d "{\"roleName\":\"$2\"}" > /dev/null
}
promote "$BY" BASKAN_YARDIMCISI
promote "$BK" BASKAN

echo "==> Hesaplarin zorunlu parola degisimi"
# Bu adim atlanirsa hesaplar kullanilamaz: JwtAuthenticationFilter
# mustChangePassword=true iken yalnizca change-password, logout ve
# GET /api/users/me uclarina izin verip digerlerini 403 ile keser.
# e-posta -> accessToken
activate() {
	local t
	t=$(login "$1" "$TEMP_PW")
	change_pw "$t" "$TEMP_PW" "$FINAL_PW"
	login "$1" "$FINAL_PW"
}
TC1=$(activate "$C1_MAIL")
TC2=$(activate "$C2_MAIL")
TBY=$(activate "$BY_MAIL")
TBK=$(activate "$BK_MAIL")

echo "==> Ornek kayitlar"
CAT=$(api "$BASE/api/categories" -H "Authorization: Bearer $TC1" | jq -r '.[0].id')

# token baslik -> uuid
new_record() {
	api -X POST "$BASE/api/records" -H "Authorization: Bearer $1" \
		-d "{\"title\":\"$2\",\"description\":\"TEST ortami ornek kaydi.\",\"categoryId\":$CAT}" \
		| jq -r .id
}

# token recordId aksiyon [aciklama]
act() {
	local body="{\"action\":\"$3\""
	[ $# -ge 4 ] && body="$body,\"comment\":\"$4\""
	api -X POST "$BASE/api/records/$2/workflow/actions" \
		-H "Authorization: Bearer $1" -d "$body}" > /dev/null
}

# Alti durumun her biri gercek gecislerle uretiliyor; boylece bildirimler ve
# islem gecmisi satirlari da kendiliginden olusuyor.
new_record "$TC1" "Taslak halinde dilekce" > /dev/null            # TASLAK

R=$(new_record "$TC1" "Bsk. Yrd. incelemesinde teklif")
act "$TC1" "$R" GONDER                                            # BSK_YRD_INCELEMESINDE

R=$(new_record "$TC1" "Baskan onayinda butce talebi")
act "$TC1" "$R" GONDER
act "$TBY" "$R" BASKANA_ILET                                      # BASKAN_INCELEMESINDE

R=$(new_record "$TC1" "Duzenleme bekleyen izin formu")
act "$TC1" "$R" GONDER
act "$TBY" "$R" CALISANA_GERI_GONDER "Ek belge eksik."            # DUZENLEME_BEKLIYOR

R=$(new_record "$TC1" "Onaylanmis satinalma talebi")
act "$TC1" "$R" GONDER
act "$TBY" "$R" BASKANA_ILET
act "$TBK" "$R" ONAYLA "Uygundur."                                # ONAYLANDI

R=$(new_record "$TC1" "Reddedilmis harcama talebi")
act "$TC1" "$R" GONDER
act "$TBY" "$R" BASKANA_ILET
act "$TBK" "$R" REDDET "Butce disi."                              # REDDEDILDI

# Gorunurluk kapsaminin negatif tarafi: calisan1 bu kaydi gormemeli.
new_record "$TC2" "Baska calisanin taslagi" > /dev/null

cat <<BILGI

Seed tamam.

  Calisan 1 : $C1_MAIL
  Calisan 2 : $C2_MAIL
  Bsk. Yrd. : $BY_MAIL
  Baskan    : $BK_MAIL
  Admin     : $ADMIN_MAIL
  Parola    : $FINAL_PW  (hepsi ayni)

Parolayi ekip kanalindan paylasin; envantere veya repoya yazmayin.
.env icindeki BOOTSTRAP_ADMIN_PASSWORD satirini artik silebilirsiniz.
BILGI
