package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.auth.security.CurrentVisibilityActorProvider;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.department.repository.DepartmentRepository;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.dto.AvailableActionView;
import btk.staj.WorkFlowProject.workflow.dto.AvailableActionsResponse;
import btk.staj.WorkFlowProject.workflow.dto.TargetDepartmentView;
import btk.staj.WorkFlowProject.workflow.dto.TargetDepartmentsResponse;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowActionEntity;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.port.WorkflowRecordPort;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowActionRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Workflow'un okuma uclari (APP-9): kullanilabilir aksiyonlar ve hedef departman kesfi.
 *
 * <p>Yazma yapan {@link WorkflowActionService}'in okuma tarafindaki esidir. Kural hesabi
 * saf {@link AvailableActionResolver}'da durur; buradaki is gorunurluk sinirini uygulamak
 * ve cekirdegin bilmedigi katalog/ad bilgilerini (aksiyon gosterim adi, departman adi)
 * yanita eklemektir.
 *
 * <p>Gorunurluk {@code GET /api/records/&#123;id&#125;} ile <strong>birebir aynidir</strong>
 * (SS1.4): once bulunamayan/silinmis kayit 404, sonra kapsam disi aktor 403. Sira onemli —
 * gorunurluk kontrolunu one almak, silinmis bir kaydin varligini dogrulayan bir kanal acardi.
 */
@Service
@Transactional(readOnly = true)
public class WorkflowQueryService {

    private final RecordRepository recordRepository;
    private final RecordAccessPolicy recordAccessPolicy;
    private final CurrentVisibilityActorProvider visibilityActorProvider;
    private final CurrentActorProvider currentActorProvider;
    private final WorkflowRecordPort recordPort;
    private final AvailableActionResolver availableActionResolver;
    private final DepartmentRoutingResolver departmentRoutingResolver;
    private final TransitionRuleSource ruleSource;
    private final WorkflowActionRepository workflowActionRepository;
    private final DepartmentRepository departmentRepository;

    public WorkflowQueryService(RecordRepository recordRepository,
                                RecordAccessPolicy recordAccessPolicy,
                                CurrentVisibilityActorProvider visibilityActorProvider,
                                CurrentActorProvider currentActorProvider,
                                WorkflowRecordPort recordPort,
                                AvailableActionResolver availableActionResolver,
                                DepartmentRoutingResolver departmentRoutingResolver,
                                TransitionRuleSource ruleSource,
                                WorkflowActionRepository workflowActionRepository,
                                DepartmentRepository departmentRepository) {
        this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository");
        this.recordAccessPolicy = Objects.requireNonNull(recordAccessPolicy, "recordAccessPolicy");
        this.visibilityActorProvider = Objects.requireNonNull(visibilityActorProvider, "visibilityActorProvider");
        this.currentActorProvider = Objects.requireNonNull(currentActorProvider, "currentActorProvider");
        this.recordPort = Objects.requireNonNull(recordPort, "recordPort");
        this.availableActionResolver = Objects.requireNonNull(availableActionResolver, "availableActionResolver");
        this.departmentRoutingResolver =
                Objects.requireNonNull(departmentRoutingResolver, "departmentRoutingResolver");
        this.ruleSource = Objects.requireNonNull(ruleSource, "ruleSource");
        this.workflowActionRepository =
                Objects.requireNonNull(workflowActionRepository, "workflowActionRepository");
        this.departmentRepository = Objects.requireNonNull(departmentRepository, "departmentRepository");
    }

    public AvailableActionsResponse availableActions(UUID recordId) {
        WorkflowRecordSnapshot record = visibleRecord(recordId);
        TransitionRuleSource snapshot = ruleSource.snapshot();

        List<WorkflowAction> actions = availableActionResolver.resolve(
                currentActorProvider.currentActor(), record, snapshot);

        Map<String, String> displayNames = actions.isEmpty() ? Map.of() : displayNames();

        return new AvailableActionsResponse(
                record.id(),
                record.status(),
                record.version(),
                actions.stream()
                        .map(action -> new AvailableActionView(
                                action,
                                displayNames.getOrDefault(action.name(), action.name()),
                                action.isCommentRequired(),
                                action.isTargetUserIdRequiredInRequest(),
                                action.isTargetDepartmentIdRequiredInRequest()))
                        .toList());
    }

    public TargetDepartmentsResponse targetDepartments(UUID recordId) {
        WorkflowRecordSnapshot record = visibleRecord(recordId);
        TransitionRuleSource snapshot = ruleSource.snapshot();
        CurrentActor actor = currentActorProvider.currentActor();

        // Uc yalniz DEPARTMANA_GONDER kullanilabilirken anlamlidir (SS2.4). Aktorun bu
        // durumda boyle bir kurali yoksa cevap bos listedir, hata degil.
        Optional<TransitionRule> departmentRule = snapshot.find(
                record.status(), WorkflowAction.DEPARTMANA_GONDER, actor.roleId());
        if (departmentRule.isEmpty()) {
            return new TargetDepartmentsResponse(List.of());
        }

        // Iniş durumu kuralin hedefidir; gonderim dogrulamasi da (validateTarget) ayni
        // durumu kullanir, boylece liste ile gercek gonderim ayni sonucu verir.
        Set<Integer> usable = departmentRoutingResolver.usableTargetDepartments(
                departmentRule.get().to(), record.createdBy(), snapshot);
        if (usable.isEmpty()) {
            return new TargetDepartmentsResponse(List.of());
        }

        return new TargetDepartmentsResponse(departmentRepository.findAllById(usable).stream()
                .map(department -> new TargetDepartmentView(department.getId(), department.getName()))
                .sorted(Comparator.comparing(TargetDepartmentView::name))
                .toList());
    }

    /**
     * Kaydi gorunurluk sinirinden gecirip workflow cekirdeginin anladigi snapshot'a cevirir.
     *
     * @throws ResourceNotFoundException kayit yoksa veya silinmisse (404)
     */
    private WorkflowRecordSnapshot visibleRecord(UUID recordId) {
        Objects.requireNonNull(recordId, "recordId");

        Record record = recordRepository.findById(recordId)
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Kayıt bulunamadı: " + recordId));

        VisibilityActor actor = visibilityActorProvider.currentVisibilityActor();
        recordAccessPolicy.assertCanView(actor, record);

        return recordPort.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Kayıt bulunamadı: " + recordId));
    }

    private Map<String, String> displayNames() {
        return workflowActionRepository.findAllByOrderByIdAsc().stream()
                .filter(entity -> entity.getDisplayName() != null)
                .collect(Collectors.toMap(WorkflowActionEntity::getName,
                        WorkflowActionEntity::getDisplayName, (first, second) -> first));
    }
}
