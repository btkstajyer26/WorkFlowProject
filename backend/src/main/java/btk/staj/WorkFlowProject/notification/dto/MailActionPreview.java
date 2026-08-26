package btk.staj.WorkFlowProject.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Onay ekraninin ihtiyaci olan bilgi. Anahtarin kendisi <strong>donmez</strong>;
 * istemci zaten elindeki degeri tasir.
 *
 * @param recordId      aksiyonun uygulanacagi evrak
 * @param recordTitle   kullanicinin neyi onayladigini gormesi icin
 * @param recordStatus  evragin o anki durumu; arada degistiyse arayuz uyarabilir
 * @param action        {@code WorkflowAction} enum adi
 * @param recipientName anahtarin verildigi kisi; yanlis hesapla acilan
 *                      baglantida kullanici durumu anlayabilsin diye
 * @param expiresAt     baglantinin son gecerlilik ani
 */
public record MailActionPreview(
        UUID recordId,
        String recordTitle,
        String recordStatus,
        String action,
        String recipientName,
        LocalDateTime expiresAt) {
}
