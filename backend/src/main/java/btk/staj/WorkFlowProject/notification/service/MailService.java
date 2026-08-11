package btk.staj.WorkFlowProject.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // Backend sunucu adresimiz (Hızlı onay API isteği için)
    @Value("${app.backend-url:http://localhost:8086}")
    private String backendUrl;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendStatusChangeMail(String toEmail, String recipientName, UUID recordId, String title, String status, String reason) {
        try {
            log.info("E-posta gönderimi başlatılıyor. Alıcı: {}, Evrak: {}", toEmail, recordId);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String deepLink = frontendUrl + "/records/" + recordId;
            
            // Doğrudan onaylama yapacak backend adresi
            String quickApproveUrl = backendUrl + "/api/records/quick-approve?recordId=" + recordId + "&userEmail=" + toEmail;

            String htmlContent = """
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#eef1f5; padding:32px 0; font-family:Segoe UI, Arial, sans-serif;">
  <tr>
    <td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 6px rgba(0,0,0,0.08);">

        <tr>
          <td style="background-color:#0078d4; padding:24px 32px;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
              <tr>
                <td style="font-family:Segoe UI, Arial, sans-serif; font-size:18px; font-weight:bold; color:#ffffff;">
                  📄 İş Akışı ve Onay Yönetim Sistemi (EBYS)
                </td>
              </tr>
            </table>
          </td>
        </tr>

        <tr>
          <td style="padding:32px;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
              <tr>
                <td style="font-family:Segoe UI, Arial, sans-serif; font-size:16px; color:#1a1a1a; padding-bottom:8px;">
                  Sayın <b>%s</b>,
                </td>
              </tr>
              <tr>
                <td style="font-family:Segoe UI, Arial, sans-serif; font-size:14px; color:#555555; line-height:22px; padding-bottom:24px;">
                  Sistemde onayınızı bekleyen bir evrak bulunmaktadır. Aşağıdaki butonları kullanarak hızlıca işlem yapabilirsiniz.
                </td>
              </tr>

              <tr>
                <td>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f7f9fc; border:1px solid #e3e8ef; border-radius:6px;">
                    <tr>
                      <td style="padding:16px 20px; font-family:Segoe UI, Arial, sans-serif; font-size:13px; color:#8a8f98; border-bottom:1px solid #e3e8ef;">
                        EVRAK NO
                      </td>
                      <td style="padding:16px 20px; font-family:Segoe UI, Arial, sans-serif; font-size:14px; color:#1a1a1a; font-weight:600; text-align:right; border-bottom:1px solid #e3e8ef;">
                        %s
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:16px 20px; font-family:Segoe UI, Arial, sans-serif; font-size:13px; color:#8a8f98; border-bottom:1px solid #e3e8ef;">
                        BAŞLIK
                      </td>
                      <td style="padding:16px 20px; font-family:Segoe UI, Arial, sans-serif; font-size:14px; color:#1a1a1a; font-weight:600; text-align:right; border-bottom:1px solid #e3e8ef;">
                        %s
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:16px 20px; font-family:Segoe UI, Arial, sans-serif; font-size:13px; color:#8a8f98; border-bottom:1px solid #e3e8ef;">
                        GÜNCEL DURUM
                      </td>
                      <td style="padding:16px 20px; text-align:right; border-bottom:1px solid #e3e8ef;">
                        <span style="display:inline-block; background-color:#e6f2fc; color:#0078d4; font-family:Segoe UI, Arial, sans-serif; font-size:13px; font-weight:bold; padding:4px 12px; border-radius:12px;">
                          %s
                        </span>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:16px 20px; font-family:Segoe UI, Arial, sans-serif; font-size:13px; color:#8a8f98;">
                        AÇIKLAMA
                      </td>
                      <td style="padding:16px 20px; font-family:Segoe UI, Arial, sans-serif; font-size:14px; color:#333333; font-style:italic; text-align:right;">
                        %s
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>

              <tr>
                <td align="center" style="padding-top:32px;">
                  <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                    <tr>
                      <td align="center" bgcolor="#107c41" style="border-radius:4px; padding-right:10px;">
                        <a href="%s"
                           target="_blank"
                           style="display:inline-block; padding:12px 22px; font-family:Segoe UI, Arial, sans-serif; font-size:14px; font-weight:bold; color:#ffffff; text-decoration:none; border-radius:4px;">
                          Evrağı Onayla
                        </a>
                      </td>
                      <td width="16" style="width:16px; font-size:0; line-height:0;">&nbsp;</td>

                      <td align="center" bgcolor="#0078d4" style="border-radius:4px;">
                        <a href="%s"
                           target="_blank"
                           style="display:inline-block; padding:12px 22px; font-family:Segoe UI, Arial, sans-serif; font-size:14px; font-weight:bold; color:#ffffff; text-decoration:none; border-radius:4px;">
                          Evrağı Görüntüle
                        </a>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>

              <tr>
                <td style="font-family:Segoe UI, Arial, sans-serif; font-size:12px; color:#9aa0a6; text-align:center; padding-top:28px;">
                  Bu bağlantılara erişemiyorsanız, sistem yöneticinizle iletişime geçiniz.
                </td>
              </tr>
            </table>
          </td>
        </tr>

        <tr>
          <td style="background-color:#f7f9fc; padding:18px 32px; border-top:1px solid #e3e8ef;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
              <tr>
                <td style="font-family:Segoe UI, Arial, sans-serif; font-size:11px; color:#9aa0a6;">
                  Bu otomatik bir bilgilendirme e-postasıdır, lütfen yanıtlamayınız.
                </td>
              </tr>
            </table>
          </td>
        </tr>

      </table>
    </td>
  </tr>
</table>
""".formatted(recipientName, recordId.toString(), title, status, reason, quickApproveUrl, deepLink);

            helper.setFrom("hello@demomailtrap.co");
            helper.setTo(toEmail);
            helper.setSubject("EBYS - Evrak Durum Değişikliği Bildirimi [#" + recordId.toString().substring(0, 8) + "]");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("E-posta başarıyla gönderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("E-posta gönderilirken hata oluştu! Alıcı: {}, Hata: {}", toEmail, e.getMessage());
        }
    }
}