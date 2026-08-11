package btk.staj.WorkFlowProject.search.specification;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kapsam kurali sartnamenin §2 "Kayit Gorunurlugu Kapsami" satiri; yanlis
 * uygulanirsa kullanici baskasinin evragini gorur. Bu yuzden hangi kolona
 * bakildigi burada dogrulanir.
 */
@DisplayName("Arama gorunurluk kapsami")
class RecordSpecificationsTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000060");

    private final Root<Record> root = mock(Root.class, RETURNS_DEEP_STUBS);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

    @Test
    @DisplayName("Calisan yalnizca kendi olusturduklarini gorur")
    void anEmployeeIsScopedToTheirOwnRecords() {
        Path<Object> createdBy = pathFor("createdBy");

        build(RoleName.CALISAN);

        verify(cb).equal(createdBy, USER_ID);
    }

    @Test
    @DisplayName("Bsk. Yrd. yalnizca kendisine atananlari gorur")
    void aDeputyIsScopedToTheirAssignments() {
        Path<Object> assignedTo = pathFor("assignedTo");

        build(RoleName.BASKAN_YARDIMCISI);

        verify(cb).equal(assignedTo, USER_ID);
    }

    @Test
    @DisplayName("Baskan onay asamasindakileri ve kendisine atananlari gorur")
    void aPresidentSeesRecordsAwaitingApprovalOrAssignedToThem() {
        Path<Object> status = pathFor("status");
        Path<Object> assignedTo = pathFor("assignedTo");

        build(RoleName.BASKAN);

        verify(cb).equal(status, RecordStatus.BASKAN_INCELEMESINDE);
        verify(cb).equal(assignedTo, USER_ID);
        verify(cb).or(any(Predicate.class), any(Predicate.class));
    }

    @Test
    @DisplayName("Admin evrak goremez")
    void anAdminSeesNothing() {
        build(RoleName.ADMIN);

        verify(cb).disjunction();
        verify(cb, never()).equal(any(Path.class), any(UUID.class));
    }

    @Test
    @DisplayName("silinmis kayitlar hicbir rolde donmez")
    void softDeletedRecordsAreAlwaysExcluded() {
        Path<Object> deletedAt = pathFor("deletedAt");

        build(RoleName.CALISAN);

        verify(cb).isNull(deletedAt);
    }

    @Test
    @DisplayName("kullanici veya rol verilmeden olusturulamaz")
    void refusesToBuildWithoutAnActor() {
        RecordSearchCriteria criteria = new RecordSearchCriteria();

        assertThatNullPointerException().isThrownBy(
                () -> RecordSpecifications.withFilters(criteria, null, RoleName.CALISAN));
        assertThatNullPointerException().isThrownBy(
                () -> RecordSpecifications.withFilters(criteria, USER_ID, null));
    }

    @SuppressWarnings("unchecked")
    private Path<Object> pathFor(String attribute) {
        Path<Object> path = mock(Path.class);
        when(root.get(attribute)).thenReturn(path);
        return path;
    }

    private void build(RoleName role) {
        Specification<Record> specification =
                RecordSpecifications.withFilters(new RecordSearchCriteria(), USER_ID, role);
        specification.toPredicate(root, query, cb);
    }
}
