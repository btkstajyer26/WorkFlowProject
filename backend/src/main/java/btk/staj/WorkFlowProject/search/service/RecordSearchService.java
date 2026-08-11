package btk.staj.WorkFlowProject.search.service;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.dto.RecordSearchResponse;
import org.springframework.data.domain.Pageable;

public interface RecordSearchService {

    PagedResponse<RecordSearchResponse> search(
            RecordSearchCriteria criteria,
            Pageable pageable);
}