package btk.staj.WorkFlowProject.department.dto;

import btk.staj.WorkFlowProject.department.entity.DepartmentEntity;

/**
 * Departman katalogunun API yaniti. {@code parentDepartmentId} yalniz yapisal
 * bilgidir; hedef cozumunde kullanilmaz (WORKFLOW_V1_V2_PLANI.md SS14,
 * DB_1_VERI_MODELI_SOZLESMESI.md SS15).
 */
public record DepartmentResponse(Integer id,
                                 String name,
                                 Integer parentDepartmentId,
                                 boolean active) {

    public static DepartmentResponse from(DepartmentEntity department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getParentDepartmentId(),
                department.isActive());
    }
}
