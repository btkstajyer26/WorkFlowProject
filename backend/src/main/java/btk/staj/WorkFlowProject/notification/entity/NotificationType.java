package btk.staj.WorkFlowProject.notification.entity;

import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;

/** Bildirimi doguran onay akisi olayinin turu. */
public enum NotificationType {

    /** Calisan evragi Bsk. Yrd.'na gonderdi (ilk gonderim veya revizyon sonrasi). */
    RECORD_SUBMITTED,

    /** Bsk. Yrd. evragi Baskana iletti. */
    RECORD_FORWARDED,

    /** Baskan nihai onayi verdi. */
    RECORD_APPROVED,

    /** Baskan evragi reddetti. */
    RECORD_REJECTED,

    /** Evrak duzeltme icin geri gonderildi. */
    RECORD_RETURNED,

    /** Parent kayit alt gorevlere ayrildi. */
    RECORD_SPLIT,

    /** Parent kaydin butun alt gorevleri sonuclandi. */
    SUBTASKS_COMPLETED,

    /** Kullaniciya yeni bir alt gorev atandi. */
    SUBTASK_ASSIGNED,

    /** Alt gorev ara durumda ilerledi. */
    SUBTASK_UPDATED,

    /** Alt gorev onaylanarak tamamlandi. */
    SUBTASK_COMPLETED,

    /** Alt gorev reddedilerek sonuclandi. */
    SUBTASK_REJECTED;

    /**
     * Her workflow aksiyonu icin bildirim davranisi tanimlidir.
     */
    public static boolean supports(WorkflowAction action) {
        return switch (action) {
            case ALT_GOREVLERE_AYIR, ALT_GOREVLER_SONUCLANDI,
                    GONDER, TEKRAR_GONDER, DEPARTMANA_GONDER, BASKANA_ILET,
                    ONAYLA, REDDET, CALISANA_GERI_GONDER,
                    BASKAN_YARDIMCISINA_GERI_GONDER -> true;
        };
    }

    public static NotificationType of(WorkflowAction action) {
        return switch (action) {
            case GONDER, TEKRAR_GONDER, DEPARTMANA_GONDER -> RECORD_SUBMITTED;
            case BASKANA_ILET -> RECORD_FORWARDED;
            case ONAYLA -> RECORD_APPROVED;
            case REDDET -> RECORD_REJECTED;
            case CALISANA_GERI_GONDER, BASKAN_YARDIMCISINA_GERI_GONDER -> RECORD_RETURNED;
            case ALT_GOREVLERE_AYIR -> RECORD_SPLIT;
            case ALT_GOREVLER_SONUCLANDI -> SUBTASKS_COMPLETED;
        };
    }
}
