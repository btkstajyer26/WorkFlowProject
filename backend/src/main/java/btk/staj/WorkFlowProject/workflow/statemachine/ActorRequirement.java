package btk.staj.WorkFlowProject.workflow.statemachine;

/**
 * Bir gecisi yapabilmek icin aktorun kayitla kurmasi gereken iliski.
 *
 * <p>Insan gecislerinde rol sahibi olmak tek basina yeterli degildir; aktor
 * ayrica kaydin sahibi veya guncel atanmis kullanicisi olmalidir. Otomatik
 * {@link #SYSTEM} gecisi bu insan iliskilerini aramaz, fakat gecisin rol
 * eslesmesi ayrica korunur.
 */
public enum ActorRequirement {

    /** Aktor {@code records.created_by} olmalidir. */
    CREATOR,

    /** Aktor {@code records.assigned_to} olmalidir. */
    ASSIGNEE,

    /** Aktor hem {@code created_by} hem {@code assigned_to} olmalidir. */
    CREATOR_AND_ASSIGNEE,

    /**
     * Aktor icin kayit sahipligi veya atama iliskisi aranmaz.
     *
     * <p>Bu deger rol kontrolunu atlamaz: gecis yine kendi {@code actor_role_id}
     * degeriyle eslesen bir aktor tarafindan calistirilmalidir.
     */
    SYSTEM,

    /**
     * {@link #SYSTEM} ile ayni sekilde kayit iliskisi aramaz, ama SISTEM aktoru icin
     * degil <strong>insan</strong> aktorler icin kullanilir (ADR-0010): rolu tutan
     * herhangi bir kullanici gecisi yapabilir, tipki DUZENLEME_BEKLIYOR kuyrugundaki
     * rol-geneli gorunurlukle ayni mantikta. Parent/Subtask alt akisinda bolme sonrasi
     * {@code assigned_to} bos kaldigi icin ASSIGNEE/CREATOR kurallari hic kimseyi
     * eslestiremez; KONTROL'den Baskana iletme burada bu degeri kullanir.
     */
    ROLE_ONLY;

    /**
     * Verilen iliskilerin bu gereksinimi karsilayip karsilamadigini doner.
     *
     * @param actorIsCreator  aktor kaydi olusturan kullanici mi
     * @param actorHoldsAssignment atama aktorde mi (dogrudan atanan veya
     *                             ADR-0005 sonrasi departman uzerinden yetkili)
     */
    public boolean isSatisfiedBy(boolean actorIsCreator, boolean actorHoldsAssignment) {
        return switch (this) {
            case CREATOR -> actorIsCreator;
            case ASSIGNEE -> actorHoldsAssignment;
            case CREATOR_AND_ASSIGNEE -> actorIsCreator && actorHoldsAssignment;
            case SYSTEM, ROLE_ONLY -> true;
        };
    }
}
