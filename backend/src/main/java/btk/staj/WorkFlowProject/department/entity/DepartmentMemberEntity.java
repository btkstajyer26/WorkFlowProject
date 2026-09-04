package btk.staj.WorkFlowProject.department.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "department_members")
public class DepartmentMemberEntity {

    @EmbeddedId
    private DepartmentMemberEntity.Id id;

    public DepartmentMemberEntity() {}

    public DepartmentMemberEntity(Integer departmentId, UUID userId) {
        this.id = new Id(departmentId, userId);
    }

    public Id getId() { return id; }
    public void setId(Id id) { this.id = id; }

    @Embeddable
    public static class Id implements Serializable {

        @Column(name = "department_id")
        private Integer departmentId;

        @Column(name = "user_id")
        private UUID userId;

        public Id() {}

        public Id(Integer departmentId, UUID userId) {
            this.departmentId = departmentId;
            this.userId = userId;
        }

        public Integer getDepartmentId() { return departmentId; }
        public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id)) return false;
            Id id = (Id) o;
            return Objects.equals(departmentId, id.departmentId) && Objects.equals(userId, id.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(departmentId, userId);
        }
    }
}