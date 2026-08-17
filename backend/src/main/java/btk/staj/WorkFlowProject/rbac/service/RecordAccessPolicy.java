package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Sartnamedeki "Kayit Gorunurlugu Kapsami" kuralini uygular:
 * Calisan yalnizca kendi olusturdugu kayitlari, Baskan Yardimcisi kendisine
 * atanan kayitlari (ve duzeltme bekleyenleri), Baskan ise onay asamasina gelen kayitlari gorebilir.
 */
@Component
public class RecordAccessPolicy {

    public boolean canView(RoleName role,
                           UUID currentUserId,
                           UUID recordCreatedBy,
                           UUID recordAssignedTo,
                           RecordStatus status) {

        return switch (role) {
            case CALISAN -> currentUserId.equals(recordCreatedBy);
            
            // Bsk. Yrd. kendisine atanan kayitlari VE düzeltme bekleyen kayıtları gorur (Salt Okunur).
            case BASKAN_YARDIMCISI -> currentUserId.equals(recordAssignedTo) 
                    || status == RecordStatus.DUZENLEME_BEKLIYOR;
            
            case BASKAN -> status == RecordStatus.BASKAN_INCELEMESINDE
                    || currentUserId.equals(recordAssignedTo);
            
            // ADMIN yalnizca kullanici ve rol yonetiminden sorumludur; evrak goremez.
            case ADMIN -> false;
        };
    }

    /**
     * Gorunurluk kurali saglanmiyorsa {@link ForbiddenException} firlatir.
     * Cagiran tarafin ayrica kontrol yazmasina gerek kalmaz.
     */
    public void assertCanView(RoleName role,
                              UUID currentUserId,
                              UUID recordCreatedBy,
                              UUID recordAssignedTo,
                              RecordStatus status) {

        if (!canView(role, currentUserId, recordCreatedBy, recordAssignedTo, status)) {
            throw new ForbiddenException("Bu kaydı görüntüleme yetkiniz yok");
        }
    }
}