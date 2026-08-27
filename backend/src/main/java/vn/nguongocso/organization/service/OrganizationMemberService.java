package vn.nguongocso.organization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.dto.request.AddMemberRequest;
import vn.nguongocso.auth.dto.request.AssignRoleRequest;
import vn.nguongocso.auth.dto.request.DeactivateMemberRequest;
import vn.nguongocso.auth.dto.request.ReactivateMemberRequest;
import vn.nguongocso.auth.dto.response.MemberLotSummary;
import vn.nguongocso.auth.dto.response.OrganizationUserResponse;
import vn.nguongocso.auth.dto.response.ReplacementCandidateResponse;
import vn.nguongocso.auth.dto.response.ReplacementRequiredError;
import vn.nguongocso.auth.dto.response.UnfinishedLotsResponse;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.LotAssignment;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.service.LotAssignmentService;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.permission.repository.OrganizationRolePermissionRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý thành viên của tổ chức.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationMemberService {

    private final OrganizationUserRepository orgUserRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final LotAssignmentService lotAssignmentService;
    private final OrganizationRolePermissionRepository orgRolePermissionRepository;

    private final ApplicationEventPublisher eventPublisher;

    // helper
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

    /**
     * VT-02 chỉ được cấp vai trò Người ghi sự kiện cho thành viên.
     * VT-01 vẫn được giữ nguyên quyền quản trị hiện tại.
     */
    private void validateAssignableRole(String currentRoleCode, Role targetRole) {
        if (RoleCode.ORG_MANAGER.equals(currentRoleCode)
                && !RoleCode.EVENT_RECORDER.equals(targetRole.getCode())) {
            throw new BusinessException(
                    "Quản lý hợp tác xã chỉ được cấp vai trò Người ghi sự kiện");
        }
    }

    // business methods
    /**
     * Lấy danh sách thành viên của tổ chức hiện tại theo trạng thái membership.
     *
     * <p>
     * {@code status} nhận {@code ACTIVE}/{@code INACTIVE}; bỏ qua tham số
     * thì mặc định trả về {@code ACTIVE} (giữ hành vi cũ), truyền giá trị
     * rỗng thì trả về tất cả phục vụ màn hình kích hoạt lại thành viên.
     * </p>
     */
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

    /**
     * Precheck các lô chưa hoàn thành đang phân công cho thành viên
     * trước khi vô hiệu hóa (QTN-32 mục 7).
     */
    public UnfinishedLotsResponse getUnfinishedLotsOfMember(UUID userId) {
        OrganizationUser membership = loadMembershipInCurrentOrganization(userId);

        List<LotAssignment> assignments = lotAssignmentService.findUnfinishedAssignments(
                getCurrentOrganizationId(), membership.getUser().getUserId());

        List<MemberLotSummary> lots = assignments.stream()
                .map(this::toLotSummary)
                .collect(Collectors.toList());

        return UnfinishedLotsResponse.builder()
                .userId(membership.getUser().getUserId())
                .hasUnfinishedLots(!lots.isEmpty())
                .total(lots.size())
                .replacementRequired(!lots.isEmpty())
                .lots(lots)
                .build();
    }

    /**
     * Danh sách thành viên đủ điều kiện thay thế thành viên sắp bị vô hiệu
     * hóa: membership ACTIVE cùng tổ chức, khác thành viên bị vô hiệu hóa,
     * tài khoản không bị khóa toàn cục và có quyền ghi sự kiện
     * (vai trò VT-03 hoặc được cấp quyền {@code chain_event:CREATE}).
     *
     * @param userId  ID thành viên sắp bị vô hiệu hóa
     * @param lotId   nếu truyền, chỉ trả về ứng viên cho lô đó
     * @param keyword từ khóa tìm theo tên/username, không phân biệt hoa thường
     */
    public List<ReplacementCandidateResponse> getReplacementCandidates(UUID userId, UUID lotId, String keyword) {
        UUID orgId = getCurrentOrganizationId();
        OrganizationUser deactivating = loadMembershipInCurrentOrganization(userId);

        List<UUID> pendingLotIds = lotAssignmentService
                .findUnfinishedAssignments(orgId, deactivating.getUser().getUserId())
                .stream()
                .map(assignment -> assignment.getProductionLot().getId())
                .filter(lotIds -> lotId == null || lotId.equals(lotIds))
                .distinct()
                .collect(Collectors.toList());

        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);

        return orgUserRepository
                .findByOrganization_OrganizationIdAndStatus(orgId, OrganizationUserStatus.ACTIVE)
                .stream()
                .filter(member -> !member.getUser().getUserId().equals(userId))
                .filter(member -> member.getUser().getStatus() == UserStatus.ACTIVE)
                .filter(member -> hasEventRecordingPermission(orgId, member.getRole()))
                .filter(member -> matchesKeyword(member, normalizedKeyword))
                .map(member -> toReplacementCandidate(member, pendingLotIds))
                .collect(Collectors.toList());
    }

    private boolean matchesKeyword(OrganizationUser member, String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.isEmpty()) {
            return true;
        }
        String fullName = member.getUser().getFullName();
        String username = member.getUser().getUserName();
        return (fullName != null && fullName.toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                || (username != null && username.toLowerCase(Locale.ROOT).contains(normalizedKeyword));
    }

    private ReplacementCandidateResponse toReplacementCandidate(OrganizationUser member, List<UUID> pendingLotIds) {
        User user = member.getUser();
        Role role = member.getRole();
        return ReplacementCandidateResponse.builder()
                .userId(user.getUserId())
                .username(user.getUserName())
                .fullName(user.getFullName())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .eligibleLotIds(List.copyOf(pendingLotIds))
                .build();
    }

    /**
     * Kiểm tra vai trò có quyền ghi sự kiện trong tổ chức:
     * mặc định là VT-03, hoặc vai trò được cấu hình quyền
     * {@code chain_event:CREATE} theo phân quyền theo tổ chức (QTN-01).
     */
    private boolean hasEventRecordingPermission(UUID orgId, Role role) {
        if (RoleCode.EVENT_RECORDER.equals(role.getCode())) {
            return true;
        }
        return orgRolePermissionRepository
                .findByOrganization_OrganizationIdAndRole_RoleId(orgId, role.getRoleId())
                .stream()
                .filter(config -> Boolean.TRUE.equals(config.getEnabled()))
                .anyMatch(config -> "chain_event".equals(config.getPermission().getResource())
                        && "CREATE".equals(config.getPermission().getAction()));
    }

    /**
     * Gán vai trò mới cho một thành viên trong tổ chức hiện tại
     */
    @Transactional
    public OrganizationUserResponse assignRole(AssignRoleRequest request) {
        UUID orgId = getCurrentOrganizationId();
        String currentRoleCode = getCurrentRoleCode();

        OrganizationUser orgUser = orgUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(orgId, request.getUserId())
                .orElseThrow(() -> new BusinessException("Thành viên không thuộc tổ chức này"));

        Role newRole = roleRepository.findById(request.getRoleId())
        .orElseThrow(() -> new ResourceNotFoundException("Vai trò không tồn tại"));

        // Không cho cấp hoặc đổi vai trò khi thành viên đang bị vô hiệu hóa
        if (orgUser.getStatus() != OrganizationUserStatus.ACTIVE) {
            throw new BusinessException(
                    "Thành viên đã bị vô hiệu hóa. Vui lòng kích hoạt lại trước khi cấp quyền");
        }

        // VT-02 chỉ được cấp vai trò VT-03
        validateAssignableRole(currentRoleCode, newRole);

        if (RoleCode.ADMIN.equals(newRole.getCode())
                && !RoleCode.ADMIN.equals(currentRoleCode)) {
            throw new BusinessException(
                    "Quản lý HTX không thể gán vai trò Quản trị viên nền tảng");
        }

        // Nếu role mới là VT-02, kiểm tra và chuyển quyền quản lý cũ
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

        // Gán role mới cho thành viên được chọn
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

    /**
     * Thêm thành viên mới vào tổ chức hiện tại
     */
    @Transactional
    public OrganizationUserResponse addMember(AddMemberRequest request) {
        UUID orgId = getCurrentOrganizationId();
        Organization org = getCurrentOrganization();

        if (userRepository.findByUserName(request.getUsername()).isPresent()) {
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }

        // Kiểm tra trùng email
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email đã tồn tại");
        }

        // Kiểm tra trùng số điện thoại
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

        Role role = roleRepository.findById(request.getRoleId())
        .orElseThrow(() -> new ResourceNotFoundException("Vai trò không tồn tại"));

        String currentRoleCode = getCurrentRoleCode();

        // VT-02 chỉ được tạo thành viên với vai trò VT-03
        validateAssignableRole(currentRoleCode, role);

        if (RoleCode.ADMIN.equals(role.getCode())
                && !RoleCode.ADMIN.equals(currentRoleCode)) {
            throw new BusinessException(
                    "Quản lý HTX không thể tạo tài khoản admin");
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

    /**
     * Vô hiệu hóa thành viên của tổ chức hiện tại: thu hồi toàn bộ quyền
     * ngay lập tức, chuyển giao lô chưa hoàn thành cho người thay thế
     * (nếu có), chấm dứt phiên đang mở và ghi audit log (QTN-32).
     *
     * <p>
     * Toàn bộ thao tác ghi (chuyển giao phân công + đổi trạng thái
     * membership) chạy trong cùng transaction. Phiên đăng nhập đang mở bị
     * chấm dứt tức thời nhờ kiểm tra trạng thái membership ở luồng xác
     * thực từng request
     * ({@code CustomUserDetailsService.loadUserByUserIdAndOrganizationId}).
     * </p>
     */
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

        List<LotAssignment> unfinishedAssignments = lotAssignmentService
                .findUnfinishedAssignments(orgId, userId);

        if (!unfinishedAssignments.isEmpty() && request.getReplacementUserId() == null) {
            publishActivityLog(currentUser, "DEACTIVATE_BLOCKED",
                    "Từ chối vô hiệu hóa thành viên " + targetUser.getFullName()
                            + " vì còn " + unfinishedAssignments.size() + " lô chưa hoàn thành",
                    "OrganizationUser",
                    membership.getId().toString());
            throw unfinishedLotConflict(unfinishedAssignments);
        }

        User replacementUser = null;
        if (request.getReplacementUserId() != null) {
            replacementUser = validateReplacementMember(orgId, userId, request.getReplacementUserId());
        }

        if (!unfinishedAssignments.isEmpty()) {
            lotAssignmentService.transferActiveAssignments(
                    orgId, userId, replacementUser, currentUser);
        }

        // Không xóa bản ghi, không đổi users.status và không đổi vai trò —
        // vai trò cũ được giữ nguyên để kích hoạt lại đúng quyền ban đầu.
        membership.setStatus(OrganizationUserStatus.INACTIVE);
        membership = orgUserRepository.save(membership);

        publishActivityLog(currentUser, "DEACTIVATE",
                "Vô hiệu hóa thành viên " + targetUser.getFullName() + " (" + targetUser.getUserName()
                        + "). Lý do: " + request.getReason()
                        + ". Người thay thế: "
                        + (replacementUser != null ? replacementUser.getFullName() : "Không"),
                "OrganizationUser",
                membership.getId().toString());

        log.info("Vô hiệu hóa thành viên thành công: userId={}, orgId={}, actor={}",
                userId, orgId, currentUser.getUserId());

        return toResponse(membership);
    }

    /**
     * Kích hoạt lại thành viên đã ngừng hoạt động, bắt buộc nhập lý do.
     * Vai trò cũ lưu trong {@code organization_users.role_id} được giữ
     * nguyên (không tự cấp lại quyền); phân công lô cũ không được hồi tố.
     */
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

    // ==================== deactivate/reactivate helpers ====================

    /** BR-9: không cho phép vô hiệu hóa chính mình. */
    private void validateNotSelfDeactivation(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException("Không thể tự vô hiệu hóa tài khoản của chính mình");
        }
    }

    /**
     * Phạm vi vai trò khi vô hiệu hóa: người thao tác phải là VT-01/VT-02
     * (tầng 2 phòng thủ, tầng 1 là {@code @PreAuthorize} trên controller);
     * VT-02 chỉ được vô hiệu hóa thành viên vai trò Người ghi sự kiện
     * (khớp {@code validateAssignableRole}).
     */
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

    /**
     * BR-4: người thay thế phải là membership ACTIVE cùng tổ chức, khác
     * thành viên đang bị vô hiệu hóa, tài khoản không bị khóa toàn cục và
     * đủ quyền ghi sự kiện.
     */
    private User validateReplacementMember(UUID orgId, UUID deactivatingUserId, UUID replacementUserId) {
        if (replacementUserId.equals(deactivatingUserId)) {
            throw invalidReplacement(replacementUserId);
        }

        return orgUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(orgId, replacementUserId)
                .filter(member -> member.getStatus() == OrganizationUserStatus.ACTIVE)
                .filter(member -> member.getUser().getStatus() == UserStatus.ACTIVE)
                .filter(member -> !member.getUser().getUserId().equals(deactivatingUserId))
                .filter(member -> hasEventRecordingPermission(orgId, member.getRole()))
                .map(OrganizationUser::getUser)
                .orElseThrow(() -> invalidReplacement(replacementUserId));
    }

    private BusinessException invalidReplacement(UUID replacementUserId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("replacementUserId", replacementUserId.toString());
        return new BusinessException("Người thay thế không hợp lệ", details);
    }

    /** BR-3: 409 kèm danh sách lô chưa hoàn thành để FE hiển thị chọn người thay thế. */
    private BusinessException unfinishedLotConflict(List<LotAssignment> assignments) {
        List<MemberLotSummary> pendingLots = assignments.stream()
                .map(this::toLotSummary)
                .collect(Collectors.toList());

        ReplacementRequiredError details = ReplacementRequiredError.builder()
                .code("MEMBER_HAS_UNFINISHED_LOTS")
                .requiresReplacement(true)
                .pendingLots(pendingLots)
                .build();

        return new BusinessException(HttpStatus.CONFLICT,
                "Thành viên đang được phân công vào " + assignments.size()
                        + " lô chưa hoàn thành. Vui lòng chọn người thay thế",
                details);
    }

    private MemberLotSummary toLotSummary(LotAssignment assignment) {
        ProductionLot lot = assignment.getProductionLot();
        return MemberLotSummary.builder()
                .lotId(lot.getId())
                .lotName(lot.getName())
                .lotStatus(lot.getStatus().name())
                .plantingDate(lot.getPlantingDate())
                .harvestDate(lot.getHarvestDate())
                .build();
    }

    /**
     * Load membership của một thành viên trong tổ chức hiện tại (scope
     * theo JWT). User không tồn tại → 404; không thuộc tổ chức hiện tại
     * → 400 (không lộ thông tin membership chéo tổ chức).
     */
    private OrganizationUser loadMembershipInCurrentOrganization(UUID userId) {
        UUID orgId = getCurrentOrganizationId();
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại"));
        return orgUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(orgId, userId)
                .orElseThrow(() -> new BusinessException("Thành viên không thuộc tổ chức này"));
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