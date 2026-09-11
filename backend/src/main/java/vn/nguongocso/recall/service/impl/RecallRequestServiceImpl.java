package vn.nguongocso.recall.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.common.annotation.Auditable;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.recall.dto.request.ApproveRecallRequest;
import vn.nguongocso.recall.dto.request.CreateRecallRequest;
import vn.nguongocso.recall.dto.request.RejectRecallRequest;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;
import vn.nguongocso.recall.entity.RecallRequest;
import vn.nguongocso.recall.enums.RecallRequestStatus;
import vn.nguongocso.recall.repository.RecallRequestRepository;
import vn.nguongocso.recall.service.RecallRequestService;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.service.ShipmentRecallService;

/**
 * Triển khai dịch vụ quản lý yêu cầu thu hồi lô sản xuất (NCL-08-CN-008).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RecallRequestServiceImpl implements RecallRequestService {

    private static final String MSG_SHIPMENT_NOT_FOUND = "Không tìm thấy lô hàng.";
    private static final String MSG_REQUEST_NOT_FOUND = "Không tìm thấy yêu cầu thu hồi.";
    private static final String MSG_SHIPMENT_ALREADY_RECALLED = "Lô hàng đã bị thu hồi trước đó.";
    private static final String MSG_SPLIT_PARENT_NOT_RECALLABLE = "Không thể thu hồi lô cha đã tách; vui lòng chọn lô con trong phạm vi ảnh hưởng.";
    private static final String MSG_LOT_NOT_ACTIVE = "Chỉ có thể tạo yêu cầu thu hồi cho lô sản xuất đang hiệu lực (APPROVED, HARVESTED hoặc PACKAGED).";
    private static final String MSG_PENDING_EXISTS = "Lô hàng này đã có yêu cầu thu hồi đang chờ duyệt.";
    private static final String MSG_SHIPMENT_REQUIRED = "Phải xác định lô hàng cần thu hồi.";
    private static final String MSG_SHIPMENT_MISMATCH = "Lô hàng không thuộc lô sản xuất của phản ánh.";
    private static final String MSG_TRACE_SHIPMENT_MISMATCH = "Lô hàng phải là lô chứa mã tem của phản ánh.";
    private static final String MSG_NOT_PENDING = "Chỉ có thể xử lý yêu cầu ở trạng thái PENDING.";
    private static final String MSG_CANNOT_APPROVE_OWN = "Bạn không thể duyệt yêu cầu do chính mình tạo.";
    private static final String MSG_REJECT_REASON_REQUIRED = "Lý do từ chối không được để trống.";
    private static final String MSG_USER_NOT_FOUND = "Người dùng không tồn tại.";

    private final RecallRequestRepository recallRequestRepository;
    private final ProductFeedbackRepository productFeedbackRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentRecallService shipmentRecallService;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ChainEventRepository chainEventRepository;
    private final NotificationService notificationService;

    @Override
    @Auditable(action = "CREATE_RECALL_REQUEST", entityType = "RECALL_REQUEST", description = "'Tạo yêu cầu thu hồi lô hàng ID: ' + #request.shipmentId")
    public RecallRequestResponse create(CreateRecallRequest request, CustomUserDetails currentUser) {
        Shipment shipment = shipmentRepository.findOwnedByIdForRecallUpdate(
                request.getShipmentId(), currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_SHIPMENT_NOT_FOUND));

        if (shipment.getStatus() == ShipmentStatus.RECALLED) {
            throw new BusinessException(MSG_SHIPMENT_ALREADY_RECALLED);
        }
        validateRecallableShipment(shipment);

        ProductionLot lot = shipment.getProductionLot();
        if (lot.getStatus() != ProductionLotStatus.APPROVED
                && lot.getStatus() != ProductionLotStatus.HARVESTED
                && lot.getStatus() != ProductionLotStatus.PACKAGED) {
            throw new BusinessException(MSG_LOT_NOT_ACTIVE);
        }

        if (recallRequestRepository.existsByShipment_IdAndStatus(shipment.getId(), RecallRequestStatus.PENDING)) {
            throw new BusinessException(MSG_PENDING_EXISTS);
        }

        User requester = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        RecallRequest recallRequest = new RecallRequest();
        recallRequest.setProductionLot(lot);
        recallRequest.setShipment(shipment);
        recallRequest.setRequestedBy(requester);
        recallRequest.setRequestedAt(LocalDateTime.now());
        recallRequest.setReason(request.getReason().trim());
        recallRequest.setEvidence(request.getEvidence() != null && !request.getEvidence().isBlank()
                ? request.getEvidence().trim()
                : null);
        recallRequest.setStatus(RecallRequestStatus.PENDING);

        RecallRequest saved = recallRequestRepository.save(recallRequest);
        return toResponse(saved);
    }

    @Override
    @Auditable(action = "CREATE_RECALL_REQUEST", entityType = "RECALL_REQUEST", description = "'Tạo yêu cầu thu hồi từ phản ánh ID: ' + #feedback.id")
    public RecallRequestResponse createFromFeedback(
            ProductFeedback feedback,
            UUID requestedShipmentId,
            String reason,
            String evidence,
            CustomUserDetails currentUser) {
        UUID expectedShipmentId;
        if (feedback.getTraceCode() != null) {
            Shipment linkedShipment = feedback.getTraceCode().getShipment();
            if (linkedShipment == null) {
                throw new BusinessException(MSG_SHIPMENT_REQUIRED);
            }
            expectedShipmentId = linkedShipment.getId();
            if (requestedShipmentId != null && !expectedShipmentId.equals(requestedShipmentId)) {
                throw new BusinessException(MSG_TRACE_SHIPMENT_MISMATCH);
            }
        } else {
            if (requestedShipmentId == null) {
                throw new BusinessException(MSG_SHIPMENT_REQUIRED);
            }
            expectedShipmentId = requestedShipmentId;
        }

        Shipment shipment = shipmentRepository.findOwnedByIdForRecallUpdate(
                expectedShipmentId, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_SHIPMENT_NOT_FOUND));

        if (!shipment.getProductionLot().getId().equals(feedback.getProductionLot().getId())) {
            throw new BusinessException(MSG_SHIPMENT_MISMATCH);
        }

        if (shipment.getStatus() == ShipmentStatus.RECALLED) {
            throw new BusinessException(MSG_SHIPMENT_ALREADY_RECALLED);
        }
        validateRecallableShipment(shipment);

        ProductionLot lot = shipment.getProductionLot();
        if (lot.getStatus() != ProductionLotStatus.APPROVED
                && lot.getStatus() != ProductionLotStatus.HARVESTED
                && lot.getStatus() != ProductionLotStatus.PACKAGED) {
            throw new BusinessException(MSG_LOT_NOT_ACTIVE);
        }

        if (recallRequestRepository.existsByShipment_IdAndStatus(shipment.getId(), RecallRequestStatus.PENDING)) {
            throw new BusinessException(MSG_PENDING_EXISTS);
        }

        User requester = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        RecallRequest recallRequest = new RecallRequest();
        recallRequest.setProductionLot(lot);
        recallRequest.setShipment(shipment);
        recallRequest.setSourceFeedback(feedback);
        recallRequest.setRequestedBy(requester);
        recallRequest.setRequestedAt(LocalDateTime.now());
        recallRequest.setReason(reason.trim());
        recallRequest.setEvidence(evidence != null && !evidence.isBlank() ? evidence.trim() : null);
        recallRequest.setStatus(RecallRequestStatus.PENDING);

        RecallRequest saved = recallRequestRepository.save(recallRequest);
        return toResponse(saved);
    }

    private void validateRecallableShipment(Shipment shipment) {
        if (shipment.getStatus() == ShipmentStatus.SPLIT) {
            throw new BusinessException(MSG_SPLIT_PARENT_NOT_RECALLABLE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecallRequestResponse> list(String status, int page, int size, CustomUserDetails currentUser) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));

        Page<RecallRequest> result;
        if (status != null && !status.isBlank()) {
            RecallRequestStatus requestStatus;
            try {
                requestStatus = RecallRequestStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Trạng thái không hợp lệ: " + status);
            }
            result = recallRequestRepository.findByProductionLot_Organization_OrganizationIdAndStatus(
                    currentUser.getOrganizationId(), requestStatus, pageable);
        } else {
            result = recallRequestRepository.findByProductionLot_Organization_OrganizationId(
                    currentUser.getOrganizationId(), pageable);
        }

        List<RecallRequestResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(result, items);
    }

    @Override
    @Transactional(readOnly = true)
    public RecallRequestResponse getById(UUID id, CustomUserDetails currentUser) {
        RecallRequest recallRequest = recallRequestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                id, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));
        return toResponse(recallRequest);
    }

    @Override
    @Auditable(action = "APPROVE_RECALL_REQUEST", entityType = "RECALL_REQUEST", description = "'Duyệt yêu cầu thu hồi ID: ' + #id")
    public RecallRequestResponse approve(UUID id, ApproveRecallRequest request, CustomUserDetails currentUser) {
        RecallRequest recallRequest = recallRequestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                id, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        if (recallRequest.getStatus() != RecallRequestStatus.PENDING) {
            throw new BusinessException(MSG_NOT_PENDING);
        }

        // QTN-22: không cho tự duyệt yêu cầu của chính mình
        if (recallRequest.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
            throw new BusinessException(MSG_CANNOT_APPROVE_OWN);
        }

        if (recallRequest.getShipment() == null) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Yêu cầu cũ chưa xác định được lô hàng; vui lòng từ chối và tạo lại yêu cầu.");
        }

        User approver = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        // Thu hồi lô hàng và toàn bộ mã tem thuộc lô hàng đó
        vn.nguongocso.trace.dto.request.RecallRequest shipmentRecall = new vn.nguongocso.trace.dto.request.RecallRequest();
        shipmentRecall.setReason(recallRequest.getReason());
        shipmentRecallService.recallShipment(recallRequest.getShipment().getId(), shipmentRecall, null);

        // Cập nhật trạng thái yêu cầu
        recallRequest.setStatus(RecallRequestStatus.APPROVED);
        recallRequest.setApprovedBy(approver);
        recallRequest.setApprovedAt(LocalDateTime.now());
        recallRequest.setApprovalRemarks(request != null ? request.getRemarks() : null);

        RecallRequest saved = recallRequestRepository.save(recallRequest);

        if (saved.getSourceFeedback() != null) {
            ProductFeedback feedback = saved.getSourceFeedback();
            feedback.setStatus(ProductFeedbackStatus.ESCALATED_TO_RECALL);
            productFeedbackRepository.save(feedback);
        }

        // Gửi thông báo cho các doanh nghiệp thu mua (người mua) của lô hàng
        int notifiedBuyerCount = sendBuyerNotifications(saved.getShipment(), saved.getReason());

        RecallRequestResponse response = toResponse(saved);
        response.setNotifiedBuyerCount(notifiedBuyerCount);
        return response;
    }

    @Override
    @Auditable(action = "REJECT_RECALL_REQUEST", entityType = "RECALL_REQUEST", description = "'Từ chối yêu cầu thu hồi ID: ' + #id")
    public RecallRequestResponse reject(UUID id, RejectRecallRequest request, CustomUserDetails currentUser) {
        RecallRequest recallRequest = recallRequestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                id, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        if (recallRequest.getStatus() != RecallRequestStatus.PENDING) {
            throw new BusinessException(MSG_NOT_PENDING);
        }

        if (request == null || request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new BusinessException(MSG_REJECT_REASON_REQUIRED);
        }

        User rejecter = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        recallRequest.setStatus(RecallRequestStatus.REJECTED);
        recallRequest.setRejectedBy(rejecter);
        recallRequest.setRejectedAt(LocalDateTime.now());
        recallRequest.setRejectionReason(request.getRejectionReason().trim());

        RecallRequest saved = recallRequestRepository.save(recallRequest);

        if (saved.getSourceFeedback() != null
                && saved.getSourceFeedback().getStatus() == ProductFeedbackStatus.ESCALATED_TO_RECALL) {
            ProductFeedback feedback = saved.getSourceFeedback();
            feedback.setStatus(ProductFeedbackStatus.IN_PROGRESS);
            productFeedbackRepository.save(feedback);
        }

        return toResponse(saved);
    }

    /**
     * Xác định các doanh nghiệp thu mua (người mua) có liên quan đến lô hàng
     * và gửi thông báo thu hồi cho họ.
     *
     * <p>
     * Các doanh nghiệp thu mua được xác định qua sự kiện PROCUREMENT
     * (do người dùng VT-04 ghi) trên lô hàng.
     * </p>
     *
     * @param shipment lô hàng bị thu hồi
     * @param reason   lý do thu hồi
     * @return số lượng người dùng đã nhận thông báo
     */
    private int sendBuyerNotifications(Shipment shipment, String reason) {
        // Lấy đúng tổ chức mà người mua đại diện tại thời điểm ghi sự kiện PROCUREMENT.
        List<UUID> buyerOrgIds = chainEventRepository
                .findDistinctProcurementOrganizationIdsByShipmentIds(List.of(shipment.getId()));

        if (buyerOrgIds.isEmpty()) {
            return 0;
        }

        // Lấy tất cả user đang hoạt động thuộc đúng các tổ chức thu mua đó.
        List<UUID> recipientIds = new ArrayList<>();
        for (UUID orgId : buyerOrgIds) {
            List<OrganizationUser> members = organizationUserRepository
                    .findByOrganization_OrganizationIdAndStatus(orgId, OrganizationUserStatus.ACTIVE);
            for (OrganizationUser ou : members) {
                UUID userId = ou.getUser().getUserId();
                if (!recipientIds.contains(userId)) {
                    recipientIds.add(userId);
                }
            }
        }

        if (recipientIds.isEmpty()) {
            return 0;
        }

        // 4. Gửi thông báo
        return notificationService.sendRecallNotification(shipment.getName(), reason, recipientIds);
    }

    /**
     * Chuyển đổi entity sang response DTO.
     */
    private RecallRequestResponse toResponse(RecallRequest entity) {
        RecallRequestResponse.UserInfo requestedBy = entity.getRequestedBy() != null
                ? RecallRequestResponse.UserInfo.builder()
                        .userId(entity.getRequestedBy().getUserId())
                        .fullName(entity.getRequestedBy().getFullName())
                        .build()
                : null;

        RecallRequestResponse.UserInfo approvedBy = entity.getApprovedBy() != null
                ? RecallRequestResponse.UserInfo.builder()
                        .userId(entity.getApprovedBy().getUserId())
                        .fullName(entity.getApprovedBy().getFullName())
                        .build()
                : null;

        RecallRequestResponse.UserInfo rejectedBy = entity.getRejectedBy() != null
                ? RecallRequestResponse.UserInfo.builder()
                        .userId(entity.getRejectedBy().getUserId())
                        .fullName(entity.getRejectedBy().getFullName())
                        .build()
                : null;

        return RecallRequestResponse.builder()
                .id(entity.getId())
                .shipmentId(entity.getShipment() != null ? entity.getShipment().getId() : null)
                .shipmentName(entity.getShipment() != null ? entity.getShipment().getName() : null)
                .lotId(entity.getProductionLot().getId())
                .lotName(entity.getProductionLot().getName())
                .sourceFeedbackId(entity.getSourceFeedback() != null ? entity.getSourceFeedback().getId() : null)
                .requestedBy(requestedBy)
                .requestedAt(entity.getRequestedAt())
                .status(entity.getStatus().name())
                .reason(entity.getReason())
                .evidence(entity.getEvidence())
                .approvedBy(approvedBy)
                .approvedAt(entity.getApprovedAt())
                .approvalRemarks(entity.getApprovalRemarks())
                .rejectedBy(rejectedBy)
                .rejectedAt(entity.getRejectedAt())
                .rejectionReason(entity.getRejectionReason())
                .notifiedBuyerCount(0)
                .build();
    }
}
