package btk.staj.WorkFlowProject.workflow.config;

import btk.staj.WorkFlowProject.workflow.adapter.DbTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.port.AuditService;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.port.WorkflowEventPublisher;
import btk.staj.WorkFlowProject.workflow.port.WorkflowRecordPort;
import btk.staj.WorkFlowProject.workflow.port.WorkflowUserPort;
import btk.staj.WorkFlowProject.workflow.service.TargetUserResolver;
import btk.staj.WorkFlowProject.workflow.service.WorkflowApplicationService;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowTransitionValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Onay akisi cekirdegini Spring'e tanitir.
 *
 * <p>Cekirdek siniflar bilerek anotasyonsuz birakilmistir: durum makinesi ve
 * uygulama servisi altyapi bilmeden {@code new} ile orneklenebilsin, boylece
 * Spring context'i olmadan test edilebilsinler. Bu yuzden {@code @Service}
 * eklenmedi, bean tanimlari disaridan burada yapiliyor.
 */
@Configuration
public class WorkflowConfiguration {

    /** Gecis zamani tek yerden gelsin; testte sabit saatle degistirilebilir. */
    @Bean
    public Clock workflowClock() {
        return Clock.systemUTC();
    }

    /**
     * Gecis kurallarinin kaynagi: {@code workflow_transitions} tablosu.
     *
     * <p>Kurallar acilista bir kez okunup bellege alinir; canli reload bu
     * iterasyonun kapsaminda degil (WF-4). Seed eksik veya bozuksa uygulama
     * <strong>acilmaz</strong> &mdash; bu bilincli bir fail-fast tercihidir:
     * yarim bir kural tablosuyla calisan bir workflow, sessizce yanlis
     * kararlar verirdi.
     *
     * <p>{@code StaticTransitionRuleSource} kaldirilmadi: SM-9 parity testinin
     * karsilastirdigi referans odur. Statik tablonun kaldirilmasi ayri bir is
     * (TZ-1).
     */
    @Bean
    public TransitionRuleSource transitionRuleSource(TransitionRuleRecordReader ruleRecordReader) {
        return new DbTransitionRuleSource(ruleRecordReader);
    }

    @Bean
    public WorkflowTransitionValidator workflowTransitionValidator(TransitionRuleSource ruleSource) {
        return new WorkflowTransitionValidator(ruleSource);
    }

    @Bean
    public TargetUserResolver targetUserResolver(WorkflowUserPort userPort) {
        return new TargetUserResolver(userPort);
    }

    @Bean
    public WorkflowApplicationService workflowApplicationService(
            WorkflowRecordPort recordPort,
            CurrentActorProvider currentActorProvider,
            TargetUserResolver targetUserResolver,
            WorkflowTransitionValidator validator,
            TransitionRuleSource ruleSource,
            AuditService auditService,
            WorkflowEventPublisher eventPublisher,
            Clock workflowClock) {

        return new WorkflowApplicationService(
                recordPort,
                currentActorProvider,
                targetUserResolver,
                validator,
                ruleSource,
                auditService,
                eventPublisher,
                workflowClock);
    }
}
