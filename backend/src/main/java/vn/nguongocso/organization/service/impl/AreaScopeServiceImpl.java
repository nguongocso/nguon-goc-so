package vn.nguongocso.organization.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.entity.AdministrativeUnit;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.UserAreaAssignment;
import vn.nguongocso.organization.enums.AdministrativeUnitLevel;
import vn.nguongocso.organization.repository.AdministrativeUnitRepository;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.UserAreaAssignmentRepository;
import vn.nguongocso.organization.service.AreaScopeResult;
import vn.nguongocso.organization.service.AreaScopeService;

/** Phân tích phạm vi địa bàn cho báo cáo theo phân công người dùng. */
@Service
@RequiredArgsConstructor
public class AreaScopeServiceImpl implements AreaScopeService {
    private final UserAreaAssignmentRepository userAreaAssignmentRepository;

    private final AdministrativeUnitRepository administrativeUnitRepository;

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public AreaScopeResult resolveOrganizationsForReports(CustomUserDetails user, List<UUID> unitIds) {
        if (user == null) {
            return AreaScopeResult.emptyScope();
        }

        String role = user.getRoleCode();

        if (RoleCode.ADMIN.equals(role)) {
            if (unitIds == null || unitIds.isEmpty()) {
                return AreaScopeResult.all();
            }
            return filterByUnits(new HashSet<>(unitIds));
        }

        if (RoleCode.REGULATOR.equals(role)) {
            List<UserAreaAssignment> assignments = userAreaAssignmentRepository
                    .findAllByUser_UserIdOrderByAssignedAtDesc(user.getUserId());

            if (assignments.isEmpty()) {
                return AreaScopeResult.emptyScope();
            }

            Set<UUID> assignedUnitIds = new HashSet<>();
            for (UserAreaAssignment assignment : assignments) {
                assignedUnitIds.add(assignment.getUnit().getId());
            }

            Set<UUID> effectiveUnits = assignedUnitIds;
            if (unitIds != null && !unitIds.isEmpty()) {
                effectiveUnits = new HashSet<>(assignedUnitIds);
                effectiveUnits.retainAll(unitIds);
            }
            return filterByUnits(effectiveUnits);
        }

        return AreaScopeResult.all();
    }

    /** Lọc danh sách ID tổ chức từ danh sách đơn vị hành chính. */
    private AreaScopeResult filterByUnits(Set<UUID> unitIds) {
        Set<UUID> provinceIds = new HashSet<>();
        Set<UUID> communeIds = new HashSet<>();

        for (AdministrativeUnit unit : administrativeUnitRepository.findAllById(unitIds)) {
            if (unit.getLevel() == AdministrativeUnitLevel.PROVINCE) {
                provinceIds.add(unit.getId());
            } else if (unit.getLevel() == AdministrativeUnitLevel.COMMUNE) {
                communeIds.add(unit.getId());
            }
        }

        if (provinceIds.isEmpty() && communeIds.isEmpty()) {
            return AreaScopeResult.of(Set.of());
        }

        List<Organization> organizations = organizationRepository.findByProvince_IdInOrCommune_IdIn(
                provinceIds, communeIds);

        Set<UUID> organizationIds = new HashSet<>();
        for (Organization organization : organizations) {
            organizationIds.add(organization.getOrganizationId());
        }
        return AreaScopeResult.of(organizationIds);
    }
}
