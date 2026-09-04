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
    RECORD_RETURNED;

    public static NotificationType of(WorkflowAction action) {
        return switch (action) {
            case GONDER, TEKRAR_GONDER, DEPARTMANA_GONDER -> RECORD_SUBMITTED;
            case BASKANA_ILET -> RECORD_FORWARDED;
            case ONAYLA -> RECORD_APPROVED;
            case REDDET -> RECORD_REJECTED;
            case CALISANA_GERI_GONDER, BASKAN_YARDIMCISINA_GERI_GONDER -> RECORD_RETURNED;
        };
    }
}
