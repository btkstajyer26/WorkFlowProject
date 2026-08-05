package btk.staj.WorkFlowProject.workflow.statemachine;

/**
 * Kaydin yasam dongusundeki durumlar. Degerler veritabanindaki
 * {@code statuses.name} kolonuyla birebir ayni yazilmistir; sayisal durum
 * ID'leri koda tasinmaz.
 *
 * <p>{@link #isTerminal()} ve {@link #isEditableByCreator()} metotlari record ve
 * attachment modulleri tarafindan da kullanilmak uzere aciktir. Kilitleme kurali
 * her modulde ayri ayri yazilmamalidir.
 */
public enum RecordStatus {

    TASLAK(false, true),
    BSK_YRD_INCELEMESINDE(false, false),
    BASKAN_INCELEMESINDE(false, false),
    DUZENLEME_BEKLIYOR(false, true),
    ONAYLANDI(true, false),
    REDDEDILDI(true, false);

    private final boolean terminal;
    private final boolean editableByCreator;

    RecordStatus(boolean terminal, boolean editableByCreator) {
        this.terminal = terminal;
        this.editableByCreator = editableByCreator;
    }

    /**
     * Surecin tamamlandigi ve baska gecise izin verilmeyen durumlari belirtir.
     * Terminal kayit kilitlidir: yeni workflow aksiyonu uygulanamaz, basligi,
     * aciklamasi, kategorisi ve ekleri degistirilemez.
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Kaydi olusturan Calisanin kayit icerigini duzenleyebilecegi durumlari
     * belirtir. Diger roller icin duzenleme her durumda kapalidir.
     */
    public boolean isEditableByCreator() {
        return editableByCreator;
    }
}
