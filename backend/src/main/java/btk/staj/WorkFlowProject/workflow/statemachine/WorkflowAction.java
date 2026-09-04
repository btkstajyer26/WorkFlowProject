package btk.staj.WorkFlowProject.workflow.statemachine;

/**
 * Kayit uzerinde gerceklestirilebilecek workflow aksiyonlari.
 *
 * <p>Istemci hedef durumu dogrudan gondermez; yalnizca aksiyonu gonderir, yeni
 * durumu backend hesaplar.
 *
 * <p>Aksiyon yalnizca <strong>istekle ilgili</strong> bilgiyi tasir: istemcinin
 * {@code targetUserId} veya {@code targetDepartmentId} gondermesi gerekip
 * gerekmedigi ve aciklamanin zorunlu olup olmadigi. Beklenmeyen bir alan
 * gonderilirse istek bu bayraklar uzerinden reddedilir.
 *
 * <p><strong>Hedefin kim olacagi burada tutulmaz.</strong> Beklenen hedef rol ve hedef
 * cozum stratejisi gecisin ozelligidir; {@code TransitionRule} uzerinden
 * {@code workflow_transitions} satirindan okunur (DB-1 SS6.5). Ayni aksiyon farkli
 * gecislerde farkli hedefe gidebilir: {@code CALISANA_GERI_GONDER} hem Baskan
 * Yardimcisinin hem Baskanin kullandigi iki ayri satirda bulunur.
 */
public enum WorkflowAction {

    /** Calisanin taslak kaydi onaya gondermesi. */
    GONDER(false, false),

    /** Calisanin duzelttigi kaydi yeniden gondermesi. */
    TEKRAR_GONDER(false, false),

    /** Baskan Yardimcisinin kaydi Baskana iletmesi. */
    BASKANA_ILET(false, false),

    /** Kaydin duzeltilmek uzere Calisana geri gonderilmesi. */
    CALISANA_GERI_GONDER(false, true),

    /** Baskanin kaydi bir onceki adima, Baskan Yardimcisina geri gondermesi. */
    BASKAN_YARDIMCISINA_GERI_GONDER(false, true),

    /** Baskanin kayda nihai onay vermesi. */
    ONAYLA(false, false),

    /** Baskanin kaydi nihai olarak reddetmesi. */
    REDDET(false, true),

    /**
     * Calisanin kaydi bir <strong>departmana</strong> gondermesi (ADR-0006).
     *
     * <p>Hedef departman istekte gelir; {@code GONDER} ile birlikte durur ve onun
     * yerine gecmez. Aciklama zorunlulugu {@code GONDER} ile aynidir.
     */
    DEPARTMANA_GONDER(false, true, false);

    private final boolean targetUserIdRequiredInRequest;
    private final boolean targetDepartmentIdRequiredInRequest;
    private final boolean commentRequired;

    /** Departman hedefi beklemeyen aksiyonlar icin kisa yol. */
    WorkflowAction(boolean targetUserIdRequiredInRequest, boolean commentRequired) {
        this(targetUserIdRequiredInRequest, false, commentRequired);
    }

    WorkflowAction(boolean targetUserIdRequiredInRequest,
                   boolean targetDepartmentIdRequiredInRequest,
                   boolean commentRequired) {
        this.targetUserIdRequiredInRequest = targetUserIdRequiredInRequest;
        this.targetDepartmentIdRequiredInRequest = targetDepartmentIdRequiredInRequest;
        this.commentRequired = commentRequired;
    }

    /**
     * Istemcinin istekte {@code targetUserId} gondermesi gerekip gerekmedigi.
     * Su an butun aksiyonlar icin {@code false}: hedefi her zaman backend cozer
     * ve istemci yine de gonderirse istek {@code WORKFLOW_TARGET_NOT_ALLOWED}
     * ile reddedilir.
     */
    public boolean isTargetUserIdRequiredInRequest() {
        return targetUserIdRequiredInRequest;
    }

    /**
     * Istemcinin istekte {@code targetDepartmentId} gondermesi gerekip gerekmedigi.
     * Yalniz {@code DEPARTMANA_GONDER} icin {@code true}.
     */
    public boolean isTargetDepartmentIdRequiredInRequest() {
        return targetDepartmentIdRequiredInRequest;
    }

    public boolean isTargetExpectedInRequest() {
        return targetUserIdRequiredInRequest || targetDepartmentIdRequiredInRequest;
    }

    /** Aksiyonun aciklama zorunlulugu. Butun geri gondermeler ve red icin {@code true}. */
    public boolean isCommentRequired() {
        return commentRequired;
    }

}
