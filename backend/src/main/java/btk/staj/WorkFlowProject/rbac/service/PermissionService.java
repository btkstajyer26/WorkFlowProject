package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.rbac.RoleName;
import btk.staj.WorkFlowProject.rbac.RecordStatus;
import org.springframework.stereotype.Component;

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

    public boolean isRecordLocked(RecordStatus currentStatus) {
        return currentStatus == RecordStatus.ONAYLANDI || currentStatus == RecordStatus.REDDEDILDI;
    }
}