package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
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

    private final List<TransitionRule> snapshot;
    private final Map<Key, TransitionRule> index;

    public DbTransitionRuleSource(TransitionRuleRecordReader reader) {
        if (reader == null) {
            throw new TransitionRuleConfigurationException(
                    "TransitionRuleRecordReader must not be null");
        }

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

    private static TransitionRule map(TransitionRuleRecord record, int rowNumber) {
        try {
            return new TransitionRule(
                    mapEnum(record.fromStatus(), "fromStatus", "workflow status", RecordStatus.class, rowNumber),
                    mapEnum(record.action(), "action", "workflow action", WorkflowAction.class, rowNumber),
                    mapEnum(record.actorRole(), "actorRole", "actor role", RoleName.class, rowNumber),
                    mapEnum(
                            record.actorRequirement(),
                            "actorRequirement",
                            "actor requirement",
                            ActorRequirement.class,
                            rowNumber),
                    mapEnum(record.toStatus(), "toStatus", "workflow status", RecordStatus.class, rowNumber),
                    mapEnum(
                            record.targetStrategy(),
                            "targetStrategy",
                            "target strategy",
                            TargetStrategy.class,
                            rowNumber),
                    mapNullableEnum(
                            record.expectedTargetRole(),
                            "expectedTargetRole",
                            "actor role",
                            RoleName.class,
                            rowNumber),
                    record.requiredPermissionCode());
        } catch (IllegalArgumentException exception) {
            // TransitionRule'un compact constructor'i hedef stratejisi ile beklenen hedef
            // rolun tutarli olmasini zorunlu kilar. Ihlali burada yakalayip satir numarasi
            // ile birlikte yapilandirma hatasina cevirmezsek, cagiran taraf ham bir
            // IllegalArgumentException gorur ve hangi satirin bozuk oldugunu bilemez.
            throw new TransitionRuleConfigurationException(
                    "Inconsistent transition configuration at row " + rowNumber + ": "
                            + exception.getMessage(),
                    exception);
        }
    }

    /**
     * Bos gelmesi mesru olan tek alan icin. {@code null} aynen gecirilir; dolu bir deger
     * {@link #mapEnum} ile ayni katilikta cozulur, yani yazim hatasi yine yakalanir.
     */
    private static <E extends Enum<E>> E mapNullableEnum(
            String value,
            String fieldName,
            String enumDescription,
            Class<E> enumType,
            int rowNumber) {

        return value == null ? null : mapEnum(value, fieldName, enumDescription, enumType, rowNumber);
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
