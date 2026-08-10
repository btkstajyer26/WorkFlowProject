package btk.staj.WorkFlowProject.user.repository;

import btk.staj.WorkFlowProject.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    List<User> findByRole_NameAndActive(String roleName, boolean active);
}