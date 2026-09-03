package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.TargetStrategy;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Veritabani adapter'inin dondurdugu teknik gecis satirlarini mevcut domain
 * kurallarina map eden {@link TransitionRuleSource} implementasyonu.
 *
 * <p>Reader yalnizca constructor sirasinda cagirilir. Basarili yuklemenin
 * ardindan hem sirali kural listesi hem de arama index'i immutable snapshot
 * olarak tutulur; canli reload bu adapter'in sorumlulugu degildir.
 */
public final class DbTransitionRuleSource implements TransitionRuleSource {

    private final Map<RoleId, RoleName> legacyRoles;
    private final List<TransitionRule> snapshot;
    private final Map<Key, TransitionRule> index;

    public DbTransitionRuleSource(TransitionRuleRecordReader reader, Map<RoleId, RoleName> legacyRoles) {
        if (reader == null) {
            throw new TransitionRuleConfigurationException(
                    "TransitionRuleRecordReader must not be null");
        }

        this.legacyRoles = Map.copyOf(legacyRoles);
        List<TransitionRuleRecord> records = reader.findAllActive();
        if (records == null) {
            throw new TransitionRuleConfigurationException(
                    "TransitionRuleRecordReader.findAllActive() returned null");
        }
        if (records.isEmpty()) {
            throw new TransitionRuleConfigurationException(
                    "Transition configuration contains no active transition rules");
        }

        List<TransitionRule> rules = new ArrayList<>(records.size());
        Map<Key, TransitionRule> rulesByKey = new HashMap<>();

        for (int rowIndex = 0; rowIndex < records.size(); rowIndex++) {
            int rowNumber = rowIndex + 1;
            TransitionRuleRecord record = records.get(rowIndex);
            if (record == null) {
                throw new TransitionRuleConfigurationException(
                        "Transition configuration row " + rowNumber + " is null");
            }

            TransitionRule rule = map(record, rowNumber);
            Key key = new Key(rule.from(), rule.action(), rule.actorRole());
            TransitionRule previous = rulesByKey.putIfAbsent(key, rule);
            if (previous != null) {
                throw new TransitionRuleConfigurationException(
                        "Duplicate transition configuration for (fromStatus, action, actorRole)"
                                + " at row " + rowNumber + ": "
                                + rule.from() + ", " + rule.action() + ", " + rule.actorRole());
            }
            rules.add(rule);
        }

        this.snapshot = List.copyOf(rules);
        this.index = Map.copyOf(rulesByKey);
    }

    @Override
    public Optional<TransitionRule> find(
            RecordStatus from,
            WorkflowAction action,
            RoleName actorRole) {

        return Optional.ofNullable(index.get(new Key(from, action, actorRole)));
    }

    @Override
    public List<TransitionRule> all() {
        return snapshot;
    }

    private TransitionRule map(TransitionRuleRecord record, int rowNumber) {
        RoleId actorRoleId = mapRoleId(record.actorRoleId(), "actorRoleId", rowNumber);
        RoleId targetRoleId = record.expectedTargetRoleId() == null ? null
                : mapRoleId(record.expectedTargetRoleId(), "expectedTargetRoleId", rowNumber);
        try {
            return new TransitionRule(
                    mapEnum(record.fromStatus(), "fromStatus", "workflow status", RecordStatus.class, rowNumber),
                    mapEnum(record.action(), "action", "workflow action", WorkflowAction.class, rowNumber),
                    legacyRole(actorRoleId, "actorRoleId", rowNumber),
                    mapEnum(record.actorRequirement(), "actorRequirement", "actor requirement", ActorRequirement.class, rowNumber),
                    mapEnum(record.toStatus(), "toStatus", "workflow status", RecordStatus.class, rowNumber),
                    mapEnum(record.targetStrategy(), "targetStrategy", "target strategy", TargetStrategy.class, rowNumber),
                    targetRoleId == null ? null : legacyRole(targetRoleId, "expectedTargetRoleId", rowNumber),
                    record.requiredPermissionCode(), actorRoleId, targetRoleId);
        } catch (IllegalArgumentException exception) {
            throw new TransitionRuleConfigurationException(
                    "Inconsistent transition configuration at row " + rowNumber + ": " + exception.getMessage(), exception);
        }
    }

    private static RoleId mapRoleId(Integer value, String fieldName, int rowNumber) {
        if (value == null || value <= 0) {
            throw new TransitionRuleConfigurationException("Transition configuration field '" + fieldName
                    + "' must be positive at row " + rowNumber + ": " + value);
        }
        return new RoleId(value);
    }

    /** Temporary compatibility bridge; dynamic actors become usable in WF-2D2 PR 2. */
    private RoleName legacyRole(RoleId id, String fieldName, int rowNumber) {
        RoleName role = legacyRoles.get(id);
        if (role == null) {
            throw new TransitionRuleConfigurationException("Unknown actor role in transition configuration field '"
                    + fieldName + "' at row " + rowNumber + ": " + id.value()
                    + ". Dynamic roles require the WF-2D2 actor rollout");
        }
        return role;
    }

    private static <E extends Enum<E>> E mapEnum(
            String value,
            String fieldName,
            String enumDescription,
            Class<E> enumType,
            int rowNumber) {

        if (value == null) {
            throw new TransitionRuleConfigurationException(
                    "Transition configuration field '" + fieldName + "' is null at row " + rowNumber);
        }
        if (value.isBlank()) {
            throw new TransitionRuleConfigurationException(
                    "Transition configuration field '" + fieldName + "' is blank at row "
                            + rowNumber + ": '" + value + "'");
        }

        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new TransitionRuleConfigurationException(
                    "Unknown " + enumDescription + " in transition configuration field '"
                            + fieldName + "' at row " + rowNumber + ": " + value,
                    exception);
        }
    }

    private record Key(RecordStatus from, WorkflowAction action, RoleName actorRole) {
    }
}
