package btk.staj.WorkFlowProject.record.dto;

/**
 * Başkan bir kaydı geri gönderirken hedefi seçebilir (Çalışan ya da Başkan Yardımcısı).
 * Başkan Yardımcısı için hedef her zaman Çalışan olduğundan bu DTO'yu kullanmaz,
 * RecordActionRequest yeterlidir.
 */
public record RecordReturnRequest(
    String note,
    ReturnTarget target
) {
    public enum ReturnTarget {
        CALISAN,
        BASKAN_YARDIMCISI
    }
}