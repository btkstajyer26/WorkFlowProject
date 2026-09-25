package btk.staj.WorkFlowProject.common.dto;

import java.util.UUID;

/**
 * Kayit atamasinin ortak sozlesmesi (B11); kisi ve departman atamasi tek nesnede tasinir.
 *
 * <p>Kayit yanitlarinin ucunde de ({@code RecordResponse}, {@code RecordSearchResponse},
 * {@code WorkflowActionResponse}) ayni sekilde bulunur; {@link #kind()} tek dogruluk
 * kaynagidir.
 *
 * <p>Gosterim adlari yanitla birlikte gelir: normal kullanicinin kullanici veya departman
 * cozebilecegi bir uc yoktur, ad verilmezse istemci denetim izini tarayip yanlis ad
 * gosterir. Departman adi kaydi gorebilen herkese gosterilir &mdash; bu yonlendirme
 * bilgisidir, uyelik bilgisi degildir; uye kimlikleri hicbir yanitta yer almaz.
 */
public record AssignmentView(
        AssignmentKind kind,
        UUID userId,
        String userFullName,
        Integer departmentId,
        String departmentName) {

    private static final AssignmentView NONE =
            new AssignmentView(AssignmentKind.NONE, null, null, null, null);

    public static AssignmentView none() {
        return NONE;
    }

    public static AssignmentView user(UUID userId, String userFullName) {
        return new AssignmentView(AssignmentKind.USER, userId, userFullName, null, null);
    }

    public static AssignmentView department(Integer departmentId, String departmentName) {
        return new AssignmentView(AssignmentKind.DEPARTMENT, null, null, departmentId, departmentName);
    }

    /**
     * Kimliklerden yola cikip turu secer; adlar sonradan {@link #withNames} ile eklenir.
     *
     * <p>Saf workflow cekirdegi kullanici/departman deposuna erisemedigi icin yaniti once
     * adsiz uretir, Spring sinirinda adlar doldurulur.
     */
    public static AssignmentView of(UUID userId, Integer departmentId) {
        if (userId != null) return user(userId, null);
        if (departmentId != null) return department(departmentId, null);
        return none();
    }

    /** Kimlikleri koruyarak gosterim adlarini yerlestirir. */
    public AssignmentView withNames(String resolvedUserFullName, String resolvedDepartmentName) {
        return switch (kind) {
            case USER -> new AssignmentView(kind, userId, resolvedUserFullName, null, null);
            case DEPARTMENT -> new AssignmentView(kind, null, null, departmentId, resolvedDepartmentName);
            case NONE -> this;
        };
    }
}
