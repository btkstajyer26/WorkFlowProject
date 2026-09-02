package btk.staj.WorkFlowProject.workflow.statemachine;

/**
 * Bir gecis basarili oldugunda kaydin kime atanacaginin nasil cozulecegi.
 *
 * <p>Hedef, aksiyonun degil <strong>gecisin</strong> ozelligidir: ayni aksiyon farkli
 * gecislerde farkli hedefe gidebilir. Ornegin {@code CALISANA_GERI_GONDER} hem Baskan
 * Yardimcisinin hem Baskanin kullandigi iki ayri gecis satirinda bulunur.
 *
 * <p>Degerler {@code workflow_transitions.target_strategy} kolonundaki
 * {@code chk_transition_target_strategy} kisitiyla ve DB-1 sozlesmesi SS7.2 ile birebir
 * aynidir. Kisit bes degere izin verdigi icin bes degerin tamami burada tanimlidir;
 * eksik birakilan bir deger, gecerli bir veritabani satirinda uygulamanin acilmasini
 * engellerdi.
 *
 * <p>Departman calismasi icin ayrilan {@code DEPARTMENT}, {@code DEPARTMENT_ROLE},
 * {@code PARENT_DEPARTMENT} ve {@code EXPLICIT_USER} kavramlari bu iterasyonda ne DB
 * kisitinda ne burada bulunur (DB-1 SS7.2).
 */
public enum TargetStrategy {

    /** Hedef kullanici yoktur; basarili gecis {@code assigned_to = NULL} yazar. */
    NONE,

    /**
     * Hedef, gecisin {@code expected_target_role_id} rolundeki <strong>tek aktif</strong>
     * kullanicidir; backend cozer, istemciden alinmaz.
     */
    ROLE,

    /** Hedef {@code records.created_by} kullanicisidir. */
    CREATOR,

    /** Hedef, gecis oncesindeki {@code records.assigned_to} kullanicisidir. */
    CURRENT_ASSIGNEE,

    /**
     * Hedef, kaydi Baskana ileten son Baskan Yardimcisidir
     * ({@code records.last_deputy_id}).
     *
     * <p>Genel bir audit gecmisi taramasi <strong>degildir</strong>: DB-1 SS7.2 bu
     * primitive'i acikca mevcut {@code last_deputy_id} semantigiyle sinirlar.
     */
    PREVIOUS_ACTOR
}
