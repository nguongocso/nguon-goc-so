package vn.nguongocso.recall.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
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
import vn.nguongocso.recall.entity.BulkRecallRequest;
import vn.nguongocso.recall.entity.BulkRecallShipment;
import vn.nguongocso.recall.enums.BulkRecallRequestStatus;
import vn.nguongocso.recall.repository.BulkRecallRequestRepository;
import vn.nguongocso.recall.repository.BulkRecallShipmentRepository;
import vn.nguongocso.recall.service.BulkRecallNotificationService;
import vn.nguongocso.recall.service.BulkRecallRequestService;
import vn.nguongocso.trace.dto.request.RecallRequest;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.service.ShipmentRecallService;

/**
 * Triển khai dịch vụ quản lý yêu cầu thu hồi hàng loạt theo phạm vi ảnh hưởng (NCL-08-CN-011).
 */
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
    private static final String MSG_SPLIT_PARENT_NOT_RECALLABLE = "Không thể thu hồi lô cha đã tách; vui lòng chọn các lô con trong phạm vi ảnh hưởng.";
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

            // Kiểm tra đã RECALLED chưa
            if (shipment.getStatus() == ShipmentStatus.RECALLED) {
                throw new BusinessException(MSG_SHIPMENT_ALREADY_RECALLED);
            }
            validateRecallableShipment(shipment);

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

        // 8. Thu hồi từng lô hàng
        Set<UUID> notifiedUserIds = new HashSet<>();
        for (BulkRecallShipment shipmentRecord : includedShipments) {
            Shipment shipment = shipmentRecord.getShipment();

            // Kiểm tra lại trạng thái trước khi thu hồi (concurrency check)
            Shipment freshShipment = shipmentRepository.findOwnedByIdForRecallUpdate(
                    shipment.getId(), currentUser.getOrganizationId())
                    .orElseThrow(() -> new BusinessException(MSG_SHIPMENT_NOT_FOUND));

            if (freshShipment.getStatus() == ShipmentStatus.RECALLED) {
                throw new BusinessException(HttpStatus.CONFLICT, MSG_SHIPMENT_RECALLED_BY_OTHER);
            }
            validateRecallableShipment(freshShipment);

            // Gọi ShipmentRecallService để thu hồi
            RecallRequest recallRequest = new RecallRequest();
            recallRequest.setReason(bulkRequest.getReason());
            shipmentRecallService.recallShipment(freshShipment.getId(), recallRequest, null);

            // Thu thập user nhận notification (từ các tổ chức thu mua)
            collectBuyerUserIds(freshShipment, notifiedUserIds);
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

    private void validateRecallableShipment(Shipment shipment) {
        if (shipment.getStatus() == ShipmentStatus.SPLIT) {
            throw new BusinessException(MSG_SPLIT_PARENT_NOT_RECALLABLE);
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

        // Load shipments từ repository
        List<BulkRecallShipment> shipments = bulkRecallShipmentRepository
                .findByBulkRecallRequestId(entity.getId());

        List<BulkRecallShipmentItem> shipmentItems = new ArrayList<>();
        for (BulkRecallShipment s : shipments) {
            shipmentItems.add(BulkRecallShipmentItem.builder()
                    .id(s.getId())
                    .shipmentId(s.getShipment().getId())
                    .shipmentCode(s.getShipment().getName())
                    .shipmentName(s.getShipment().getName())
                    .shipmentStatus(s.getShipment().getStatus().name())
                    .included(s.isIncluded())
                    .exclusionReason(s.getExclusionReason())
                    .build());
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
                .shipments(shipmentItems)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
