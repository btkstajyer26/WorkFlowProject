package btk.staj.WorkFlowProject.record.controller;

import btk.staj.WorkFlowProject.record.dto.RecordNoteRequest;
import btk.staj.WorkFlowProject.record.dto.RecordNoteResponse;
import btk.staj.WorkFlowProject.record.service.RecordNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordNoteController {

    private final RecordNoteService recordNoteService;

    // Kendi notunu getirme ucu
    @GetMapping("/{id}/notes/me")
    public ResponseEntity<RecordNoteResponse> getMyNote(@PathVariable("id") UUID recordId) {
        RecordNoteResponse response = recordNoteService.getMyNote(recordId);
        return ResponseEntity.ok(response);
    }

    // Kendi notunu güncelleme ucu
    @PutMapping("/{id}/notes/me")
    public ResponseEntity<RecordNoteResponse> updateMyNote(
            @PathVariable("id") UUID recordId,
            @Valid @RequestBody RecordNoteRequest request) { // @Valid: 1000 karakter ve boş olmama kuralını tetikler
        
        RecordNoteResponse response = recordNoteService.updateMyNote(recordId, request);
        return ResponseEntity.ok(response);
    }
}