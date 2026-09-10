package btk.staj.WorkFlowProject.department.dto;

import btk.staj.WorkFlowProject.user.dto.UserResponse;

import java.util.List;

/** Bir departmanin butun uyeleri (aktif/pasif kullanici ayrimi yapmaz - liste tam). */
public record DepartmentMembersResponse(Integer departmentId,
                                        String departmentName,
                                        List<UserResponse> members) {
}
