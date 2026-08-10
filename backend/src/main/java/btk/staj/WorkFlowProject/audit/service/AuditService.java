package btk.staj.WorkFlowProject.audit.service;

import java.util.UUID;

/**
 * Onay akışının (workflow) kayıt üzerindeki her durum değişikliğini
 * denetim izine (audit log) yazmak için kullandığı port.
 */
public interface AuditService {

    /**
     * Bir kayıt (evrak) üzerinde gerçekleşen işlemi denetim izine yazar.
     *
     * @param recordId       işlemin yapıldığı kayıt
     * @param userId         işlemi yapan kullanıcı
     * @param roleId         kullanıcının o anki rolü
     * @param action         yapılan işlem (ör. "ONAYLANDI", "REDDEDILDI")
     * @param previousStatus önceki durum (varsa)
     * @param newStatus      yeni durum
     * @param comment        açıklama/not (varsa)
     */
    void logIslem(UUID recordId, UUID userId, Integer roleId, String action,
                  String previousStatus, String newStatus, String comment);
}