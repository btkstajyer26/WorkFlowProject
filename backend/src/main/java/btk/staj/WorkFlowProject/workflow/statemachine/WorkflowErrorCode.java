package btk.staj.WorkFlowProject.workflow.statemachine;

/**
 * Workflow modulunun hata sozlugu.
 *
 * <p>Bu enum yalnizca hata kodunu tasir; HTTP durum kodu eslemesi merkezi hata
 * yonetimi (common/exception) katmaninda yapilir. Eslemeler icin
 * {@code docs/workflow-gorev-dagilimi.md} bolum 3 (E5) ve dayanak belgenin
 * 15. bolumune bakiniz.
 *
 * <p>{@code RECORD_NOT_FOUND} bilerek burada tanimlanmamistir: kayit bulunamama
 * durumu workflow'a ozgu degildir ve ortak hata sozlugune aittir.
 */
public enum WorkflowErrorCode {

    /** Mevcut durumda bu aksiyona izin verilmiyor. */
    WORKFLOW_INVALID_TRANSITION,

    /** Kullanici kayit sahibi veya atanan kullanici degil. */
    WORKFLOW_FORBIDDEN,

    /** Kayit terminal durumda, kilitli. */
    WORKFLOW_RECORD_LOCKED,

    /** Zorunlu aciklama eksik veya yalnizca bosluktan olusuyor. */
    WORKFLOW_COMMENT_REQUIRED,

    /** Hedef kullanici gerekli ama istekte gonderilmemis. */
    WORKFLOW_TARGET_REQUIRED,

    /** Hedef gonderilmemesi gereken aksiyonda hedef gonderilmis. */
    WORKFLOW_TARGET_NOT_ALLOWED,

    /** Hedef kullanici beklenen role sahip degil ({@code ADMIN} secimi dahil). */
    WORKFLOW_TARGET_ROLE_INVALID,

    /** Secilen hedef kullanici pasif. */
    WORKFLOW_TARGET_INACTIVE,

    /** Workflow aktoru olmayan bir rol ({@code ADMIN}) aksiyon denedi. */
    WORKFLOW_ROLE_NOT_ALLOWED,

    /** Beklenen status seed verisi bulunamadi. Servis katmani uretir. */
    WORKFLOW_STATUS_NOT_CONFIGURED,

    /** Beklenen role seed verisi bulunamadi veya tek Baskan cozulemedi. Servis katmani uretir. */
    WORKFLOW_ROLE_NOT_CONFIGURED,

    /**
     * Kayit, okundugu andan beri baska bir islem tarafindan degistirilmis.
     *
     * <p>Diger kodlardan farkli olarak bunu <strong>durum makinesi uretmez</strong>:
     * gecis kurallari acisindan istek gecerlidir, yalnizca dayandigi surum
     * eskimistir. Kod {@link btk.staj.WorkFlowProject.workflow.port.WorkflowRecordPort}
     * uygulamasindan gelir ve {@code WorkflowApplicationException} icinde tasinir.
     *
     * <p>Istemci ayni istegi guncel veriyle tekrarlayabilir; bu yuzden kalici bir
     * kural ihlali degil, gecici bir catisma bildirir.
     */
    WORKFLOW_VERSION_CONFLICT
}
