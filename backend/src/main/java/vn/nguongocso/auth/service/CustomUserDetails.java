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

/** Thông tin chi tiết người dùng và phân quyền tổ chức cho Spring Security. */
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
    private final UUID organizationProvinceId;
    private final UUID organizationCommuneId;
    private final String roleCode;
    private final String roleName;
    private final List<GrantedAuthority> authorities;

    /** Khởi tạo CustomUserDetails từ thông tin người dùng, tổ chức và vai trò. */
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
        this.organizationProvinceId = orgUser.getOrganization() != null && orgUser.getOrganization().getProvince() != null
                ? orgUser.getOrganization().getProvince().getId()
                : null;
        this.organizationCommuneId = orgUser.getOrganization() != null && orgUser.getOrganization().getCommune() != null
                ? orgUser.getOrganization().getCommune().getId()
                : null;
        this.roleCode = role.getCode();
        this.roleName = role.getName();
        this.authorities = List.of(
                new SimpleGrantedAuthority(ROLE_PREFIX + roleCode));
    }

    /** Lấy thực thể User gốc. */
    public User getUser() {
        return user;
    }

    /** Lấy danh sách quyền hạn của người dùng. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /** Lấy mật khẩu đã băm của người dùng. */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** Lấy tên đăng nhập của người dùng. */
    @Override
    public String getUsername() {
        return username;
    }

    /** Kiểm tra tài khoản chưa hết hạn. */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Kiểm tra tài khoản không bị khóa (trạng thái ACTIVE). */
    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    /** Kiểm tra thông tin xác thực (mật khẩu) chưa hết hạn. */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Kiểm tra tài khoản có đang hoạt động hay không. */
    @Override
    public boolean isEnabled() {
        return true;
    }

    // Các getter bổ sung
    /** Lấy ID của người dùng. */
    public UUID getUserId() {
        return userId;
    }

    /** Lấy họ và tên của người dùng. */
    public String getFullName() {
        return fullName;
    }

    /** Lấy ID của tổ chức trực thuộc. */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /** Lấy tên của tổ chức trực thuộc. */
    public String getOrganizationName() {
        return organizationName;
    }

    /** Lấy mã của tổ chức trực thuộc. */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /** Lấy loại hình của tổ chức trực thuộc. */
    public OrganizationType getOrganizationType() {
        return organizationType;
    }

    /** Lấy mã vai trò của người dùng trong tổ chức. */
    public String getRoleCode() {
        return roleCode;
    }

    /** Lấy tên hiển thị vai trò của người dùng. */
    public String getRoleName() {
        return roleName;
    }

    /** Lấy số điện thoại của người dùng. */
    public String getPhone() {
        return user != null ? user.getPhone() : null;
    }

    /** Lấy địa chỉ email của người dùng. */
    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }

    /** Lấy ID tỉnh/thành phố của tổ chức trực thuộc. */
    public UUID getOrganizationProvinceId() {
        return organizationProvinceId;
    }

    /** Lấy ID xã/phường của tổ chức trực thuộc. */
    public UUID getOrganizationCommuneId() {
        return organizationCommuneId;
    }

}