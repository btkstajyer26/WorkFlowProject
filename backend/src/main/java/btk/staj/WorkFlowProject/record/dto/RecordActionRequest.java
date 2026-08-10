package btk.staj.WorkFlowProject.record.dto;

/**
 * Onaylama, reddetme, geri gönderme gibi workflow aksiyonlarında kullanılır.
 * note: Geri gönderme (return) işlemlerinde şartname gereği ZORUNLUDUR,
 * onaylamada opsiyoneldir (Service katmanında kontrol edilir).
 */
public record RecordActionRequest(
    String note
) {}