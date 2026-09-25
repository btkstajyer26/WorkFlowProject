package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.department.port.DepartmentOpenUsagePort;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * {@link DepartmentOpenUsagePort}'un uygulamasi. {@code RecordRepository.hasOpenRecordsForDepartment}
 * sorgusunu tuketir - WF-8'in {@code WorkflowRoleUsageAdapter}'i ile ayni desen.
 *
 * <p>{@code Propagation.REQUIRED} (varsayilan) bilincli: cagiran
 * {@code DepartmentAdminService.update} departman satirini {@code findByIdForUpdate}
 * ile kilitlemis durumda ve bu kontrol ayni transaction icinde calismalidir.
 */
@Component
public class DepartmentOpenUsageAdapter implements DepartmentOpenUsagePort {

    private final RecordRepository records;

    public DepartmentOpenUsageAdapter(RecordRepository records) {
        this.records = Objects.requireNonNull(records, "records");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOpenRecords(int departmentId) {
        return records.hasOpenRecordsForDepartment(departmentId);
    }
}
