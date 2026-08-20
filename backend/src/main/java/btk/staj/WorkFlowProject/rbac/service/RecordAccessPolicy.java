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

            // Bsk. Yrd. kendisine atanan kayitlari VE düzeltme bekleyen kayıtları gorur (Salt Okunur).
            // Ucuncu kol Baskana ilettigi evraki kapsar: BASKANA_ILET ile
            // assignedTo Baskana gecer ama lastDeputyId ileten yardimcida
            // kalir. Bu olmadan panodaki "Baskan incelemesinde" ve
            // "Sonuclananlar" sayaclari kalici olarak 0 gorunurdu.
            case BASKAN_YARDIMCISI -> currentUserId.equals(recordAssignedTo)
                    || status == RecordStatus.DUZENLEME_BEKLIYOR
                    || currentUserId.equals(recordLastDeputyId);

            // Baskanin kapsami role bagli, kullaniciya degil: onayina gelen her
            // kaydi zaten atanmis olup olmadigina bakmadan gorur.
            //
            // Sonuclanan kayitlar da kapsamda: ONAYLA/REDDET aksiyonu
            // assignedTo'yu bosaltir, dolayisiyla kendi verdigi karardan sonra
            // kayit ona kapanirdi. "Onaylananlar" ve "Reddedilenler" sekmeleri
            // bu yuzden kalici olarak bos gorunuyordu. Bu iki duruma yalnizca
            // Baskanin karariyla gelinebildigi icin kapsami genisletmez.
            case BASKAN -> status == RecordStatus.BASKAN_INCELEMESINDE
                    || status == RecordStatus.ONAYLANDI
                    || status == RecordStatus.REDDEDILDI;

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

    /**
     * Baskanin islem gecmisini, evragin kendisine ilk ulastigi andan itibaren
     * gordugunu bildirir. Oncesindeki Calisan&ndash;Baskan Yardimcisi trafigi
     * (olusturma, duzeltme turlari, geri gonderme notlari) ona kapalidir.
     *
     * <p>{@link #seesRecordAsOfHandoff} ile ayni fikrin ters yonu: orada
     * kullanici evraki elinden <em>cikardigi</em> anda kesilir, burada evrak
     * eline <em>gectigi</em> anda baslar.
     *
     * <p>Kesme noktasi ilk iletimdir, sonuncusu degil. Baskan evraki
     * yardimciya geri gonderip tekrar aldiginda son iletime gore kirpmak, kendi
     * yazdigi ret/geri gonderme gerekcesini de gizlerdi; karar verirken en cok
     * ihtiyac duydugu satir odur.
     *
     * <p>Rol disinda kosul aranmaz: Baskan bir kaydi zaten ancak onayina
     * geldiyse veya sonuclandirdiysa gorebiliyor ({@link #canView}), ikisinde de
     * evrak en az bir kez kendisine iletilmis olur.
     */
    public boolean seesHistoryFromPresidentHandover(RoleName role) {
        return role == RoleName.BASKAN;
    }
}