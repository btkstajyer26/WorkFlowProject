package btk.staj.WorkFlowProject.rbac;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @EmbeddedId
    private RolePermission.Id id;

    public RolePermission() {}

    public RolePermission(Integer roleId, Integer permissionId) {
        this.id = new Id(roleId, permissionId);
    }

    public Id getId() { return id; }
    public void setId(Id id) { this.id = id; }

    @Embeddable
    public static class Id implements Serializable {

        @Column(name = "role_id")
        private Integer roleId;

        @Column(name = "permission_id")
        private Integer permissionId;

        public Id() {}

        public Id(Integer roleId, Integer permissionId) {
            this.roleId = roleId;
            this.permissionId = permissionId;
        }

        public Integer getRoleId() { return roleId; }
        public void setRoleId(Integer roleId) { this.roleId = roleId; }

        public Integer getPermissionId() { return permissionId; }
        public void setPermissionId(Integer permissionId) { this.permissionId = permissionId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id)) return false;
            Id id = (Id) o;
            return Objects.equals(roleId, id.roleId) && Objects.equals(permissionId, id.permissionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleId, permissionId);
        }
    }
}