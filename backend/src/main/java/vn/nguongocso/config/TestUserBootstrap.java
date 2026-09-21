package vn.nguongocso.config;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/**
 * Thành phần khởi tạo tài khoản, vai trò và token cho môi trường kiểm thử runtime.
 */
@Slf4j
@Component
@Profile("runtime-test")
@RequiredArgsConstructor
public class TestUserBootstrap {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Dữ liệu đóng gói thông tin người dùng và token phục vụ kiểm thử runtime (internal carrier).
     */
    @Getter
    @AllArgsConstructor
    public static class BootstrapUserData {
        private final User managerUser;
        private final User recorderUser;
        private final String managerToken;
        private final String recorderToken;
    }

    /**
     * Khởi tạo các vai trò và người dùng mẫu, gán vào tổ chức và sinh access token.
     *
     * @param org tổ chức mẫu
     * @return đối tượng chứa user và token đã sinh
     */
    public BootstrapUserData bootstrapUsers(Organization org) {
        Role roleManager = getOrCreateRole("VT-02", "Quản lý Hợp tác xã");
        Role roleRecorder = getOrCreateRole("VT-03", "Người ghi sự kiện nông nghiệp");

        User managerUser = getOrCreateUser(
                "cooperative_manager",
                "manager@nguongocso.vn",
                "Quản Lý Hợp Tác Xã Demo",
                "password123"
        );
        OrganizationUser managerOrgUser = getOrCreateMembership(managerUser, org, roleManager);
        CustomUserDetails managerDetails = new CustomUserDetails(managerUser, managerOrgUser, roleManager);
        String managerToken = jwtTokenProvider.generateAccessToken(managerDetails);

        User recorderUser = getOrCreateUser(
                "event_recorder",
                "recorder@nguongocso.vn",
                "Người Ghi Sự Kiện Demo",
                "password123"
        );
        OrganizationUser recorderOrgUser = getOrCreateMembership(recorderUser, org, roleRecorder);
        CustomUserDetails recorderDetails = new CustomUserDetails(recorderUser, recorderOrgUser, roleRecorder);
        String recorderToken = jwtTokenProvider.generateAccessToken(recorderDetails);

        return new BootstrapUserData(managerUser, recorderUser, managerToken, recorderToken);
    }

    private Role getOrCreateRole(String code, String name) {
        return roleRepository.findByCode(code).orElseGet(() -> {
            Role r = new Role();
            r.setCode(code);
            r.setName(name);
            return roleRepository.save(r);
        });
    }

    private User getOrCreateUser(String username, String email, String fullName, String rawPassword) {
        return userRepository.findByUserName(username).orElseGet(() -> {
            User u = new User();
            u.setUserName(username);
            u.setEmail(email);
            u.setFullName(fullName);
            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });
    }

    private OrganizationUser getOrCreateMembership(User user, Organization org, Role role) {
        return organizationUserRepository
                .findByUser_UserIdAndOrganization_OrganizationId(user.getUserId(), org.getOrganizationId())
                .orElseGet(() -> {
                    OrganizationUser ou = new OrganizationUser();
                    ou.setId(UUID.randomUUID());
                    ou.setUser(user);
                    ou.setOrganization(org);
                    ou.setRole(role);
                    ou.setStatus(OrganizationUserStatus.ACTIVE);
                    ou.setJoinedAt(LocalDateTime.now());
                    return organizationUserRepository.save(ou);
                });
    }
}
