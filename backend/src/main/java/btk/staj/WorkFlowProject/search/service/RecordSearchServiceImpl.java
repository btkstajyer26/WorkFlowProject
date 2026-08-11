package btk.staj.WorkFlowProject.search.service;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.dto.RecordSearchResponse;
import btk.staj.WorkFlowProject.search.specification.RecordSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RecordSearchServiceImpl implements RecordSearchService {

    private final RecordRepository recordRepository;

    public RecordSearchServiceImpl(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Override
    public PagedResponse<RecordSearchResponse> search(
            RecordSearchCriteria criteria,
            Pageable pageable) {

        // =========================
        // SECURITY CONTEXT
        // =========================

        SecurityContext context = SecurityContextHolder.getContext();

        Authentication authentication = context.getAuthentication();

        // Mevcut kullanıcı bilgilerini al
        UUID currentUserId = extractUserId(authentication);

        String currentUserRole = extractUserRole(authentication);

        // =========================
        // SEARCH
        // =========================

        Page<Record> recordPage = recordRepository.findAll(
                RecordSpecifications.withFilters(
                        criteria,
                        currentUserId,
                        currentUserRole),
                pageable);

        // =========================
        // RESPONSE
        // =========================

        return new PagedResponse<>(
                recordPage.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),

                recordPage.getNumber(),
                recordPage.getSize(),
                recordPage.getTotalElements(),
                recordPage.getTotalPages());
    }

    /**
     * Authentication içerisinden kullanıcı ID'sini alır.
     * Auth sistemi henüz tamamen entegre değilse,
     * mevcut RecordServiceImpl'deki geçici kullanıcı ID'si kullanılır.
     */
    private UUID extractUserId(Authentication authentication) {

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() != null
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            try {
                return UUID.fromString(
                        authentication.getName());
            } catch (Exception ignored) {
                // Authentication henüz UUID dönmüyorsa
                // geçici kullanıcıya düşülür.
            }
        }

        return UUID.fromString(
                "11111111-1111-1111-1111-111111111111");
    }

    /**
     * Authentication içerisinden kullanıcının rolünü alır.
     */
    private String extractUserRole(Authentication authentication) {

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities() != null
                && !authentication.getAuthorities().isEmpty()) {

            return authentication.getAuthorities()
                    .iterator()
                    .next()
                    .getAuthority()
                    .replace("ROLE_", "");
        }

        // Auth sistemi tamamlanana kadar
        // mevcut test kullanıcısını çalışan kabul ediyoruz.
        return "CALISAN";
    }

    /**
     * Record Entity -> Search Response DTO
     */
    private RecordSearchResponse toResponse(Record record) {

        RecordSearchResponse response = new RecordSearchResponse();

        response.setId(record.getId());
        response.setTitle(record.getTitle());
        response.setDescription(record.getDescription());
        response.setCategoryId(record.getCategoryId());
        response.setStatus(record.getStatus());
        response.setCreatedBy(record.getCreatedBy());
        response.setAssignedTo(record.getAssignedTo());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());

        return response;
    }
}