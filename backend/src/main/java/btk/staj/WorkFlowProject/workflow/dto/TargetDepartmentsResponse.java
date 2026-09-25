package btk.staj.WorkFlowProject.workflow.dto;

import java.util.List;

/** Kayit kapsamli hedef departman kesfi (APP-9 SS2); global departman listesi degildir. */
public record TargetDepartmentsResponse(List<TargetDepartmentView> departments) {
}
