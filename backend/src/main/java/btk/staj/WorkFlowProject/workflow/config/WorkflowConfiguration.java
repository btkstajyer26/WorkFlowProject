package btk.staj.WorkFlowProject.workflow.config;

import btk.staj.WorkFlowProject.workflow.port.AuditService;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.port.WorkflowEventPublisher;
import btk.staj.WorkFlowProject.workflow.port.WorkflowRecordPort;
import btk.staj.WorkFlowProject.workflow.port.WorkflowUserPort;
import btk.staj.WorkFlowProject.workflow.service.TargetUserResolver;
import btk.staj.WorkFlowProject.workflow.service.WorkflowApplicationService;
import btk.staj.WorkFlowProject.workflow.statemachine.StaticTransitionRuleSource;
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
     * Gecis kurallarinin kaynagi. Su an merkezi statik tabloyu saran adapter;
     * ilerideki iterasyonda yalnizca bu bean degistirilerek kurallar
     * veritabanindan okunabilir.
     */
    @Bean
    public TransitionRuleSource transitionRuleSource() {
        return new StaticTransitionRuleSource();
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
            AuditService auditService,
            WorkflowEventPublisher eventPublisher,
            Clock workflowClock) {

        return new WorkflowApplicationService(
                recordPort,
                currentActorProvider,
                targetUserResolver,
                validator,
                auditService,
                eventPublisher,
                workflowClock);
    }
}
