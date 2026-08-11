package btk.staj.WorkFlowProject.search.controller;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.dto.RecordSearchResponse;
import btk.staj.WorkFlowProject.search.service.RecordSearchService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records/search")
public class RecordSearchController {

    private final RecordSearchService recordSearchService;

    public RecordSearchController(RecordSearchService recordSearchService) {
        this.recordSearchService = recordSearchService;
    }

    @GetMapping
    public PagedResponse<RecordSearchResponse> search(
            RecordSearchCriteria criteria,
            Pageable pageable) {
        return recordSearchService.search(criteria, pageable);
    }
}