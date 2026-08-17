package btk.staj.WorkFlowProject.search.service;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.dto.RecordSearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.UUID;
class RecordSearchServiceImplTest {

    @Test
    void search_shouldPassPageableToRepository() {

        // Arrange
        RecordRepository recordRepository =
                mock(RecordRepository.class);

        RecordSearchServiceImpl service =
                new RecordSearchServiceImpl(recordRepository);

        RecordSearchCriteria criteria =
                new RecordSearchCriteria();

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"));

        Page<Record> emptyPage =
                new PageImpl<>(
                        Collections.emptyList(),
                        pageable,
                        0);

        when(recordRepository.findAll(
                any(Specification.class),
                eq(pageable)))
                .thenReturn(emptyPage);

        // Act
        PagedResponse<RecordSearchResponse> response =
                service.search(criteria, pageable);

        // Assert
        assertNotNull(response);

        verify(recordRepository, times(1))
                .findAll(
                        any(Specification.class),
                        eq(pageable));
    }
    @Test
    void search_shouldPassAscendingSortAndPageableToRepository() {

        // Arrange
        RecordRepository recordRepository =
                mock(RecordRepository.class);

        RecordSearchServiceImpl service =
                new RecordSearchServiceImpl(recordRepository);

        RecordSearchCriteria criteria =
                new RecordSearchCriteria();

        Pageable pageable = PageRequest.of(
                1,
                5,
                Sort.by(
                        Sort.Direction.ASC,
                        "title"));

        Page<Record> emptyPage =
                new PageImpl<>(
                        Collections.emptyList(),
                        pageable,
                        0);

        when(recordRepository.findAll(
                any(Specification.class),
                eq(pageable)))
                .thenReturn(emptyPage);

        // Act
        PagedResponse<RecordSearchResponse> response =
                service.search(criteria, pageable);

        // Assert
        assertNotNull(response);

        verify(recordRepository, times(1))
                .findAll(
                        any(Specification.class),
                        eq(pageable));
    }
    @Test
    void search_shouldKeepCalisanVisibility_whenCreatorIsProvided() {

        // Arrange
        RecordRepository recordRepository =
                mock(RecordRepository.class);

        RecordSearchServiceImpl service =
                new RecordSearchServiceImpl(recordRepository);

        UUID currentUserId = UUID.randomUUID();

        RecordSearchCriteria criteria =
                new RecordSearchCriteria();

        criteria.setCreator("Ahmet");

        Pageable pageable =
                PageRequest.of(0, 10);

        Page<Record> emptyPage =
                new PageImpl<>(
                        Collections.emptyList(),
                        pageable,
                        0);

        when(recordRepository.findAll(
                any(Specification.class),
                eq(pageable)))
                .thenReturn(emptyPage);

        // Act
        PagedResponse<RecordSearchResponse> response =
                service.search(criteria, pageable);

        // Assert
        assertNotNull(response);

        verify(recordRepository, times(1))
                .findAll(
                        any(Specification.class),
                        eq(pageable));
    }
}