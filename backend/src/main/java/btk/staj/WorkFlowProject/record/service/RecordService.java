package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.record.dto.*;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Kayitlarin icerik yonetimi (CRUD).
 *
 * <p>Durum gecisleri bu arayuze ait degildir. Onay akisi
 * {@code POST /api/records/{recordId}/workflow/actions} ucundan, durum
 * makinesini calistiran {@code WorkflowActionService} uzerinden yurur; boylece
 * gecis kurallari, zorunlu aciklama ve denetim izi tek yoldan gecer.
 */
public interface RecordService {

    RecordResponse createRecord(RecordCreateRequest request);

    RecordResponse getRecordById(UUID id);

    Page<RecordResponse> getFilteredRecords(RecordStatus status, Integer categoryId, String keyword, Pageable pageable);

    RecordResponse updateRecord(UUID id, RecordUpdateRequest request);

    void deleteRecord(UUID id);
}
