package btk.staj.WorkFlowProject.record.view;

import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Bir kaydin icerigini, ona bakan kullaniciya gore cozer.
 *
 * <p>Cogu kullanici icin cevap kaydin guncel halidir. Kaydi elinden cikarmis
 * olan Baskan Yardimcisi ise onu devir anindaki haliyle gorur: Calisan
 * duzeltme yaparken kaydettigi degisiklikler, evrak {@code TEKRAR_GONDER} ile
 * geri gelene kadar ona yansimaz (bkz.
 * {@link RecordAccessPolicy#seesRecordAsOfHandoff}).
 *
 * <p>Detay ve liste uclari ayni kurali uygulamak zorunda: yalnizca biri
 * dondurulsaydi liste basligi sizdirmaya devam ederdi. Kural bu yuzden tek
 * yerde durur.
 */
@Component
public class RecordContentView {

    private final RecordAccessPolicy recordAccessPolicy;

    public RecordContentView(RecordAccessPolicy recordAccessPolicy) {
        this.recordAccessPolicy = Objects.requireNonNull(recordAccessPolicy, "recordAccessPolicy");
    }

    /**
     * Gosterilecek icerik.
     *
     * @param asOf dondurulmus icerik gosteriliyorsa devir ani, aksi halde
     *             {@code null}. Ek dosyalari suzmek icin kullanilir.
     */
    public record Content(String title, String description, Integer categoryId, LocalDateTime asOf) {

        /** Dondurulmus icerik mi gosteriliyor? */
        public boolean frozen() {
            return asOf != null;
        }
    }

    public Content visibleContent(Record record, RoleName viewerRole, UUID viewerId) {
        Objects.requireNonNull(record, "record");

        boolean asOfHandoff = recordAccessPolicy.seesRecordAsOfHandoff(
                viewerRole, viewerId, record.getAssignedTo(), record.getStatus());

        // snapshotAt bos ise dondurulacak bir sey yok. V9 migration'i mevcut
        // duzeltmedeki kayitlari geri doldurdugu ve gecis her seferinde anlik
        // goruntu aldigi icin bu dal pratikte olusmaz; yine de guncel icerige
        // dusmek, ekrani bos birakmaktan iyidir.
        if (!asOfHandoff || record.getSnapshotAt() == null) {
            return live(record);
        }

        return new Content(
                record.getSnapshotTitle(),
                record.getSnapshotDescription(),
                record.getSnapshotCategoryId(),
                record.getSnapshotAt());
    }

    /** Kaydin guncel hali; kirpma uygulanmayan kullanicilar icin. */
    public static Content live(Record record) {
        return new Content(
                record.getTitle(),
                record.getDescription(),
                record.getCategoryId(),
                null);
    }
}
