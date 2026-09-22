package vn.nguongocso.organization.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.dto.request.AssignAreasRequest;
import vn.nguongocso.organization.dto.request.UpdateOrganizationDivisionsRequest;
import vn.nguongocso.organization.dto.response.AssignAreasResult;
import vn.nguongocso.organization.dto.response.AssignedAreaResponse;
import vn.nguongocso.organization.dto.response.RegulatorUserResponse;
import vn.nguongocso.organization.dto.response.UnassignAreaResult;
import vn.nguongocso.organization.entity.AdministrativeUnit;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.entity.UserAreaAssignment;
import vn.nguongocso.organization.enums.AdministrativeUnitLevel;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.AdministrativeUnitRepository;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.organization.repository.UserAreaAssignmentRepository;
import vn.nguongocso.organization.service.AreaAssignmentService;

/** Triển khai nghiệp vụ gán, gỡ và xem địa bàn quản lý của tài khoản. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AreaAssignmentServiceImpl implements AreaAssignmentService {
    private static final String NOT_FOUND_USER_MESSAGE = "Tài khoản không tồn tại.";

    private static final String FORBIDDEN_MESSAGE = "Bạn không có quyền thực hiện thao tác này.";

    private static final String NOT_REGULATOR_MESSAGE = "Tài khoản không có vai trò Cán bộ quản lý ngành.";

    private static final String UNKNOWN_UNIT_MESSAGE = "Địa bàn không nằm trong danh mục hành chính.";

    private static final String DUPLICATE_ASSIGN_MESSAGE = "Địa bàn đã được gán cho tài khoản này.";

    private static final String ASSIGNMENT_NOT_FOUND_MESSAGE = "Tài khoản chưa được gán địa bàn này.";

    private static final String ORGANIZATION_NOT_FOUND_MESSAGE = "Tổ chức không tồn tại.";

    private static final String AUDIT_ENTITY_TYPE = "UserAreaAssignment";

    private final UserRepository userRepository;

    private final OrganizationUserRepository organizationUserRepository;

    private final AdministrativeUnitRepository administrativeUnitRepository;

    private final UserAreaAssignmentRepository userAreaAssignmentRepository;

    private final OrganizationRepository organizationRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RegulatorUserResponse> listRegulators(String keyword, Pageable pageable) {
        requireOperator(RoleCode.ADMIN);

        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Page<User> page = userRepository.findUsersByActiveRoleCode(
                RoleCode.REGULATOR, normalizedKeyword, pageable);

        List<RegulatorUserResponse> items = page.getContent().stream()
                .map(this::toRegulatorOption)
                .toList();

        return PageResponse.from(page, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedAreaResponse> getAssignedAreas(UUID userId) {
        requireOperator(RoleCode.ADMIN);
        requireUserExists(userId);
        return userAreaAssignmentRepository.findAllByUser_UserIdOrderByAssignedAtDesc(userId).stream()
                .map(this::toAssignedArea)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedAreaResponse> getMyAreas(CustomUserDetails currentUser) {
        CustomUserDetails user = currentUser != null ? currentUser : SecurityUtils.getCurrentUserDetails();
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        return userAreaAssignmentRepository.findAllByUser_UserIdOrderByAssignedAtDesc(user.getUserId())
                .stream()
                .map(this::toAssignedArea)
                .toList();
    }

    @Override
    @Transactional
    public AssignAreasResult assignAreas(CustomUserDetails operator, UUID userId, AssignAreasRequest request) {
        requireOperator(RoleCode.ADMIN);

        User targetUser = requireUserExists(userId);
        List<UUID> requestedIds = request.getUnitIds() == null ? List.of() : request.getUnitIds();

        if (!hasActiveRoleMembership(userId, RoleCode.REGULATOR)) {
            throw new BusinessException(NOT_REGULATOR_MESSAGE);
        }

        Set<UUID> distinctIds = new HashSet<>(requestedIds);
        if (distinctIds.size() != requestedIds.size()) {
            throw new BusinessException(DUPLICATE_ASSIGN_MESSAGE);
        }
        for (UUID unitId : requestedIds) {
            if (userAreaAssignmentRepository.existsByUser_UserIdAndUnit_Id(userId, unitId)) {
                throw new BusinessException(DUPLICATE_ASSIGN_MESSAGE);
            }
        }

        List<AdministrativeUnit> units = new ArrayList<>(administrativeUnitRepository.findAllById(requestedIds));
        if (units.size() != requestedIds.size()
                || units.stream().anyMatch(unit -> !unit.isActive())) {
            throw new BusinessException(UNKNOWN_UNIT_MESSAGE);
        }

        User assignedBy = operator != null && operator.getUserId() != null
                ? userRepository.findById(operator.getUserId()).orElse(null)
                : null;

        List<UserAreaAssignment> rows = units.stream()
                .<UserAreaAssignment>map(unit -> UserAreaAssignment.builder()
                        .user(targetUser)
                        .unit(unit)
                        .assignedBy(assignedBy)
                        .build())
                .toList();

        try {
            userAreaAssignmentRepository.saveAll(rows);
            userAreaAssignmentRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            log.warn("Trùng lặp gán địa bàn khi lưu DB: userId={}, ex={}", userId, ex.getMessage());
            throw new BusinessException(DUPLICATE_ASSIGN_MESSAGE);
        }

        List<AssignedAreaResponse> assigned = rows.stream().map(this::toAssignedArea).toList();

        String unitListText = joinUnits(units);
        publishAudit(
                operator,
                "ASSIGN_AREA",
                "Gán địa bàn cho tài khoản " + targetUser.getUserName() + ": " + unitListText
                        + " (tổng " + units.size() + " địa bàn)",
                targetUser.getUserId());

        return AssignAreasResult.builder()
                .assignedCount(assigned.size())
                .assigned(assigned)
                .message("Đã gán " + assigned.size() + " địa bàn cho tài khoản.")
                .build();
    }

    @Override
    @Transactional
    public UnassignAreaResult unassignArea(CustomUserDetails operator, UUID userId, UUID unitId) {
        requireOperator(RoleCode.ADMIN);

        User targetUser = requireUserExists(userId);
        UserAreaAssignment assignment = userAreaAssignmentRepository
                .findFirstByUser_UserIdAndUnit_Id(userId, unitId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ASSIGNMENT_NOT_FOUND_MESSAGE));

        AdministrativeUnit unit = assignment.getUnit();
        String unitName = unit.getName();

        userAreaAssignmentRepository.delete(assignment);
        userAreaAssignmentRepository.flush();

        publishAudit(
                operator,
                "UNASSIGN_AREA",
                "Gỡ địa bàn " + unit.getCode() + " - " + unitName
                        + " khỏi tài khoản " + targetUser.getUserName(),
                targetUser.getUserId());

        return new UnassignAreaResult("Đã gỡ địa bàn " + unitName + " khỏi tài khoản.");
    }

    @Override
    @Transactional
    public void updateOrganizationDivisions(
            CustomUserDetails operator,
            UUID organizationId,
            UpdateOrganizationDivisionsRequest request) {
        requireOperator(RoleCode.ADMIN);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ORGANIZATION_NOT_FOUND_MESSAGE));

        AdministrativeUnit province = null;
        if (request.getProvinceId() != null) {
            province = administrativeUnitRepository.findById(request.getProvinceId())
                    .filter(unit -> unit.isActive() && unit.getLevel() == AdministrativeUnitLevel.PROVINCE)
                    .orElseThrow(() -> new BusinessException(UNKNOWN_UNIT_MESSAGE));
        }

        AdministrativeUnit commune = null;
        if (request.getCommuneId() != null) {
            commune = administrativeUnitRepository.findById(request.getCommuneId())
                    .filter(unit -> unit.isActive() && unit.getLevel() == AdministrativeUnitLevel.COMMUNE)
                    .orElseThrow(() -> new BusinessException(UNKNOWN_UNIT_MESSAGE));

            if (province != null) {
                UUID parentProvinceId = commune.getProvince() != null ? commune.getProvince().getId()
                        : (commune.getParent() != null ? commune.getParent().getId() : null);
                if (parentProvinceId == null || !parentProvinceId.equals(province.getId())) {
                    throw new BusinessException("Xã/phường không thuộc tỉnh/thành phố đã chọn.");
                }
            }
        }

        organization.setProvince(province);
        organization.setCommune(commune);
        organizationRepository.save(organization);

        publishAudit(operator, "UPDATE_ORGANIZATION_DIVISIONS",
                String.format("Cập nhật địa bàn cho tổ chức %s: province=%s, commune=%s",
                        organization.getName(),
                        province != null ? province.getName() : "null",
                        commune != null ? commune.getName() : "null"),
                organizationId);
    }

    private void requireOperator(String requiredRoleCode) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        if (currentUser == null || !requiredRoleCode.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE);
        }
    }

    private User requireUserExists(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, NOT_FOUND_USER_MESSAGE));
    }

    private boolean hasActiveRoleMembership(UUID userId, String roleCode) {
        List<OrganizationUser> memberships = organizationUserRepository
                .findByUser_UserIdAndStatus(userId, OrganizationUserStatus.ACTIVE);
        return memberships.stream()
                .anyMatch(m -> m.getRole() != null && roleCode.equals(m.getRole().getCode()));
    }

    private RegulatorUserResponse toRegulatorOption(User user) {
        String organizationName = organizationUserRepository
                .findByUser_UserIdAndStatus(user.getUserId(), OrganizationUserStatus.ACTIVE).stream()
                .filter(m -> m.getRole() != null && RoleCode.REGULATOR.equals(m.getRole().getCode()))
                .map(m -> m.getOrganization() != null ? m.getOrganization().getName() : "")
                .findFirst()
                .orElse("");

        return RegulatorUserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUserName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .organizationName(organizationName)
                .build();
    }

    private AssignedAreaResponse toAssignedArea(UserAreaAssignment assignment) {
        AdministrativeUnit unit = assignment.getUnit();
        AdministrativeUnit province = unit.getLevel() == AdministrativeUnitLevel.PROVINCE
                ? unit
                : (unit.getProvince() != null ? unit.getProvince() : unit.getParent());

        return AssignedAreaResponse.builder()
                .assignmentId(assignment.getId())
                .unitId(unit.getId())
                .unitCode(unit.getCode())
                .unitName(unit.getName())
                .unitLevel(unit.getLevel().name())
                .provinceId(province != null ? province.getId() : null)
                .provinceName(province != null ? province.getName() : null)
                .assignedAt(assignment.getAssignedAt())
                .build();
    }

    private String joinUnits(List<AdministrativeUnit> units) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < units.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(units.get(i).getCode()).append(" - ").append(units.get(i).getName());
        }
        return sb.toString();
    }

    private void publishAudit(CustomUserDetails operator, String action, String description, UUID entityId) {
        CustomUserDetails current = operator != null ? operator : SecurityUtils.getCurrentUserDetails();
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(current != null ? current.getUserId() : null)
                .username(current != null ? current.getUsername() : "SYSTEM")
                .fullName(current != null ? current.getFullName() : "Hệ thống")
                .organizationId(current != null ? current.getOrganizationId() : null)
                .action(action)
                .description(description)
                .entityType(AUDIT_ENTITY_TYPE)
                .entityId(entityId != null ? entityId.toString() : null)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }
}
