package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.rbac.RoleName;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RecordAccessPolicy {

    /**
     * Şartnamedeki kayıt görünürlük kapsamı kuralı:
     * - Çalışan: yalnızca kendi oluşturduğu kayıtları görebilir
     * - Başkan Yardımcısı: yalnızca kendisine atanan/gelen kayıtları görebilir
     * - Başkan: yalnızca onay aşamasına gelen kayıtları görebilir
     *
     * NOT: currentUserId ve record'a ait alanlar (createdBy, assignedTo) record modülünden gelir.
     * Alperen/Fevzi'nin Record entity/DTO'sundaki gerçek alan adlarına göre
     * bu metodun çağrıldığı yerdeki parametreler uyarlanabilir.
     */
    public boolean canView(RoleName role, UUID currentUserId, UUID recordCreatedBy, UUID recordAssignedTo, boolean isInBaskanReview) {
        return switch (role) {
            case CALISAN -> currentUserId.equals(recordCreatedBy);
            case BASKAN_YARDIMCISI -> currentUserId.equals(recordAssignedTo);
            case BASKAN -> isInBaskanReview;
        };
    }
}