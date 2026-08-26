package vn.nguongocso.farm.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.CreateFarmAreaRequest;
import vn.nguongocso.farm.dto.response.FarmAreaResponse;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.service.FarmAreaService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;

import vn.nguongocso.farm.dto.request.UpdateFarmAreaRequest;
import vn.nguongocso.farm.repository.ProductionLotRepository;

/**
 * Triển khai các nghiệp vụ quản lý vùng trồng.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FarmAreaServiceImpl implements FarmAreaService {
	private final FarmAreaRepository farmAreaRepository;
	private final ProductCategoryRepository productCategoryRepository;
	private final OrganizationRepository organizationRepository;
	private final ProductionLotRepository productionLotRepository;
	private final GeometryFactory geometryFactory;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	public List<FarmAreaResponse> getFarmAreas() {
		return getFarmAreas(null);
	}

	@Override
	public List<FarmAreaResponse> getFarmAreas(Boolean activeOnly) {
		CustomUserDetails currentUser = getCurrentUser();

		List<FarmArea> farmAreas;
		if (Boolean.TRUE.equals(activeOnly)) {
			farmAreas = farmAreaRepository
					.findByOrganization_OrganizationIdAndIsActiveTrue(currentUser.getOrganizationId());
		} else {
			farmAreas = farmAreaRepository
					.findByOrganization_OrganizationId(currentUser.getOrganizationId());
		}

		return farmAreas.stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	public FarmAreaResponse getFarmAreaById(UUID id) {
		CustomUserDetails currentUser = getCurrentUser();
		FarmArea farmArea = farmAreaRepository.findByIdAndOrganization_OrganizationId(id, currentUser.getOrganizationId())
				.orElseThrow(() -> new BusinessException("Không tìm thấy vùng trồng"));
		return toResponse(farmArea);
	}

	/**
	 * Tạo mới vùng trồng cho tổ chức của người dùng đang đăng nhập.
	 *
	 * @param request thông tin vùng trồng cần tạo
	 * @return thông tin vùng trồng sau khi tạo
	 */
	@Override
	public FarmAreaResponse create(CreateFarmAreaRequest request) {

		CustomUserDetails currentUser = getCurrentUser();

		Organization organization = getOrganization(currentUser.getOrganizationId());

		ProductCategory cropType = getCropType(request.getCropType());

		FarmArea farmArea = buildFarmArea(request, organization, cropType);

		FarmArea saved = farmAreaRepository.save(farmArea);

		// Ghi nhật ký hoạt động (TASK-27): tạo vùng trồng
		publishActivityLog(
				currentUser,
				"CREATE_FARM_AREA",
				"Tạo vùng trồng '" + saved.getName() + "'",
				"FARM_AREA",
				saved.getId().toString());

		return toResponse(saved);
	}

	/**
	 * Cập nhật thông tin vùng trồng (US NCL-02-CN-005).
	 */
	@Override
	public FarmAreaResponse update(UUID id, UpdateFarmAreaRequest request) {
		CustomUserDetails currentUser = getCurrentUser();

		FarmArea farmArea = farmAreaRepository.findByIdAndOrganization_OrganizationId(id, currentUser.getOrganizationId())
				.orElseThrow(() -> new BusinessException("Không tìm thấy vùng trồng hoặc bạn không có quyền cập nhật"));

		if (request.getArea() == null || request.getArea().compareTo(java.math.BigDecimal.ZERO) <= 0) {
			throw new BusinessException("Diện tích phải là số dương lớn hơn 0");
		}
		if (request.getLatitude() == null || request.getLongitude() == null) {
			throw new BusinessException("Kinh độ và vĩ độ không được để trống");
		}

		ProductCategory newCropType = getCropType(request.getCropType());
		AreaUnit newAreaUnit = request.getAreaUnit() != null ? request.getAreaUnit() : AreaUnit.HA;
		Point newLocation = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
		java.math.BigDecimal newAreaInHa = newAreaUnit.toHectares(request.getArea());

		long associatedLotsCount = productionLotRepository.countByFarmAreaId(id);

		String oldValues = String.format("Tên: '%s', Loại cây: '%s', Diện tích: %s %s, Vị trí: (Lat: %.6f, Long: %.6f)",
				farmArea.getName(),
				farmArea.getCropType() != null ? farmArea.getCropType().getName() : "",
				farmArea.getArea(),
				farmArea.getAreaUnit(),
				farmArea.getLocation() != null ? farmArea.getLocation().getY() : 0.0,
				farmArea.getLocation() != null ? farmArea.getLocation().getX() : 0.0);

		farmArea.setName(request.getName());
		farmArea.setCropType(newCropType);
		farmArea.setLocation(newLocation);
		farmArea.setArea(newAreaInHa);
		farmArea.setAreaUnit(newAreaUnit);

		FarmArea saved = farmAreaRepository.save(farmArea);

		String logDescription = String.format("Cập nhật vùng trồng '%s'. Giá trị trước khi sửa: [%s]. Số lô sản xuất bị ảnh hưởng: %d",
				saved.getName(), oldValues, associatedLotsCount);

		publishActivityLog(
				currentUser,
				"UPDATE_FARM_AREA",
				logDescription,
				"FARM_AREA",
				saved.getId().toString());

		return toResponse(saved);
	}

	/**
	 * Đổi trạng thái kích hoạt / ngừng sử dụng vùng trồng (US NCL-02-CN-005).
	 */
	@Override
	public FarmAreaResponse toggleStatus(UUID id, boolean isActive) {
		CustomUserDetails currentUser = getCurrentUser();

		FarmArea farmArea = farmAreaRepository.findByIdAndOrganization_OrganizationId(id, currentUser.getOrganizationId())
				.orElseThrow(() -> new BusinessException("Không tìm thấy vùng trồng hoặc bạn không có quyền thao tác"));

		farmArea.setIsActive(isActive);
		FarmArea saved = farmAreaRepository.save(farmArea);

		String action = isActive ? "ACTIVATE_FARM_AREA" : "DEACTIVATE_FARM_AREA";
		String description = (isActive ? "Kích hoạt lại" : "Ngừng sử dụng") + " vùng trồng '" + saved.getName() + "'";

		publishActivityLog(
				currentUser,
				action,
				description,
				"FARM_AREA",
				saved.getId().toString());

		return toResponse(saved);
	}

	/**
	 * Xóa vùng trồng (US NCL-02-CN-005). Chặn xóa nếu có lô sản xuất liên quan.
	 */
	@Override
	public void delete(UUID id) {
		CustomUserDetails currentUser = getCurrentUser();

		FarmArea farmArea = farmAreaRepository.findByIdAndOrganization_OrganizationId(id, currentUser.getOrganizationId())
				.orElseThrow(() -> new BusinessException("Không tìm thấy vùng trồng hoặc bạn không có quyền xóa"));

		long associatedLotsCount = productionLotRepository.countByFarmAreaId(id);
		if (associatedLotsCount > 0) {
			throw new BusinessException("Không thể xóa vùng trồng đã có " + associatedLotsCount + " lô sản xuất liên quan. Bạn chỉ có thể chuyển sang trạng thái Ngừng sử dụng.");
		}

		farmAreaRepository.delete(farmArea);

		publishActivityLog(
				currentUser,
				"DELETE_FARM_AREA",
				"Xóa vùng trồng '" + farmArea.getName() + "'",
				"FARM_AREA",
				id.toString());
	}

	/**
	 * Ghi nhật ký hoạt động theo convention của hệ thống (TASK-27).
	 * <p>
	 * Actor lấy từ người dùng đã xác thực trong security context,
	 * organization lấy từ organization của người thực hiện.
	 */
	private void publishActivityLog(CustomUserDetails currentUser, String action, String description,
			String entityType, String entityId) {
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

	@Override
	public List<AreaUnit> getAreaUnits() {
		return Arrays.asList(AreaUnit.values());
	}

	private CustomUserDetails getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		return (CustomUserDetails) authentication.getPrincipal();
	}

	private Organization getOrganization(UUID organizationId) {
		return organizationRepository.findById(organizationId)
				.orElseThrow(() -> new BusinessException("Không tìm thấy tổ chức"));
	}

	private ProductCategory getCropType(UUID cropTypeId) {
		ProductCategory cropType = productCategoryRepository.findById(cropTypeId)
				.orElseThrow(() -> new BusinessException("Không tìm thấy loại cây trồng"));
		if (Boolean.FALSE.equals(cropType.getIsActive())) {
			throw new BusinessException(
					"Loại cây trồng " + cropType.getName() + " hiện đã bị ẩn và không thể dùng để tạo vùng trồng mới");
		}
		return cropType;
	}

	private FarmArea buildFarmArea(CreateFarmAreaRequest request, Organization organization, ProductCategory cropType) {

		Point location = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));

		AreaUnit areaUnit = request.getAreaUnit() != null ? request.getAreaUnit() : AreaUnit.HA;

		FarmArea farmArea = new FarmArea();
		farmArea.setOrganization(organization);
		farmArea.setName(request.getName());
		farmArea.setCropType(cropType);
		farmArea.setLocation(location);
		farmArea.setArea(areaUnit.toHectares(request.getArea()));
		farmArea.setAreaUnit(areaUnit);
		farmArea.setIsActive(true);

		return farmArea;
	}

	private FarmAreaResponse toResponse(FarmArea farmArea) {

		Point point = farmArea.getLocation();
		long associatedLotsCount = productionLotRepository.countByFarmAreaId(farmArea.getId());

		return FarmAreaResponse.builder().id(farmArea.getId()).name(farmArea.getName())

				.organizationId(farmArea.getOrganization().getOrganizationId())
				.organizationName(farmArea.getOrganization().getName())

				.cropTypeId(farmArea.getCropType().getId()).cropTypeName(farmArea.getCropType().getName())

				.latitude(point != null ? point.getY() : null).longitude(point != null ? point.getX() : null)

				.area(farmArea.getArea()).areaUnit(farmArea.getAreaUnit())

				.isActive(farmArea.getIsActive() != null ? farmArea.getIsActive() : true)

				.associatedLotsCount(associatedLotsCount)

				.createdAt(farmArea.getCreatedAt()).updatedAt(farmArea.getUpdatedAt()).build();
	}
}