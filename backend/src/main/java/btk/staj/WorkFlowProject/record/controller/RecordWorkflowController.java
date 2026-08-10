package btk.staj.WorkFlowProject.record.controller;

import btk.staj.WorkFlowProject.record.dto.RecordActionRequest;
import btk.staj.WorkFlowProject.record.dto.RecordResponse;
import btk.staj.WorkFlowProject.record.dto.RecordReturnRequest;
import btk.staj.WorkFlowProject.record.service.RecordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// import org.springframework.security.access.prepost.PreAuthorize; // Auth paketi hazır olunca aç

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/records")
public class RecordWorkflowController {

    private final RecordService recordService;

    public RecordWorkflowController(RecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * Çalışan: TASLAK -> BSK_YRD_INCELEMESINDE
     */
    // @PreAuthorize("hasRole('CALISAN')")
    @PostMapping("/{id}/submit")
    public ResponseEntity<RecordResponse> submitToDeputy(@PathVariable UUID id) {
        return ResponseEntity.ok(recordService.submitToDeputy(id));
    }

    /**
     * Başkan Yardımcısı: BSK_YRD_INCELEMESINDE -> BASKAN_INCELEMESINDE
     */
    // @PreAuthorize("hasRole('BASKAN_YARDIMCISI')")
    @PostMapping("/{id}/forward")
    public ResponseEntity<RecordResponse> forwardToChairman(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RecordActionRequest request) {

        return ResponseEntity.ok(recordService.forwardToChairman(id, request));
    }

    /**
     * Başkan Yardımcısı: BSK_YRD_INCELEMESINDE -> DUZENLEME_BEKLIYOR (Çalışana geri).
     * Açıklama zorunludur — kontrol service katmanında yapılır.
     */
    // @PreAuthorize("hasRole('BASKAN_YARDIMCISI')")
    @PostMapping("/{id}/return-to-employee")
    public ResponseEntity<RecordResponse> returnToEmployeeByDeputy(
            @PathVariable UUID id,
            @Valid @RequestBody RecordActionRequest request) {

        return ResponseEntity.ok(recordService.returnToEmployee(id, request));
    }

    /**
     * Başkan: BASKAN_INCELEMESINDE -> DUZENLEME_BEKLIYOR ya da -> BSK_YRD_INCELEMESINDE.
     * Hedef seçimi RecordReturnRequest.target ile belirlenir. Açıklama zorunludur.
     */
    // @PreAuthorize("hasRole('BASKAN')")
    @PostMapping("/{id}/return-by-chairman")
    public ResponseEntity<RecordResponse> returnByChairman(
            @PathVariable UUID id,
            @Valid @RequestBody RecordReturnRequest request) {

        return ResponseEntity.ok(recordService.returnByChairman(id, request));
    }

    /**
     * Başkan: BASKAN_INCELEMESINDE -> ONAYLANDI (terminal durum).
     */
    // @PreAuthorize("hasRole('BASKAN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<RecordResponse> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) RecordActionRequest request) {

        return ResponseEntity.ok(recordService.approve(id, request));
    }

    /**
     * Başkan: BASKAN_INCELEMESINDE -> REDDEDILDI (terminal durum).
     */
    // @PreAuthorize("hasRole('BASKAN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<RecordResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RecordActionRequest request) {

        return ResponseEntity.ok(recordService.reject(id, request));
    }

    /**
     * Çalışan: DUZENLEME_BEKLIYOR durumundaki kaydı revize edip tekrar gönderme.
     * updateRecord ile içerik güncellenir, bu endpoint sadece durumu tekrar
     * BSK_YRD_INCELEMESINDE'ye çeker.
     */
    // @PreAuthorize("hasRole('CALISAN')")
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<RecordResponse> resubmit(@PathVariable UUID id) {
        return ResponseEntity.ok(recordService.resubmit(id));
    }
}