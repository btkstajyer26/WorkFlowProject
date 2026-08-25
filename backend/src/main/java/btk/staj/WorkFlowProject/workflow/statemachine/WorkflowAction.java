package btk.staj.WorkFlowProject.workflow.statemachine;

/**
 * Kayit uzerinde gerceklestirilebilecek workflow aksiyonlari.
 *
 * <p>Istemci hedef durumu dogrudan gondermez; yalnizca aksiyonu gonderir, yeni
 * durumu backend hesaplar.
 *
 * <p>Her aksiyon iki farkli hedef bilgisi tasir:
 * <ul>
 *   <li>{@link #isTargetUserIdRequiredInRequest()} &ndash; istemcinin istekte
 *       {@code targetUserId} gondermesi gerekip gerekmedigi;</li>
 *   <li>{@link #getExpectedTargetRole()} &ndash; servis tarafindan cozulen hedef
 *       kullanicinin hangi rolde olmasi gerektigi.</li>
 * </ul>
 * Ikisi ayni sey degildir: ornegin {@code BASKANA_ILET} icin istemci hedef
 * gondermez (sistemde tek Baskan vardir, backend bulur) ama cozulen hedefin
 * {@code BASKAN} rolunde ve aktif olmasi dogrulanir.
 *
 * <p>Su an <strong>hicbir</strong> aksiyon istemciden hedef beklemiyor: hedefi
 * her zaman backend cozer. Ilk bayrak yine de tasiniyor, cunku istemci yine de
 * {@code targetUserId} gonderirse istek bu bayrak uzerinden reddedilir.
 */
public enum WorkflowAction {

    /** Calisanin taslak kaydi Baskan Yardimcisina gondermesi. Hedef: sistemdeki tek yardimci. */
    GONDER(false, false, RoleName.BASKAN_YARDIMCISI),

    /** Calisanin duzelttigi kaydi yeniden gondermesi. Hedef: sistemdeki tek yardimci. */
    TEKRAR_GONDER(false, false, RoleName.BASKAN_YARDIMCISI),

    /** Baskan Yardimcisinin kaydi Baskana iletmesi. Hedef: sistemdeki tek Baskan. */
    BASKANA_ILET(false, false, RoleName.BASKAN),

    /** Kaydin olusturan Calisana geri gonderilmesi. Hedef: {@code records.created_by}. */
    CALISANA_GERI_GONDER(false, true, RoleName.CALISAN),

    /** Baskanin kaydi ileten yardimciya geri gondermesi. Hedef: {@code records.last_deputy_id}. */
    BASKAN_YARDIMCISINA_GERI_GONDER(false, true, RoleName.BASKAN_YARDIMCISI),

    /** Baskanin kayda nihai onay vermesi. */
    ONAYLA(false, false, null),

    /** Baskanin kaydi nihai olarak reddetmesi. */
    REDDET(false, true, null);

    private final boolean targetUserIdRequiredInRequest;
    private final boolean commentRequired;
    private final RoleName expectedTargetRole;

    WorkflowAction(boolean targetUserIdRequiredInRequest,
                   boolean commentRequired,
                   RoleName expectedTargetRole) {
        this.targetUserIdRequiredInRequest = targetUserIdRequiredInRequest;
        this.commentRequired = commentRequired;
        this.expectedTargetRole = expectedTargetRole;
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

    /** Aksiyonun aciklama zorunlulugu. Butun geri gondermeler ve red icin {@code true}. */
    public boolean isCommentRequired() {
        return commentRequired;
    }

    /**
     * Cozulen hedef kullanicinin tasimasi gereken rol. Hedef kullanici
     * gerektirmeyen aksiyonlarda ({@code ONAYLA}, {@code REDDET}) {@code null} doner.
     */
    public RoleName getExpectedTargetRole() {
        return expectedTargetRole;
    }

    /** Bu aksiyonun bir hedef kullaniciya ihtiyac duyup duymadigi. */
    public boolean requiresTargetUser() {
        return expectedTargetRole != null;
    }
}
