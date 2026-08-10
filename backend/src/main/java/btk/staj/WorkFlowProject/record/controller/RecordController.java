package btk.staj.WorkFlowProject.record.controller;

import btk.staj.WorkFlowProject.record.dto.RecordCreateRequest;
import btk.staj.WorkFlowProject.record.dto.RecordResponse;
import btk.staj.WorkFlowProject.record.dto.RecordUpdateRequest;
import btk.staj.WorkFlowProject.record.service.RecordService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * Yeni Kayıt (Taslak) Oluşturma — Sadece Çalışan.
     */
    @PreAuthorize("hasRole('CALISAN')")
    @PostMapping
    public ResponseEntity<RecordResponse> createRecord(@Valid @RequestBody RecordCreateRequest request) {
        RecordResponse response = recordService.createRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ID'ye göre tekil kayıt getirme.
     * Görünürlük kontrolü (kendi kaydı / atanan / onay aşamasında) service katmanında yapılır.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecordResponse> getRecordById(@PathVariable UUID id) {
        return ResponseEntity.ok(recordService.getRecordById(id));
    }

    /**
     * Arama ve Filtreleme Modülü (Sayfalamalı) — şartname 4.4.
     */
    @GetMapping
    public ResponseEntity<Page<RecordResponse>> getAllRecords(
            @RequestParam(required = false) RecordStatus status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        Page<RecordResponse> response = recordService.getFilteredRecords(status, categoryId, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Taslak veya Düzenleme Bekliyor durumundaki kaydı düzenleme.
     * Yalnızca kaydı oluşturan Çalışan yapabilir (isEditableByCreator() kontrolü service'te).
     */
    @PreAuthorize("hasRole('CALISAN')")
    @PutMapping("/{id}")
    public ResponseEntity<RecordResponse> updateRecord(
            @PathVariable UUID id,
            @Valid @RequestBody RecordUpdateRequest request) {

        return ResponseEntity.ok(recordService.updateRecord(id, request));
    }

    /**
     * Taslak halindeki kaydı silme (soft delete — deletedAt set edilir).
     */
    @PreAuthorize("hasRole('CALISAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable UUID id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}