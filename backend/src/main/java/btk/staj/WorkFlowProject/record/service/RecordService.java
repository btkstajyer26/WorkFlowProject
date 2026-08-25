package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.record.dto.*;

import java.util.UUID;

/**
 * Kayitlarin icerik yonetimi (CRUD).
 *
 * <p>Listeleme bu arayuzde yer almaz: filtreleme ve gorunurluk kapsami
 * {@code RecordSearchService} tarafindan saglanir (bkz. karar 2.2 — tek
 * gorunurluk kurali, tek yer). {@code RecordController} listeleme icin
 * dogrudan {@code RecordSearchService}'i cagirir.
 *
 * <p>Durum gecisleri de bu arayuze ait degildir. Onay akisi
 * {@code POST /api/records/{recordId}/workflow/actions} ucundan, durum
 * makinesini calistiran {@code WorkflowActionService} uzerinden yurur; boylece
 * gecis kurallari, zorunlu aciklama ve denetim izi tek yoldan gecer.
 */
public interface RecordService {

    RecordResponse createRecord(RecordCreateRequest request);

    RecordResponse getRecordById(UUID id);

    RecordResponse updateRecord(UUID id, RecordUpdateRequest request);

    void deleteRecord(UUID id);
}