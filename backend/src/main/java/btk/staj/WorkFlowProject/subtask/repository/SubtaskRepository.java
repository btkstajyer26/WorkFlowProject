package btk.staj.WorkFlowProject.subtask.repository;

import btk.staj.WorkFlowProject.subtask.entity.Subtask;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubtaskRepository extends JpaRepository<Subtask, UUID> {

    /** Action path needs the assignee role for authorization and the parent for mapping. */
    @EntityGraph(attributePaths = {"parentRecord", "assignedTo", "assignedTo.role"})
    Optional<Subtask> findOneById(UUID id);

    /** Deterministic parent detail list; assignee names can be mapped without N+1 reads. */
    @EntityGraph(attributePaths = "assignedTo")
    List<Subtask> findAllByParentRecord_IdOrderByCreatedAtAscIdAsc(UUID parentRecordId);

    /** Efficient join guard: checks for unfinished siblings without loading their entities. */
    boolean existsByParentRecord_IdAndStatusNotIn(
            UUID parentRecordId,
            Collection<SubtaskStatus> terminalStatuses);

    default boolean hasNonTerminalSubtasks(UUID parentRecordId) {
        return existsByParentRecord_IdAndStatusNotIn(
                parentRecordId,
                SubtaskStatus.terminalStatuses());
    }

    /** Total count supports split-state checks without loading the parent's children. */
    long countByParentRecord_Id(UUID parentRecordId);

    /** Grants a Subtask assignee view access to the Parent their Subtask belongs to. */
    boolean existsByParentRecord_IdAndAssignedTo_Id(UUID parentRecordId, UUID assignedToId);

    /** One aggregate query supplies approval, rejection, and remaining-status counts. */
    @Query("""
            SELECT new btk.staj.WorkFlowProject.subtask.repository.SubtaskStatusCount(
                s.status, COUNT(s))
            FROM Subtask s
            WHERE s.parentRecord.id = :parentRecordId
            GROUP BY s.status
            """)
    List<SubtaskStatusCount> countStatusesByParentRecordId(
            @Param("parentRecordId") UUID parentRecordId);

    /** Basic assignee work-list lookup with parent data fetched in the same query. */
    @EntityGraph(attributePaths = "parentRecord")
    List<Subtask> findAllByAssignedTo_IdOrderByCreatedAtDescIdDesc(UUID assignedTo);
}
