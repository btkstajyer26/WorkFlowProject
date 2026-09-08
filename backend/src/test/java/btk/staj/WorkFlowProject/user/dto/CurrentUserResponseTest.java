package btk.staj.WorkFlowProject.user.dto;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Oturum kullanicisi yaniti")
class CurrentUserResponseTest {

    @Test
    @DisplayName("rol kimligi gosterim adi ve aktif yetkileri birlikte tasir")
    void rolKimligiVeYetkileriTasir() {
        Role role = new Role();
        role.setId(1);
        role.setName("Uzman Personel");
        role.setSystemKey("CALISAN");
        role.setActive(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Ahmet");
        user.setLastName("Yılmaz");
        user.setEmail("ahmet@example.com");
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user,
                Set.of("RECORD_CREATE", "RECORD_EDIT"));

        CurrentUserResponse response = CurrentUserResponse.from(authenticatedUser);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.roleId()).isEqualTo(1);
        assertThat(response.systemKey()).isEqualTo("CALISAN");
        assertThat(response.roleName()).isEqualTo("Uzman Personel");
        assertThat(response.permissionCodes())
                .containsExactlyInAnyOrder("RECORD_CREATE", "RECORD_EDIT");
    }

    @Test
    @DisplayName("dinamik rol sistem anahtari olmadan yetkilerini tasir")
    void dinamikRolYetkileriniTasir() {
        Role role = new Role();
        role.setId(9);
        role.setName("Hukuk Uzmanı");
        role.setSystemKey(null);
        role.setActive(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Ayşe");
        user.setLastName("Yılmaz");
        user.setEmail("ayse@example.com");
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user,
                Set.of("RECORD_CREATE"));

        CurrentUserResponse response = CurrentUserResponse.from(authenticatedUser);

        assertThat(response.roleId()).isEqualTo(9);
        assertThat(response.systemKey()).isNull();
        assertThat(response.roleName()).isEqualTo("Hukuk Uzmanı");
        assertThat(response.permissionCodes()).containsExactly("RECORD_CREATE");
    }
}
