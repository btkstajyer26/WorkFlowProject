package btk.staj.WorkFlowProject.search.specification;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class RecordSpecifications {

    private RecordSpecifications() {
    }

    /**
     * Arama kriterlerini, kullanicinin gorme yetkisiyle birlestirir.
     *
     * <p>Yetki kosulu her zaman eklenir ve kriterlerle AND'lenir; kullanici
     * filtreleri gevseterek kapsaminin disina cikamaz.
     */
    public static Specification<Record> withFilters(
            RecordSearchCriteria criteria,
            UUID currentUserId,
            RoleName currentUserRole) {

        Objects.requireNonNull(criteria, "criteria");
        Objects.requireNonNull(currentUserId, "currentUserId");
        Objects.requireNonNull(currentUserRole, "currentUserRole");

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(visibilityScope(root, cb, currentUserId, currentUserRole));

            if (criteria.getText() != null && !criteria.getText().isBlank()) {
                String text = "%" + criteria.getText().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), text),
                        cb.like(cb.lower(root.get("description")), text)));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), criteria.getCategoryId()));
            }

            if (criteria.getUserId() != null) {
                predicates.add(cb.equal(root.get("createdBy"), criteria.getUserId()));
            }

            if (criteria.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getStartDate()));
            }

            if (criteria.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getEndDate()));
            }

            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Sartnamedeki "Kayit Gorunurlugu Kapsami" (§2) kuralinin SQL karsiligi.
     *
     * <p>Tek kayit icin ayni kural
     * {@code btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy} icinde
     * duruyor. Ayni kuralin iki bicimi olmasinin sebebi teknik: orasi tek kayda
     * bakan bir boolean, burasi sorguya giren bir kosul. <strong>Biri
     * degisirse digeri de degismeli.</strong>
     */
    private static Predicate visibilityScope(
            jakarta.persistence.criteria.Root<Record> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            UUID currentUserId,
            RoleName role) {

        return switch (role) {
            // Calisan yalnizca kendi olusturdugu kayitlari gorur.
            case CALISAN -> cb.equal(root.get("createdBy"), currentUserId);

            // Bsk. Yrd. kendisine atanan kayitlari gorur.
            case BASKAN_YARDIMCISI -> cb.equal(root.get("assignedTo"), currentUserId);

            // Baskan onay asamasina gelenleri ve kendisine atananlari gorur.
            case BASKAN -> cb.or(
                    cb.equal(root.get("status"), RecordStatus.BASKAN_INCELEMESINDE),
                    cb.equal(root.get("assignedTo"), currentUserId));

            // ADMIN yalnizca kullanici ve rol yonetiminden sorumludur; evrak goremez.
            case ADMIN -> cb.disjunction();
        };
    }
}
