package btk.staj.WorkFlowProject.user.specification;

import btk.staj.WorkFlowProject.user.dto.AdminUserSearchCriteria;
import btk.staj.WorkFlowProject.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> withFilters(AdminUserSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getQ() != null && !criteria.getQ().isBlank()) {
                String text = "%" + criteria.getQ().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), text),
                        cb.like(cb.lower(root.get("lastName")), text),
                        cb.like(cb.lower(root.get("email")), text)));
            }

            if (criteria.getRole() != null && !criteria.getRole().isBlank()) {
                predicates.add(cb.equal(root.get("role").get("name"), criteria.getRole()));
            }

            if (criteria.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.getActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}