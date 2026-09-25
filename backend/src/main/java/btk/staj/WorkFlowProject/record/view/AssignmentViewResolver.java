package btk.staj.WorkFlowProject.record.view;

import btk.staj.WorkFlowProject.common.dto.AssignmentView;
import btk.staj.WorkFlowProject.department.entity.DepartmentEntity;
import btk.staj.WorkFlowProject.department.repository.DepartmentRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kayit atamasini gosterime hazir hale getirir (B11).
 *
 * <p>{@link RecordContentView} ile ayni rolu oynar: kimlikten gosterime giden donusum tek
 * yerde durur, her okuma yolu ayni kurali tuketir. Silinmis kullanici veya departman icin
 * ad {@code null} kalir &mdash; {@code createdByFullName} ile ayni davranis; istemci
 * kimlige duser.
 *
 * <p>Liste yolu icin toplu ({@link #resolveAll}) girisi vardir: satir basina sorgu
 * acmak arama sayfasinda N+1 uretirdi.
 */
@Component
public class AssignmentViewResolver {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public AssignmentViewResolver(UserRepository userRepository, DepartmentRepository departmentRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.departmentRepository = Objects.requireNonNull(departmentRepository, "departmentRepository");
    }

    /** Tek kayit icin; detay ve aksiyon yollarinda kullanilir. */
    public AssignmentView resolve(UUID assignedTo, Integer assignedDepartmentId) {
        if (assignedTo != null) {
            return AssignmentView.user(assignedTo,
                    userRepository.findById(assignedTo).map(AssignmentViewResolver::fullName).orElse(null));
        }
        if (assignedDepartmentId != null) {
            return AssignmentView.department(assignedDepartmentId,
                    departmentRepository.findById(assignedDepartmentId)
                            .map(DepartmentEntity::getName).orElse(null));
        }
        return AssignmentView.none();
    }

    /**
     * Toplu ad cozumu; iki sorgu ile butun sayfayi karsilar.
     *
     * @return kimlikten ada esleme tasiyan, satir basina {@link Names#assignmentFor} ile
     *         sorgulanabilen bir gorunum
     */
    public Names resolveAll(Collection<UUID> userIds, Collection<Integer> departmentIds) {
        Set<UUID> users = new LinkedHashSet<>(Objects.requireNonNull(userIds, "userIds"));
        users.remove(null);
        Set<Integer> departments = new LinkedHashSet<>(Objects.requireNonNull(departmentIds, "departmentIds"));
        departments.remove(null);

        Map<UUID, String> userNames = users.isEmpty() ? Map.of()
                : userRepository.findAllById(users).stream()
                        .collect(Collectors.toMap(User::getId, AssignmentViewResolver::fullName));
        Map<Integer, String> departmentNames = departments.isEmpty() ? Map.of()
                : departmentRepository.findAllById(departments).stream()
                        .collect(Collectors.toMap(DepartmentEntity::getId, DepartmentEntity::getName));

        return new Names(userNames, departmentNames);
    }

    /** Onceden toplu cekilmis adlar; sorgu acmaz. */
    public record Names(Map<UUID, String> userNames, Map<Integer, String> departmentNames) {

        public AssignmentView assignmentFor(UUID assignedTo, Integer assignedDepartmentId) {
            if (assignedTo != null) {
                return AssignmentView.user(assignedTo, userNames.get(assignedTo));
            }
            if (assignedDepartmentId != null) {
                return AssignmentView.department(assignedDepartmentId, departmentNames.get(assignedDepartmentId));
            }
            return AssignmentView.none();
        }
    }

    private static String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
