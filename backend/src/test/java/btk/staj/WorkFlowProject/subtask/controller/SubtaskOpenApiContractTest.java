package btk.staj.WorkFlowProject.subtask.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubtaskOpenApiContractTest {

    private final JsonNode document = readOpenApi();

    @Test
    void documentsAllFourControllerEndpoints() {
        JsonNode paths = document.path("paths");

        assertThat(paths.path("/api/records/{recordId}/subtasks/split").has("post")).isTrue();
        assertThat(paths.path("/api/records/{recordId}/subtasks").has("get")).isTrue();
        assertThat(paths.path("/api/records/{recordId}/subtasks/assignable-users").has("get")).isTrue();
        assertThat(paths.path("/api/subtasks/{subtaskId}/actions").has("post")).isTrue();
    }

    @Test
    void assignableUsersViewContainsExactlyTheExpectedFields() {
        JsonNode properties = schema("SubtaskAssignableUserView").path("properties");

        assertThat(fieldNames(properties)).containsExactlyInAnyOrder("id", "fullName");
    }

    @Test
    void subtaskViewContainsExactlyThePublicChildFields() {
        JsonNode properties = schema("SubtaskView").path("properties");

        assertThat(fieldNames(properties)).containsExactlyInAnyOrder(
                "id",
                "parentRecordId",
                "title",
                "description",
                "assignedTo",
                "assignedToName",
                "status",
                "resolutionComment",
                "createdAt",
                "completedAt");
        assertThat(properties.has("approvalPolicy")).isFalse();
        assertThat(properties.has("requiredApprovals")).isFalse();
    }

    @Test
    void listContractKeepsNullablePolicyFieldsAtTheTopLevel() {
        JsonNode properties = schema("SubtaskListResponse").path("properties");

        assertThat(fieldNames(properties))
                .containsExactlyInAnyOrder("approvalPolicy", "requiredApprovals", "subtasks");
        assertThat(properties.path("approvalPolicy").path("oneOf").toString()).contains("null");
        assertThat(properties.path("requiredApprovals").path("oneOf").toString()).contains("null");
        assertThat(properties.path("subtasks").path("items").path("$ref").asText())
                .isEqualTo("#/components/schemas/SubtaskView");
    }

    private JsonNode schema(String name) {
        return document.path("components").path("schemas").path(name);
    }

    private static Set<String> fieldNames(JsonNode node) {
        return StreamSupport.stream(
                        ((Iterable<String>) () -> node.fieldNames()).spliterator(),
                        false)
                .collect(Collectors.toSet());
    }

    private static JsonNode readOpenApi() {
        Path workingDirectory = Path.of("").toAbsolutePath();
        Path path = workingDirectory.resolve("../docs/openapi.json").normalize();
        if (!Files.exists(path)) {
            path = workingDirectory.resolve("docs/openapi.json");
        }
        try {
            return new ObjectMapper().readTree(path.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("docs/openapi.json okunamadı: " + path, exception);
        }
    }
}
