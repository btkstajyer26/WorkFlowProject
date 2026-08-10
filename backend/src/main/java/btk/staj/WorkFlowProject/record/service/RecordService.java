package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.record.dto.*;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecordService {

    // --- CRUD İŞLEMLERİ ---
    RecordResponse createRecord(RecordCreateRequest request);
    
    RecordResponse getRecordById(UUID id);
    
    Page<RecordResponse> getFilteredRecords(RecordStatus status, Integer categoryId, String keyword, Pageable pageable);
    
    RecordResponse updateRecord(UUID id, RecordUpdateRequest request);
    
    void deleteRecord(UUID id);

    // --- WORKFLOW (DURUM GEÇİŞİ) AKSİYONLARI ---
    RecordResponse submitToDeputy(UUID id);
    
    RecordResponse forwardToChairman(UUID id, RecordActionRequest request);
    
    RecordResponse returnToEmployee(UUID id, RecordActionRequest request);
    
    RecordResponse returnByChairman(UUID id, RecordReturnRequest request);
    
    RecordResponse approve(UUID id, RecordActionRequest request);
    
    RecordResponse reject(UUID id, RecordActionRequest request);
    
    RecordResponse resubmit(UUID id);
}