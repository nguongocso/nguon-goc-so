package vn.nguongocso.farm.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.FarmLogService;

/**
 * Triển khai dịch vụ quản lý nhật ký canh tác.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FarmLogServiceImpl implements FarmLogService {

	private final FarmLogRepository farmLogRepository;
	private final ProductionLotRepository productionLotRepository;
	private final FarmLogAttachmentRepository attachmentRepository;

	private final ApplicationEventPublisher eventPublisher;

	private static final String EVENT_RECORDER_ROLE = "VT-03";
	private static final String ORG_MANAGER_ROLE = "VT-02";

	private static final String CREATE_PERMISSION_MESSAGE = "Bạn không có quyền ghi nhật ký canh tác.";
	private static final String VIEW_PERMISSION_MESSAGE = "Bạn không có quyền xem lịch sử nhật ký canh tác.";
	private static final String ORGANIZATION_ACCESS_MESSAGE = "Bạn không thuộc tổ chức của lô sản xuất.";
	private static final String PRODUCTION_LOT_NOT_FOUND_MESSAGE = "Không tìm thấy lô sản xuất";
	private static final String INVALID_LOT_STATUS_MESSAGE = "Chỉ được ghi nhật ký cho lô đã duyệt hoặc đang thu hoạch.";

	private static final Sort FARM_LOG_SORT = Sort.by(
			Sort.Order.desc("executedDate"),
			Sort.Order.desc("createdAt"));

	/**
	 * Tạo nhật ký canh tác.
	 *
	 * @param request thông tin nhật ký
	 * @return thông tin nhật ký đã tạo
	 */
	@Override
	public FarmLogResponse create(CreateFarmLogRequest request) {

		CustomUserDetails currentUser = getCurrentUser();

		validateRole(currentUser, EVENT_RECORDER_ROLE, CREATE_PERMISSION_MESSAGE);

		ProductionLot productionLot = getProductionLot(request.getProductionLotId());

		validateProductionLotStatus(productionLot);

		validateOrganizationAccess(currentUser, productionLot);

		FarmLog farmLog = buildFarmLog(request, productionLot, currentUser.getUser());

		FarmLog saved = farmLogRepository.save(farmLog);

		publishActivityLog(
				currentUser,
				"CREATE",
				"Ghi nhật ký canh tác cho lô " + saved.getProductionLotId().getName(),
				"FarmLog",
				saved.getId().toString());

		return toResponse(saved);
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
				.ipAddress(getClientIp()) // lấy từ request context nếu có
				.timestamp(LocalDateTime.now())
				.build());
	}

	private String getClientIp() {
		// Có thể lấy từ SecurityContext hoặc truyền từ controller
		return "127.0.0.1"; // tạm thời
	}

	private CustomUserDetails getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return (CustomUserDetails) authentication.getPrincipal();
	}

	private ProductionLot getProductionLot(UUID productionLotId) {
		return productionLotRepository.findById(productionLotId)
				.orElseThrow(() -> new BusinessException(PRODUCTION_LOT_NOT_FOUND_MESSAGE));
	}

	private FarmLog buildFarmLog(CreateFarmLogRequest request, ProductionLot productionLot, User createdBy) {

		return FarmLog.builder()
				.productionLotId(productionLot)
				.activityType(request.getActivityType())
				.material(request.getMaterial())
				.quantity(request.getQuantity())
				.unit(request.getUnit())
				.executedDate(request.getExecutedDate())
				.notes(request.getNotes())
				.createdBy(createdBy)
				.build();
	}

	private FarmLogResponse toResponse(FarmLog farmLog) {
		return FarmLogResponse.builder()
				.id(farmLog.getId())
				.productionLotId(farmLog.getProductionLotId().getId())
				.productionLotName(farmLog.getProductionLotId().getName())
				.activityType(farmLog.getActivityType())
				.material(farmLog.getMaterial())
				.quantity(farmLog.getQuantity())
				.unit(farmLog.getUnit())
				.executedDate(farmLog.getExecutedDate())
				.notes(farmLog.getNotes())
				.createdByName(farmLog.getCreatedBy().getFullName())
				.createdAt(farmLog.getCreatedAt())
				.build();
	}

	private void validateOrganizationAccess(
			CustomUserDetails currentUser,
			ProductionLot productionLot) {

		if (!productionLot.getFarmArea()
				.getOrganization()
				.getOrganizationId()
				.equals(currentUser.getOrganizationId())) {

			throw new BusinessException(ORGANIZATION_ACCESS_MESSAGE);
		}
	}

	private void validateRole(
			CustomUserDetails currentUser,
			String expectedRole,
			String message) {

		if (!expectedRole.equals(currentUser.getRoleCode())) {
			throw new BusinessException(message);
		}
	}

	private void validateProductionLotStatus(ProductionLot productionLot) {

		if (productionLot.getStatus() != ProductionLotStatus.APPROVED
				&& productionLot.getStatus() != ProductionLotStatus.HARVESTED) {

			throw new BusinessException(INVALID_LOT_STATUS_MESSAGE);
		}
	}

	/**
	 * Lấy danh sách nhật ký canh tác của lô sản xuất theo phân trang.
	 *
	 * @param productionLotId mã lô sản xuất
	 * @param page            số trang (bắt đầu từ 0)
	 * @param size            số bản ghi trên mỗi trang
	 * @return dữ liệu nhật ký canh tác theo phân trang
	 */
	@Override
	public PageResponse<FarmLogResponse> getFarmLogsByProductionLot(
			UUID productionLotId,
			int page,
			int size) {

		CustomUserDetails currentUser = getCurrentUser();

		String roleCode = currentUser.getRoleCode();
		if (!ORG_MANAGER_ROLE.equals(roleCode) && !EVENT_RECORDER_ROLE.equals(roleCode)) {
			throw new BusinessException(VIEW_PERMISSION_MESSAGE);
		}

		ProductionLot productionLot = getProductionLot(productionLotId);
		validateOrganizationAccess(currentUser, productionLot);

		Pageable pageable = PageRequest.of(page, size, FARM_LOG_SORT);
		Page<FarmLog> farmLogs = farmLogRepository.findByProductionLotId(productionLot, pageable);

		List<FarmLogResponse> responses = farmLogs.getContent().stream()
				.map(log -> {
					int count = attachmentRepository.countByFarmLogId(log.getId());
					return FarmLogResponse.builder()
							.id(log.getId())
							.productionLotId(log.getProductionLotId().getId())
							.productionLotName(log.getProductionLotId().getName())
							.activityType(log.getActivityType())
							.material(log.getMaterial())
							.quantity(log.getQuantity())
							.unit(log.getUnit())
							.executedDate(log.getExecutedDate())
							.notes(log.getNotes())
							.createdByName(log.getCreatedBy().getFullName())
							.createdAt(log.getCreatedAt())
							.attachmentCount(count)
							.build();
				})
				.toList();

		return PageResponse.from(farmLogs, responses);
	}
}