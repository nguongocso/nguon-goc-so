package vn.nguongocso.organization.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.dto.request.AddMemberRequest;
import vn.nguongocso.auth.dto.request.AssignRoleRequest;
import vn.nguongocso.auth.dto.request.DeactivateMemberRequest;
import vn.nguongocso.auth.dto.request.ReactivateMemberRequest;
import vn.nguongocso.auth.dto.response.OrganizationUserResponse;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.permission.repository.OrganizationRolePermissionRepository;

/** Service quản lý thành viên của tổ chức. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationMemberService {
    private final OrganizationUserRepository orgUserRepository;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final OrganizationRepository organizationRepository;

    private final PasswordEncoder passwordEncoder;

    private final OrganizationRolePermissionRepository orgRolePermissionRepository;

    private final ApplicationEventPublisher eventPublisher;

    private UUID getCurrentOrganizationId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("Chưa đăng nhập");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getOrganizationId();
    }

    private String getCurrentRoleCode() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getRoleCode();
    }

    private Organization getCurrentOrganization() {
        UUID orgId = getCurrentOrganizationId();
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException("Tổ chức không tồn tại"));
    }

    /** Kiểm tra vai trò hợp lệ có thể gán. */
    private void validateAssignableRole(String currentRoleCode, Role targetRole) {
        if (RoleCode.ORG_MANAGER.equals(currentRoleCode)
                && !RoleCode.EVENT_RECORDER.equals(targetRole.getCode())) {
            throw new BusinessException("Quản lý hợp tác xã chỉ được cấp vai trò Người ghi sự kiện");
        }
    }

    /** Lấy danh sách thành viên của tổ chức hiện tại theo trạng thái membership. */
    public List<OrganizationUserResponse> getMembersOfCurrentOrganization(String status) {
        UUID orgId = getCurrentOrganizationId();
        List<OrganizationUser> orgUsers;

        if (status == null) {
            orgUsers = orgUserRepository
                    .findByOrganization_OrganizationIdAndStatus(orgId, OrganizationUserStatus.ACTIVE);
        } else if (status.isBlank()) {
            orgUsers = orgUserRepository.findByOrganization_OrganizationId(orgId);
        } else {
            orgUsers = orgUserRepository
                    .findByOrganization_OrganizationIdAndStatus(orgId, parseMembershipStatus(status));
        }

        return orgUsers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private OrganizationUserStatus parseMembershipStatus(String status) {
        try {
            return OrganizationUserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái thành viên không hợp lệ: " + status);
        }
    }

    /** Gán vai trò mới cho một thành viên trong tổ chức hiện tại. */
    @Transactional
    public OrganizationUserResponse assignRole(AssignRoleRequest request) {
        UUID orgId = getCurrentOrganizationId();
        String currentRoleCode = getCurrentRoleCode();

        OrganizationUser orgUser = orgUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(orgId, request.getUserId())
                .orElseThrow(() -> new BusinessException("Thành viên không thuộc tổ chức này"));

        Role newRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò không tồn tại"));

        if (orgUser.getStatus() != OrganizationUserStatus.ACTIVE) {
            throw new BusinessException("Thành viên đã bị vô hiệu hóa. Vui lòng kích hoạt lại trước khi cấp quyền");
        }

        validateAssignableRole(currentRoleCode, newRole);

        if (RoleCode.ADMIN.equals(newRole.getCode())
                && !RoleCode.ADMIN.equals(currentRoleCode)) {
            throw new BusinessException("Quản lý HTX không thể gán vai trò Quản trị viên nền tảng");
        }

        // Chuyển quyền quản lý cũ nếu role mới là VT-02
        if (RoleCode.ORG_MANAGER.equals(newRole.getCode())) {
            OrganizationUser currentManager = orgUserRepository
                    .findByOrganization_OrganizationIdAndRole_Code(orgId, RoleCode.ORG_MANAGER)
                    .filter(m -> !m.getUser().getUserId().equals(request.getUserId()))
                    .orElse(null);

            if (currentManager != null) {
                Role vt03Role = roleRepository.findByCode(RoleCode.EVENT_RECORDER)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role VT-03"));
                currentManager.setRole(vt03Role);
                orgUserRepository.save(currentManager);
                log.info("Hạ quản lý cũ {} xuống VT-03", currentManager.getUser().getFullName());
            }
        }

        orgUser.setRole(newRole);
        orgUser = orgUserRepository.save(orgUser);

        CustomUserDetails currentUser = getCurrentUser();
        User targetUser = orgUser.getUser();
        publishActivityLog(
                currentUser,
                "ASSIGN_ROLE",
                "Gán vai trò " + newRole.getName() + " cho " + targetUser.getFullName(),
                "OrganizationUser",
                orgUser.getId().toString());

        log.info("Gán role thành công: userId={}, orgId={}, newRole={}", request.getUserId(), orgId, newRole.getCode());
        return toResponse(orgUser);
    }

    /** Thêm thành viên mới vào tổ chức hiện tại. */
    @Transactional
    public OrganizationUserResponse addMember(AddMemberRequest request) {
        UUID orgId = getCurrentOrganizationId();
        Organization org = getCurrentOrganization();

        if (userRepository.findByUserName(request.getUsername()).isPresent()) {
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email đã tồn tại");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò không tồn tại"));

        String currentRoleCode = getCurrentRoleCode();
        validateAssignableRole(currentRoleCode, role);

        if (RoleCode.ADMIN.equals(role.getCode())
                && !RoleCode.ADMIN.equals(currentRoleCode)) {
            throw new BusinessException("Quản lý HTX không thể tạo tài khoản admin");
        }

        User newUser = new User();
        newUser.setUserId(UUID.randomUUID());
        newUser.setUserName(request.getUsername());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setFullName(request.getFullName());
        newUser.setPhone(request.getPhone());
        newUser.setEmail(request.getEmail());
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(newUser);

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setId(UUID.randomUUID());
        orgUser.setOrganization(org);
        orgUser.setUser(newUser);
        orgUser.setRole(role);
        orgUser.setJoinedAt(LocalDateTime.now());
        orgUser.setStatus(OrganizationUserStatus.ACTIVE);
        orgUserRepository.save(orgUser);

        CustomUserDetails currentUser = getCurrentUser();
        publishActivityLog(
                currentUser,
                "CREATE",
                "Thêm thành viên " + newUser.getFullName() + " vào tổ chức",
                "OrganizationUser",
                orgUser.getId().toString());

        log.info("Thêm thành viên thành công: userId={}, orgId={}, role={}",
                newUser.getUserId(), orgId, role.getCode());

        return toResponse(orgUser);
    }

    /** Vô hiệu hóa thành viên của tổ chức hiện tại. */
    @Transactional
    public OrganizationUserResponse deactivateMember(UUID userId, DeactivateMemberRequest request) {
        UUID orgId = getCurrentOrganizationId();
        CustomUserDetails currentUser = getCurrentUser();

        validateNotSelfDeactivation(currentUser.getUserId(), userId);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại"));

        OrganizationUser membership = orgUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(orgId, userId)
                .orElseThrow(() -> new BusinessException("Thành viên không thuộc tổ chức này"));

        if (membership.getStatus() != OrganizationUserStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "Thành viên đã ngừng hoạt động");
        }

        validateDeactivationScope(getCurrentRoleCode(), membership);
        validateNotLastActiveManager(orgId, membership);

        membership.setStatus(OrganizationUserStatus.INACTIVE);
        membership = orgUserRepository.save(membership);

        publishActivityLog(currentUser, "DEACTIVATE",
                "Vô hiệu hóa thành viên " + targetUser.getFullName() + " (" + targetUser.getUserName()
                        + "). Lý do: " + request.getReason(),
                "OrganizationUser",
                membership.getId().toString());

        log.info("Vô hiệu hóa thành viên thành công: userId={}, orgId={}, actor={}",
                userId, orgId, currentUser.getUserId());

        return toResponse(membership);
    }

    /** Kích hoạt lại thành viên đã ngừng hoạt động. */
    @Transactional
    public OrganizationUserResponse reactivateMember(UUID userId, ReactivateMemberRequest request) {
        UUID orgId = getCurrentOrganizationId();
        CustomUserDetails currentUser = getCurrentUser();

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại"));

        OrganizationUser membership = orgUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(orgId, userId)
                .orElseThrow(() -> new BusinessException("Thành viên không thuộc tổ chức này"));

        if (membership.getStatus() != OrganizationUserStatus.INACTIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "Thành viên đang hoạt động, không thể kích hoạt lại");
        }

        membership.setStatus(OrganizationUserStatus.ACTIVE);
        membership = orgUserRepository.save(membership);

        publishActivityLog(currentUser, "REACTIVATE",
                "Kích hoạt lại thành viên " + targetUser.getFullName() + " (" + targetUser.getUserName()
                        + "). Lý do: " + request.getReason(),
                "OrganizationUser",
                membership.getId().toString());

        log.info("Kích hoạt lại thành viên thành công: userId={}, orgId={}, actor={}",
                userId, orgId, currentUser.getUserId());

        return toResponse(membership);
    }

    /** Không cho phép tự vô hiệu hóa chính mình. */
    private void validateNotSelfDeactivation(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException("Không thể tự vô hiệu hóa tài khoản của chính mình");
        }
    }

    /** Kiểm tra quyền hạn khi vô hiệu hóa thành viên. */
    private void validateDeactivationScope(String currentRoleCode, OrganizationUser targetMembership) {
        if (!RoleCode.ADMIN.equals(currentRoleCode) && !RoleCode.ORG_MANAGER.equals(currentRoleCode)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện chức năng này");
        }
        if (RoleCode.ORG_MANAGER.equals(currentRoleCode)
                && !RoleCode.EVENT_RECORDER.equals(targetMembership.getRole().getCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Quản lý hợp tác xã không thể vô hiệu hóa vai trò này");
        }
    }

    /** Không cho vô hiệu hóa quản lý duy nhất còn lại của tổ chức. */
    private void validateNotLastActiveManager(UUID orgId, OrganizationUser targetMembership) {
        if (!RoleCode.ORG_MANAGER.equals(targetMembership.getRole().getCode())) {
            return;
        }
        long activeManagers = orgUserRepository.countByOrganization_OrganizationIdAndStatusAndRole_Code(
                orgId, OrganizationUserStatus.ACTIVE, RoleCode.ORG_MANAGER);
        if (activeManagers <= 1) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Không thể vô hiệu hóa quản lý duy nhất còn lại của tổ chức");
        }
    }

    private CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("Chưa đăng nhập");
        }
        return (CustomUserDetails) auth.getPrincipal();
    }

    private void publishActivityLog(CustomUserDetails currentUser, String action, String description, String entityType,
            String entityId) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private OrganizationUserResponse toResponse(OrganizationUser orgUser) {
        User user = orgUser.getUser();
        Role role = orgUser.getRole();

        return OrganizationUserResponse.builder()
                .id(orgUser.getId())
                .organizationId(orgUser.getOrganization().getOrganizationId())
                .userId(user.getUserId())
                .username(user.getUserName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleId(role.getRoleId())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .status(user.getStatus())
                .membershipStatus(orgUser.getStatus())
                .joinedAt(orgUser.getJoinedAt())
                .build();
    }
}
