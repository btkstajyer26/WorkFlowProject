package btk.staj.WorkFlowProject.rbac;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    // Yerlesik rolun degismez semantik anahtari. Rol adi (name) kullaniciya
    // gorunur ve degistirilebilir; system_key asla degismez. bkz.
    // DB_1_VERI_MODELI_SOZLESMESI.md SS6.1 / SS4.
    @Column(name = "system_key", unique = true, length = 50)
    private String systemKey;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "is_workflow_actor", nullable = false)
    private boolean workflowActor;

    // NULL = sinirsiz. Dolu ise sayima yalniz aktif kullanicilar girer;
    // bu sinir DB CHECK'i ile degil, uygulama katmaninda transaction
    // icinde kilitli sekilde dogrulanir (sozlesme SS6.1).
    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public Role() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSystemKey() { return systemKey; }
    public void setSystemKey(String systemKey) { this.systemKey = systemKey; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public boolean isWorkflowActor() { return workflowActor; }
    public void setWorkflowActor(boolean workflowActor) { this.workflowActor = workflowActor; }

    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}