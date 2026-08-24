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
	private final GeometryFactory geometryFactory;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * Tạo mới vùng trồng cho tổ chức của người dùng đang đăng nhập.
	 * 
	 * @param request thông tin vùng trồng cần tạo
	 * @return thông tin vùng trồng sau khi tạo
	 */
	@Override
	public List<FarmAreaResponse> getFarmAreas() {
		CustomUserDetails currentUser = getCurrentUser();

		List<FarmArea> farmAreas = farmAreaRepository
				.findByOrganization_OrganizationId(currentUser.getOrganizationId());

		return farmAreas.stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
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

	/**
	 * Cập nhật thông tin vùng trồng.
	 *
	 * @param farmAreaId ID của vùng trồng cần cập nhật
	 * @param request    thông tin cập nhật
	 * @return thông tin vùng trồng sau khi cập nhật
	 */
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

		return farmArea;
	}

	private FarmAreaResponse toResponse(FarmArea farmArea) {

		Point point = farmArea.getLocation();

		return FarmAreaResponse.builder().id(farmArea.getId()).name(farmArea.getName())

				.organizationId(farmArea.getOrganization().getOrganizationId())
				.organizationName(farmArea.getOrganization().getName())

				.cropTypeId(farmArea.getCropType().getId()).cropTypeName(farmArea.getCropType().getName())

				.latitude(point.getY()).longitude(point.getX())

				.area(farmArea.getArea()).areaUnit(farmArea.getAreaUnit())

				.createdAt(farmArea.getCreatedAt()).updatedAt(farmArea.getUpdatedAt()).build();
	}

}