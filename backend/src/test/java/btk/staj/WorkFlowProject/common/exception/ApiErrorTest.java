package btk.staj.WorkFlowProject.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiError")
class ApiErrorTest {

    @Test
    @DisplayName("4 parametreli constructor tum alanlari dogru doldurur, fieldErrors null kalir")
    void fourArgConstructor_setsFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.now();

        ApiError error = new ApiError("NOT_FOUND", "Kayıt bulunamadı", 404, now);

        assertThat(error.getCode()).isEqualTo("NOT_FOUND");
        assertThat(error.getMessage()).isEqualTo("Kayıt bulunamadı");
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getTimestamp()).isEqualTo(now);
        assertThat(error.getFieldErrors()).isNull();
    }

    @Test
    @DisplayName("5 parametreli constructor fieldErrors listesini de doldurur")
    void fiveArgConstructor_setsFieldErrorsToo() {
        LocalDateTime now = LocalDateTime.now();
        List<ApiError.FieldError> fieldErrors = List.of(
                new ApiError.FieldError("email", "Email boş olamaz"));

        ApiError error = new ApiError("VALIDATION_ERROR", "Girilen veriler geçersiz", 400, now, fieldErrors);

        assertThat(error.getFieldErrors()).hasSize(1);
        assertThat(error.getFieldErrors().get(0).getField()).isEqualTo("email");
        assertThat(error.getFieldErrors().get(0).getMessage()).isEqualTo("Email boş olamaz");
    }

    @Test
    @DisplayName("Setter'lar alanlari gunceller")
    void setters_updateFields() {
        ApiError error = new ApiError("OLD", "eski mesaj", 500, LocalDateTime.now());

        error.setCode("NEW");
        error.setMessage("yeni mesaj");
        error.setStatus(200);

        assertThat(error.getCode()).isEqualTo("NEW");
        assertThat(error.getMessage()).isEqualTo("yeni mesaj");
        assertThat(error.getStatus()).isEqualTo(200);
    }
}