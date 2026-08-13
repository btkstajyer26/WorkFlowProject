package btk.staj.WorkFlowProject.record.repository;

import btk.staj.WorkFlowProject.record.entity.RecordNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordNoteRepository extends JpaRepository<RecordNote, UUID> {
    
    Optional<RecordNote> findByRecordIdAndAuthorId(UUID recordId, UUID authorId);
    // olmama ihtimaline karşın optional döndürüldü

    void deleteByRecordIdAndAuthorId(UUID recordId, UUID authorId);
}