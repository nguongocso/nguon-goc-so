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

/** Quản lý quy trình đề nghị và duyệt thu hồi một lô hàng. */
@Service
@Transactional
@RequiredArgsConstructor
public class RecallRequestServiceImpl implements RecallRequestService {

    private static final String MSG_SHIPMENT_NOT_FOUND = "Không tìm thấy lô hàng.";
    private static final String MSG_REQUEST_NOT_FOUND = "Không tìm thấy yêu cầu thu hồi.";
    private static final String MSG_SHIPMENT_ALREADY_RECALLED = "Lô hàng đã bị thu hồi trước đó.";
    private static final String MSG_LOT_NOT_ACTIVE = "Chỉ có thể tạo yêu cầu thu hồi cho lô sản xuất đang hiệu lực (APPROVED, HARVESTED hoặc PACKAGED).";
    private static final String MSG_PENDING_EXISTS = "Lô hàng này đã có yêu cầu thu hồi đang chờ duyệt.";
    private static final String MSG_SHIPMENT_REQUIRED = "Phải xác định lô hàng cần thu hồi.";
    private static final String MSG_SHIPMENT_MISMATCH = "Lô hàng không thuộc lô sản xuất của phản ánh.";
    private static final String MSG_TRACE_SHIPMENT_MISMATCH = "Lô hàng phải là lô chứa mã tem của phản ánh.";
    private static final String MSG_NOT_PENDING = "Chỉ có thể xử lý yêu cầu ở trạng thái PENDING.";
    private static final String MSG_CANNOT_APPROVE_OWN = "Bạn không thể duyệt yêu cầu do chính mình tạo (QTN-22).";
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
    @Auditable(action = "CREATE_RECALL_REQUEST", entityType = "RECALL_REQUEST",
            description = "'Tạo yêu cầu thu hồi lô hàng ID: ' + #request.shipmentId")
    public RecallRequestResponse create(CreateRecallRequest request, CustomUserDetails currentUser) {
        Shipment shipment = loadOwnedShipment(request.getShipmentId(), currentUser);
        validateRecallable(shipment);

        User requester = loadUser(currentUser.getUserId());
        RecallRequest entity = new RecallRequest();
        entity.setProductionLot(shipment.getProductionLot());
        entity.setShipment(shipment);
        entity.setRequestedBy(requester);
        entity.setRequestedAt(LocalDateTime.now());
        entity.setReason(request.getReason().trim());
        entity.setEvidence(normalizeOptional(request.getEvidence()));
        entity.setStatus(RecallRequestStatus.PENDING);
        return toResponse(recallRequestRepository.save(entity));
    }

    @Override
    @Auditable(action = "CREATE_RECALL_REQUEST", entityType = "RECALL_REQUEST",
            description = "'Tạo yêu cầu thu hồi từ phản ánh ID: ' + #feedback.id")
    public RecallRequestResponse createFromFeedback(
            ProductFeedback feedback,
            UUID requestedShipmentId,
            String reason,
            String evidence,
            CustomUserDetails currentUser) {
        Shipment shipment;
        if (feedback.getTraceCode() != null) {
            shipment = feedback.getTraceCode().getShipment();
            if (shipment == null) {
                throw new BusinessException(MSG_SHIPMENT_REQUIRED);
            }
            if (requestedShipmentId != null && !shipment.getId().equals(requestedShipmentId)) {
                throw new BusinessException(MSG_TRACE_SHIPMENT_MISMATCH);
            }
        } else {
            if (requestedShipmentId == null) {
                throw new BusinessException(MSG_SHIPMENT_REQUIRED);
            }
            shipment = loadOwnedShipment(requestedShipmentId, currentUser);
        }

        if (!shipment.getProductionLot().getId().equals(feedback.getProductionLot().getId())) {
            throw new BusinessException(MSG_SHIPMENT_MISMATCH);
        }
        validateRecallable(shipment);

        RecallRequest entity = new RecallRequest();
        entity.setProductionLot(shipment.getProductionLot());
        entity.setShipment(shipment);
        entity.setSourceFeedback(feedback);
        entity.setRequestedBy(loadUser(currentUser.getUserId()));
        entity.setRequestedAt(LocalDateTime.now());
        entity.setReason(reason.trim());
        entity.setEvidence(normalizeOptional(evidence));
        entity.setStatus(RecallRequestStatus.PENDING);
        return toResponse(recallRequestRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecallRequestResponse> list(String status, int page, int size, CustomUserDetails currentUser) {
        page = Math.max(page, 0);
        size = size <= 0 ? 20 : size;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));

        Page<RecallRequest> result;
        if (status != null && !status.isBlank()) {
            RecallRequestStatus requestStatus;
            try {
                requestStatus = RecallRequestStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("Trạng thái không hợp lệ: " + status);
            }
            result = recallRequestRepository.findByProductionLot_Organization_OrganizationIdAndStatus(
                    currentUser.getOrganizationId(), requestStatus, pageable);
        } else {
            result = recallRequestRepository.findByProductionLot_Organization_OrganizationId(
                    currentUser.getOrganizationId(), pageable);
        }

        return PageResponse.from(result, result.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RecallRequestResponse getById(UUID id, CustomUserDetails currentUser) {
        return toResponse(loadOwnedRequest(id, currentUser));
    }

    @Override
    @Auditable(action = "APPROVE_RECALL_REQUEST", entityType = "RECALL_REQUEST",
            description = "'Duyệt yêu cầu thu hồi ID: ' + #id")
    public RecallRequestResponse approve(UUID id, ApproveRecallRequest request, CustomUserDetails currentUser) {
        RecallRequest entity = loadOwnedRequest(id, currentUser);
        ensurePending(entity);
        if (entity.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
            throw new BusinessException(MSG_CANNOT_APPROVE_OWN);
        }
        if (entity.getShipment() == null) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Yêu cầu cũ chưa xác định được lô hàng; vui lòng từ chối và tạo lại yêu cầu.");
        }

        vn.nguongocso.trace.dto.request.RecallRequest shipmentRecall =
                new vn.nguongocso.trace.dto.request.RecallRequest();
        shipmentRecall.setReason(entity.getReason());
        shipmentRecallService.recallShipment(entity.getShipment().getId(), shipmentRecall, null);

        entity.setStatus(RecallRequestStatus.APPROVED);
        entity.setApprovedBy(loadUser(currentUser.getUserId()));
        entity.setApprovedAt(LocalDateTime.now());
        entity.setApprovalRemarks(request != null ? request.getRemarks() : null);
        RecallRequest saved = recallRequestRepository.save(entity);

        if (saved.getSourceFeedback() != null) {
            ProductFeedback feedback = saved.getSourceFeedback();
            feedback.setStatus(ProductFeedbackStatus.ESCALATED_TO_RECALL);
            productFeedbackRepository.save(feedback);
        }

        RecallRequestResponse response = toResponse(saved);
        response.setNotifiedBuyerCount(sendBuyerNotifications(saved.getShipment(), saved.getReason()));
        return response;
    }

    @Override
    @Auditable(action = "REJECT_RECALL_REQUEST", entityType = "RECALL_REQUEST",
            description = "'Từ chối yêu cầu thu hồi ID: ' + #id")
    public RecallRequestResponse reject(UUID id, RejectRecallRequest request, CustomUserDetails currentUser) {
        RecallRequest entity = loadOwnedRequest(id, currentUser);
        ensurePending(entity);
        if (request == null || request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new BusinessException(MSG_REJECT_REASON_REQUIRED);
        }

        entity.setStatus(RecallRequestStatus.REJECTED);
        entity.setRejectedBy(loadUser(currentUser.getUserId()));
        entity.setRejectedAt(LocalDateTime.now());
        entity.setRejectionReason(request.getRejectionReason().trim());
        RecallRequest saved = recallRequestRepository.save(entity);

        if (saved.getSourceFeedback() != null
                && saved.getSourceFeedback().getStatus() == ProductFeedbackStatus.ESCALATED_TO_RECALL) {
            ProductFeedback feedback = saved.getSourceFeedback();
            feedback.setStatus(ProductFeedbackStatus.IN_PROGRESS);
            productFeedbackRepository.save(feedback);
        }
        return toResponse(saved);
    }

    private Shipment loadOwnedShipment(UUID shipmentId, CustomUserDetails currentUser) {
        return shipmentRepository.findByIdAndOrganization_OrganizationId(shipmentId, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_SHIPMENT_NOT_FOUND));
    }

    private RecallRequest loadOwnedRequest(UUID id, CustomUserDetails currentUser) {
        return recallRequestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                        id, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));
    }

    private void validateRecallable(Shipment shipment) {
        if (shipment.getStatus() == ShipmentStatus.RECALLED) {
            throw new BusinessException(MSG_SHIPMENT_ALREADY_RECALLED);
        }
        ProductionLot lot = shipment.getProductionLot();
        if (lot.getStatus() != ProductionLotStatus.APPROVED
                && lot.getStatus() != ProductionLotStatus.HARVESTED
                && lot.getStatus() != ProductionLotStatus.PACKAGED) {
            throw new BusinessException(MSG_LOT_NOT_ACTIVE);
        }
        if (recallRequestRepository.existsByShipment_IdAndStatus(shipment.getId(), RecallRequestStatus.PENDING)) {
            throw new BusinessException(MSG_PENDING_EXISTS);
        }
    }

    private void ensurePending(RecallRequest entity) {
        if (entity.getStatus() != RecallRequestStatus.PENDING) {
            throw new BusinessException(MSG_NOT_PENDING);
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int sendBuyerNotifications(Shipment shipment, String reason) {
        List<UUID> recorderIds = chainEventRepository
                .findDistinctProcurementRecorderIdsByShipmentIds(List.of(shipment.getId()));
        if (recorderIds.isEmpty()) {
            return 0;
        }

        List<UUID> buyerOrgIds = new ArrayList<>();
        for (UUID recorderId : recorderIds) {
            for (OrganizationUser membership : organizationUserRepository.findAllByUser_UserId(recorderId)) {
                if (membership.getStatus() == OrganizationUserStatus.ACTIVE) {
                    UUID organizationId = membership.getOrganization().getOrganizationId();
                    if (!buyerOrgIds.contains(organizationId)) {
                        buyerOrgIds.add(organizationId);
                    }
                    break;
                }
            }
        }

        List<UUID> recipientIds = new ArrayList<>();
        for (UUID organizationId : buyerOrgIds) {
            for (OrganizationUser membership : organizationUserRepository
                    .findByOrganization_OrganizationIdAndStatus(organizationId, OrganizationUserStatus.ACTIVE)) {
                UUID userId = membership.getUser().getUserId();
                if (!recipientIds.contains(userId)) {
                    recipientIds.add(userId);
                }
            }
        }
        return notificationService.sendRecallNotification(
                shipment.getName(), reason, recipientIds);
    }

    private RecallRequestResponse toResponse(RecallRequest entity) {
        return RecallRequestResponse.builder()
                .id(entity.getId())
                .shipmentId(entity.getShipment() != null ? entity.getShipment().getId() : null)
                .shipmentName(entity.getShipment() != null ? entity.getShipment().getName() : null)
                .lotId(entity.getProductionLot().getId())
                .lotName(entity.getProductionLot().getName())
                .sourceFeedbackId(entity.getSourceFeedback() != null ? entity.getSourceFeedback().getId() : null)
                .requestedBy(toUserInfo(entity.getRequestedBy()))
                .requestedAt(entity.getRequestedAt())
                .status(entity.getStatus().name())
                .reason(entity.getReason())
                .evidence(entity.getEvidence())
                .approvedBy(toUserInfo(entity.getApprovedBy()))
                .approvedAt(entity.getApprovedAt())
                .approvalRemarks(entity.getApprovalRemarks())
                .rejectedBy(toUserInfo(entity.getRejectedBy()))
                .rejectedAt(entity.getRejectedAt())
                .rejectionReason(entity.getRejectionReason())
                .build();
    }

    private RecallRequestResponse.UserInfo toUserInfo(User user) {
        return user == null ? null : RecallRequestResponse.UserInfo.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .build();
    }
}
