package btk.staj.WorkFlowProject.search.specification;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
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

                                        case "CALISAN":
                                                predicates.add(
                                                        criteriaBuilder.equal(
                                                                root.get("createdBy"),
                                                                currentUserId));
                                                break;

                                        case "BASKAN_YARDIMCISI":
                                                predicates.add(
                                                        criteriaBuilder.equal(
                                                                root.get("assignedTo"),
                                                                currentUserId));
                                                break;

                                        case "BASKAN":
                                                predicates.add(
                                                        criteriaBuilder.equal(
                                                                root.get("status"),
                                                                RecordStatus.BASKAN_INCELEMESINDE));
                                                break;

                                        case "ADMIN":
                                                break;

                                        default:
                                                predicates.add(
                                                        criteriaBuilder.disjunction());
                                                break;
                                }

                        } else {

                                predicates.add(
                                        criteriaBuilder.disjunction());
                        }

                        // =========================
                        // NORMAL ARAMA FİLTRELERİ
                        // =========================

                        // Başlık / açıklama içinde metin araması
                        if (criteria.getQ() != null
                                && !criteria.getQ().isBlank()) {

                                String text =
                                        "%" + criteria.getQ().toLowerCase() + "%";

                                Predicate titlePredicate =
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(
                                                        root.get("title")),
                                                text);

                                Predicate descriptionPredicate =
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(
                                                        root.get("description")),
                                                text);

                                predicates.add(
                                        criteriaBuilder.or(
                                                titlePredicate,
                                                descriptionPredicate));
                        }

                        // Oluşturan kullanıcı adına / soyadına göre filtre
                        if (criteria.getCreator() != null
                                && !criteria.getCreator().isBlank()) {

                                String creator =
                                        "%" + criteria.getCreator().toLowerCase() + "%";

                                Subquery<UUID> subquery =
                                        query.subquery(UUID.class);

                                var userRoot = subquery.from(User.class);

                                Predicate firstNamePredicate =
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(
                                                        userRoot.get("firstName")),
                                                creator);

                                Predicate lastNamePredicate =
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(
                                                        userRoot.get("lastName")),
                                                creator);

                                subquery.select(
                                                userRoot.get("id"))
                                        .where(
                                                criteriaBuilder.or(
                                                        firstNamePredicate,
                                                        lastNamePredicate),
                                                criteriaBuilder.equal(
                                                        userRoot.get("id"),
                                                        root.get("createdBy"))
                                        );

                                predicates.add(
                                        criteriaBuilder.exists(subquery));
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

                        // Başlangıç tarihi
                        if (criteria.getFrom() != null) {

                                predicates.add(
                                        criteriaBuilder.greaterThanOrEqualTo(
                                                root.get("createdAt"),
                                                criteria.getFrom()));
                        }

                        // Bitiş tarihi
                        if (criteria.getTo() != null) {

                                predicates.add(
                                        criteriaBuilder.lessThanOrEqualTo(
                                                root.get("createdAt"),
                                                criteria.getTo()));
                        }

                        // =========================
                        // SOFT DELETE
                        // =========================

                        predicates.add(
                                criteriaBuilder.isNull(
                                        root.get("deletedAt")));

                        return criteriaBuilder.and(
                                predicates.toArray(new Predicate[0]));
                };
        }
}