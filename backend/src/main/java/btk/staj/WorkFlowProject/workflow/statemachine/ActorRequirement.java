package btk.staj.WorkFlowProject.workflow.statemachine;

/**
 * Bir gecisi yapabilmek icin aktorun kayitla kurmasi gereken iliski.
 *
 * <p>Rol sahibi olmak tek basina yeterli degildir; aktor ayrica kaydin sahibi
 * veya guncel atanmis kullanicisi olmalidir.
 */
public enum ActorRequirement {

    /** Aktor {@code records.created_by} olmalidir. */
    CREATOR,

    /** Aktor {@code records.assigned_to} olmalidir. */
    ASSIGNEE,

    /** Aktor hem {@code created_by} hem {@code assigned_to} olmalidir. */
    CREATOR_AND_ASSIGNEE;

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
        };
    }
}
