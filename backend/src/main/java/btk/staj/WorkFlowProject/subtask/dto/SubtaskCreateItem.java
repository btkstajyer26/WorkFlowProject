package btk.staj.WorkFlowProject.subtask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** One requested child in a Parent split operation. */
public record SubtaskCreateItem(
        @NotBlank
        @Size(max = 255)
        String title,
        String description,
        @NotNull UUID assignedTo) {
}
