package btk.staj.WorkFlowProject.user.dto;

/**
 * GET /api/admin/users icin sorgu parametreleri. Alanlar Spring Data Web
 * tarafindan query string'den otomatik baglanir (q, role, active).
 */
public class AdminUserSearchCriteria {

    private String q;

    private String role;

    private Boolean active;

    public AdminUserSearchCriteria() {
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}