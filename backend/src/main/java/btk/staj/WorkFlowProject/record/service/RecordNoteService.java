package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.record.dto.RecordNoteRequest;
import btk.staj.WorkFlowProject.record.dto.RecordNoteResponse;
import java.util.UUID;

public interface RecordNoteService {
    RecordNoteResponse getMyNote(UUID recordId);
    RecordNoteResponse updateMyNote(UUID recordId, RecordNoteRequest request);
}