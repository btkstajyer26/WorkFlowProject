package btk.staj.WorkFlowProject.workflow.dto;

/**
 * Gonderilebilecek tek bir departman (APP-9 SS2).
 *
 * <p>Yalniz kimlik ve ad tasir. Uye kimlikleri, uye sayisi, hedef rol ve hiyerarsi
 * <strong>donmez</strong> (SS2.3): gonderen kullanicinin bunlara ihtiyaci yoktur ve
 * organizasyon dizini bu uctan acilmaz.
 */
public record TargetDepartmentView(int id, String name) {
}
