package vn.nguongocso.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.organization.dto.request.AssignAreasRequest;
import vn.nguongocso.organization.dto.request.UpdateOrganizationDivisionsRequest;
import vn.nguongocso.organization.dto.response.AdministrativeUnitNode;
import vn.nguongocso.organization.dto.response.AssignAreasResult;
import vn.nguongocso.organization.dto.response.AssignedAreaResponse;
import vn.nguongocso.organization.dto.response.RegulatorUserResponse;
import vn.nguongocso.organization.dto.response.UnassignAreaResult;
import vn.nguongocso.organization.entity.AdministrativeUnit;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.AdministrativeUnitLevel;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.AdministrativeUnitRepository;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/**
 * Kiểm thử tích hợp nghiệp vụ gán địa bàn NCL-743: TC-03 → TC-07 theo
 * NCL-739 §6, kèm test shape cây danh mục hành chính và audit activity_logs.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AreaAssignmentServiceIntegrationTest {

    private static final String FORBIDDEN_MESSAGE = "Bạn không có quyền thực hiện thao tác này.";
    private static final String NOT_FOUND_USER_MESSAGE = "Tài khoản không tồn tại.";
    private static final String NOT_REGULATOR_MESSAGE = "Tài khoản không có vai trò Cán bộ quản lý ngành.";
    private static final String UNKNOWN_UNIT_MESSAGE = "Địa bàn không nằm trong danh mục hành chính.";
    private static final String DUPLICATE_ASSIGN_MESSAGE = "Địa bàn đã được gán cho tài khoản này.";
    private static final String ASSIGNMENT_NOT_FOUND_MESSAGE = "Tài khoản chưa được gán địa bàn này.";

    @Autowired
    private AreaAssignmentService areaAssignmentService;

    @Autowired
    private AdministrativeUnitService administrativeUnitService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationUserRepository organizationUserRepository;

    @Autowired
    private AdministrativeUnitRepository administrativeUnitRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    private Role adminRole;
    private Role managerRole;
    private Role regulatorRole;

    private Organization operatorOrg;
    private User adminOperator;
    private User managerOperator;
    private User regulatorTarget;
    private AdministrativeUnit tinhNinhBinh;
    private AdministrativeUnit xaHoaLau;
    private AdministrativeUnit xaGiaVien;

    @BeforeEach
    void setUp() {
        adminRole = createRoleIfMissing("VT-01", "Quản trị hệ thống");
        managerRole = createRoleIfMissing("VT-02", "Quản lý tổ chức");
        regulatorRole = createRoleIfMissing("VT-05", "Cán bộ quản lý ngành");

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        operatorOrg = organizationRepository.save(Organization.builder()
                .name("Sở NN&PTNT " + suffix)
                .code("SO-NN-" + suffix)
                .type(OrganizationType.GOVERNMENT)
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminOperator = createUser("vt01-" + suffix, "Trần Quản Trị");
        attachMembership(adminOperator, operatorOrg, adminRole);

        managerOperator = createUser("vt02-" + suffix, "Lê Quản Lý");
        attachMembership(managerOperator, operatorOrg, managerRole);

        regulatorTarget = createUser("vt05-target-" + suffix, "Nguyễn Văn Cán Bộ");
        attachMembership(regulatorTarget, operatorOrg, regulatorRole);

        tinhNinhBinh = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("36")
                .name("Ninh Bình")
                .level(AdministrativeUnitLevel.PROVINCE)
                .active(true)
                .build());
        xaHoaLau = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("04098")
                .name("Hoa Lư")
                .level(AdministrativeUnitLevel.COMMUNE)
                .parent(tinhNinhBinh)
                .province(tinhNinhBinh)
                .active(true)
                .build());
        xaGiaVien = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("04099")
                .name("Gia Viễn")
                .level(AdministrativeUnitLevel.COMMUNE)
                .parent(tinhNinhBinh)
                .province(tinhNinhBinh)
                .active(true)
                .build());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
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
        organizationUserRepository.save(buildMembership(user, organization, role));
    }

    private OrganizationUser buildMembership(User user, Organization organization, Role role) {
        OrganizationUser membership = new OrganizationUser();
        membership.setUser(user);
        membership.setOrganization(organization);
        membership.setRole(role);
        membership.setStatus(OrganizationUserStatus.ACTIVE);
        membership.setJoinedAt(LocalDateTime.now());
        return membership;
    }

    private CustomUserDetails loginAs(User user) {
        OrganizationUser membership =
                organizationUserRepository.findByUser_UserIdAndOrganization_OrganizationId(
                        user.getUserId(), operatorOrg.getOrganizationId()).orElseThrow();
        Role role = membership.getRole();
        CustomUserDetails details = new CustomUserDetails(user, membership, role);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        return details;
    }

    // ==================== happy path ====================

    @Test
    void assignAreas_assignsBothUnits_andReturnsContractShape() {
        CustomUserDetails operator = loginAs(adminOperator);

        AssignAreasResult result = areaAssignmentService.assignAreas(
                operator,
                regulatorTarget.getUserId(),
                new AssignAreasRequest(List.of(xaHoaLau.getId(), tinhNinhBinh.getId())));

        assertThat(result.getAssignedCount()).isEqualTo(2);
        assertThat(result.getAssigned())
                .extracting(AssignedAreaResponse::getUnitCode)
                .containsExactlyInAnyOrder("04098", "36");

        // Đơn vị cấp xã phải mang provinceId/provinceName trỏ về tỉnh gốc.
        AssignedAreaResponse communeRow = result.getAssigned().stream()
                .filter(area -> "COMMUNE".equals(area.getUnitLevel()))
                .findFirst().orElseThrow();
        assertThat(communeRow.getProvinceId()).isEqualTo(tinhNinhBinh.getId());
        assertThat(communeRow.getProvinceName()).isEqualTo("Ninh Bình");

        List<AssignedAreaResponse> stored =
                areaAssignmentService.getAssignedAreas(regulatorTarget.getUserId());
        assertThat(stored).hasSize(2);
    }

    // ==================== TC-03: sai vai trò => 403 ====================

    @Test
    void tc03_nonAdminOperator_isRejectedWith403AndExactMessage() {
        CustomUserDetails operator = loginAs(managerOperator);

        assertThatThrownBy(() -> areaAssignmentService.assignAreas(
                operator, regulatorTarget.getUserId(),
                new AssignAreasRequest(List.of(xaHoaLau.getId()))))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN))
                .hasMessage(FORBIDDEN_MESSAGE);

        assertThatThrownBy(() -> areaAssignmentService.listRegulators(null,
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(FORBIDDEN_MESSAGE);

        assertThatThrownBy(() -> areaAssignmentService.getAssignedAreas(regulatorTarget.getUserId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(FORBIDDEN_MESSAGE);
    }

    // ==================== TC-04: audit ASSIGN_AREA / UNASSIGN_AREA ====================

    @Test
    void tc04_assignThenUnassign_writesAuditRowsForBothActions() throws InterruptedException {
        CustomUserDetails operator = loginAs(adminOperator);

        areaAssignmentService.assignAreas(
                operator,
                regulatorTarget.getUserId(),
                new AssignAreasRequest(List.of(xaHoaLau.getId(), tinhNinhBinh.getId())));

        UnassignAreaResult unassignResult = areaAssignmentService.unassignArea(
                operator,
                regulatorTarget.getUserId(),
                xaHoaLau.getId());

        assertThat(unassignResult.getMessage())
                .isEqualTo("Đã gỡ địa bàn Hoa Lư khỏi tài khoản.");

        // ActivityLogListener chạy async (REQUIRES_NEW): poll tối đa ~10 giây.
        List<ActivityLog> matching = pollActivityLogs(
                regulatorTarget.getUserId().toString(),
                List.of("ASSIGN_AREA", "UNASSIGN_AREA"));

        ActivityLog assignLog = matching.stream()
                .filter(log -> "ASSIGN_AREA".equals(log.getAction()))
                .findFirst().orElseThrow();
        ActivityLog unassignLog = matching.stream()
                .filter(log -> "UNASSIGN_AREA".equals(log.getAction()))
                .findFirst().orElseThrow();

        for (ActivityLog log : List.of(assignLog, unassignLog)) {
            assertThat(log.getUsername()).isEqualTo(operator.getUsername());
            assertThat(log.getEntityType()).isEqualTo("UserAreaAssignment");
            assertThat(log.getEntityId()).isEqualTo(regulatorTarget.getUserId().toString());
            assertThat(log.getUserId()).isEqualTo(adminOperator.getUserId());
        }
        assertThat(assignLog.getDescription())
                .contains(regulatorTarget.getUserName())
                .contains("Hoa Lư")
                .contains("tổng 2 địa bàn");
        assertThat(unassignLog.getDescription())
                .contains(regulatorTarget.getUserName())
                .contains("Hoa Lư");
    }

    private List<ActivityLog> pollActivityLogs(String entityId, List<String> actions)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            List<ActivityLog> found = activityLogRepository.findAll().stream()
                    .filter(log -> entityId.equals(log.getEntityId()))
                    .filter(log -> actions.contains(log.getAction()))
                    .collect(Collectors.toList());
            if (found.size() >= actions.size()) {
                return found;
            }
            Thread.sleep(200);
        }
        return List.of();
    }

    // ==================== TC-05: gán trùng ====================

    @Test
    void tc05_duplicateAssign_isRejectedWithExactMessage() {
        CustomUserDetails operator = loginAs(adminOperator);

        areaAssignmentService.assignAreas(
                operator,
                regulatorTarget.getUserId(),
                new AssignAreasRequest(List.of(xaHoaLau.getId())));

        assertThatThrownBy(() -> areaAssignmentService.assignAreas(
                operator,
                regulatorTarget.getUserId(),
                new AssignAreasRequest(List.of(tinhNinhBinh.getId(), xaHoaLau.getId()))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(DUPLICATE_ASSIGN_MESSAGE);

        // Trùng ngay trong request cũng bị chặn.
        assertThatThrownBy(() -> areaAssignmentService.assignAreas(
                operator,
                regulatorTarget.getUserId(),
                new AssignAreasRequest(List.of(tinhNinhBinh.getId(), tinhNinhBinh.getId()))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(DUPLICATE_ASSIGN_MESSAGE);
    }

    // ==================== TC-06: unit lạ /inactive ====================

    @Test
    void tc06_unknownOrInactiveUnit_isRejectedWithExactMessage() {
        CustomUserDetails operator = loginAs(adminOperator);

        assertThatThrownBy(() -> areaAssignmentService.assignAreas(
                operator,
                regulatorTarget.getUserId(),
                new AssignAreasRequest(List.of(UUID.randomUUID()))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UNKNOWN_UNIT_MESSAGE);

        xaGiaVien.setActive(false);
        administrativeUnitRepository.saveAndFlush(xaGiaVien);

        assertThatThrownBy(() -> areaAssignmentService.assignAreas(
                operator,
                regulatorTarget.getUserId(),
                new AssignAreasRequest(List.of(xaGiaVien.getId()))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UNKNOWN_UNIT_MESSAGE);
    }

    // ==================== TC-07: target không có membership VT-05 ====================

    @Test
    void tc07_targetWithoutRegulatorRole_isRejectedWithExactMessage() {
        CustomUserDetails operator = loginAs(adminOperator);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User plainUser = createUser("plain-" + suffix, "Nguyễn Văn Thường");
        attachMembership(plainUser, operatorOrg, managerRole);

        assertThatThrownBy(() -> areaAssignmentService.assignAreas(
                operator,
                plainUser.getUserId(),
                new AssignAreasRequest(List.of(xaHoaLau.getId()))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(NOT_REGULATOR_MESSAGE);

        assertThatThrownBy(() -> areaAssignmentService.assignAreas(
                operator,
                UUID.randomUUID(),
                new AssignAreasRequest(List.of(xaHoaLau.getId()))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(NOT_FOUND_USER_MESSAGE);
    }

    // ==================== V6: gỡ bản ghi không tồn tại ====================

    @Test
    void unassign_missingRecord_returns404ExactMessage() {
        CustomUserDetails operator = loginAs(adminOperator);

        assertThatThrownBy(() -> areaAssignmentService.unassignArea(
                operator,
                regulatorTarget.getUserId(),
                xaHoaLau.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND))
                .hasMessage(ASSIGNMENT_NOT_FOUND_MESSAGE);
    }

    // ==================== mapping tổ chức ====================

    @Test
    void updateOrganizationDivisions_mapsUnits_andValidatesLevel() {
        CustomUserDetails operator = loginAs(adminOperator);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Organization target = organizationRepository.save(Organization.builder()
                .name("HTX Đích " + suffix)
                .code("HTX-DICH-" + suffix)
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .build());

        areaAssignmentService.updateOrganizationDivisions(
                operator,
                target.getOrganizationId(),
                new UpdateOrganizationDivisionsRequest(tinhNinhBinh.getId(), xaHoaLau.getId()));

        Organization reloaded = organizationRepository.findById(target.getOrganizationId()).orElseThrow();
        assertThat(reloaded.getProvince().getId()).isEqualTo(tinhNinhBinh.getId());
        assertThat(reloaded.getCommune().getId()).isEqualTo(xaHoaLau.getId());

        // Sai cấp: dùng xã làm provinceId => từ chối.
        assertThatThrownBy(() -> areaAssignmentService.updateOrganizationDivisions(
                operator,
                target.getOrganizationId(),
                new UpdateOrganizationDivisionsRequest(xaGiaVien.getId(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UNKNOWN_UNIT_MESSAGE);

        // Tổ chức không tồn tại.
        assertThatThrownBy(() -> areaAssignmentService.updateOrganizationDivisions(
                operator,
                UUID.randomUUID(),
                new UpdateOrganizationDivisionsRequest(tinhNinhBinh.getId(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tổ chức không tồn tại.");
    }

    // ==================== danh sách VT-05 ====================

    @Test
    void listRegulators_returnsOnlyUsersWithActiveVt05Membership() {
        loginAs(adminOperator);

        var page = areaAssignmentService.listRegulators(
                null, org.springframework.data.domain.PageRequest.of(0, 50));

        assertThat(page.getItems())
                .extracting(RegulatorUserResponse::getUsername)
                .contains(regulatorTarget.getUserName())
                .doesNotContain(adminOperator.getUserName(), managerOperator.getUserName());

        RegulatorUserResponse row = page.getItems().stream()
                .filter(item -> item.getUsername().equals(regulatorTarget.getUserName()))
                .findFirst().orElseThrow();
        assertThat(row.getFullName()).isEqualTo(regulatorTarget.getFullName());
        assertThat(row.getOrganizationName()).isEqualTo(operatorOrg.getName());

        // keyword lọc theo username/fullName.
        var filtered = areaAssignmentService.listRegulators(
                regulatorTarget.getUserName(),
                org.springframework.data.domain.PageRequest.of(0, 50));
        assertThat(filtered.getItems())
                .extracting(RegulatorUserResponse::getUsername)
                .containsExactly(regulatorTarget.getUserName());
    }

    // ==================== cây danh mục hành chính ====================

    @Test
    void unitTree_nestsCommunesUnderProvinces_sortedByName() {
        // Thêm một tỉnh khác để kiểm tra sắp xếp theo tên.
        administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("38")
                .name("Thanh Hóa")
                .level(AdministrativeUnitLevel.PROVINCE)
                .active(true)
                .build());

        List<AdministrativeUnitNode> tree = administrativeUnitService.getUnitTree();

        AdministrativeUnitNode ninhBinhNode = tree.stream()
                .filter(node -> "36".equals(node.getCode()))
                .findFirst().orElseThrow();
        assertThat(ninhBinhNode.getLevel()).isEqualTo("PROVINCE");
        // Sắp xếp theo tên: Gia Viễn trước Hoa Lư.
        assertThat(ninhBinhNode.getChildren())
                .extracting(AdministrativeUnitNode::getName)
                .containsExactly("Gia Viễn", "Hoa Lư");

        AdministrativeUnitNode thanhHoaNode = tree.stream()
                .filter(node -> "38".equals(node.getCode()))
                .findFirst().orElseThrow();
        assertThat(thanhHoaNode.getChildren()).isEmpty();

        // Cây chỉ lồng đúng một mức: node con không có cháu.
        assertThat(ninhBinhNode.getChildren())
                .allSatisfy(child -> assertThat(child.getChildren()).isEmpty());
    }
}
