package vn.nguongocso.recall.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.dto.request.ActivityLogRequest;
import vn.nguongocso.alert.service.ActivityLogService;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.recall.dto.request.ApproveBulkRecallRequest;
import vn.nguongocso.recall.dto.request.CreateBulkRecallRequest;
import vn.nguongocso.recall.dto.request.RejectBulkRecallRequest;
import vn.nguongocso.recall.dto.response.BulkRecallRequestResponse;
import vn.nguongocso.recall.dto.response.BulkRecallShipmentItem;
import vn.nguongocso.recall.dto.response.RecallEvidenceResponse;
import vn.nguongocso.recall.entity.BulkRecallRequest;
import vn.nguongocso.recall.entity.BulkRecallShipment;
import vn.nguongocso.recall.entity.RecallEvidenceFile;
import vn.nguongocso.recall.enums.BulkRecallRequestStatus;
import vn.nguongocso.recall.repository.BulkRecallRequestRepository;
import vn.nguongocso.recall.repository.BulkRecallShipmentRepository;
import vn.nguongocso.recall.repository.RecallEvidenceFileRepository;
import vn.nguongocso.recall.service.BulkRecallNotificationService;
import vn.nguongocso.recall.service.BulkRecallRequestService;
import vn.nguongocso.trace.dto.request.RecallRequest;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;
import vn.nguongocso.trace.recall.entity.RecallCase;
import vn.nguongocso.trace.recall.entity.RecallLotResult;
import vn.nguongocso.trace.recall.enums.LotResolution;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;
import vn.nguongocso.trace.recall.repository.RecallCaseRepository;
import vn.nguongocso.trace.recall.repository.RecallLotResultRepository;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.ShipmentRecallService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ quản lý yêu cầu thu hồi hàng loạt theo phạm vi ảnh hưởng (NCL-08-CN-011, NCL-08-CN-012).
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BulkRecallRequestServiceImpl implements BulkRecallRequestService {

    // =========================================================
    // Message constants
    // =========================================================
    private static final String MSG_PRODUCTION_LOT_NOT_FOUND = "Không tìm thấy lô sản xuất.";
    private static final String MSG_SHIPMENT_NOT_FOUND = "Không tìm thấy lô hàng.";
    private static final String MSG_REQUEST_NOT_FOUND = "Không tìm thấy yêu cầu thu hồi.";
    private static final String MSG_REASON_REQUIRED = "Lý do thu hồi không được để trống.";
    private static final String MSG_SHIPMENT_REQUIRED = "Phải chọn ít nhất một lô hàng để thu hồi.";
    private static final String MSG_EXCLUSION_REASON_REQUIRED = "Lô hàng bị loại phải có lý do loại bỏ.";
    private static final String MSG_SHIPMENT_ALREADY_RECALLED = "Lô hàng đã được thu hồi trước đó.";
    private static final String MSG_SHIPMENT_NOT_BELONG_TO_LOT = "Lô hàng không thuộc lô sản xuất đã chọn.";
    private static final String MSG_CANNOT_APPROVE_OWN = "Bạn không thể phê duyệt yêu cầu do chính mình tạo.";
    private static final String MSG_NOT_PENDING = "Chỉ có thể xử lý yêu cầu ở trạng thái PENDING.";
    private static final String MSG_REJECT_REASON_REQUIRED = "Lý do từ chối không được để trống.";
    private static final String MSG_USER_NOT_FOUND = "Người dùng không tồn tại.";
    private static final String MSG_NO_PERMISSION_OTHER_ORG = "Bạn không có quyền thao tác trên lô sản xuất của tổ chức khác.";
    private static final String MSG_PENDING_EXISTS = "Đã có yêu cầu thu hồi đang chờ duyệt cho lô sản xuất này.";
    private static final String MSG_SHIPMENT_RECALLED_BY_OTHER = "Lô hàng đã bị thu hồi bởi một yêu cầu khác.";
    private static final String MSG_ORG_MISMATCH = "Lô hàng thuộc tổ chức khác, không thể đưa vào phạm vi thu hồi.";

    // =========================================================
    // Dependencies
    // =========================================================
    private final BulkRecallRequestRepository bulkRecallRequestRepository;
    private final BulkRecallShipmentRepository bulkRecallShipmentRepository;
    private final ProductionLotRepository productionLotRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ChainEventRepository chainEventRepository;
    private final ShipmentRecallService shipmentRecallService;
    private final NotificationService notificationService;
    private final BulkRecallNotificationService bulkRecallNotificationService;
    private final ActivityLogService activityLogService;
    private final RecallCaseRepository recallCaseRepository;
    private final RecallLotResultRepository recallLotResultRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final CodeRangeRepository codeRangeRepository;
    private final RecallEvidenceFileRepository recallEvidenceFileRepository;

    @Value("${app.upload.base-dir}")
    private String baseDir;

    // =========================================================
    // Public methods
    // =========================================================

    /**
     * {@inheritDoc}
     *
     * <p>Quy trình tạo yêu cầu:
     * <ol>
     *   <li>Validate đầu vào</li>
     *   <li>Kiểm tra quyền truy cập lô sản xuất</li>
     *   <li>Validate các lô hàng thuộc phạm vi</li>
     *   <li>Loại bỏ lô đã RECALLED</li>
     *   <li>Lưu yêu cầu và chi tiết</li>
     *   <li>Ghi ActivityLog</li>
     * </ol>
     */
    @Override
    public BulkRecallRequestResponse createBulkRecallRequest(
            CreateBulkRecallRequest request, CustomUserDetails currentUser) {

        // 1. Validate đầu vào
        validateCreateRequest(request);

        // 2. Lấy và kiểm tra lô sản xuất
        ProductionLot productionLot = productionLotRepository.findById(request.getProductionLotId())
                .orElseThrow(() -> new BusinessException(MSG_PRODUCTION_LOT_NOT_FOUND));
        validateOrganizationAccess(currentUser, productionLot);

        // 3. Kiểm tra không có yêu cầu PENDING cho lot này
        if (bulkRecallRequestRepository.existsByProductionLot_IdAndStatus(
                productionLot.getId(), BulkRecallRequestStatus.PENDING)) {
            throw new BusinessException(MSG_PENDING_EXISTS);
        }

        // 4. Lấy user hiện tại
        User requestedBy = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        // 5. Validate tất cả shipments TRƯỚC khi tạo yêu cầu
        List<BulkRecallShipment> shipmentRecords = new ArrayList<>();

        for (UUID shipmentId : request.getIncludedShipmentIds()) {
            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new BusinessException(MSG_SHIPMENT_NOT_FOUND));

            // Validate shipment thuộc production lot
            if (!shipment.getProductionLot().getId().equals(productionLot.getId())) {
                throw new BusinessException(MSG_SHIPMENT_NOT_BELONG_TO_LOT);
            }

            // Validate organization
            if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
                throw new BusinessException(MSG_ORG_MISMATCH);
            }

            // Kiểm tra đã RECALLED hoặc RECALLING chưa
            if (shipment.getStatus() == ShipmentStatus.RECALLED || shipment.getStatus() == ShipmentStatus.RECALLING) {
                throw new BusinessException(MSG_SHIPMENT_ALREADY_RECALLED);
            }

            BulkRecallShipment record = new BulkRecallShipment();
            record.setShipment(shipment);
            record.setIncluded(true);
            record.setExclusionReason(null);
            shipmentRecords.add(record);
        }

        // 6. Xử lý các lô hàng excluded
        if (request.getExcludedShipments() != null) {
            for (CreateBulkRecallRequest.ExcludedShipment excluded : request.getExcludedShipments()) {
                // Validate lý do loại
                if (excluded.getExclusionReason() == null || excluded.getExclusionReason().trim().isEmpty()) {
                    throw new BusinessException(MSG_EXCLUSION_REASON_REQUIRED);
                }

                Shipment shipment = shipmentRepository.findById(excluded.getShipmentId())
                        .orElseThrow(() -> new BusinessException(MSG_SHIPMENT_NOT_FOUND));

                // Validate shipment thuộc production lot
                if (!shipment.getProductionLot().getId().equals(productionLot.getId())) {
                    throw new BusinessException(MSG_SHIPMENT_NOT_BELONG_TO_LOT);
                }

                BulkRecallShipment record = new BulkRecallShipment();
                record.setShipment(shipment);
                record.setIncluded(false);
                record.setExclusionReason(excluded.getExclusionReason());
                shipmentRecords.add(record);
            }
        }

        // 7. Validate phạm vi cuối cùng phải có ít nhất 1 lô included
        long includedCount = shipmentRecords.stream().filter(BulkRecallShipment::isIncluded).count();
        if (includedCount == 0) {
            throw new BusinessException(MSG_SHIPMENT_REQUIRED);
        }

        // 8. Tạo yêu cầu SAU KHI đã validate thành công
        BulkRecallRequest bulkRequest = new BulkRecallRequest();
        bulkRequest.setProductionLot(productionLot);
        bulkRequest.setReason(request.getReason());
        bulkRequest.setEvidence(request.getEvidence());
        bulkRequest.setRequestedBy(requestedBy);
        bulkRequest.setStatus(BulkRecallRequestStatus.PENDING);

        BulkRecallRequest savedRequest = bulkRecallRequestRepository.save(bulkRequest);

        // 9. Set cho từng shipment record
        for (BulkRecallShipment record : shipmentRecords) {
            record.setBulkRecallRequest(savedRequest);
        }

        // 10. Lưu chi tiết
        bulkRecallShipmentRepository.saveAll(shipmentRecords);

        // 11. Ghi ActivityLog
        logActivity(requestedBy, "CREATE_BULK_RECALL_REQUEST",
                "Tạo yêu cầu thu hồi hàng loạt cho lô sản xuất: " + productionLot.getName() +
                        ". Số lô thuộc phạm vi: " + includedCount,
                "bulk_recall_request", savedRequest.getId());

        // 12. Gửi thông báo workflow nội bộ cho các manager cùng tổ chức
        bulkRecallNotificationService.sendBulkRecallWorkflowNotification(
                currentUser, "CREATE", savedRequest, savedRequest.getId());

        return toResponse(savedRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public BulkRecallRequestResponse getBulkRecallRequest(UUID id, CustomUserDetails currentUser) {
        BulkRecallRequest request = bulkRecallRequestRepository
                .findByIdAndProductionLot_Organization_OrganizationId(id, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        return toResponse(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<BulkRecallRequestResponse> listBulkRecallRequests(
            String status, int page, int size, CustomUserDetails currentUser) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BulkRecallRequest> resultPage;

        if (status != null && !status.isEmpty()) {
            BulkRecallRequestStatus requestStatus;
            try {
                requestStatus = BulkRecallRequestStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Trạng thái không hợp lệ: " + status);
            }
            resultPage = bulkRecallRequestRepository
                    .findByProductionLot_Organization_OrganizationIdAndStatus(
                            currentUser.getOrganizationId(), requestStatus, pageable);
        } else {
            resultPage = bulkRecallRequestRepository
                    .findByProductionLot_Organization_OrganizationId(currentUser.getOrganizationId(), pageable);
        }

        List<BulkRecallRequestResponse> content = resultPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<BulkRecallRequestResponse>builder()
                .items(content)
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Quy trình phê duyệt (transaction boundary):
     * <ol>
     *   <li>Validate không tự phê duyệt</li>
     *   <li>Kiểm tra trạng thái PENDING</li>
     *   <li>Cập nhật trạng thái yêu cầu → APPROVED</li>
     *   <li>Với mỗi lô included: gọi ShipmentRecallService để chuyển RECALLED</li>
     *   <li>Gửi notification cho các bên liên quan</li>
     *   <li>Ghi ActivityLog</li>
     * </ol>
     */
    @Override
    public BulkRecallRequestResponse approveBulkRecallRequest(
            UUID id, ApproveBulkRecallRequest request, CustomUserDetails currentUser) {

        // 1. Lấy yêu cầu với pessimistic lock
        BulkRecallRequest bulkRequest = bulkRecallRequestRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        // 2. Kiểm tra quyền truy cập tổ chức
        validateOrganizationAccess(currentUser, bulkRequest.getProductionLot());

        // 3. Kiểm tra không tự phê duyệt (QTN-22)
        if (bulkRequest.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, MSG_CANNOT_APPROVE_OWN);
        }

        // 4. Kiểm tra trạng thái PENDING
        if (bulkRequest.getStatus() != BulkRecallRequestStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, MSG_NOT_PENDING);
        }

        // 5. Lấy user phê duyệt
        User approvedBy = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        // 6. Lấy danh sách lô hàng included
        List<BulkRecallShipment> includedShipments = bulkRecallShipmentRepository
                .findByBulkRecallRequestIdAndIncluded(id, true);

        if (includedShipments.isEmpty()) {
            throw new BusinessException(MSG_SHIPMENT_REQUIRED);
        }

        // 7. Cập nhật trạng thái yêu cầu
        bulkRequest.setStatus(BulkRecallRequestStatus.APPROVED);
        bulkRequest.setApprovedBy(approvedBy);
        bulkRequest.setApprovedAt(LocalDateTime.now());
        bulkRequest.setApprovalRemarks(request.getRemarks());
        bulkRecallRequestRepository.save(bulkRequest);

        // 8. Chuyển trạng thái từng lô hàng sang RECALLING (Đang thu hồi)
        Set<UUID> notifiedUserIds = new HashSet<>();
        for (BulkRecallShipment shipmentRecord : includedShipments) {
            Shipment shipment = shipmentRecord.getShipment();

            // Kiểm tra lại trạng thái trước khi thu hồi (concurrency check)
            Shipment freshShipment = shipmentRepository.findOwnedByIdForRecallUpdate(
                    shipment.getId(), currentUser.getOrganizationId())
                    .orElseThrow(() -> new BusinessException(MSG_SHIPMENT_NOT_FOUND));

            if (freshShipment.getStatus() == ShipmentStatus.RECALLED
                    || freshShipment.getStatus() == ShipmentStatus.RECALLING) {
                throw new BusinessException(HttpStatus.CONFLICT, MSG_SHIPMENT_RECALLED_BY_OTHER);
            }

            // Chuyển trạng thái lô hàng sang RECALLING (Đang thu hồi)
            freshShipment.setStatus(ShipmentStatus.RECALLING);
            shipmentRepository.save(freshShipment);
            shipmentRecord.setShipment(freshShipment);

            // Thu thập user nhận notification (từ các tổ chức thu mua)
            collectBuyerUserIds(freshShipment, notifiedUserIds);
        }

        // Đảm bảo mở/tạo vụ việc thu hồi (RecallCase) ở trạng thái OPEN nếu chưa tồn tại
        if (!recallCaseRepository.existsByProductionLotId(bulkRequest.getProductionLot().getId())) {
            RecallCase recallCase = RecallCase.builder()
                    .caseCode("RC-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                            + "-" + String.format("%04X", ThreadLocalRandom.current().nextInt(0x10000)))
                    .productionLot(bulkRequest.getProductionLot())
                    .organizationId(currentUser.getOrganizationId())
                    .status(RecallCaseStatus.OPEN)
                    .build();
            recallCaseRepository.save(recallCase);
        }

        // 9. Gửi notification cho các bên liên quan
        sendBulkRecallNotifications(bulkRequest, notifiedUserIds);

        // 10. Gửi thông báo workflow nội bộ cho các manager cùng tổ chức
        bulkRecallNotificationService.sendBulkRecallWorkflowNotification(
                currentUser, "APPROVE", bulkRequest, bulkRequest.getId());

        // 11. Gửi thông báo cho doanh nghiệp thu mua
        bulkRecallNotificationService.sendBulkRecallNotificationToPurchasingBusiness(bulkRequest);

        // 12. Ghi ActivityLog
        logActivity(approvedBy, "APPROVE_BULK_RECALL_REQUEST",
                "Phê duyệt yêu cầu thu hồi hàng loạt. Lô sản xuất: " +
                        bulkRequest.getProductionLot().getName() +
                        ". Số lô thu hồi: " + includedShipments.size(),
                "bulk_recall_request", bulkRequest.getId());

        return toResponse(bulkRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BulkRecallRequestResponse rejectBulkRecallRequest(
            UUID id, RejectBulkRecallRequest request, CustomUserDetails currentUser) {

        // 1. Validate lý do từ chối
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException(MSG_REJECT_REASON_REQUIRED);
        }

        // 2. Lấy yêu cầu với pessimistic lock
        BulkRecallRequest bulkRequest = bulkRecallRequestRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        // 3. Kiểm tra quyền truy cập tổ chức
        validateOrganizationAccess(currentUser, bulkRequest.getProductionLot());

        // 4. Kiểm tra trạng thái PENDING
        if (bulkRequest.getStatus() != BulkRecallRequestStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, MSG_NOT_PENDING);
        }

        // 5. Lấy user từ chối
        User rejectedBy = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        // 6. Cập nhật trạng thái
        bulkRequest.setStatus(BulkRecallRequestStatus.REJECTED);
        bulkRequest.setRejectedBy(rejectedBy);
        bulkRequest.setRejectedAt(LocalDateTime.now());
        bulkRequest.setRejectionReason(request.getReason());
        bulkRecallRequestRepository.save(bulkRequest);

        // 7. Gửi thông báo workflow nội bộ cho các manager cùng tổ chức
        bulkRecallNotificationService.sendBulkRecallWorkflowNotification(
                currentUser, "REJECT", bulkRequest, bulkRequest.getId());

        // 8. Ghi ActivityLog
        logActivity(rejectedBy, "REJECT_BULK_RECALL_REQUEST",
                "Từ chối yêu cầu thu hồi hàng loạt. Lý do: " + request.getReason(),
                "bulk_recall_request", bulkRequest.getId());

        return toResponse(bulkRequest);
    }

    // =========================================================
    // Private helper methods
    // =========================================================

    /**
     * Validate request tạo yêu cầu.
     */
    private void validateCreateRequest(CreateBulkRecallRequest request) {
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException(MSG_REASON_REQUIRED);
        }

        if (request.getIncludedShipmentIds() == null || request.getIncludedShipmentIds().isEmpty()) {
            throw new BusinessException(MSG_SHIPMENT_REQUIRED);
        }
    }

    /**
     * Kiểm tra người dùng thuộc tổ chức sở hữu lô sản xuất.
     * Vi phạm là lỗi authorization → trả 403 theo contract API (BULK_RECALL_007).
     */
    private void validateOrganizationAccess(CustomUserDetails currentUser, ProductionLot productionLot) {
        if (currentUser.getOrganizationId() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, MSG_NO_PERMISSION_OTHER_ORG);
        }

        UUID lotOrgId = productionLot.getOrganization().getOrganizationId();
        if (!currentUser.getOrganizationId().equals(lotOrgId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, MSG_NO_PERMISSION_OTHER_ORG);
        }
    }

    /**
     * Thu thập ID user thuộc các tổ chức thu mua đã nhận lô hàng.
     */
    private void collectBuyerUserIds(Shipment shipment, Set<UUID> notifiedUserIds) {
        List<UUID> buyerOrgIds = chainEventRepository
                .findDistinctProcurementOrganizationIdsByShipmentIds(List.of(shipment.getId()));

        for (UUID orgId : buyerOrgIds) {
            List<OrganizationUser> members = organizationUserRepository
                    .findByOrganization_OrganizationIdAndStatus(orgId, OrganizationUserStatus.ACTIVE);
            for (OrganizationUser ou : members) {
                notifiedUserIds.add(ou.getUser().getUserId());
            }
        }
    }

    /**
     * Gửi notification thu hồi cho các bên liên quan.
     */
    private void sendBulkRecallNotifications(BulkRecallRequest bulkRequest, Set<UUID> recipientIds) {
        if (recipientIds.isEmpty()) {
            return;
        }

        String shipmentNames = bulkRequest.getShipments().stream()
                .filter(BulkRecallShipment::isIncluded)
                .map(s -> s.getShipment().getName())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        String content = String.format(
                "Lô sản xuất '%s' đã được thu hồi. Các lô hàng bị ảnh hưởng: %s. Lý do: %s",
                bulkRequest.getProductionLot().getName(),
                shipmentNames,
                bulkRequest.getReason());

        notificationService.sendRecallNotification(
                "Thu hồi hàng loạt - " + bulkRequest.getProductionLot().getName(),
                content,
                new ArrayList<>(recipientIds));
    }

    /**
     * Ghi ActivityLog.
     */
    private void logActivity(User user, String action, String description, String entityType, UUID entityId) {
        // Lấy organization ID từ organization_users
        UUID orgId = organizationUserRepository
                .findFirstByUser(user)
                .map(ou -> ou.getOrganization().getOrganizationId())
                .orElse(null);

        ActivityLogRequest logRequest = ActivityLogRequest.builder()
                .userId(user.getUserId())
                .username(user.getUserName())
                .fullName(user.getFullName())
                .organizationId(orgId)
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .build();

        activityLogService.logActivity(logRequest);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Quy trình kết thúc vụ việc thu hồi gắn liền với yêu cầu thu hồi hàng loạt (NCL-08-CN-012):
     * <ol>
     *   <li>Kiểm tra vai trò VT-02 và cách ly tổ chức</li>
     *   <li>Kiểm tra trạng thái yêu cầu phải là APPROVED</li>
     *   <li>Validate biện pháp khắc phục phòng ngừa bắt buộc (QTN-27)</li>
     *   <li>Validate danh sách kết quả xử lý phải phủ hết các lô included</li>
     *   <li>Lưu kết quả xử lý từng lô vào RecallLotResult</li>
     *   <li>Chuyển trạng thái các lô hàng sang RECALLED, hoàn trả mã tem</li>
     *   <li>Cập nhật vụ việc thu hồi RecallCase sang CLOSED</li>
     *   <li>Cập nhật yêu cầu thu hồi hàng loạt sang COMPLETED ("Đã xử lý")</li>
     *   <li>Gửi thông báo tới các doanh nghiệp thu mua liên quan</li>
     *   <li>Ghi lịch sử hoạt động ActivityLog</li>
     * </ol>
     */
    @Override
    public BulkRecallRequestResponse closeBulkRecallRequest(
            UUID id, CloseRecallCaseRequest request, CustomUserDetails currentUser) {

        // 1. Kiểm tra vai trò VT-02
        if (!"VT-02".equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền kết thúc vụ việc thu hồi.");
        }

        // 2. Lấy yêu cầu thu hồi hàng loạt với pessimistic lock
        BulkRecallRequest bulkRequest = bulkRecallRequestRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        // 3. Kiểm tra quyền truy cập tổ chức
        validateOrganizationAccess(currentUser, bulkRequest.getProductionLot());

        // 4. Kiểm tra trạng thái yêu cầu: chỉ được kết thúc khi đã APPROVED
        if (bulkRequest.getStatus() != BulkRecallRequestStatus.APPROVED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Chỉ có thể kết thúc vụ việc khi yêu cầu ở trạng thái Đã duyệt (APPROVED).");
        }

        // 5. Biện pháp khắc phục phòng ngừa là bắt buộc (QTN-27)
        String remediation = request.getRemediationMeasures() == null
                ? ""
                : request.getRemediationMeasures().trim();
        if (remediation.isEmpty()) {
            throw new BusinessException("Biện pháp khắc phục phòng ngừa là bắt buộc trước khi đóng vụ việc.");
        }

        // Kiểm tra số lượng tệp biên bản đính kèm (tối đa 5 tệp)
        if (request.getEvidenceFileIds() != null && request.getEvidenceFileIds().size() > 5) {
            throw new BusinessException("Chỉ được đính kèm tối đa 5 tệp biên bản thu hồi.");
        }

        // 6. Lấy danh sách các lô hàng included trong yêu cầu thu hồi này
        List<BulkRecallShipment> includedBulkShipments = bulkRecallShipmentRepository
                .findByBulkRecallRequestIdAndIncluded(id, true);
        if (includedBulkShipments.isEmpty()) {
            throw new BusinessException("Yêu cầu thu hồi không có lô hàng nào trong phạm vi.");
        }

        List<Shipment> shipments = includedBulkShipments.stream()
                .map(BulkRecallShipment::getShipment)
                .toList();

        // 7. Mọi lô hàng included phải có kết quả xử lý
        Map<UUID, CloseRecallCaseRequest.LotResultItem> itemByShipment = new HashMap<>();
        List<CloseRecallCaseRequest.LotResultItem> lotResultItems = request.getLotResults() != null
                ? request.getLotResults()
                : List.of();

        for (CloseRecallCaseRequest.LotResultItem item : lotResultItems) {
            if (item.getShipmentId() == null) {
                throw new BusinessException("Lô hàng không thuộc vụ việc thu hồi này.");
            }
            if (itemByShipment.containsKey(item.getShipmentId())) {
                throw new BusinessException(
                        String.format("Lô hàng %s bị trùng trong danh sách kết quả xử lý.", item.getShipmentId()));
            }
            itemByShipment.put(item.getShipmentId(), item);
        }

        List<Shipment> missing = shipments.stream()
                .filter(s -> !itemByShipment.containsKey(s.getId()))
                .toList();
        if (!missing.isEmpty()) {
            String missingNames = missing.stream().map(Shipment::getName).collect(Collectors.joining(", "));
            throw new BusinessException(String.format(
                    "Còn %d lô chưa có kết quả xử lý: %s. Vui lòng nhập đủ kết quả xử lý cho tất cả các lô.",
                    missing.size(), missingNames));
        }

        // 8. Tìm hoặc tạo RecallCase cho lô sản xuất này
        RecallCase recallCase = recallCaseRepository.findByProductionLotId(bulkRequest.getProductionLot().getId())
                .orElseGet(() -> {
                    RecallCase newCase = RecallCase.builder()
                            .caseCode("RC-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                                    + "-" + String.format("%04X", ThreadLocalRandom.current().nextInt(0x10000)))
                            .productionLot(bulkRequest.getProductionLot())
                            .organizationId(currentUser.getOrganizationId())
                            .status(RecallCaseStatus.OPEN)
                            .build();
                    return recallCaseRepository.save(newCase);
                });

        // 9. Kiểm tra và lưu kết quả xử lý từng lô
        Map<UUID, RecallLotResult> existingResults = recallLotResultRepository
                .findByRecallCaseId(recallCase.getId())
                .stream()
                .collect(Collectors.toMap(r -> r.getShipment().getId(), r -> r));

        List<RecallLotResult> resultsToSave = new ArrayList<>();
        for (Shipment shipment : shipments) {
            CloseRecallCaseRequest.LotResultItem item = itemByShipment.get(shipment.getId());

            if (item.getResolution() == null) {
                throw new BusinessException(String.format("Kết quả xử lý của lô hàng %s là bắt buộc.", shipment.getName()));
            }
            BigDecimal quantity = item.getRecoveredQuantity();
            if (quantity == null) {
                throw new BusinessException(String.format("Số lượng thu hồi được của lô hàng %s là bắt buộc.", shipment.getName()));
            }
            BigDecimal maxQuantity = BigDecimal.valueOf(shipment.getTotalQuantity());
            if (quantity.signum() < 0 || quantity.compareTo(maxQuantity) > 0) {
                throw new BusinessException(String.format(
                        "Số lượng thu hồi được của lô hàng %s phải nằm trong khoảng từ 0 đến %s.",
                        shipment.getName(), shipment.getTotalQuantity()));
            }
            if (item.getResolution() == LotResolution.UNRECOVERABLE && (item.getNotes() == null || item.getNotes().isBlank())) {
                throw new BusinessException(String.format(
                        "Lô hàng %s: khi chọn kết quả \"Không thu hồi được\", bắt buộc nhập lý do và biện pháp xử lý rủi ro.",
                        shipment.getName()));
            }

            RecallLotResult result = existingResults.getOrDefault(
                    shipment.getId(), RecallLotResult.builder().build());
            result.setRecallCase(recallCase);
            result.setShipment(shipment);
            result.setResolution(item.getResolution());
            result.setRecoveredQuantity(quantity);
            result.setNotes(item.getNotes());
            resultsToSave.add(result);
        }
        recallLotResultRepository.saveAll(resultsToSave);

        // 10. Chuyển trạng thái các lô hàng sang RECALLED và hoàn trả mã tem
        for (Shipment shipment : shipments) {
            shipment.setStatus(ShipmentStatus.RECALLED);
            shipmentRepository.save(shipment);

            // Cập nhật trạng thái toàn bộ TraceCode sang RECALLED
            List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(shipment.getId());
            traceCodes.forEach(code -> code.setStatus(TraceCodeStatus.RECALLED));
            traceCodeRepository.saveAll(traceCodes);

            // Hoàn trả số lượng mã đã dùng cho dải mã của tổ chức
            if (!traceCodes.isEmpty()) {
                CodeRange codeRange = shipment.getCodeRange() != null
                        ? shipment.getCodeRange()
                        : codeRangeRepository
                                .findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(
                                        shipment.getOrganization().getOrganizationId())
                                .orElse(null);
                if (codeRange != null) {
                    codeRange.setUsedCount(Math.max(0, codeRange.getUsedCount() - traceCodes.size()));
                    codeRangeRepository.save(codeRange);
                }
            }
        }

        // 11. Đóng RecallCase (đồng bộ để hỗ trợ tra cứu tem công khai QTN-09, QTN-27)
        User currentUserEntity = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        String evidenceSerialized = serializeEvidence(request.getEvidenceFileIds());

        recallCase.setStatus(RecallCaseStatus.CLOSED);
        recallCase.setRemediationMeasures(remediation);
        recallCase.setEvidenceFileIds(evidenceSerialized);
        recallCase.setClosedBy(currentUserEntity);
        recallCase.setClosedAt(now);
        recallCaseRepository.save(recallCase);

        // 12. Cập nhật BulkRecallRequest sang trạng thái COMPLETED
        bulkRequest.setStatus(BulkRecallRequestStatus.COMPLETED);
        bulkRequest.setClosedBy(currentUserEntity);
        bulkRequest.setClosedAt(now);
        bulkRequest.setRemediationMeasures(remediation);
        bulkRequest.setEvidenceFileIds(evidenceSerialized);
        bulkRecallRequestRepository.save(bulkRequest);

        // 13. Gửi thông báo kết thúc thu hồi tới doanh nghiệp thu mua liên quan
        notifyProcurementOrganizations(recallCase, shipments);

        // 14. Ghi lịch sử hoạt động ActivityLog
        logActivity(currentUserEntity, "CLOSE_BULK_RECALL_REQUEST",
                String.format("Kết thúc vụ việc thu hồi lô sản xuất: %s. Mã vụ việc: %s. Số lô xử lý: %d",
                        bulkRequest.getProductionLot().getName(), recallCase.getCaseCode(), shipments.size()),
                "bulk_recall_request", bulkRequest.getId());

        return toResponse(bulkRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RecallEvidenceResponse uploadEvidenceFile(MultipartFile file, CustomUserDetails currentUser) {
        // 1. Kiểm tra vai trò quản lý
        if (!"VT-02".equals(currentUser.getRoleCode()) && !"VT-01".equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền tải lên tệp biên bản thu hồi.");
        }

        // 2. Validate tệp không rỗng
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Tệp tải lên không được để trống.");
        }

        // 3. Giới hạn dung lượng 10MB
        if (file.getSize() > 10 * 1024 * 1024L) {
            throw new BusinessException("Dung lượng tệp vượt quá giới hạn 10MB.");
        }

        // 4. Kiểm tra định dạng PDF hoặc Word (.docx, .doc)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("Tên tệp không hợp lệ.");
        }
        String lower = originalFilename.toLowerCase();
        if (!lower.endsWith(".pdf") && !lower.endsWith(".docx") && !lower.endsWith(".doc")) {
            throw new BusinessException("Chỉ chấp nhận tệp biên bản định dạng PDF (.pdf) hoặc Word (.docx, .doc).");
        }

        // 5. Lưu tệp lên ổ đĩa
        try {
            Path uploadDir = Paths.get(baseDir, "recall-evidences");
            Files.createDirectories(uploadDir);

            String filePrefix = UUID.randomUUID().toString();
            String safeName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
            String storedFileName = filePrefix + "_" + safeName;
            Path targetPath = uploadDir.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            User user = userRepository.findById(currentUser.getUserId())
                    .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

            RecallEvidenceFile evidenceFile = RecallEvidenceFile.builder()
                    .fileName(originalFilename)
                    .filePath(targetPath.toString())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .uploadedBy(user)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            evidenceFile = recallEvidenceFileRepository.save(evidenceFile);

            return RecallEvidenceResponse.builder()
                    .id(evidenceFile.getId())
                    .fileName(evidenceFile.getFileName())
                    .fileSize(evidenceFile.getFileSize())
                    .contentType(evidenceFile.getContentType())
                    .downloadUrl("/api/v1/recall-requests/bulk/evidence/" + evidenceFile.getId())
                    .uploadedAt(evidenceFile.getUploadedAt())
                    .build();
        } catch (IOException e) {
            log.error("Lỗi khi lưu tệp biên bản thu hồi", e);
            throw new BusinessException("Không thể lưu trữ tệp biên bản tải lên.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public EvidenceFileContent getEvidenceFile(UUID fileId, CustomUserDetails currentUser) {
        RecallEvidenceFile evidenceFile = recallEvidenceFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp biên bản thu hồi."));

        Path filePath = Paths.get(evidenceFile.getFilePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Tệp biên bản không tồn tại trên máy chủ.");
        }

        return new EvidenceFileContent(
                new FileSystemResource(filePath),
                evidenceFile.getFileName(),
                evidenceFile.getContentType()
        );
    }

    /**
     * Gửi thông báo kết thúc thu hồi tới doanh nghiệp thu mua liên quan.
     */
    private void notifyProcurementOrganizations(RecallCase recallCase, List<Shipment> shipments) {
        List<UUID> shipmentIds = shipments.stream().map(Shipment::getId).toList();
        List<UUID> procurementOrgIds =
                chainEventRepository.findDistinctProcurementOrganizationIdsByShipmentIds(shipmentIds);
        if (procurementOrgIds.isEmpty()) {
            return;
        }

        Set<UUID> recipientIds = new LinkedHashSet<>();
        for (UUID orgId : procurementOrgIds) {
            organizationUserRepository
                    .findByOrganization_OrganizationIdAndStatus(orgId, OrganizationUserStatus.ACTIVE)
                    .forEach(ou -> {
                        if (ou.getUser() != null) {
                            recipientIds.add(ou.getUser().getUserId());
                        }
                    });
        }
        if (!recipientIds.isEmpty()) {
            notificationService.sendRecallCaseClosedNotification(
                    recallCase.getCaseCode(), new ArrayList<>(recipientIds));
        }
    }

    /**
     * Chuỗi hóa danh sách ID tệp biên bản (phân tách bởi dấu phẩy) hoặc null.
     */
    private String serializeEvidence(List<UUID> evidenceFileIds) {
        if (evidenceFileIds == null || evidenceFileIds.isEmpty()) {
            return null;
        }
        return evidenceFileIds.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    /**
     * Phân giải chuỗi ID tệp biên bản đã lưu thành danh sách UUID hợp lệ.
     */
    private List<UUID> parseEvidence(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return UUID.fromString(s);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Chuyển đổi entity sang response DTO.
     */
    private BulkRecallRequestResponse toResponse(BulkRecallRequest entity) {
        BulkRecallRequestResponse.UserInfo requestedBy = entity.getRequestedBy() != null
                ? BulkRecallRequestResponse.UserInfo.builder()
                        .userId(entity.getRequestedBy().getUserId())
                        .fullName(entity.getRequestedBy().getFullName())
                        .build()
                : null;

        BulkRecallRequestResponse.UserInfo approvedBy = entity.getApprovedBy() != null
                ? BulkRecallRequestResponse.UserInfo.builder()
                        .userId(entity.getApprovedBy().getUserId())
                        .fullName(entity.getApprovedBy().getFullName())
                        .build()
                : null;

        BulkRecallRequestResponse.UserInfo rejectedBy = entity.getRejectedBy() != null
                ? BulkRecallRequestResponse.UserInfo.builder()
                        .userId(entity.getRejectedBy().getUserId())
                        .fullName(entity.getRejectedBy().getFullName())
                        .build()
                : null;

        BulkRecallRequestResponse.UserInfo closedBy = entity.getClosedBy() != null
                ? BulkRecallRequestResponse.UserInfo.builder()
                        .userId(entity.getClosedBy().getUserId())
                        .fullName(entity.getClosedBy().getFullName())
                        .build()
                : null;

        // Lấy thông tin vụ việc liên kết nếu có
        RecallCase recallCase = recallCaseRepository.findByProductionLotId(entity.getProductionLot().getId())
                .orElse(null);
        String caseCode = recallCase != null ? recallCase.getCaseCode() : null;

        Map<UUID, RecallLotResult> resultMap = new HashMap<>();
        if (recallCase != null) {
            recallLotResultRepository.findByRecallCaseId(recallCase.getId())
                    .forEach(r -> resultMap.put(r.getShipment().getId(), r));
        }

        // Load shipments từ repository
        List<BulkRecallShipment> shipments = bulkRecallShipmentRepository
                .findByBulkRecallRequestId(entity.getId());

        List<BulkRecallShipmentItem> shipmentItems = new ArrayList<>();
        for (BulkRecallShipment s : shipments) {
            RecallLotResult lotResult = resultMap.get(s.getShipment().getId());
            String unit = s.getShipment().getProductionLot() != null
                    ? s.getShipment().getProductionLot().getExpectedQuantityUnit()
                    : null;

            shipmentItems.add(BulkRecallShipmentItem.builder()
                    .id(s.getId())
                    .shipmentId(s.getShipment().getId())
                    .shipmentCode(s.getShipment().getName())
                    .shipmentName(s.getShipment().getName())
                    .shipmentStatus(s.getShipment().getStatus().name())
                    .included(s.isIncluded())
                    .exclusionReason(s.getExclusionReason())
                    .unit(unit)
                    .totalQuantity(s.getShipment().getTotalQuantity())
                    .resolution(lotResult != null ? lotResult.getResolution() : null)
                    .recoveredQuantity(lotResult != null ? lotResult.getRecoveredQuantity() : null)
                    .notes(lotResult != null ? lotResult.getNotes() : null)
                    .build());
        }

        String remediation = entity.getRemediationMeasures() != null
                ? entity.getRemediationMeasures()
                : (recallCase != null ? recallCase.getRemediationMeasures() : null);

        String evidenceRaw = entity.getEvidenceFileIds() != null
                ? entity.getEvidenceFileIds()
                : (recallCase != null ? recallCase.getEvidenceFileIds() : null);

        LocalDateTime closedAt = entity.getClosedAt() != null
                ? entity.getClosedAt()
                : (recallCase != null ? recallCase.getClosedAt() : null);

        if (closedBy == null && recallCase != null && recallCase.getClosedBy() != null) {
            closedBy = BulkRecallRequestResponse.UserInfo.builder()
                    .userId(recallCase.getClosedBy().getUserId())
                    .fullName(recallCase.getClosedBy().getFullName())
                    .build();
        }

        List<UUID> parsedEvidenceIds = parseEvidence(evidenceRaw);
        List<RecallEvidenceResponse> evidenceFiles = List.of();
        if (parsedEvidenceIds != null && !parsedEvidenceIds.isEmpty()) {
            evidenceFiles = recallEvidenceFileRepository.findByIdIn(parsedEvidenceIds).stream()
                    .map(f -> RecallEvidenceResponse.builder()
                            .id(f.getId())
                            .fileName(f.getFileName())
                            .fileSize(f.getFileSize())
                            .contentType(f.getContentType())
                            .downloadUrl("/api/v1/recall-requests/bulk/evidence/" + f.getId())
                            .uploadedAt(f.getUploadedAt())
                            .build())
                    .toList();
        }

        return BulkRecallRequestResponse.builder()
                .id(entity.getId())
                .productionLotId(entity.getProductionLot().getId())
                .productionLotName(entity.getProductionLot().getName())
                .reason(entity.getReason())
                .evidence(entity.getEvidence())
                .status(entity.getStatus().name())
                .requestedBy(requestedBy)
                .requestedAt(entity.getRequestedAt())
                .approvedBy(approvedBy)
                .approvedAt(entity.getApprovedAt())
                .approvalRemarks(entity.getApprovalRemarks())
                .rejectedBy(rejectedBy)
                .rejectedAt(entity.getRejectedAt())
                .rejectionReason(entity.getRejectionReason())
                .closedBy(closedBy)
                .closedAt(closedAt)
                .remediationMeasures(remediation)
                .evidenceFileIds(parsedEvidenceIds)
                .evidenceFiles(evidenceFiles)
                .caseCode(caseCode)
                .shipments(shipmentItems)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
