package btk.staj.WorkFlowProject.user.dto;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Kullanici yaniti")
class UserResponseTest {

    // Spring Boot'un yapilandirdigi mapper gibi JSR-310 modulunu kaydeder;
    // aksi halde LocalDateTime alanlari serilestirilemez.
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private User ornekKullanici() {
        Role role = new Role();
        role.setId(1);
        role.setName("CALISAN");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Ahmet");
        user.setLastName("Yılmaz");
        user.setEmail("ahmet@example.com");
        user.setPasswordHash("$2a$10$COKGIZLIBCRYPTOZETI0123456789abcdefghijklmnopqrstuv");
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    @DisplayName("sifre ozetini disari sizdirmaz")
    void sifreOzetiSizmaz() throws Exception {
        String json = objectMapper.writeValueAsString(UserResponse.from(ornekKullanici()));

        assertThat(json).doesNotContain("passwordHash", "COKGIZLI", "$2a$10$");
    }

    @Test
    @DisplayName("gerekli alanlari tasir ve rol adini duzlestirir")
    void gerekliAlanlariTasir() {
        User user = ornekKullanici();

        UserResponse response = UserResponse.from(user);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.firstName()).isEqualTo("Ahmet");
        assertThat(response.lastName()).isEqualTo("Yılmaz");
        assertThat(response.email()).isEqualTo("ahmet@example.com");
        assertThat(response.roleName()).isEqualTo("CALISAN");
        assertThat(response.createdAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    @DisplayName("rolu olmayan kullanicida hata vermez")
    void rolsuzKullanicidaHataVermez() {
        User user = ornekKullanici();
        user.setRole(null);

        assertThat(UserResponse.from(user).roleName()).isNull();
    }

    @Test
    @DisplayName("User entity'si dogrudan serilestirilse bile sifre ozeti yazilmaz")
    void entityDogrudanSerilestirilseDeSizmaz() throws Exception {
        String json = objectMapper.writeValueAsString(ornekKullanici());

        assertThat(json).doesNotContain("passwordHash", "COKGIZLI", "$2a$10$");
        assertThat(json).contains("ahmet@example.com");
    }
}
