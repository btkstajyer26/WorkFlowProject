package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.workflow.model.TargetResolution.DataIntegrityReason;
import btk.staj.WorkFlowProject.workflow.model.TargetResolution;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.port.WorkflowUserPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.TargetStrategy;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the workflow target dictated by the transition's strategy without applying
 * transition validation. Role and active-state checks deliberately remain the state
 * machine's responsibility.
 *
 * <p>Hangi stratejinin uygulanacagi <strong>aksiyondan degil gecisten</strong> gelir:
 * {@code workflow_transitions.target_strategy} kolonu, {@code TransitionRule} uzerinden
 * buraya tasinir. Boylece ayni aksiyon farkli gecislerde farkli hedefe gidebilir
 * (DB-1 SS6.5).
 *
 * <p>Hedefi her zaman backend cozer; istekten gelen {@code targetUserId} hicbir kolda
 * kullanilmaz. {@code ROLE} stratejisi sistemdeki tek aktif kullaniciyi bulur:
 * Calisanin aktif Baskan Yardimcisinin kimligini ogrenebilecegi guvenli bir uc yoktur
 * ve tekil rol karari geregi acilmayacaktir.</p>
 */
public final class TargetUserResolver {

    private final WorkflowUserPort userPort;

    public TargetUserResolver(WorkflowUserPort userPort) {
        this.userPort = Objects.requireNonNull(userPort, "userPort");
    }

    /**
     * @param strategy gecisin hedef cozum primitive'i (DB-1 SS7.2)
     * @param expectedTargetRoleId {@code ROLE} stratejisinde aranacak rol; diger
     *        stratejilerde bilgi amaclidir ve okunmaz
     * @param requestedTargetUserId istekten gelen hedef; <strong>bilerek yok
     *        sayilir</strong>. Hedefi her zaman backend cozdugu icin hicbir kol bu
     *        degeri okumaz. Parametre, istegin hedef tasiyip tasimadigini
     *        {@code WorkflowApplicationService}'in ayrica dogruladigini gizlememek
     *        ve imzayi bozmamak icin duruyor.
     */
    public TargetResolution resolve(
            TargetStrategy strategy,
            RoleId expectedTargetRoleId,
            UUID requestedTargetUserId,
            WorkflowRecordSnapshot record) {
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(record, "record");

        return switch (strategy) {
            case NONE -> new TargetResolution.NotProvided();
            case ROLE -> resolveSingleActiveRole(Objects.requireNonNull(
                    expectedTargetRoleId, "expectedTargetRoleId"));
            case CREATOR -> resolveCreatedBy(record.createdBy());
            case CURRENT_ASSIGNEE -> resolveCurrentAssignee(record.assignedTo());
            case PREVIOUS_ACTOR -> resolveLastDeputy(record.lastDeputyId());
            // Hedef bir kullanici degil departmandir; istekten okunur ve
            // WorkflowApplicationService dogrular (ADR-0006 S6). Bu resolver
            // yalniz kullanici cozer.
            case DEPARTMENT -> new TargetResolution.NotProvided();
        };
    }

    private TargetResolution resolveSingleActiveRole(RoleId roleId) {
        List<WorkflowUserSnapshot> activeUsers = Objects.requireNonNull(
                userPort.findActiveByRole(roleId),
                "userPort.findActiveByRole(roleId)");

        if (activeUsers.size() != 1) {
            return new TargetResolution.RoleNotConfigured(roleId, activeUsers.size());
        }
        return new TargetResolution.Resolved(activeUsers.getFirst());
    }

    private TargetResolution resolveCreatedBy(UUID createdBy) {
        Optional<WorkflowUserSnapshot> user = findById(createdBy);
        if (user.isEmpty()) {
            return new TargetResolution.DataIntegrityFailure(
                    DataIntegrityReason.CREATED_BY_USER_NOT_FOUND,
                    createdBy);
        }
        return new TargetResolution.Resolved(user.get());
    }

    /**
     * Hedef, gecis oncesindeki atanan kullanicidir.
     *
     * <p>Seed edilmis sekiz gecisin hicbiri bu stratejiyi kullanmiyor; yine de
     * uygulanmistir, cunku {@code chk_transition_target_strategy} bu degere izin verir ve
     * DB-1 SS7.2 anlamini kesin olarak tanimlar. Desteklenmemesi, gecerli bir veritabani
     * satirinin uygulamayi acilista dusurmesi anlamina gelirdi.
     */
    private TargetResolution resolveCurrentAssignee(UUID assignedTo) {
        if (assignedTo == null) {
            return new TargetResolution.DataIntegrityFailure(
                    DataIntegrityReason.CURRENT_ASSIGNEE_MISSING,
                    null);
        }

        Optional<WorkflowUserSnapshot> user = findById(assignedTo);
        if (user.isEmpty()) {
            return new TargetResolution.DataIntegrityFailure(
                    DataIntegrityReason.CURRENT_ASSIGNEE_USER_NOT_FOUND,
                    assignedTo);
        }
        return new TargetResolution.Resolved(user.get());
    }

    private TargetResolution resolveLastDeputy(UUID lastDeputyId) {
        if (lastDeputyId == null) {
            return new TargetResolution.DataIntegrityFailure(
                    DataIntegrityReason.LAST_DEPUTY_ID_MISSING,
                    null);
        }

        Optional<WorkflowUserSnapshot> user = findById(lastDeputyId);
        if (user.isEmpty()) {
            return new TargetResolution.DataIntegrityFailure(
                    DataIntegrityReason.LAST_DEPUTY_USER_NOT_FOUND,
                    lastDeputyId);
        }
        return new TargetResolution.Resolved(user.get());
    }

    private Optional<WorkflowUserSnapshot> findById(UUID userId) {
        return Objects.requireNonNull(userPort.findById(userId), "userPort.findById(userId)");
    }
}
