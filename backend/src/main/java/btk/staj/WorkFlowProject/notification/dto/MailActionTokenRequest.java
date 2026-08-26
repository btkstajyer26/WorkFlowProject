package btk.staj.WorkFlowProject.notification.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Anahtar govdede tasinir, adres cubugunda degil.
 *
 * <p>Proje kararina uyar: tek kullanimlik anahtarlar URL'ye yazilmaz (bkz.
 * parola sifirlama akisi). URL'ye yazilsaydi anahtar erisim log'larina,
 * {@code Referer} basligina ve tarayici gecmisine duserdi.
 */
public record MailActionTokenRequest(
        @NotBlank(message = "Bağlantı anahtarı boş olamaz") String token) {
}
