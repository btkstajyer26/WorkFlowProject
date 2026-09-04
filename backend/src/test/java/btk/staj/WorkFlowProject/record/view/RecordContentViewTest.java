package btk.staj.WorkFlowProject.record.view;

import static btk.staj.WorkFlowProject.support.AuthorizationFixtures.visibility;

import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Kayit icerigi gorunurlugu")
class RecordContentViewTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final UUID DEPUTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000032");
    private static final LocalDateTime HANDOFF = LocalDateTime.of(2026, 8, 19, 10, 0);

    private final RecordContentView view = new RecordContentView(new RecordAccessPolicy());

    @Test
    @DisplayName("duzeltmedeki kaydin guncel halini Bsk. Yrd. gormez, devir anindakini gorur")
    void theDeputySeesTheContentAsOfTheHandoff() {
        Record record = returnedRecord();

        RecordContentView.Content content =
                view.visibleContent(record, visibility(RoleName.BASKAN_YARDIMCISI, DEPUTY_ID));

        assertThat(content.frozen()).isTrue();
        assertThat(content.title()).isEqualTo("Gönderilen başlık");
        assertThat(content.description()).isEqualTo("Gönderilen açıklama");
        assertThat(content.categoryId()).isEqualTo(1);
        assertThat(content.asOf()).isEqualTo(HANDOFF);
    }

    @Test
    @DisplayName("kaydin sahibi Calisan kendi duzenledigi guncel icerigi gorur")
    void theOwnerSeesTheLiveContent() {
        Record record = returnedRecord();

        RecordContentView.Content content =
                view.visibleContent(record, visibility(RoleName.CALISAN, OWNER_ID));

        assertThat(content.frozen()).isFalse();
        assertThat(content.title()).isEqualTo("Düzeltilmiş başlık");
        assertThat(content.categoryId()).isEqualTo(2);
    }

    @Test
    @DisplayName("kayit yeniden gonderilince Bsk. Yrd. guncel icerigi gorur")
    void theDeputySeesTheLiveContentOnceTheRecordComesBack() {
        Record record = returnedRecord();
        // TEKRAR_GONDER sonrasi: kayit yeniden yardimciya atanir.
        record.setStatus(RecordStatus.BSK_YRD_INCELEMESINDE);
        record.setAssignedTo(DEPUTY_ID);

        RecordContentView.Content content =
                view.visibleContent(record, visibility(RoleName.BASKAN_YARDIMCISI, DEPUTY_ID));

        assertThat(content.frozen()).isFalse();
        assertThat(content.title()).isEqualTo("Düzeltilmiş başlık");
        // Eski anlik goruntu satirda kalmis olabilir ama artik okunmaz.
        assertThat(record.getSnapshotTitle()).isEqualTo("Gönderilen başlık");
    }

    @Test
    @DisplayName("anlik goruntu yoksa guncel icerige dusulur")
    void fallsBackToTheLiveContentWithoutASnapshot() {
        Record record = returnedRecord();
        record.setSnapshotAt(null);

        RecordContentView.Content content =
                view.visibleContent(record, visibility(RoleName.BASKAN_YARDIMCISI, DEPUTY_ID));

        assertThat(content.frozen()).isFalse();
        assertThat(content.title()).isEqualTo("Düzeltilmiş başlık");
    }

    /** Geri gonderilmis, ardindan Calisan tarafindan duzenlenmis kayit. */
    private static Record returnedRecord() {
        Record record = new Record();
        record.setId(UUID.randomUUID());
        record.setCreatedBy(OWNER_ID);
        record.setAssignedTo(OWNER_ID);
        record.setStatus(RecordStatus.DUZENLEME_BEKLIYOR);
        record.setTitle("Düzeltilmiş başlık");
        record.setDescription("Düzeltilmiş açıklama");
        record.setCategoryId(2);
        record.setSnapshotTitle("Gönderilen başlık");
        record.setSnapshotDescription("Gönderilen açıklama");
        record.setSnapshotCategoryId(1);
        record.setSnapshotAt(HANDOFF);
        return record;
    }
}
