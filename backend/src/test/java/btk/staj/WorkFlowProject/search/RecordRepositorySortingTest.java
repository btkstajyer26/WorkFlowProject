package btk.staj.WorkFlowProject.search;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.specification.RecordSpecifications;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class RecordRepositorySortingTest {

    @Autowired
    private RecordRepository recordRepository;

    @Test
    void shouldSortRecordsByTitleAscending() {

        // Arrange
        UUID userId = UUID.randomUUID();
        String testPrefix = "SORT_TEST_" + UUID.randomUUID();

        Record record1 = Record.builder()
                .title(testPrefix + "_B")
                .description("Sorting test B")
                .categoryId(5)
                .status(RecordStatus.TASLAK)
                .createdBy(userId)
                .build();

        Record record2 = Record.builder()
                .title(testPrefix + "_A")
                .description("Sorting test A")
                .categoryId(5)
                .status(RecordStatus.TASLAK)
                .createdBy(userId)
                .build();

        recordRepository.save(record1);
        recordRepository.save(record2);
        recordRepository.flush();

        RecordSearchCriteria criteria =
                new RecordSearchCriteria();

        criteria.setQ(testPrefix);

        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.ASC,
                        "title"));

        // Act
        Page<Record> result = recordRepository.findAll(
                RecordSpecifications.withFilters(
                        criteria,
                        userId,
                        "ADMIN"),
                pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals(
                testPrefix + "_A",
                result.getContent().get(0).getTitle());
        assertEquals(
                testPrefix + "_B",
                result.getContent().get(1).getTitle());

        // Cleanup
        recordRepository.deleteAll(result.getContent());
        recordRepository.flush();
    }
}