package btk.staj.WorkFlowProject.record.entity;

import lombok.Getter;

@Getter
public enum RecordStatus {
    TASLAK("Taslak"),
    BSK_YRD_INCELEMESINDE("Başkan Yardımcısı İncelemesinde"),
    BASKAN_INCELEMESINDE("Başkan İncelemesinde"),
    DUZENLEME_BEKLIYOR("Düzenleme Bekliyor"),
    ONAYLANDI("Onaylandı"),
    REDDEDILDI("Reddedildi");

    private final String displayName;

    RecordStatus(String displayName) {
        this.displayName = displayName;
    }
}