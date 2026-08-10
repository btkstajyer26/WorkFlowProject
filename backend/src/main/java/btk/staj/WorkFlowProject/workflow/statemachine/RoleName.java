package btk.staj.WorkFlowProject.workflow.statemachine;

/**
 * Sistemdeki rol adlari. Degerler veritabanindaki {@code roles.name} kolonuyla
 * birebir ayni yazilmistir; sayisal rol ID'leri koda tasinmaz.
 *
 * <p>Not: Projedeki tek rol tipidir. rbac modulu de bu tipi kullanir; ayri bir
 * rol enum'u tanimlanmamalidir.
 */
public enum RoleName {

    CALISAN(true),
    BASKAN_YARDIMCISI(true),
    BASKAN(true),

    /** Kullanici ve rol yonetiminden sorumlu yonetim rolu; workflow aktoru degildir. */
    ADMIN(false);

    private final boolean workflowActor;

    RoleName(boolean workflowActor) {
        this.workflowActor = workflowActor;
    }

    /**
     * Bu rolun bir workflow gecisini baslatabilip baslatamayacagini belirtir.
     * {@code ADMIN} icin daima {@code false} doner.
     */
    public boolean isWorkflowActor() {
        return workflowActor;
    }
}
