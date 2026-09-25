package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.rbac.port.WorkflowRoleUsagePort;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowTransitionEntity;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowTransitionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * {@link WorkflowRoleUsagePort}'un workflow tarafindaki uygulamasi.
 *
 * <p>WF-8'in {@code unbind} korumasiyla ayni sorguyu tuketir
 * ({@code WorkflowTransitionRepository.hasOpenRecords}); tek fark, orada tek bir bag
 * icin sorulan sorunun burada rolun butun aktif baglari icin sorulmasidir. Iki yol
 * ayni cevabi vermek zorundadir: aksi halde Admin, WF-8'in reddettigi sonucu rol
 * ekranindan elde edebilir.
 *
 * <p>{@code Propagation.REQUIRED} (varsayilan) bilincli: cagiran
 * {@code RoleAdminService.update} rol satirini {@code findByIdForUpdate} ile kilitlemis
 * durumda ve bu kontrol ayni transaction icinde, ayni tutarli goruntude calismalidir.
 */
@Component
public class WorkflowRoleUsageAdapter implements WorkflowRoleUsagePort {

    private final WorkflowTransitionRepository transitions;

    public WorkflowRoleUsageAdapter(WorkflowTransitionRepository transitions) {
        this.transitions = Objects.requireNonNull(transitions, "transitions");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOpenWorkflowUsage(int roleId) {
        for (WorkflowTransitionEntity binding : transitions.findAllByActorRoleIdAndActiveTrue(roleId)) {
            if (transitions.hasOpenRecords(
                    binding.getFromStatusId(), roleId, binding.getActorRequirement().name())) {
                return true;
            }
        }
        return false;
    }
}
