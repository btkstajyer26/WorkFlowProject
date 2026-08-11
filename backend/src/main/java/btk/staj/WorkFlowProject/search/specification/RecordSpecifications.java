package btk.staj.WorkFlowProject.search.specification;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RecordSpecifications {

        private RecordSpecifications() {
        }

        public static Specification<Record> withFilters(
                        RecordSearchCriteria criteria,
                        UUID currentUserId,
                        String currentUserRole) {

                return (root, query, criteriaBuilder) -> {

                        List<Predicate> predicates = new ArrayList<>();

                        // =========================
                        // YETKİ FİLTRESİ (RBAC)
                        // =========================

                        if (currentUserId != null && currentUserRole != null) {

                                switch (currentUserRole.toUpperCase()) {

                                        // ÇALIŞAN
                                        // Sadece kendi oluşturduğu kayıtları görebilir.
                                        case "CALISAN":
                                                predicates.add(
                                                                criteriaBuilder.equal(
                                                                                root.get("createdBy"),
                                                                                currentUserId));
                                                break;

                                        // BAŞKAN YARDIMCISI
                                        // Sadece kendisine atanan/gelen kayıtları görebilir.
                                        case "BASKAN_YARDIMCISI":
                                                predicates.add(
                                                                criteriaBuilder.equal(
                                                                                root.get("assignedTo"),
                                                                                currentUserId));
                                                break;

                                        // BAŞKAN
                                        // Onay aşamasına gelen kayıtları görebilir.
                                        case "BASKAN":
                                                predicates.add(
                                                                criteriaBuilder.equal(
                                                                                root.get("status"),
                                                                                RecordStatus.BASKAN_INCELEMESINDE));
                                                break;

                                        // ADMIN
                                        // Tüm kayıtları görebilir.
                                        case "ADMIN":
                                                break;

                                        // Tanımsız rol:
                                        // Güvenli tarafta kal ve hiçbir kayıt gösterme.
                                        default:
                                                predicates.add(
                                                                criteriaBuilder.disjunction());
                                                break;
                                }

                        } else {

                                // Kullanıcı veya rol bilgisi yoksa
                                // hiçbir kayıt gösterme.
                                predicates.add(
                                                criteriaBuilder.disjunction());
                        }

                        // =========================
                        // NORMAL ARAMA FİLTRELERİ
                        // =========================

                        // Başlık / açıklama içinde metin araması
                        if (criteria.getText() != null
                                        && !criteria.getText().isBlank()) {

                                String text = "%" + criteria.getText().toLowerCase() + "%";

                                Predicate titlePredicate = criteriaBuilder.like(
                                                criteriaBuilder.lower(root.get("title")),
                                                text);

                                Predicate descriptionPredicate = criteriaBuilder.like(
                                                criteriaBuilder.lower(root.get("description")),
                                                text);

                                predicates.add(
                                                criteriaBuilder.or(
                                                                titlePredicate,
                                                                descriptionPredicate));
                        }

                        // Durum filtresi
                        if (criteria.getStatus() != null) {

                                predicates.add(
                                                criteriaBuilder.equal(
                                                                root.get("status"),
                                                                criteria.getStatus()));
                        }

                        // Kategori filtresi
                        if (criteria.getCategoryId() != null) {

                                predicates.add(
                                                criteriaBuilder.equal(
                                                                root.get("categoryId"),
                                                                criteria.getCategoryId()));
                        }

                        // Oluşturan kullanıcı filtresi
                        if (criteria.getUserId() != null) {

                                predicates.add(
                                                criteriaBuilder.equal(
                                                                root.get("createdBy"),
                                                                criteria.getUserId()));
                        }

                        // Başlangıç tarihi
                        if (criteria.getStartDate() != null) {

                                predicates.add(
                                                criteriaBuilder.greaterThanOrEqualTo(
                                                                root.get("createdAt"),
                                                                criteria.getStartDate()));
                        }

                        // Bitiş tarihi
                        if (criteria.getEndDate() != null) {

                                predicates.add(
                                                criteriaBuilder.lessThanOrEqualTo(
                                                                root.get("createdAt"),
                                                                criteria.getEndDate()));
                        }

                        // =========================
                        // SOFT DELETE
                        // =========================

                        predicates.add(
                                        criteriaBuilder.isNull(
                                                        root.get("deletedAt")));

                        // Tüm koşullar AND ile birleşir.
                        return criteriaBuilder.and(
                                        predicates.toArray(new Predicate[0]));
                };
        }
}