package btk.staj.WorkFlowProject.subtask.repository;

import btk.staj.WorkFlowProject.subtask.entity.Subtask;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.parser.PartTree;

import static org.assertj.core.api.Assertions.assertThat;

class SubtaskRepositoryContractTest {

    @Test
    void derivedRepositoryMethodsResolveAgainstTheSubtaskModel() {
        assertThat(new PartTree("findOneById", Subtask.class))
                .isNotNull();
        assertThat(new PartTree(
                "findAllByParentRecord_IdOrderByCreatedAtAscIdAsc", Subtask.class))
                .isNotNull();
        assertThat(new PartTree(
                "existsByParentRecord_IdAndStatusNotIn", Subtask.class))
                .isNotNull();
        assertThat(new PartTree("countByParentRecord_Id", Subtask.class))
                .isNotNull();
        assertThat(new PartTree(
                "findAllByAssignedTo_IdOrderByCreatedAtDescIdDesc", Subtask.class))
                .isNotNull();
    }

    @Test
    void listQueriesDeclareTheAssociationsNeededByTheirConsumers() throws Exception {
        Method actionLookup = SubtaskRepository.class.getMethod(
                "findOneById", java.util.UUID.class);
        Method parentList = SubtaskRepository.class.getMethod(
                "findAllByParentRecord_IdOrderByCreatedAtAscIdAsc", java.util.UUID.class);
        Method assigneeList = SubtaskRepository.class.getMethod(
                "findAllByAssignedTo_IdOrderByCreatedAtDescIdDesc", java.util.UUID.class);

        assertThat(actionLookup.getAnnotation(EntityGraph.class).attributePaths())
                .containsExactly("parentRecord", "assignedTo", "assignedTo.role");
        assertThat(parentList.getAnnotation(EntityGraph.class).attributePaths())
                .containsExactly("assignedTo");
        assertThat(assigneeList.getAnnotation(EntityGraph.class).attributePaths())
                .containsExactly("parentRecord");
    }

    @Test
    void statusCountIsAGroupedProjectionRatherThanAnEntityLoad() throws Exception {
        Method method = SubtaskRepository.class.getMethod(
                "countStatusesByParentRecordId", java.util.UUID.class);
        String query = method.getAnnotation(Query.class).value();

        assertThat(query)
                .contains("SubtaskStatusCount")
                .contains("COUNT(s)")
                .contains("GROUP BY s.status");
        assertThat(method.getGenericReturnType().getTypeName())
                .contains("java.util.List")
                .contains("SubtaskStatusCount");
    }

    @Test
    void terminalStatusSetContainsOnlyCompletedAndRejected() {
        assertThat(SubtaskStatus.terminalStatuses())
                .containsExactlyInAnyOrder(SubtaskStatus.TAMAMLANDI, SubtaskStatus.REDDEDILDI);
        assertThat(Arrays.stream(SubtaskStatus.values())
                .filter(SubtaskStatus::isTerminal)
                .toList())
                .containsExactlyInAnyOrder(SubtaskStatus.TAMAMLANDI, SubtaskStatus.REDDEDILDI);
    }

    @Test
    void groupedCountProjectionRejectsInvalidValues() {
        assertThat(new SubtaskStatusCount(SubtaskStatus.TAMAMLANDI, 2))
                .isEqualTo(new SubtaskStatusCount(SubtaskStatus.TAMAMLANDI, 2));
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new SubtaskStatusCount(SubtaskStatus.REDDEDILDI, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
