package btk.staj.WorkFlowProject.auth.dto;

/**
 * Kod doğrulandığında dönen tek kullanımlık anahtar. Arayüz bunu şifre
 * değiştirme ekranına taşır ve {@code /reset-password} çağrısında kullanır.
 *
 * @param resetToken       tek kullanımlık sıfırlama anahtarı
 * @param expiresInSeconds anahtarın kalan geçerlilik süresi
 */
public record VerifyResetCodeResponse(String resetToken, long expiresInSeconds) {
}
