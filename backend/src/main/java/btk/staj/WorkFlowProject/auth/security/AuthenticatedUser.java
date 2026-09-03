package btk.staj.WorkFlowProject.auth.security;

import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;

public class AuthenticatedUser implements UserDetails {

    private final User user;
    private final Set<String> permissionCodes;
    private final List<SimpleGrantedAuthority> authorities;

    public AuthenticatedUser(User user, Collection<String> permissionCodes) {
        this.user = Objects.requireNonNull(user, "user");
        this.permissionCodes = user.getRole() != null && user.getRole().isActive()
                ? Set.copyOf(permissionCodes) : Set.of();
        this.authorities = this.permissionCodes.stream().sorted()
                .map(SimpleGrantedAuthority::new).toList();
    }

    public UUID getId() {
        return user.getId();
    }

    public User getUser() {
        return user;
    }

    public String getRoleName() {
        return user.getRole().getName();
    }
    public Integer getRoleId() {
        return user.getRole().getId();
    }

    public Set<String> getPermissionCodes() { return permissionCodes; }

    public String getSystemKey() { return user.getRole().getSystemKey(); }

    public RoleName getLegacyRole() {
        return SystemRoleKey.from(getSystemKey())
                .map(SystemRoleKey::legacyRole)
                .orElseThrow(() -> new ForbiddenException(
                        "Bu rol için kayıt görünürlüğü henüz yapılandırılmadı"));
    }

    public boolean isWorkflowActor() {
        return isEnabled() && user.getRole().isWorkflowActor();
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() { return user.getPasswordHash(); }

    @Override
    public String getUsername() { return user.getEmail(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return isEnabled(); }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return user.isActive() && user.getRole() != null && user.getRole().isActive();
    }
}
