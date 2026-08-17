package btk.staj.WorkFlowProject.search.specification;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
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

            if (criteria.getQ() != null && !criteria.getQ().isBlank()) {
                String text = "%" + criteria.getQ().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), text),
                        cb.like(cb.lower(root.get("description")), text)));
            }

            if (criteria.getCreator() != null && !criteria.getCreator().isBlank()) {
                predicates.add(createdByNameLike(root, query, cb, criteria.getCreator()));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), criteria.getCategoryId()));
            }

            if (criteria.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getFrom()));
            }

            if (criteria.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getTo()));
            }

            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Kaydi olusturan kullanicinin adina/soyadina gore filtre (sozlesme §5
     * {@code creator} parametresi).
     *
     * <p>{@code records.created_by} yalnizca UUID tutuyor, {@code users} ile
     * tanimli bir JPA iliskisi yok; bu yuzden esleme korele bir {@code EXISTS}
     * alt sorgusuyla yapilir. Alt sorgu yalnizca daraltir, gorunurluk kapsamini
     * gevsetmez.
     */
    private static Predicate createdByNameLike(
            jakarta.persistence.criteria.Root<Record> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String creator) {

        String pattern = "%" + creator.toLowerCase() + "%";

        Subquery<UUID> subquery = query.subquery(UUID.class);
        var user = subquery.from(User.class);

        Predicate nameMatches = cb.or(
                cb.like(cb.lower(user.get("firstName")), pattern),
                cb.like(cb.lower(user.get("lastName")), pattern),
                // "Ahmet Yilmaz" gibi tam ad aramasi da calissin.
                cb.like(cb.lower(cb.concat(cb.concat(user.get("firstName"), " "), user.get("lastName"))), pattern));

        subquery.select(user.get("id"))
                .where(nameMatches, cb.equal(user.get("id"), root.get("createdBy")));

        return cb.exists(subquery);
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

            // Bsk. Yrd. kendisine atanan kayitlari VE düzeltme bekleyen kayıtları gorur (Salt Okunur).
            case BASKAN_YARDIMCISI -> cb.or(
                    cb.equal(root.get("assignedTo"), currentUserId),
                    cb.equal(root.get("status"), RecordStatus.DUZENLEME_BEKLIYOR));

            // Baskan onay asamasina gelenleri ve kendisine atananlari gorur.
            case BASKAN -> cb.or(
                    cb.equal(root.get("status"), RecordStatus.BASKAN_INCELEMESINDE),
                    cb.equal(root.get("assignedTo"), currentUserId));

            // ADMIN yalnizca kullanici ve rol yonetiminden sorumludur; evrak goremez.
            case ADMIN -> cb.disjunction();
        };
    }
}