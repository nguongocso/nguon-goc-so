package vn.nguongocso.farm.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.response.AttachmentResponse;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.projection.FarmLogProjection;
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
	
	private static final String EVENT_RECORDER_ROLE = "VT-03";
	private static final String ORG_MANAGER_ROLE = "VT-02";
	
	private static final String CREATE_PERMISSION_MESSAGE =
	        "Bạn không có quyền ghi nhật ký canh tác.";
	private static final String VIEW_PERMISSION_MESSAGE =
	        "Bạn không có quyền xem lịch sử nhật ký canh tác.";
	private static final String ORGANIZATION_ACCESS_MESSAGE =
	        "Bạn không thuộc tổ chức của lô sản xuất.";
	private static final String PRODUCTION_LOT_NOT_FOUND_MESSAGE =
	        "Không tìm thấy lô sản xuất";
	private static final String INVALID_LOT_STATUS_MESSAGE =
	        "Chỉ được ghi nhật ký cho lô đã duyệt hoặc đang thu hoạch.";
	
	private static final Sort FARM_LOG_SORT =
	        Sort.by(
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
		
		validateRole(currentUser,EVENT_RECORDER_ROLE,CREATE_PERMISSION_MESSAGE);

		ProductionLot productionLot = getProductionLot(request.getProductionLotId());
		
		validateProductionLotStatus(productionLot);
		
		validateOrganizationAccess(currentUser, productionLot);

		FarmLog farmLog = buildFarmLog(request, productionLot, currentUser.getUser());

		FarmLog saved = farmLogRepository.save(farmLog);

		return toResponse(saved);
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

		return FarmLog.builder().productionLotId(productionLot).activityType(request.getActivityType())
				.material(request.getMaterial()).quantity(request.getQuantity()).unit(request.getUnit())
				.executedDate(request.getExecutedDate()).notes(request.getNotes()).createdBy(createdBy).build();
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

	        throw new BusinessException(
	            ORGANIZATION_ACCESS_MESSAGE);
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

	        throw new BusinessException(
	        		INVALID_LOT_STATUS_MESSAGE);
	    }
	}

	/**
	 * Lấy danh sách nhật ký canh tác của lô sản xuất theo phân trang.
	 *
	 * @param productionLotId mã lô sản xuất
	 * @param page số trang (bắt đầu từ 0)
	 * @param size số bản ghi trên mỗi trang
	 * @return dữ liệu nhật ký canh tác theo phân trang
	 */
	@Override
	public PageResponse<FarmLogResponse> getFarmLogsByProductionLot(
	        UUID productionLotId,
	        int page,
	        int size) {

	    CustomUserDetails currentUser = getCurrentUser();

	    // Allow both VT-02 (manager) and VT-03 (event recorder) to view farm logs
	    String roleCode = currentUser.getRoleCode();
	    if (!ORG_MANAGER_ROLE.equals(roleCode) && !EVENT_RECORDER_ROLE.equals(roleCode)) {
	        throw new BusinessException(VIEW_PERMISSION_MESSAGE);
	    }

	    ProductionLot productionLot = getProductionLot(productionLotId);

	    validateOrganizationAccess(currentUser, productionLot);

	    Page<FarmLogProjection> farmLogs =
	            findFarmLogs(productionLot, page, size);

	    List<FarmLogProjection> content = farmLogs.getContent();

	    // Batch load attachments for all farm logs to avoid N+1
	    Map<UUID, List<AttachmentResponse>> attachmentsByLogId = loadAttachmentsForFarmLogs(content);

	    List<FarmLogResponse> responses = content
	            .stream()
	            .map(projection -> {
	                FarmLogResponse response = toResponse(projection);
	                response.setAttachments(attachmentsByLogId.getOrDefault(projection.getId(), Collections.emptyList()));
	                return response;
	            })
	            .toList();

	    return PageResponse.from(farmLogs, responses);
	}
	
	private Page<FarmLogProjection> findFarmLogs(
	        ProductionLot productionLot,
	        int page,
	        int size) {

	    Pageable pageable = buildPageable(page, size);

	    return farmLogRepository.findByProductionLot(
	            productionLot,
	            pageable);
	}
		
	private Pageable buildPageable(int page, int size) {
	    return PageRequest.of(page, size, FARM_LOG_SORT);
	}
	
	private FarmLogResponse toResponse(FarmLogProjection projection) {
	    return FarmLogResponse.builder()
	            .id(projection.getId())
	            .productionLotId(projection.getProductionLotId())
	            .productionLotName(projection.getProductionLotName())
	            .activityType(projection.getActivityType())
	            .material(projection.getMaterial())
	            .quantity(projection.getQuantity())
	            .unit(projection.getUnit())
	            .executedDate(projection.getExecutedDate())
	            .notes(projection.getNotes())
	            .createdByName(projection.getCreatedByName())
	            .createdAt(projection.getCreatedAt())
	            .build();
	}

	/**
	 * Batch load attachments for all farm logs in the current page.
	 * Groups attachments by farm log ID to avoid N+1 queries.
	 */
	private Map<UUID, List<AttachmentResponse>> loadAttachmentsForFarmLogs(
	        List<FarmLogProjection> farmLogs) {

	    if (farmLogs == null || farmLogs.isEmpty()) {
	        return Collections.emptyMap();
	    }

	    List<UUID> farmLogIds = farmLogs.stream()
	            .map(FarmLogProjection::getId)
	            .collect(Collectors.toList());

	    List<FarmLogAttachment> attachments = attachmentRepository.findByFarmLogIdIn(farmLogIds);

	    return attachments.stream()
	            .collect(Collectors.groupingBy(
	                    att -> att.getFarmLog().getId(),
	                    Collectors.mapping(this::toAttachmentResponse, Collectors.toList())
	            ));
	}

	private AttachmentResponse toAttachmentResponse(FarmLogAttachment attachment) {
	    return AttachmentResponse.builder()
	            .id(attachment.getId())
	            .farmLogId(attachment.getFarmLog().getId())
	            .fileName(attachment.getFileName())
	            .fileSize(attachment.getFileSize())
	            .fileType(attachment.getFileType())
	            .fileUrl("/" + attachment.getFilePath())
	            .description(attachment.getDescription())
	            .uploadedBy(attachment.getUploadedBy().getFullName())
	            .uploadedAt(attachment.getUploadedAt())
	            .build();
	}
	
}
