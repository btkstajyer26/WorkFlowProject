package btk.staj.WorkFlowProject.department.repository;

import btk.staj.WorkFlowProject.department.entity.DepartmentMemberEntity;
import btk.staj.WorkFlowProject.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DepartmentMemberRepository extends JpaRepository<DepartmentMemberEntity, DepartmentMemberEntity.Id> {

    // Bir departmanin butun uyeleri (aktif/pasif ayrimi yapmaz).
    List<DepartmentMemberEntity> findAllByIdDepartmentId(Integer departmentId);

    // Bir kullanicinin uye oldugu butun departmanlar - "Kayitlarim" gibi
    // kisisel gorunum sorgulari icin.
    List<DepartmentMemberEntity> findAllByIdUserId(UUID userId);

    boolean existsByIdDepartmentIdAndIdUserId(Integer departmentId, UUID userId);

    // department_members'in kendisinde is_active yok - uyelik ikili.
    // Aktiflik users.is_active'ten gelir; WF-6 resolver'inin ihtiyaci
    // olan "bu departmanin AKTIF uyeleri" sorgusu bu yuzden cross-entity
    // JPQL join gerektirir.
    @Query("SELECT u FROM DepartmentMemberEntity dm, User u " +
           "WHERE dm.id.departmentId = :departmentId AND u.id = dm.id.userId AND u.active = true")
    List<User> findActiveUsersByDepartmentId(@Param("departmentId") Integer departmentId);
}