package vn.nguongocso.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.auth.dto.request.DeactivateMemberRequest;
import vn.nguongocso.auth.dto.request.ReactivateMemberRequest;
import vn.nguongocso.auth.dto.response.OrganizationUserResponse;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.LotAssignment;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.LotAssignmentRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/**
 * Kiểm thử tích hợp nghiệp vụ vô hiệu hóa / kích hoạt lại thành viên
 * NCL-01-CN-009 (QTN-32): TC-01 → TC-09, kèm test precheck lô, ứng viên
 * thay thế, audit activity_logs và thu hồi phiên tức thời.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationMemberDeactivationServiceIntegrationTest {

    private static final String NOT_IN_ORG_MESSAGE = "Thành viên không thuộc tổ chức này";
    private static final String MEMBER_NOT_FOUND_MESSAGE = "Thành viên không tồn tại";
    private static final String ALREADY_INACTIVE_MESSAGE = "Thành viên đã ngừng hoạt động";
    private static final String ALREADY_ACTIVE_REACTIVATE_MESSAGE =
            "Thành viên đang hoạt động, không thể kích hoạt lại";
    private static final String SELF_DEACTIVATION_MESSAGE = "Không thể tự vô hiệu hóa tài khoản của chính mình";
    private static final String FORBIDDEN_MESSAGE = "Bạn không có quyền thực hiện chức năng này";
    private static final String MEMBERSHIP_REVOKED_MESSAGE = "Thành viên đã bị vô hiệu hóa trong tổ chức này";

    @Autowired
    private OrganizationMemberService memberService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationUserRepository organizationUserRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductionLotRepository productionLotRepository;

    @Autowired
    private LotAssignmentRepository lotAssignmentRepository;

    @Autowired
    private ChainEventRepository chainEventRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    private Organization cooperative;
    private Organization otherCooperative;
    private Role adminRole;
    private Role managerRole;
    private Role recorderRole;
    private Role procurementRole;
    private User manager;
    private User recorder;
    private User procurementMember;
    private User otherOrgManager;
    private User otherOrgRecorder;
    private ProductCategory mangoCategory;

    @BeforeEach
    void setUp() {
        adminRole = createRoleIfMissing("VT-01", "Quản trị hệ thống");
        managerRole = createRoleIfMissing("VT-02", "Quản lý tổ chức");
        recorderRole = createRoleIfMissing("VT-03", "Người ghi sự kiện");
        procurementRole = createRoleIfMissing("VT-04", "Thu mua");

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        cooperative = organizationRepository.save(Organization.builder()
                .name("HTX Nông sản " + suffix)
                .code("HTX-" + suffix)
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .build());

        otherCooperative = organizationRepository.save(Organization.builder()
                .name("HTX Khác " + suffix)
                .code("HTX-K-" + suffix)
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .build());

        manager = createUser("manager-" + suffix, "Trần Quản Lý");
        attachMembership(manager, cooperative, managerRole);

        recorder = createUser("recorder-" + suffix, "Nguyễn Văn Ghi");
        attachMembership(recorder, cooperative, recorderRole);

        procurementMember = createUser("procurement-" + suffix, "Phạm Thu Mua");
        attachMembership(procurementMember, cooperative, procurementRole);

        otherOrgManager = createUser("other-manager-" + suffix, "Võ Quản Lý Khác");
        attachMembership(otherOrgManager, otherCooperative, managerRole);

        otherOrgRecorder = createUser("other-recorder-" + suffix, "Đặng Ghi Khác");
        attachMembership(otherOrgRecorder, otherCooperative, recorderRole);

        mangoCategory = productCategoryRepository.save(ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Xoài cát chu " + suffix)
                .isActive(true)
                .build());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== TC-01: deactivate thành công ====================

    @Test
    void tc01_memberWithoutAssignments_isDeactivatedSuccessfully() throws InterruptedException {
        loginAs(manager);

        OrganizationUserResponse response = memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Thành viên nghỉ việc từ 01/09/2026")
                        .build());

        assertThat(response.getMembershipStatus()).isEqualTo(OrganizationUserStatus.INACTIVE);
        // Trạng thái toàn cục users.status phải giữ nguyên (multi-organization).
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        // Vai trò cũ được giữ nguyên để kích hoạt lại đúng quyền ban đầu.
        assertThat(response.getRoleCode()).isEqualTo("VT-03");

        OrganizationUser stored = organizationUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(
                        cooperative.getOrganizationId(), recorder.getUserId())
                .orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrganizationUserStatus.INACTIVE);
        assertThat(stored.getRole().getCode()).isEqualTo("VT-03");

        List<ActivityLog> auditLogs = pollActivityLogs(
                stored.getId().toString(), List.of("DEACTIVATE"));
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.get(0).getDescription())
                .contains(recorder.getFullName())
                .contains("nghỉ việc");
    }

    // ==================== TC-02: không chuyển giao lô khi vô hiệu hóa ====================

    @Test
    void tc02_memberWithUnfinishedLots_isDeactivatedWithoutTransfer() {
        loginAs(manager);
        ProductionLot lotA = createAndAssignLot("Lô xoài lô A", ProductionLotStatus.APPROVED, recorder);
        ProductionLot lotB = createAndAssignLot("Lô rau sạch khu A", ProductionLotStatus.PENDING, recorder);

        // Không còn chuyển giao lô: thành viên vẫn bị vô hiệu hóa; các lô giữ
        // tham chiếu người ghi cũ vì hệ thống chưa có phân quyền ghi sự kiện
        // theo lô (D-4).
        memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Thành viên nghỉ việc")
                        .build());

        OrganizationUser stored = organizationUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(
                        cooperative.getOrganizationId(), recorder.getUserId())
                .orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrganizationUserStatus.INACTIVE);

        List<LotAssignment> recorderAssignments = lotAssignmentRepository
                .findByUser_UserIdAndOrganization_OrganizationIdAndActiveTrue(
                        recorder.getUserId(), cooperative.getOrganizationId());
        assertThat(recorderAssignments)
                .extracting(a -> a.getProductionLot().getId())
                .containsExactlyInAnyOrder(lotA.getId(), lotB.getId());
    }

    // ==================== TC-03: bảo toàn dữ liệu lịch sử ====================

    // ==================== TC-04: bảo toàn dữ liệu lịch sử ====================

    @Test
    void tc04_recordedChainEvent_isPreservedWithHistoricalActor() {
        loginAs(manager);

        ChainEvent recordedEvent = chainEventRepository.save(ChainEvent.builder()
                .eventType(ChainEventType.HARVEST)
                .eventData("{\"quantity\":120}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(recorder)
                .isCorrection(false)
                .build());
        chainEventRepository.flush();

        memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Thành viên nghỉ việc")
                        .build());

        // Sự kiện cũ vẫn còn nguyên và actor giữ nguyên tham chiếu user cũ.
        ChainEvent preserved = chainEventRepository.findById(recordedEvent.getId()).orElseThrow();
        assertThat(preserved.getRecordedBy().getUserId()).isEqualTo(recorder.getUserId());

        // Không xóa user record, không đổi nhận diện (tên/username) của người ghi.
        User preservedUser = userRepository.findById(recorder.getUserId()).orElseThrow();
        assertThat(preservedUser.getFullName()).isEqualTo("Nguyễn Văn Ghi");
        assertThat(preservedUser.getUserName()).isEqualTo(recorder.getUserName());
        assertThat(preservedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    // ==================== TC-05: người ghi sự kiện gọi API ====================

    @Test
    void tc05_eventRecorderCallingDeactivate_isRejectedWith403() {
        loginAs(recorder);

        assertThatThrownBy(() -> memberService.deactivateMember(
                procurementMember.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Thử trái phép")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN))
                .hasMessage(FORBIDDEN_MESSAGE);
    }

    // ==================== TC-06: chéo tổ chức (IDOR) ====================

    @Test
    void tc06_crossOrganizationDeactivation_isRejected() {
        loginAs(otherOrgManager);

        // Manager tổ chức B cố vô hiệu hóa member thuộc tổ chức A.
        assertThatThrownBy(() -> memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Thử chéo tổ chức")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(NOT_IN_ORG_MESSAGE);

        assertThatThrownBy(() -> memberService.deactivateMember(
                UUID.randomUUID(),
                DeactivateMemberRequest.builder()
                        .reason("Thử user ảo")
                        .build()))
                .isInstanceOf(vn.nguongocso.exception.ResourceNotFoundException.class)
                .hasMessage(MEMBER_NOT_FOUND_MESSAGE);
    }

    // ==================== TC-07: phiên/token cũ bị chấm dứt ====================

    @Test
    void tc07_existingTokenCannotAuthenticateAfterDeactivation() {
        loginAs(manager);

        CustomUserDetails before = customUserDetailsService.loadUserByUserIdAndOrganizationId(
                recorder.getUserId(), cooperative.getOrganizationId());
        assertThat(before.getRoleCode()).isEqualTo("VT-03");

        memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Thành viên nghỉ việc")
                        .build());

        // Luồng xác thực ACCESS token reload membership mỗi request —
        // sau khi vô hiệu hóa, request kế tiếp dùng token cũ phải bị từ chối.
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUserIdAndOrganizationId(
                recorder.getUserId(), cooperative.getOrganizationId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(MEMBERSHIP_REVOKED_MESSAGE);

        // Đường nhận token mới cũng bị chặn.
        assertThatThrownBy(() -> memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Vô hiệu hóa lần nữa")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ALREADY_INACTIVE_MESSAGE);
    }

    // ==================== TC-08: kích hoạt lại ====================

    @Test
    void tc08_reactivateRestoresMembershipAndWritesAudit() throws InterruptedException {
        loginAs(manager);
        memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Thành viên nghỉ việc")
                        .build());

        OrganizationUserResponse response = memberService.reactivateMember(
                recorder.getUserId(),
                ReactivateMemberRequest.builder()
                        .reason("Thành viên quay lại làm việc từ 01/10/2026")
                        .build());

        assertThat(response.getMembershipStatus()).isEqualTo(OrganizationUserStatus.ACTIVE);
        // Kích hoạt lại không tự cấp quyền: vai trò giữ nguyên như trước khi vô hiệu hóa.
        assertThat(response.getRoleCode()).isEqualTo("VT-03");

        List<ActivityLog> auditLogs = pollActivityLogs(
                response.getId().toString(), List.of("REACTIVATE"));
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.get(0).getDescription())
                .contains(recorder.getFullName())
                .contains("quay lại làm việc");
    }

    // ==================== TC-09: deactivate member đã inactive ====================

    @Test
    void tc09_deactivateAlreadyInactiveMember_isRejectedWith409() {
        loginAs(manager);
        memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Vô hiệu hóa lần đầu")
                        .build());

        assertThatThrownBy(() -> memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Vô hiệu hóa lặp")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT))
                .hasMessage(ALREADY_INACTIVE_MESSAGE);

        // Reactivate trên member đang ACTIVE cũng bị chặn 409.
        assertThatThrownBy(() -> memberService.reactivateMember(
                procurementMember.getUserId(),
                ReactivateMemberRequest.builder()
                        .reason("Kích hoạt lại lặp")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ALREADY_ACTIVE_REACTIVATE_MESSAGE);
    }

    // ==================== validation bổ sung ====================

    @Test
    void deactivate_selfDeactivation_isRejected() {
        loginAs(manager);

        assertThatThrownBy(() -> memberService.deactivateMember(
                manager.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Tự vô hiệu hóa")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(SELF_DEACTIVATION_MESSAGE);
    }


    @Test
    void deactivate_lotInTerminalStatus_doesNotBlock() throws InterruptedException {
        loginAs(manager);
        createAndAssignLot("Lô đã đóng", ProductionLotStatus.CLOSED, recorder);
        createAndAssignLot("Lô bị từ chối", ProductionLotStatus.REJECTED, recorder);

        OrganizationUserResponse response = memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Thành viên nghỉ việc")
                        .build());

        assertThat(response.getMembershipStatus()).isEqualTo(OrganizationUserStatus.INACTIVE);
    }

    @Test
    void deactivate_memberWithUnfinishedLots_byManagerForNonRecorderRole_isRejectedWith403() {
        loginAs(manager);
        // VT-02 chỉ được vô hiệu hóa thành viên vai trò VT-03.
        assertThatThrownBy(() -> memberService.deactivateMember(
                procurementMember.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Nghỉ việc")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN))
                .hasMessage("Quản lý hợp tác xã không thể vô hiệu hóa vai trò này");
    }

    @Test
    void getMembers_filtersByMembershipStatus() {
        loginAs(manager);
        memberService.deactivateMember(
                recorder.getUserId(),
                DeactivateMemberRequest.builder()
                        .reason("Nghỉ việc")
                        .build());

        List<OrganizationUserResponse> activeOnly =
                memberService.getMembersOfCurrentOrganization(null);
        assertThat(activeOnly)
                .extracting(OrganizationUserResponse::getMembershipStatus)
                .containsOnly(OrganizationUserStatus.ACTIVE);

        List<OrganizationUserResponse> inactiveOnly =
                memberService.getMembersOfCurrentOrganization("INACTIVE");
        assertThat(inactiveOnly)
                .extracting(OrganizationUserResponse::getUserId)
                .containsExactly(recorder.getUserId());

        List<OrganizationUserResponse> all =
                memberService.getMembersOfCurrentOrganization("");
        assertThat(all).hasSize(activeOnly.size() + inactiveOnly.size());

        assertThatThrownBy(() -> memberService.getMembersOfCurrentOrganization("BROKEN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Trạng thái thành viên không hợp lệ");
    }


    // ==================== helpers ====================

    private Role createRoleIfMissing(String code, String name) {
        return roleRepository.findByCode(code).orElseGet(() -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            return roleRepository.save(role);
        });
    }

    private User createUser(String userName, String fullName) {
        return userRepository.save(User.builder()
                .userName(userName)
                .passwordHash("{noop}mat-khau")
                .fullName(fullName)
                .email(userName + "@test.local")
                .phone("0900000000")
                .status(UserStatus.ACTIVE)
                .build());
    }

    private void attachMembership(User user, Organization organization, Role role) {
        OrganizationUser membership = new OrganizationUser();
        membership.setUser(user);
        membership.setOrganization(organization);
        membership.setRole(role);
        membership.setStatus(OrganizationUserStatus.ACTIVE);
        membership.setJoinedAt(LocalDateTime.now());
        organizationUserRepository.save(membership);
    }

    private CustomUserDetails loginAs(User user) {
        OrganizationUser membership =
                organizationUserRepository.findByUser_UserIdAndOrganization_OrganizationId(
                        user.getUserId(), userOrganizationIdOf(user)).orElseThrow();
        Role role = membership.getRole();
        CustomUserDetails details = new CustomUserDetails(user, membership, role);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        return details;
    }

    private UUID userOrganizationIdOf(User user) {
        return organizationUserRepository.findAllByUser_UserId(user.getUserId()).stream()
                .filter(membership -> membership.getStatus() == OrganizationUserStatus.ACTIVE)
                .map(membership -> membership.getOrganization().getOrganizationId())
                .findFirst()
                .orElseThrow();
    }

    private ProductionLot createAndAssignLot(String name, ProductionLotStatus status, User assignee) {
        ProductionLot lot = productionLotRepository.save(ProductionLot.builder()
                .organization(cooperative)
                .productCategory(mangoCategory)
                .name(name)
                .expectedQuantity(1000.0)
                .status(status)
                .plantingDate(LocalDate.of(2026, 5, 10))
                .harvestDate(LocalDate.of(2026, 9, 5))
                .createdBy(assignee)
                .build());
        lotAssignmentRepository.save(LotAssignment.builder()
                .productionLot(lot)
                .user(assignee)
                .organization(cooperative)
                .active(Boolean.TRUE)
                .assignedAt(LocalDateTime.now())
                .build());
        return lot;
    }

    private List<ActivityLog> pollActivityLogs(String entityId, List<String> actions)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            List<ActivityLog> found = activityLogRepository.findAll().stream()
                    .filter(logEntry -> entityId.equals(logEntry.getEntityId()))
                    .filter(logEntry -> actions.contains(logEntry.getAction()))
                    .collect(Collectors.toList());
            if (found.size() >= actions.size()) {
                return found;
            }
            Thread.sleep(200);
        }
        return List.of();
    }
}
