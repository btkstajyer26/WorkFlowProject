package btk.staj.WorkFlowProject.subtask.dto;

import btk.staj.WorkFlowProject.subtask.model.SubtaskAction;
import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SubtaskDtoContractTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void subtaskViewContainsExactlyThePublicChildFields() throws Exception {
        assertThat(componentNames(SubtaskView.class)).containsExactly(
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

        SubtaskView view = new SubtaskView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "İnceleme",
                null,
                UUID.randomUUID(),
                "Ada Lovelace",
                SubtaskStatus.DEGERLENDIRME,
                null,
                null,
                null);
        String json = new ObjectMapper().writeValueAsString(view);

        assertThat(json)
                .contains("\"parentRecordId\"", "\"assignedToName\"", "\"resolutionComment\"")
                .doesNotContain("approvalPolicy", "requiredApprovals");
    }

    @Test
    void unsplitResponseUsesNullParentSettingsAndAnEmptyList() throws Exception {
        SubtaskListResponse response = SubtaskListResponse.unsplit();

        assertThat(response.approvalPolicy()).isNull();
        assertThat(response.requiredApprovals()).isNull();
        assertThat(response.subtasks()).isEmpty();
        assertThat(new ObjectMapper().writeValueAsString(response))
                .isEqualTo("{\"approvalPolicy\":null,\"requiredApprovals\":null,\"subtasks\":[]}");
    }

    @Test
    void parentSettingsExistOnlyAtTheTopLevelOfListAndSplitResponses() {
        assertThat(componentNames(SubtaskListResponse.class)).containsExactly(
                "approvalPolicy", "requiredApprovals", "subtasks");
        assertThat(componentNames(SubtaskSplitResponse.class)).containsExactly(
                "parentRecordId", "approvalPolicy", "requiredApprovals", "subtasks");
        assertThat(componentNames(SubtaskView.class))
                .doesNotContain("approvalPolicy", "requiredApprovals");
    }

    @Test
    void splitRequestRequiresPolicyTwoItemsAndValidNestedItems() {
        SubtaskSplitRequest valid = new SubtaskSplitRequest(
                SubtaskApprovalPolicy.UNANIMOUS,
                List.of(item("Birinci"), item("İkinci")));
        assertThat(validator.validate(valid)).isEmpty();

        assertThat(paths(validator.validate(new SubtaskSplitRequest(
                null,
                List.of(item("Birinci"))))))
                .contains("approvalPolicy", "subtasks");

        SubtaskSplitRequest invalidItem = new SubtaskSplitRequest(
                SubtaskApprovalPolicy.MAJORITY,
                List.of(new SubtaskCreateItem(" ", null, null), item("İkinci")));
        assertThat(paths(validator.validate(invalidItem)))
                .anyMatch(path -> path.endsWith("title"))
                .anyMatch(path -> path.endsWith("assignedTo"));
    }

    @Test
    void actionRequestCarriesOnlyActionAndOptionalBoundedComment() {
        assertThat(componentNames(SubtaskActionRequest.class))
                .containsExactly("action", "comment");
        assertThat(Arrays.asList(SubtaskAction.values())).containsExactly(
                SubtaskAction.DEGERLENDIRMEYI_TAMAMLA,
                SubtaskAction.ISLEMI_TAMAMLA,
                SubtaskAction.ONAYLA,
                SubtaskAction.REDDET);
        assertThat(validator.validate(new SubtaskActionRequest(SubtaskAction.ONAYLA, null)))
                .isEmpty();
        assertThat(paths(validator.validate(new SubtaskActionRequest(null, "a".repeat(2001)))))
                .containsExactlyInAnyOrder("action", "comment");
    }

    private static SubtaskCreateItem item(String title) {
        return new SubtaskCreateItem(title, null, UUID.randomUUID());
    }

    private static List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
    }

    private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
