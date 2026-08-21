#!/usr/bin/env bash
#
# Public TEST dagitimindan ONCE calistirilir. Ortami ayaga kaldirmaz; yalnizca
# sunucudaki .env dosyasini denetler ve bilinen/ornek degerlerle internete
# cikilmasini engeller.
#
# Kullanim (sunucuda, repo kokunde):
#
#   ./deploy/preflight.sh              # .env dosyasini dener
#   ./deploy/preflight.sh /yol/.env    # baska bir dosyayi dener
#
# Cikis: 0 = dagitima uygun, 1 = en az bir engelleyici bulgu.
#
# Bu betik hicbir degerin tamamini yazdirmaz; ozet maskelidir. Ciktisi ekip
# kanalina yapistirilabilir olmalidir.

set -euo pipefail

ENV_FILE="${1:-.env}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
EXAMPLE_FILE="$SCRIPT_DIR/../.env.example"

ERRORS=0
WARNINGS=0

fail() { printf '  [HATA] %s\n' "$*"; ERRORS=$((ERRORS + 1)); }
warn() { printf '  [UYARI] %s\n' "$*"; WARNINGS=$((WARNINGS + 1)); }
ok()   { printf '  [OK] %s\n' "$*"; }

# dosya anahtar -> deger (son tanim kazanir, Compose'un davranisiyla ayni)
env_value() {
	[ -f "$1" ] || return 0
	sed -n "s/^[[:space:]]*$2=//p" "$1" | tail -n1
}

# Degerin ilk iki karakterini gosterir, gerisini gizler. Kisa degerler tamamen
# gizlenir; "ab***" bicimi 4 karakterlik bir parolayi ele verirdi.
mask() {
	local value=$1
	if [ -z "$value" ]; then printf '(bos)'
	elif [ "${#value}" -le 8 ]; then printf '*** (%d karakter)' "${#value}"
	else printf '%s*** (%d karakter)' "${value:0:2}" "${#value}"
	fi
}

[ -f "$ENV_FILE" ] || { printf 'HATA: %s bulunamadi.\n' "$ENV_FILE" >&2; exit 1; }

printf '== Dosya izni ==\n'
# Parolalar ve JWT anahtari burada duruyor; sunucudaki her kullanicinin
# okuyabilmesi kabul edilemez.
PERM=$(stat -c '%a' "$ENV_FILE" 2>/dev/null || printf '')
if [ -z "$PERM" ]; then
	warn "Dosya izni okunamadi (stat yok?); sunucuda 'chmod 600 $ENV_FILE' calistirin."
elif [ "$PERM" != '600' ] && [ "$PERM" != '400' ]; then
	fail "$ENV_FILE izni $PERM. Beklenen 600. Duzeltme: chmod 600 $ENV_FILE"
else
	ok "$ENV_FILE izni $PERM"
fi

printf '\n== Zorunlu anahtarlar ==\n'
REQUIRED_KEYS=(
	DB_NAME DB_USER DB_PASSWORD
	JWT_SECRET
	BOOTSTRAP_ADMIN_EMAIL BOOTSTRAP_ADMIN_PASSWORD
	CORS_ALLOWED_ORIGINS
	TEST_DOMAIN MAILPIT_USER MAILPIT_PASSWORD_HASH
)
for key in "${REQUIRED_KEYS[@]}"; do
	value=$(env_value "$ENV_FILE" "$key")
	if [ -z "$value" ]; then
		fail "$key bos veya tanimsiz."
	else
		ok "$(printf '%-24s %s' "$key" "$(mask "$value")")"
	fi
done

printf '\n== Ornek (.env.example) degerleri ==\n'
# Yasak listesini elle tutmak yerine .env.example ile karsilastiriyoruz: ornek
# dosya degistiginde bu kontrol kendiliginden guncel kalir.
if [ ! -f "$EXAMPLE_FILE" ]; then
	warn ".env.example bulunamadi; ornek deger karsilastirmasi atlandi."
else
	SECRET_KEYS=(DB_PASSWORD JWT_SECRET BOOTSTRAP_ADMIN_EMAIL BOOTSTRAP_ADMIN_PASSWORD MAILPIT_PASSWORD_HASH)
	SAME_AS_EXAMPLE=0
	for key in "${SECRET_KEYS[@]}"; do
		example_value=$(env_value "$EXAMPLE_FILE" "$key")
		actual_value=$(env_value "$ENV_FILE" "$key")
		if [ -n "$example_value" ] && [ "$actual_value" = "$example_value" ]; then
			fail "$key hala .env.example icindeki ornek degerde. Public ortamda kullanilamaz."
			SAME_AS_EXAMPLE=1
		fi
	done
	[ "$SAME_AS_EXAMPLE" -eq 0 ] && ok 'Hicbir gizli anahtar ornek degerinde degil.'
fi

printf '\n== Deger kurallari ==\n'

JWT_SECRET_VALUE=$(env_value "$ENV_FILE" JWT_SECRET)
if [ -n "$JWT_SECRET_VALUE" ] && [ "${#JWT_SECRET_VALUE}" -lt 32 ]; then
	fail "JWT_SECRET ${#JWT_SECRET_VALUE} karakter; HMAC-SHA256 en az 32 istiyor."
elif [ -n "$JWT_SECRET_VALUE" ]; then
	ok "JWT_SECRET uzunlugu ${#JWT_SECRET_VALUE} (>= 32)"
fi

TEST_DOMAIN_VALUE=$(env_value "$ENV_FILE" TEST_DOMAIN)
if printf '%s' "$TEST_DOMAIN_VALUE" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'; then
	fail "TEST_DOMAIN bir IP adresi. Let's Encrypt IP'ye sertifika vermez; alan adi gerekli."
elif [ -n "$TEST_DOMAIN_VALUE" ]; then
	ok "TEST_DOMAIN alan adi bicimindir"
fi

CORS_VALUE=$(env_value "$ENV_FILE" CORS_ALLOWED_ORIGINS)
if [ -n "$CORS_VALUE" ] && ! printf '%s' "$CORS_VALUE" | grep -q 'https://'; then
	warn 'CORS_ALLOWED_ORIGINS icinde https:// origin yok; public TEST icin beklenen bu degil.'
elif [ -n "$CORS_VALUE" ]; then
	ok 'CORS_ALLOWED_ORIGINS en az bir https origin iceriyor'
fi

# API-only topoloji kurali: TEST'te urun web frontend'i yok. FRONTEND_URL
# e-postadaki derin baglantiyi uretir; API adresi buraya yazilirsa kullanicilar
# calismayan linkler alir. Ayrinti: docs/TEST_ORTAMI_NOTU.md
FRONTEND_URL_VALUE=$(env_value "$ENV_FILE" FRONTEND_URL)
if [ -n "$TEST_DOMAIN_VALUE" ] && printf '%s' "$FRONTEND_URL_VALUE" | grep -q "$TEST_DOMAIN_VALUE"; then
	fail 'FRONTEND_URL, TEST_DOMAIN (API adresi) ile ayni. API-only TEST topolojisinde
         API adresi frontend diye tanitilmaz; bkz. docs/TEST_ORTAMI_NOTU.md'
else
	ok 'FRONTEND_URL API adresini frontend gibi gostermiyor'
fi

printf '\n== Sonuc ==\n'
if [ "$ERRORS" -gt 0 ]; then
	printf '  %d engelleyici bulgu, %d uyari. Dagitim yapilmamali.\n' "$ERRORS" "$WARNINGS"
	exit 1
fi
printf '  Engelleyici bulgu yok, %d uyari. Dagitima devam edilebilir.\n' "$WARNINGS"
