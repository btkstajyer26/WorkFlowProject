package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.RecordStatus;
import org.springframework.stereotype.Component;

@Component
public class PermissionService {

    // ---- Kayıt Oluşturma ----
    public boolean canCreateRecord(Role role) {
        return role == Role.CALISAN;
    }

    // ---- Taslak Yönetimi ----
    public boolean canEditOrDeleteDraft(Role role, RecordStatus currentStatus) {
        return role == Role.CALISAN && currentStatus == RecordStatus.TASLAK;
    }

    public boolean canEditAndResendReturnedRecord(Role role, RecordStatus currentStatus) {
        return role == Role.CALISAN && currentStatus == RecordStatus.DUZENLEME_BEKLIYOR;
    }

    // ---- Gönderme / İletme ----
    public boolean canSendToReview(Role role, RecordStatus currentStatus) {
        return role == Role.CALISAN && currentStatus == RecordStatus.TASLAK;
    }

    public boolean canForwardToBaskan(Role role, RecordStatus currentStatus) {
        return role == Role.BASKAN_YARDIMCISI && currentStatus == RecordStatus.BSK_YRD_INCELEMESINDE;
    }

    // ---- Nihai Karar ----
    public boolean canApprove(Role role, RecordStatus currentStatus) {
        return role == Role.BASKAN && currentStatus == RecordStatus.BASKAN_INCELEMESINDE;
    }

    public boolean canReject(Role role, RecordStatus currentStatus) {
        return role == Role.BASKAN && currentStatus == RecordStatus.BASKAN_INCELEMESINDE;
    }

    // ---- Geri Gönderme ----
    public boolean canReturnToCalisan(Role role) {
        return role == Role.BASKAN_YARDIMCISI || role == Role.BASKAN;
    }

    public boolean canReturnToBaskanYrd(Role role) {
        return role == Role.BASKAN;
    }

    public boolean isCommentRequiredForReturn(String comment) {
        return comment != null && !comment.trim().isEmpty();
    }

    // ---- Açıklama / Not Ekleme ----
    public boolean canAddNote(Role role) {
        return role == Role.CALISAN || role == Role.BASKAN_YARDIMCISI || role == Role.BASKAN;
    }

    // ---- Onaylanmış Kaydın Kilitlenmesi ----
    public boolean isRecordLocked(RecordStatus currentStatus) {
        return currentStatus == RecordStatus.ONAYLANDI || currentStatus == RecordStatus.REDDEDILDI;
    }

    // ---- Kayıt Görünürlük Kapsamı ----
    // TODO: Record entity hazır olunca implemente edilecek
    // - CALISAN  -> sadece kendi created_by'ı olan kayıtlar
    // - BASKAN_YARDIMCISI -> sadece assigned_to kendisi olan kayıtlar
    // - BASKAN -> sadece status_id = BASKAN_INCELEMESINDE olan kayıtlar
}