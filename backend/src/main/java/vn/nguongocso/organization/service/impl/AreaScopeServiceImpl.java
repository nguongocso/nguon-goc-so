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

/**
 * Phân tích phạm vi địa bàn cho báo cáo — rule bảo mật số 1 của NCL-670.
 *
 * <p>
 * VT-05 chưa được gán địa bàn nào ⇒ {@link AreaScopeResult#emptyScope()};
 * caller BẮT BUỘC trả dữ liệu rỗng kèm thông báo, không bao giờ fallback sang
 * toàn bộ dữ liệu.
 * </p>
 */
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

			// Rule bảo mật số 1: chưa gán địa bàn nào => KHÔNG BAO GIỜ trả toàn bộ dữ liệu.
			if (assignments.isEmpty()) {
				return AreaScopeResult.emptyScope();
			}

			Set<UUID> assignedUnitIds = new HashSet<>();
			for (UserAreaAssignment assignment : assignments) {
				assignedUnitIds.add(assignment.getUnit().getId());
			}

			// Param unitIds chỉ có thể thu hẹp, không bao giờ mở rộng tập đã gán.
			Set<UUID> effectiveUnits = assignedUnitIds;
			if (unitIds != null && !unitIds.isEmpty()) {
				effectiveUnits = new HashSet<>(assignedUnitIds);
				effectiveUnits.retainAll(unitIds);
			}
			return filterByUnits(effectiveUnits);
		}

		// Vai trò khác: giữ nguyên hành vi hiện có của từng endpoint
		// (tự kiểm tra quyền theo logic cũ).
		return AreaScopeResult.all();
	}

	/**
	 * Giải tập tổ chức từ danh sách đơn vị hành chính: khớp province_id với các
	 * đơn vị cấp tỉnh hoặc commune_id với các đơn vị cấp xã. Tổ chức chưa map
	 * (NULL/NULL) không bao giờ nằm trong kết quả.
	 */
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
