package btk.staj.WorkFlowProject.record.controller;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.record.dto.RecordCreateRequest;
import btk.staj.WorkFlowProject.record.dto.RecordResponse;
import btk.staj.WorkFlowProject.record.dto.RecordUpdateRequest;
import btk.staj.WorkFlowProject.record.service.RecordService;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.dto.RecordSearchResponse;
import btk.staj.WorkFlowProject.search.service.RecordSearchService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;
    private final RecordSearchService recordSearchService;

    public RecordController(RecordService recordService, RecordSearchService recordSearchService) {
        this.recordService = recordService;
        this.recordSearchService = recordSearchService;
    }

    /**
     * Yeni Kayıt (Taslak) Oluşturma — Sadece Çalışan.
     */
    @PreAuthorize("hasAuthority('RECORD_CREATE')")
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
     * Kayıt listeleme — şartname §5: GET /api/records?page&size&status&categoryId&q&from&to&creator&sort
     *
     * <p>Uç burada ({@code RecordController}) kalır, ancak filtreleme ve görünürlük
     * kapsamı mantığı {@code RecordSearchService}'ten gelir. Aynı erişim kuralının
     * iki ayrı yerde uygulanmasını (ve birinin unutulmasını) önlemek için tekil
     * kaynak olarak search modülü kullanılır; bu controller kendi predicate/filtre
     * mantığını tutmaz.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<RecordSearchResponse>> getAllRecords(
            @RequestParam(required = false) RecordStatus status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String creator,
            Pageable pageable) {

        RecordSearchCriteria criteria = new RecordSearchCriteria();
        criteria.setStatus(status);
        criteria.setCategoryId(categoryId);
        criteria.setQ(q);
        criteria.setFrom(from);
        criteria.setTo(to);
        criteria.setCreator(creator);

        PagedResponse<RecordSearchResponse> response = recordSearchService.search(criteria, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Taslak veya Düzenleme Bekliyor durumundaki kaydı düzenleme.
     * Yalnızca kaydı oluşturan Çalışan yapabilir (isEditableByCreator() kontrolü service'te).
     */
    @PreAuthorize("hasAuthority('RECORD_EDIT')")
    @PutMapping("/{id}")
    public ResponseEntity<RecordResponse> updateRecord(
            @PathVariable UUID id,
            @Valid @RequestBody RecordUpdateRequest request) {

        return ResponseEntity.ok(recordService.updateRecord(id, request));
    }

    /**
     * Taslak halindeki kaydı silme (soft delete — deletedAt set edilir).
     */
    @PreAuthorize("hasAuthority('RECORD_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable UUID id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
