package btk.staj.WorkFlowProject.attachment.repository;

import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    // Silinmemiş tek dosya getir
    Optional<FileEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Bir kayda ait silinmemiş tüm dosyalar
    List<FileEntity> findAllByRecordIdAndDeletedAtIsNull(UUID recordId);

    // Silinmişler dahil hepsi. Devir anındaki ek listesini kurmak için gerekli:
    // o an duran ama sonradan silinen dosya da listeye girmelidir.
    List<FileEntity> findAllByRecordId(UUID recordId);
}