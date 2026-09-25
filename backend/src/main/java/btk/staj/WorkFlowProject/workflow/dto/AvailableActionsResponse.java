package btk.staj.WorkFlowProject.workflow.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;

import java.util.List;
import java.util.UUID;

/**
 * Kullanilabilir aksiyon listesi (APP-9 SS1).
 *
 * <p>Bos {@code actions} hata degildir: kaydi gorebilen ama uzerinde islem yapamayan
 * kullanici 200 ve bos liste alir.
 */
public record AvailableActionsResponse(
        UUID recordId,
        RecordStatus status,
        int version,
        List<AvailableActionView> actions) {
}
