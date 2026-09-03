package btk.staj.WorkFlowProject.workflow;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Veritabani olmadan calisan {@code @SpringBootTest}'ler icin gecis kurali
 * kaynagi.
 *
 * <p>Production'da kurallar {@code workflow_transitions} tablosundan okunur.
 * Ancak bazi testler DataSource, JPA ve Flyway auto-configuration'ini bilerek
 * disarida birakip butun repository'leri {@code @MockitoBean} ile veriyor;
 * orada okunacak bir tablo yoktur. Bu yapilandirma o testlerde portu statik
 * gecis tablosuyla besler.
 *
 * <p>Bunun davranisi degistirmedigi varsayim degil, testli bir gercektir:
 * {@code TransitionRuleSourceParityTest} iki kaynagin ayni sekiz kurali
 * urettigini her CI kosusunda dogrular. Parity dustugu anda bu yapilandirmayi
 * kullanan testler de yanlis zeminde calisiyor demektir.
 *
 * <p>Kullanimi: {@code @Import(StaticTransitionRuleReaderConfiguration.class)}
 * ve ayrica {@code @MockitoBean WorkflowTransitionRepository} &mdash;
 * {@code JpaTransitionRuleRecordReader} bir {@code @Component} oldugu icin
 * context'te yine olusturulur, sadece {@code @Primary} olan bu bean'in
 * arkasinda kalir.
 */
@TestConfiguration
public class StaticTransitionRuleReaderConfiguration {

    @Bean
    @Primary
    public TransitionRuleRecordReader staticTransitionRuleRecordReader() {
        List<TransitionRuleRecord> records = WorkflowRoleFixtures.rules().all().stream()
                .map(StaticTransitionRuleReaderConfiguration::toRecord)
                .toList();

        return () -> records;
    }

    private static TransitionRuleRecord toRecord(TransitionRule rule) {
        return new TransitionRuleRecord(
                rule.from().name(),
                rule.action().name(),
                rule.actorRoleId().value(),
                rule.actorRequirement().name(),
                rule.to().name(),
                rule.targetStrategy().name(),
                rule.expectedTargetRoleId() == null ? null : rule.expectedTargetRoleId().value(),
                AuthorizationFixtures.requiredPermission(rule.action().name()));
    }
}
