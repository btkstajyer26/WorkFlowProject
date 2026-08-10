package btk.staj.WorkFlowProject.rbac;

/**
 * Sistemdeki rol adlari. Degerler veritabanindaki {@code roles.name} kolonuyla
 * birebir ayni yazilmistir.
 */
public enum RoleName {

    CALISAN,
    BASKAN_YARDIMCISI,
    BASKAN,

    /** Kullanici ve rol yonetiminden sorumlu sistem yoneticisi; is akisi aktoru degildir. */
    ADMIN
}
