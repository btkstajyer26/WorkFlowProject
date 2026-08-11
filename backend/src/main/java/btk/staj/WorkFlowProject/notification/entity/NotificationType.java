package btk.staj.WorkFlowProject.enums;

public enum NotificationType {
    RECORD_SUBMITTED,    // Çalışan evrağı Başk. Yrd.'na gönderdi
    RECORD_FORWARDED,    // Başk. Yrd. evrağı Başkana iletti
    RECORD_APPROVED,     // Başkan nihai onayı verdi
    RECORD_REJECTED,     // Başkan evrağı reddetti
    RECORD_RETURNED      // Evrak düzeltme için geri gönderildi
}