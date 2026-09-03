package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;
import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReloadableTransitionRuleSource")
class ReloadableTransitionRuleSourceTest {

    @Test
    @DisplayName("acilista ilk snapshot'i kurar")
    void loadsInitialSnapshot() {
        ReloadableTransitionRuleSource source = new ReloadableTransitionRuleSource(
                mutableReader(sendRule()));

        assertThat(source.all()).hasSize(1);
        assertThat(source.find(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN))
                .isPresent();
    }

    @Test
    @DisplayName("reload veritabanindaki degisikligi gorur")
    void reloadPicksUpChangedRules() {
        MutableReader reader = mutableReader(sendRule());
        ReloadableTransitionRuleSource source = new ReloadableTransitionRuleSource(reader);
        assertThat(source.all()).hasSize(1);

        reader.rows = List.of(sendRule(), approveRule());
        int count = source.reload();

        assertThat(count).isEqualTo(2);
        assertThat(source.all()).hasSize(2);
        assertThat(source.find(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.ONAYLA, RoleName.BASKAN))
                .as("reload sonrasi yeni kural bulunabilmeli")
                .isPresent();
    }

    /**
     * Bu sinifin varlik sebebi. Bozuk bir yapilandirma calisan uygulamayi kural kaynagi
     * olmadan birakmamali: hata cagirana doner ama eski snapshot yerinde kalir.
     */
    @Test
    @DisplayName("basarisiz reload eski snapshot'i korur")
    void failedReloadKeepsPreviousSnapshot() {
        MutableReader reader = mutableReader(sendRule());
        ReloadableTransitionRuleSource source = new ReloadableTransitionRuleSource(reader);

        reader.rows = List.of(new TransitionRuleRecord(
                "TASLAK", "GONDER", "CALISAN", "CREATOR", "BSK_YRD_INCELEMESINDE",
                "UNKNOWN_STRATEGY", "BASKAN_YARDIMCISI",
                AuthorizationFixtures.requiredPermission("GONDER")));

        assertThatThrownBy(source::reload)
                .isInstanceOf(TransitionRuleConfigurationException.class);

        assertThat(source.all())
                .as("bozuk reload sonrasi kurallar korunmali")
                .hasSize(1);
        assertThat(source.find(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN))
                .isPresent();
    }

    @Test
    @DisplayName("bos tablo ile yapilan reload da eski snapshot'i korur")
    void reloadWithEmptyTableKeepsPreviousSnapshot() {
        MutableReader reader = mutableReader(sendRule());
        ReloadableTransitionRuleSource source = new ReloadableTransitionRuleSource(reader);

        reader.rows = List.of();

        assertThatThrownBy(source::reload)
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("no active transition rules");
        assertThat(source.all()).hasSize(1);
    }

    /** Acilistaki fail-fast bu sarmalayici ile zayiflamamali. */
    @Test
    @DisplayName("acilista bos tablo uygulamayi durdurur")
    void stillFailsFastOnStartup() {
        assertThatThrownBy(() -> new ReloadableTransitionRuleSource(mutableReader()))
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("no active transition rules");
    }

    @Test
    @DisplayName("reader olmadan olusturulamaz")
    void rejectsNullReader() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ReloadableTransitionRuleSource(null))
                .withMessageContaining("reader");
    }

    private static MutableReader mutableReader(TransitionRuleRecord... rows) {
        MutableReader reader = new MutableReader();
        reader.rows = List.of(rows);
        return reader;
    }

    /** Reload'i test edebilmek icin sonucu degisebilen reader; lambda bunu karsilamaz. */
    private static final class MutableReader implements TransitionRuleRecordReader {
        private List<TransitionRuleRecord> rows = List.of();

        @Override
        public List<TransitionRuleRecord> findAllActive() {
            return rows;
        }
    }

    private static TransitionRuleRecord sendRule() {
        return new TransitionRuleRecord(
                "TASLAK", "GONDER", "CALISAN", "CREATOR", "BSK_YRD_INCELEMESINDE",
                "ROLE", "BASKAN_YARDIMCISI", AuthorizationFixtures.requiredPermission("GONDER"));
    }

    private static TransitionRuleRecord approveRule() {
        return new TransitionRuleRecord(
                "BASKAN_INCELEMESINDE", "ONAYLA", "BASKAN", "ASSIGNEE", "ONAYLANDI",
                "NONE", null, AuthorizationFixtures.requiredPermission("ONAYLA"));
    }
}
