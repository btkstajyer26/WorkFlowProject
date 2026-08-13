package btk.staj.WorkFlowProject.record.service;

import btk.staj.WorkFlowProject.record.dto.RecordNoteRequest;
import btk.staj.WorkFlowProject.record.dto.RecordNoteResponse;
import btk.staj.WorkFlowProject.record.entity.RecordNote;
import btk.staj.WorkFlowProject.record.repository.RecordNoteRepository;
import btk.staj.WorkFlowProject.record.entity.Record; 
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordNoteServiceImpl implements RecordNoteService {

    private final RecordNoteRepository recordNoteRepository;
    private final RecordRepository recordRepository; 
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public RecordNoteResponse getMyNote(UUID recordId) {
        UUID currentUserId = getCurrentUserId();

        RecordNote note = recordNoteRepository.findByRecordIdAndAuthorId(recordId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Çalışma notu bulunamadı."));

        return mapToResponse(note);
    }

    @Override
    @Transactional
    public RecordNoteResponse updateMyNote(UUID recordId, RecordNoteRequest request) {
        UUID currentUserId = getCurrentUserId();

        // 1. Kaydın durumu ve yetki kontrollerini yap
        validateRecordStateAndAuthorization(recordId, currentUserId);

        // 2. Mevcut notu bul (Sözleşme Kuralı: PUT yeni satır üretmez)
        RecordNote existingNote = recordNoteRepository.findByRecordIdAndAuthorId(recordId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Güncellenecek çalışma notu bulunamadı. Önce not oluşturulmalıdır."));

        // 3. Verileri güncelle
        existingNote.setBody(request.getBody());
        existingNote.setVersion(request.getVersion());
        
        // 4. Kaydet ve yanıt dön
        RecordNote updatedNote = recordNoteRepository.save(existingNote);
        return mapToResponse(updatedNote);
    }

    // --- YARDIMCI METOTLAR ---

    private void validateRecordStateAndAuthorization(UUID recordId, UUID currentUserId) {
        Record record = recordRepository.findById(recordId)
              .orElseThrow(() -> new RuntimeException("İlgili kayıt bulunamadı."));
        
        RecordStatus status = record.getStatus();

        // KURAL 1: İzin verilmeyen durumlarda not yönetilemez
        if (status == RecordStatus.ONAYLANDI || 
            status == RecordStatus.REDDEDILDI || 
            status == RecordStatus.TASLAK || 
            status == RecordStatus.DUZENLEME_BEKLIYOR) {
            
            throw new RuntimeException("Bu aşamada çalışma notu yönetilemez!"); 
        }

        // KURAL 2: Sadece mevcut aşamanın atanmış kullanıcısı not yazabilir
        // Statüye göre o anki yetkili 'assignedTo' alanından kontrol ediliyor.
        if (status == RecordStatus.BSK_YRD_INCELEMESINDE && !currentUserId.equals(record.getAssignedTo())) {
            throw new RuntimeException("Bu kayda not yazma yetkiniz yok! Yalnızca atanan Başkan Yardımcısı not yazabilir.");
        }
        
        if (status == RecordStatus.BASKAN_INCELEMESINDE && !currentUserId.equals(record.getAssignedTo())) {
            throw new RuntimeException("Bu kayda not yazma yetkiniz yok! Yalnızca atanan Başkan not yazabilir.");
        }
    }

    private RecordNoteResponse mapToResponse(RecordNote note) {
        return RecordNoteResponse.builder()
                .id(note.getId())
                .recordId(note.getRecordId())
                .authorId(note.getAuthorId())
                .authorRoleId(note.getAuthorRoleId())
                .body(note.getBody())
                .version(note.getVersion())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

    private UUID getCurrentUserId() {
        // 1. Spring Security Context'ten o anki isteği atan kullanıcının kimliğini (Authentication) alıyoruz.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Yetkisiz işlem: Kullanıcı oturumu bulunamadı.");
        }

        // 2. Emaili kullanarak veritabanından kullanıcının tüm bilgilerine (ve dolayısıyla ID'sine) erişiyoruz.
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Güvenlik hatası: Oturum açan kullanıcı veritabanında bulunamadı."));
        
        return user.getId();
    }
}