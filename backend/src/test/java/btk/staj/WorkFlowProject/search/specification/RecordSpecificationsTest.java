package btk.staj.WorkFlowProject.search.specification;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecordSpecificationsTest {

    @Test
    void withFilters_shouldCreateSpecification_whenQIsProvided() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        criteria.setQ("Sorting");

        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "ADMIN");

        // Assert
        assertNotNull(specification);
    }

    @Test
    void withFilters_shouldCreateSpecification_whenStatusIsProvided() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        criteria.setStatus(RecordStatus.TASLAK);

        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "ADMIN");

        // Assert
        assertNotNull(specification);
    }
    @Test
    void withFilters_shouldCreateSpecification_whenCategoryIdIsProvided() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        criteria.setCategoryId(5);

        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "ADMIN");

        // Assert
        assertNotNull(specification);
    }
    @Test
    void withFilters_shouldCreateSpecification_whenDateRangeIsProvided() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();

        criteria.setFrom(
                LocalDateTime.of(2026, 8, 12, 10, 0));

        criteria.setTo(
                LocalDateTime.of(2026, 8, 12, 11, 0));

        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "ADMIN");

        // Assert
        assertNotNull(specification);
    }
    @Test
    void withFilters_shouldCreateSpecification_whenSoftDeleteIsApplied() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();

        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "ADMIN");

        // Assert
        assertNotNull(specification);
    }
    @Test
    void withFilters_shouldCreateSpecification_forCalisanRole() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "CALISAN");

        // Assert
        assertNotNull(specification);
    }
    @Test
    void withFilters_shouldCreateSpecification_forBaskanYardimcisiRole() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "BASKAN_YARDIMCISI");

        // Assert
        assertNotNull(specification);
    }
    @Test
    void withFilters_shouldCreateSpecification_forBaskanRole() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "BASKAN");

        // Assert
        assertNotNull(specification);
    }
    @Test
    void withFilters_shouldCreateSpecification_forAdminRole() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "ADMIN");

        // Assert
        assertNotNull(specification);
    }
    @Test
    void withFilters_shouldCreateSpecification_forUnknownRole() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "UNKNOWN_ROLE");

        // Assert
        assertNotNull(specification);
    }
    @Test
    void withFilters_shouldCreateSpecification_whenCreatorIsProvided() {

        // Arrange
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        criteria.setCreator("ahmet");

        UUID userId = UUID.randomUUID();

        // Act
        Specification<Record> specification =
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "ADMIN");

        // Assert
        assertNotNull(specification);
    }
}