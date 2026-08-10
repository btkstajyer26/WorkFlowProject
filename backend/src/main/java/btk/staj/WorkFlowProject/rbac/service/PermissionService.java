package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.rbac.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import org.springframework.stereotype.Component;

/**
 * Sartnamedeki rol bazli yetki matrisinin kaba kontrolleri.
 *
 * <p>Durum bagimli metotlar, gecis kurallarini durum makinesiyle ayni bilgiyi ikinci
 * kez ifade eder. Nihai karar mercii her zaman
 * {@code WorkflowTransitionValidator}'dir; burasi controller seviyesinde erken
 * eleme icin kullanilir.
 */
@Component
public class PermissionService {

    public boolean canCreateRecord(RoleName role) {
        return role == RoleName.CALISAN;
    }

    public boolean canEditOrDeleteDraft(RoleName role, RecordStatus currentStatus) {
        return role == RoleName.CALISAN && currentStatus == RecordStatus.TASLAK;
    }

    public boolean canEditAndResendReturnedRecord(RoleName role, RecordStatus currentStatus) {
        return role == RoleName.CALISAN && currentStatus == RecordStatus.DUZENLEME_BEKLIYOR;
    }

    public boolean canSendToReview(RoleName role, RecordStatus currentStatus) {
        return role == RoleName.CALISAN && currentStatus == RecordStatus.TASLAK;
    }

    public boolean canForwardToBaskan(RoleName role, RecordStatus currentStatus) {
        return role == RoleName.BASKAN_YARDIMCISI && currentStatus == RecordStatus.BSK_YRD_INCELEMESINDE;
    }

    public boolean canApprove(RoleName role, RecordStatus currentStatus) {
        return role == RoleName.BASKAN && currentStatus == RecordStatus.BASKAN_INCELEMESINDE;
    }

    public boolean canReject(RoleName role, RecordStatus currentStatus) {
        return role == RoleName.BASKAN && currentStatus == RecordStatus.BASKAN_INCELEMESINDE;
    }

    public boolean canReturnToCalisan(RoleName role) {
        return role == RoleName.BASKAN_YARDIMCISI || role == RoleName.BASKAN;
    }

    public boolean canReturnToBaskanYrd(RoleName role) {
        return role == RoleName.BASKAN;
    }

    public boolean isCommentRequiredForReturn(String comment) {
        return comment != null && !comment.trim().isEmpty();
    }

    public boolean canAddNote(RoleName role) {
        return role == RoleName.CALISAN || role == RoleName.BASKAN_YARDIMCISI || role == RoleName.BASKAN;
    }

    /**
     * Kilitleme kurali durum makinesinde tek noktada tutulur; burada tekrar
     * tanimlanmaz (bkz. {@link RecordStatus#isTerminal()}).
     */
    public boolean isRecordLocked(RecordStatus currentStatus) {
        return currentStatus.isTerminal();
    }
}