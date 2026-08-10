package btk.staj.WorkFlowProject.attachment.repository;

import btk.staj.WorkFlowProject.attachment.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {
}