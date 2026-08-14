package vn.nguongocso.auth.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationType;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Custom implementation of {@link UserDetails} that stores both
 * user identity and organization-specific authorization information.
 *
 * <p>
 * Besides the standard Spring Security user information, this class
 * also exposes organization and role information used throughout the
 * application.
 * </p>
 */
public class CustomUserDetails implements UserDetails {
    private static final String ROLE_PREFIX = "ROLE_";

    private final User user;

    private final UUID userId;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final UUID organizationId;
    private final String organizationName;
    private final String organizationCode;
    private final OrganizationType organizationType;
    private final String roleCode;
    private final String roleName;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(User user, OrganizationUser orgUser, Role role) {
        this.user = user;
        this.userId = user.getUserId();
        this.username = user.getUserName();
        this.passwordHash = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.organizationId = orgUser.getOrganization().getOrganizationId();
        this.organizationName = orgUser.getOrganization().getName();
        this.organizationCode = orgUser.getOrganization().getCode();
        this.organizationType = orgUser.getOrganization().getType();
        this.roleCode = role.getCode();
        this.roleName = role.getName();
        this.authorities = List.of(
                new SimpleGrantedAuthority(ROLE_PREFIX + roleCode));
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Các getter bổ sung
    public UUID getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public OrganizationType getOrganizationType() {
        return organizationType;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

}