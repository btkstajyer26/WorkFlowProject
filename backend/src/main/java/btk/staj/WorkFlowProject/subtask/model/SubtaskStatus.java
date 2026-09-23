package btk.staj.WorkFlowProject.subtask.model;

import java.util.Set;

/** Genel workflow motorundan bagimsiz hafif alt gorev durumlari. */
public enum SubtaskStatus {
    DEGERLENDIRME,
    ISLEM,
    ONAY,
    TAMAMLANDI,
    REDDEDILDI;

    private static final Set<SubtaskStatus> TERMINAL_STATUSES =
            Set.of(TAMAMLANDI, REDDEDILDI);

    public boolean isTerminal() {
        return TERMINAL_STATUSES.contains(this);
    }

    public static Set<SubtaskStatus> terminalStatuses() {
        return TERMINAL_STATUSES;
    }
}
