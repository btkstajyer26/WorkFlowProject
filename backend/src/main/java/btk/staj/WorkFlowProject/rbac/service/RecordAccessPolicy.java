package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Sartnamedeki "Kayit Gorunurlugu Kapsami" kuralini uygular:
 * Calisan yalnizca kendi olusturdugu kayitlari, Baskan Yardimcisi kendisine
 * atanan kayitlari (ve duzeltme bekleyenleri), Baskan ise onay asamasina
 * gelen kayitlari gorebilir.
 */
@Component
public class RecordAccessPolicy {

    public boolean canView(RoleName role,
                           UUID currentUserId,
                           UUID recordCreatedBy,
                           UUID recordAssignedTo,
                           UUID recordLastDeputyId,
                           RecordStatus status) {

        return switch (role) {
            case CALISAN -> currentUserId.equals(recordCreatedBy);
            // Bsk. Yrd. kendisine atanan kayitlari, duzeltme bekleyen kayitlari
            // VE bir kez kendi elinden gecmis kayitlari gorur (salt okunur
            // takip; elinden cikardigi evraki kaybetmesin).
            //
            // Ucuncu kol Baskana ilettigi evraki kapsar: BASKANA_ILET ile
            // assignedTo Baskana gecer ama lastDeputyId ileten yardimcida
            // kalir. Bu olmadan panodaki "Baskan incelemesinde" ve
            // "Sonuclananlar" sayaclari kalici olarak 0 gorunurdu.
            case BASKAN_YARDIMCISI -> currentUserId.equals(recordAssignedTo)
                    || status == RecordStatus.DUZENLEME_BEKLIYOR
                    || currentUserId.equals(recordLastDeputyId);
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
                              UUID recordLastDeputyId,
                              RecordStatus status) {

        if (!canView(role, currentUserId, recordCreatedBy, recordAssignedTo, recordLastDeputyId, status)) {
            throw new ForbiddenException("Bu kaydı görüntüleme yetkiniz yok");
        }
    }

    /**
     * Kaydin, bakan kullanicinin masasindan cikmis olmasina ragmen hala gorunur
     * oldugu araligi bildirir. Bu aralikta kullanici kaydi <em>devir anindaki
     * haliyle</em> gorur: hem islem gecmisi devirde kesilir, hem baslik,
     * aciklama, kategori ve ek dosyalar dondurulmus kopyadan okunur.
     *
     * <p>Yalnizca Baskan Yardimcisi icin olusabilir: {@link #canView} kurali,
     * geri gonderdigi evraki kaybetmesin diye {@code DUZENLEME_BEKLIYOR}
     * kayitlarini ona acik birakiyor. Ama o pencere boyunca evrak Calisanin
     * elindedir; Calisanin bu sirada yaptigi duzenlemeler yardimciyi
     * ilgilendirmez. Evrak {@code TEKRAR_GONDER} ile geri geldiginde kayit
     * tekrar yardimciya atanir ve her sey butunuyle acilir.
     *
     * <p>Baskan icin ayni durum dogmaz: geri gonderdiginde kayit
     * {@code DUZENLEME_BEKLIYOR} olur ve {@link #canView} ona kapanir.
     */
    public boolean seesRecordAsOfHandoff(RoleName role,
                                               UUID currentUserId,
                                               UUID recordAssignedTo,
                                               RecordStatus status) {

        return role == RoleName.BASKAN_YARDIMCISI
                && status == RecordStatus.DUZENLEME_BEKLIYOR
                && !Objects.equals(currentUserId, recordAssignedTo);
    }
}
