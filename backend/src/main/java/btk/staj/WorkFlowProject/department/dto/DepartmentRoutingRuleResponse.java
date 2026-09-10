package btk.staj.WorkFlowProject.department.dto;

/**
 * AP-5: bir departmanda, belirli bir (durum, aksiyon) icin hangi rolun
 * islem yapmaya yetkili sayildigini tasir. Kural DB'de tekildir -
 * {@code (departmentId, fromStatusId, actionId)} en fazla bir satira sahiptir.
 */
public record DepartmentRoutingRuleResponse(Integer id,
                                            Integer departmentId,
                                            Integer fromStatusId, String fromStatus, String fromStatusDisplayName,
                                            Integer actionId, String action, String actionDisplayName,
                                            Integer targetRoleId, String targetRoleName,
                                            boolean active) {
}
