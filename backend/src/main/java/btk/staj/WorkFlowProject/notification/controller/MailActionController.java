package btk.staj.WorkFlowProject.notification.controller;

import btk.staj.WorkFlowProject.notification.dto.MailActionPreview;
import btk.staj.WorkFlowProject.notification.dto.MailActionTokenRequest;
import btk.staj.WorkFlowProject.notification.service.MailActionTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * E-posta bildirimindeki tek tikla aksiyon baglantisinin arkasindaki iki uc.
 *
 * <p><strong>Oturum gerektirmez.</strong> Kimlik, istekte tasinan tek
 * kullanimlik anahtardan gelir; anahtar bir evraga, bir aksiyona ve bir kisiye
 * baglidir, sureli ve tek kullanimliktir.
 *
 * <p>Iki tasarim karari bilincli:
 *
 * <ul>
 *   <li><strong>Ikisi de {@code POST}.</strong> Onizleme de {@code POST}'tur
 *       cunku anahtar govdede tasinir; URL'ye yazilsaydi erisim log'larina,
 *       {@code Referer} basligina ve tarayici gecmisine duserdi. Proje bu
 *       karari parola sifirlama akisinda zaten vermisti.</li>
 *   <li><strong>Durum degistiren uc {@code GET} degildir.</strong> Posta ag
 *       gecitleri ve baglanti tarayicilari (ornegin Outlook Safe Links)
 *       baglantilari kendiliginden getirir. Durum degisikligi {@code GET}
 *       ile tetiklenseydi, alici dugmeye hic dokunmadan evrak onaylanabilirdi.
 *       {@link #preview} guvenle onceden getirilebilir, {@link #consume} ise
 *       yalnizca kullanicinin acik onayiyla cagrilir.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/public/mail-actions")
@Tag(name = "Mail Actions", description = "E-posta bildirimindeki tek tikla aksiyon baglantisi")
public class MailActionController {

    private final MailActionTokenService mailActionTokenService;

    public MailActionController(MailActionTokenService mailActionTokenService) {
        this.mailActionTokenService = Objects.requireNonNull(
                mailActionTokenService, "mailActionTokenService");
    }

    /**
     * Anahtari dogrular ve onay ekraninin ihtiyaci olan bilgiyi dondurur.
     * Hicbir sey degistirmez; guvenle tekrarlanabilir.
     */
    @PostMapping("/preview")
    @Operation(summary = "Baglantiyi dogrula ve onay ekrani bilgisini getir (durum degistirmez)")
    public MailActionPreview preview(@Valid @RequestBody MailActionTokenRequest request) {
        return mailActionTokenService.preview(request.token());
    }

    /**
     * Anahtari tuketir ve aksiyonu yurutur.
     *
     * <p>Basari ve basarisizlik ayirt edilebilir doner: anahtar
     * kullanilamazsa {@code 400 INVALID_OR_EXPIRED_MAIL_ACTION_TOKEN}, evrak
     * arada ilerlediyse durum makinesinin kendi {@code WORKFLOW_*} kodu gelir.
     * Hata yutulmaz.
     */
    @PostMapping("/consume")
    @Operation(summary = "Baglantiyi tuket ve workflow aksiyonunu yurut")
    public ResponseEntity<Map<String, UUID>> consume(@Valid @RequestBody MailActionTokenRequest request) {
        UUID recordId = mailActionTokenService.consume(request.token());
        return ResponseEntity.ok(Map.of("recordId", recordId));
    }
}
