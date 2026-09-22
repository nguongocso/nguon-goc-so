package vn.nguongocso.farm.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.CorrectFarmLogRequest;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.request.FarmLogCorrectionData;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.FarmLogService;
import vn.nguongocso.farm.service.MilestoneReminderService;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Triển khai dịch vụ quản lý nhật ký canh tác.
*/
@Slf4j
@Service
@Transactional
public class FarmLogServiceImpl implements FarmLogService {
    private final FarmLogRepository farmLogRepository;

    private final ProductionLotRepository productionLotRepository;

    private final FarmLogAttachmentRepository attachmentRepository;

    private final TraceCodeRepository traceCodeRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final Clock clock;

    private final MilestoneReminderService milestoneReminderService;

    /** Khởi tạo service nhật ký canh tác. */
    public FarmLogServiceImpl(
            FarmLogRepository farmLogRepository,
            ProductionLotRepository productionLotRepository,
            FarmLogAttachmentRepository attachmentRepository,
            TraceCodeRepository traceCodeRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this(farmLogRepository, productionLotRepository, attachmentRepository, traceCodeRepository, eventPublisher,
                clock, null);
    }

    /** Khởi tạo service nhật ký canh tác có nhắc mốc. */
    @Autowired
    public FarmLogServiceImpl(
            FarmLogRepository farmLogRepository,
            ProductionLotRepository productionLotRepository,
            FarmLogAttachmentRepository attachmentRepository,
            TraceCodeRepository traceCodeRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            @Autowired(required = false) MilestoneReminderService milestoneReminderService) {
        this.farmLogRepository = farmLogRepository;
        this.productionLotRepository = productionLotRepository;
        this.attachmentRepository = attachmentRepository;
        this.traceCodeRepository = traceCodeRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.milestoneReminderService = milestoneReminderService;
    }

    private static final String EVENT_RECORDER_ROLE = "VT-03";

    private static final String ORG_MANAGER_ROLE = "VT-02";

    private static final String CREATE_PERMISSION_MESSAGE = "Bạn không có quyền ghi nhật ký canh tác.";

    private static final String VIEW_PERMISSION_MESSAGE = "Bạn không có quyền xem lịch sử nhật ký canh tác.";

    private static final String CORRECT_PERMISSION_MESSAGE = "Bạn không có quyền đính chính nhật ký canh tác.";

    private static final String CORRECT_NOT_OWNER_MESSAGE = "Bạn chỉ được đính chính nhật ký do bạn ghi.";

    private static final String FARM_LOG_NOT_FOUND_MESSAGE = "Không tìm thấy nhật ký canh tác";

    private static final String NO_CHANGED_FIELD_MESSAGE = "Phải có ít nhất một trường được đính chính so với bản gốc.";

    private static final String REASON_REQUIRED_MESSAGE = "Lý do đính chính không được để trống";

    private static final String ACTIVATED_TRACE_CODE_MESSAGE =
            "Lô sản xuất đã kích hoạt mã truy xuất. Bạn không thể đính chính nhật ký này.";

    private static final String ORGANIZATION_ACCESS_MESSAGE = "Bạn không thuộc tổ chức của lô sản xuất.";

    private static final String PRODUCTION_LOT_NOT_FOUND_MESSAGE = "Không tìm thấy lô sản xuất";

    private static final String INVALID_LOT_STATUS_MESSAGE = "Chỉ được ghi nhật ký cho lô đã duyệt hoặc đang thu hoạch.";

    private static final String CANCELLED_LOT_MESSAGE = "Lô sản xuất đã bị hủy, không thể thao tác nhật ký canh tác.";

    private static final Sort FARM_LOG_SORT = Sort.by(
            Sort.Order.desc("executedDate"),
            Sort.Order.desc("createdAt"));

    /** Tạo mới nhật ký canh tác cho lô sản xuất. */
    @Override
    public FarmLogResponse create(CreateFarmLogRequest request) {

        CustomUserDetails currentUser = getCurrentUser();

        String roleCode = currentUser.getRoleCode();
        if (!ORG_MANAGER_ROLE.equals(roleCode) && !EVENT_RECORDER_ROLE.equals(roleCode)) {
            throw new BusinessException(CREATE_PERMISSION_MESSAGE);
        }

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

        if (milestoneReminderService != null) {
            try {
                if (request.getMilestoneId() != null) {
                    milestoneReminderService.completeRemindersForLotAndMilestone(
                            saved.getProductionLotId().getId(),
                            request.getMilestoneId());
                } else {
                    milestoneReminderService.completeRemindersForLotAndActivity(
                            saved.getProductionLotId().getId(),
                            saved.getActivityType());
                }
            } catch (Exception e) {
                log.warn("Không thể tự động đóng nhắc việc canh tác sau khi ghi nhật ký: {}", e.getMessage());
            }
        }

        return toResponse(saved);
    }

    /** Đính chính nhật ký canh tác, tạo bản ghi mới và giữ nguyên bản gốc. */
    @Override
    public FarmLogResponse correctFarmLog(UUID id, CorrectFarmLogRequest request) {

        CustomUserDetails currentUser = getCurrentUser();

        String roleCode = currentUser.getRoleCode();
        boolean isManager = ORG_MANAGER_ROLE.equals(roleCode);

        if (!isManager && !EVENT_RECORDER_ROLE.equals(roleCode)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, CORRECT_PERMISSION_MESSAGE);
        }

        FarmLog targetLog = farmLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(FARM_LOG_NOT_FOUND_MESSAGE));

        ProductionLot productionLot = targetLog.getProductionLotId();

        validateOrganizationAccess(currentUser, productionLot);

        if (!isManager && !targetLog.getCreatedBy().getUserId().equals(currentUser.getUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, CORRECT_NOT_OWNER_MESSAGE);
        }

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException(REASON_REQUIRED_MESSAGE);
        }

        FarmLog root = resolveRoot(targetLog);
        FarmLog effective = findLatestEffectiveVersion(root);

        applyCorrectionChecks(isManager, productionLot, request.getCorrectionData(), effective);

        User actor = currentUser.getUser();
        LocalDateTime now = LocalDateTime.now(clock);

        FarmLog correction = buildCorrection(effective, root, request, actor, now);
        FarmLog saved = farmLogRepository.save(correction);

        effective.setIsCorrected(true);
        farmLogRepository.save(effective);

        publishActivityLog(
                currentUser,
                "CORRECT",
                "Đính chính nhật ký canh tác cho lô " + saved.getProductionLotId().getName(),
                "FarmLog",
                saved.getId().toString());

        return toResponse(saved);
    }

    /** Tìm bản gốc (root) của một nhật ký trong chuỗi đính chính. */
    private FarmLog resolveRoot(FarmLog log) {
        FarmLog current = log;
        while (current.isCorrection() && current.getOriginalFarmLogId() != null) {
            current = current.getOriginalFarmLogId();
        }
        return current;
    }

    /** Tìm bản ghi có hiệu lực hiện tại trong chuỗi đính chính. */
    private FarmLog findLatestEffectiveVersion(FarmLog root) {
        List<FarmLog> corrections =
                farmLogRepository.findByOriginalFarmLogId_IdOrderByCreatedAtDesc(root.getId());

        for (FarmLog correction : corrections) {
            if (!correction.isCorrected()) {
                return correction;
            }
        }
        return root;
    }

    /** Kiểm tra điều kiện nghiệp vụ trước khi tạo bản đính chính. */
    private void applyCorrectionChecks(
            boolean isManager,
            ProductionLot productionLot,
            FarmLogCorrectionData data,
            FarmLog effective) {

        if (productionLot.getStatus() == ProductionLotStatus.CANCELLED) {
            throw new BusinessException(CANCELLED_LOT_MESSAGE);
        }

        if (!isManager && traceCodeRepository.existsActivatedByProductionLotId(productionLot.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, ACTIVATED_TRACE_CODE_MESSAGE);
        }

        boolean changed =
                isChanged(data.getActivityType(), effective.getActivityType())
                        || isChanged(data.getMaterial(), effective.getMaterial())
                        || isChanged(data.getQuantity(), effective.getQuantity())
                        || isChanged(data.getUnit(), effective.getUnit())
                        || isChanged(data.getExecutedDate(), effective.getExecutedDate())
                        || isChanged(data.getNotes(), effective.getNotes());

        if (!changed) {
            throw new BusinessException(NO_CHANGED_FIELD_MESSAGE);
        }

        if (data.getExecutedDate() != null && data.getExecutedDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessException("Ngày thực hiện không được là ngày ở tương lai.");
        }
    }

    /** Xác định giá trị mới có thay đổi so với giá trị hiện tại hay không. */
    private boolean isChanged(Object newValue, Object currentValue) {
        return newValue != null && !newValue.equals(currentValue);
    }

    /** Tạo bản ghi đính chính từ dữ liệu hiệu lực và các trường được thay đổi. */
    private FarmLog buildCorrection(
            FarmLog effective,
            FarmLog root,
            CorrectFarmLogRequest request,
            User correctedBy,
            LocalDateTime createdAt) {

        FarmLogCorrectionData data = request.getCorrectionData();

        return FarmLog.builder()
                .productionLotId(effective.getProductionLotId())
                .activityType(data.getActivityType() != null ? data.getActivityType() : effective.getActivityType())
                .material(data.getMaterial() != null ? data.getMaterial() : effective.getMaterial())
                .quantity(data.getQuantity() != null ? data.getQuantity() : effective.getQuantity())
                .unit(data.getUnit() != null ? data.getUnit() : effective.getUnit())
                .executedDate(data.getExecutedDate() != null ? data.getExecutedDate() : effective.getExecutedDate())
                .notes(data.getNotes() != null ? data.getNotes() : effective.getNotes())
                .originalFarmLogId(root)
                .isCorrection(true)
                .correctionReason(request.getReason().trim())
                .correctedBy(correctedBy)
                .createdBy(correctedBy)
                .createdAt(createdAt)
                .build();
    }

    /** Ghi nhật ký hoạt động theo convention của hệ thống. */
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
                .ipAddress(getClientIp())
                .timestamp(LocalDateTime.now(clock))
                .build());
    }

    /** Lấy địa chỉ IP của máy khách. */
    private String getClientIp() {
        return "127.0.0.1";
    }

    /** Lấy thông tin người dùng đang đăng nhập. */
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) authentication.getPrincipal();
    }

    /** Lấy lô sản xuất theo ID. */
    private ProductionLot getProductionLot(UUID productionLotId) {
        return productionLotRepository.findById(productionLotId)
                .orElseThrow(() -> new BusinessException(PRODUCTION_LOT_NOT_FOUND_MESSAGE));
    }

    /** Xây dựng đối tượng nhật ký canh tác từ dữ liệu yêu cầu. */
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
                .createdAt(LocalDateTime.now(clock))
                .build();
    }

    /** Chuyển entity nhật ký canh tác sang DTO phản hồi. */
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
                .createdById(farmLog.getCreatedBy().getUserId())
                .createdAt(farmLog.getCreatedAt())
                .originalFarmLogId(farmLog.getOriginalFarmLogId() != null
                        ? farmLog.getOriginalFarmLogId().getId()
                        : null)
                .isCorrection(farmLog.isCorrection())
                .correctionReason(farmLog.getCorrectionReason())
                .correctedByName(farmLog.getCorrectedBy() != null
                        ? farmLog.getCorrectedBy().getFullName()
                        : null)
                .isCorrected(farmLog.isCorrected())
                .build();
    }

    /** Kiểm tra người dùng thuộc tổ chức sở hữu lô sản xuất. */
    private void validateOrganizationAccess(
            CustomUserDetails currentUser,
            ProductionLot productionLot) {

        UUID organizationId = null;
        if (productionLot.getOrganization() != null) {
            organizationId = productionLot.getOrganization().getOrganizationId();
        } else if (productionLot.getFarmArea() != null
                && productionLot.getFarmArea().getOrganization() != null) {
            organizationId = productionLot.getFarmArea().getOrganization().getOrganizationId();
        }

        if (organizationId == null
                || !organizationId.equals(currentUser.getOrganizationId())) {

            throw new BusinessException(ORGANIZATION_ACCESS_MESSAGE);
        }
    }

    /** Kiểm tra vai trò người dùng khớp với vai trò kỳ vọng. */
    private void validateRole(
            CustomUserDetails currentUser,
            String expectedRole,
            String message) {

        if (!expectedRole.equals(currentUser.getRoleCode())) {
            throw new BusinessException(message);
        }
    }

    /** Kiểm tra trạng thái lô cho phép ghi nhật ký canh tác. */
    private void validateProductionLotStatus(ProductionLot productionLot) {

        if (productionLot.getStatus() == ProductionLotStatus.CANCELLED) {
            throw new BusinessException(CANCELLED_LOT_MESSAGE);
        }

        if (productionLot.getStatus() != ProductionLotStatus.APPROVED
                && productionLot.getStatus() != ProductionLotStatus.HARVESTED) {

            throw new BusinessException(INVALID_LOT_STATUS_MESSAGE);
        }
    }

    /** Lấy danh sách nhật ký canh tác của lô sản xuất theo phân trang. */
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
                    FarmLogResponse response = toResponse(log);
                    response.setAttachmentCount(count);
                    return response;
                })
                .toList();

        return PageResponse.from(farmLogs, responses);
    }

    /** Lấy chi tiết một nhật ký canh tác theo ID. */
    @Override
    public FarmLogResponse getFarmLog(UUID id) {

        CustomUserDetails currentUser = getCurrentUser();

        String roleCode = currentUser.getRoleCode();
        if (!ORG_MANAGER_ROLE.equals(roleCode) && !EVENT_RECORDER_ROLE.equals(roleCode)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, VIEW_PERMISSION_MESSAGE);
        }

        FarmLog farmLog = farmLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(FARM_LOG_NOT_FOUND_MESSAGE));

        validateOrganizationAccess(currentUser, farmLog.getProductionLotId());

        int count = attachmentRepository.countByFarmLogId(farmLog.getId());
        FarmLogResponse response = toResponse(farmLog);
        response.setAttachmentCount(count);
        return response;
    }
}