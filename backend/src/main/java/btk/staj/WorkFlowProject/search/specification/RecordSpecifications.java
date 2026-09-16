package btk.staj.WorkFlowProject.search.specification;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.rbac.visibility.RecordVisibilityScope;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
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
            RecordVisibilityScope scope) {

        Objects.requireNonNull(criteria, "criteria");
        Objects.requireNonNull(scope, "scope");

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(visibilityScope(root, cb, scope));

            // B06: q ve kategori filtresi, RecordContentView.visibleContent'in
            // gosterdigi surumle (canli ya da dondurulmus) ayni satirda calismali.
            // Aksi halde aktor goremedigi canli icerikte sonuc bulabilir, ya da
            // gordugu dondurulmus icerikte sonucu kacirabilir.
            //
            // seesFrozenContent yalniz gercekten gerektiginde (q veya kategori
            // filtresi varken) cagrilir - kosulsuz cagrilirsa visibilityScope'un
            // zaten kurdugu esitlik kontrolleriyle (orn. status = DUZENLEME_BEKLIYOR)
            // cakisip gereksiz yere tekrarlanir.
            boolean needsContentAwarePredicate =
                    (criteria.getQ() != null && !criteria.getQ().isBlank())
                            || criteria.getCategoryId() != null;
            Predicate seesFrozen = needsContentAwarePredicate
                    ? seesFrozenContent(root, cb, scope)
                    : null;

            if (criteria.getQ() != null && !criteria.getQ().isBlank()) {
                String text = "%" + criteria.getQ().toLowerCase() + "%";

                Predicate matchesLiveContent = cb.or(
                        cb.like(cb.lower(root.get("title")), text),
                        cb.like(cb.lower(root.get("description")), text));
                Predicate matchesFrozenContent = cb.or(
                        cb.like(cb.lower(root.get("snapshotTitle")), text),
                        cb.like(cb.lower(root.get("snapshotDescription")), text));

                predicates.add(cb.or(
                        cb.and(seesFrozen.not(), matchesLiveContent),
                        cb.and(seesFrozen, matchesFrozenContent)));
            }

            if (criteria.getCreator() != null && !criteria.getCreator().isBlank()) {
                predicates.add(createdByNameLike(root, query, cb, criteria.getCreator()));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getCategoryId() != null) {
                Predicate matchesLiveCategory = cb.equal(root.get("categoryId"), criteria.getCategoryId());
                Predicate matchesFrozenCategory = cb.equal(root.get("snapshotCategoryId"), criteria.getCategoryId());

                predicates.add(cb.or(
                        cb.and(seesFrozen.not(), matchesLiveCategory),
                        cb.and(seesFrozen, matchesFrozenCategory)));
            }

            if (criteria.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getFrom()));
            }

            if (criteria.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getTo()));
            }


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
     * Bir satirin, aktore GORUNEN icerigin dondurulmus (snapshot) surumu mu
     * yoksa canli surumu mu oldugunu belirleyen kosul.
     *
     * <p>RecordAccessPolicy.seesRecordAsOfHandoff + RecordContentView.
     * visibleContent'in mekanik SQL cevirisidir - ikisi kayarsa arama/kategori
     * filtresi kullanicinin gormedigi icerikte sonuc uretir ya da gordugu
     * icerikte sonuc kacirir (B06).
     *
     * <p>forwardedByActor: yerlesik Baskan Yardimcisi HER duzeltmedeki kaydi,
     * herhangi bir aktor (yerlesik ya da dinamik) ise yalniz KENDI ilettigi
     * kaydi dondurulmus gorur. scope.statuses()'in DUZENLEME_BEKLIYOR
     * icermesi, aktorun yerlesik Bsk. Yrd. oldugunun zaten RecordVisibilityScope
     * .forActor tarafindan kodlanmis SQL karsiligidir - ayri bir rol kontrolu
     * gerekmez (bkz. RecordVisibilityScope).
     *
     * <p>assignedTo NULL ise "aktor mevcut atanan degil" kosulu true olmalidir
     * (Objects.equals(actorId, null) her zaman false dondurur); bu yuzden
     * esitsizlik NULL kontroluyle birlikte yaziliyor.
     *
     * <p>snapshotAt NULL ise RecordContentView canliya duser (V9 sonrasi
     * pratikte olusmaz, ama sozlesme burada da korunur).
     */
    private static Predicate seesFrozenContent(
            jakarta.persistence.criteria.Root<Record> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            RecordVisibilityScope scope) {

        boolean builtInDeputy = scope.statuses().contains(RecordStatus.DUZENLEME_BEKLIYOR);

        Predicate forwardedByActor = builtInDeputy
                ? cb.conjunction()
                : cb.equal(root.get("lastDeputyId"), scope.actorId());

        Predicate notCurrentAssignee = cb.or(
                cb.isNull(root.get("assignedTo")),
                cb.notEqual(root.get("assignedTo"), scope.actorId()));

        return cb.and(
                forwardedByActor,
                cb.equal(root.get("status"), RecordStatus.DUZENLEME_BEKLIYOR),
                notCurrentAssignee,
                cb.isNotNull(root.get("snapshotAt")));
    }

    /** Mechanical SQL translation of the shared scope; no role-specific rules here. */
    private static Predicate visibilityScope(
            jakarta.persistence.criteria.Root<Record> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            RecordVisibilityScope scope) {
        List<Predicate> alternatives = new ArrayList<>();
        for (var relation : scope.relations()) {
            String attribute = switch (relation) {
                case CREATOR -> "createdBy";
                case ASSIGNEE -> "assignedTo";
                case PREVIOUS_DEPUTY -> "lastDeputyId";
            };
            alternatives.add(cb.equal(root.get(attribute), scope.actorId()));
        }
        for (var status : scope.statuses()) alternatives.add(cb.equal(root.get("status"), status));
        for (var pair : scope.departmentScopes()) {
            alternatives.add(cb.and(cb.equal(root.get("assignedDepartmentId"), pair.departmentId()),
                    cb.equal(root.get("status"), pair.status())));
        }
        Predicate allowed = alternatives.isEmpty() ? cb.disjunction()
                : cb.or(alternatives.toArray(new Predicate[0]));
        return cb.and(cb.isNull(root.get("deletedAt")), allowed);
    }
}