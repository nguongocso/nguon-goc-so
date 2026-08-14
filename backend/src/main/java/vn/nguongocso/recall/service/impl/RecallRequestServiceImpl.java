package vn.nguongocso.recall.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
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
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Triển khai dịch vụ quản lý yêu cầu thu hồi lô sản xuất (NCL-08-CN-008).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RecallRequestServiceImpl implements RecallRequestService {

    private static final String MSG_LOT_NOT_FOUND = "Không tìm thấy lô sản xuất.";
    private static final String MSG_REQUEST_NOT_FOUND = "Không tìm thấy yêu cầu thu hồi.";
    private static final String MSG_LOT_ALREADY_RECALLED = "Lô sản xuất đã bị thu hồi trước đó.";
    private static final String MSG_LOT_NOT_ACTIVE = "Chỉ có thể tạo yêu cầu thu hồi cho lô sản xuất đang hiệu lực (APPROVED, HARVESTED hoặc PACKAGED).";
    private static final String MSG_PENDING_EXISTS = "Lô sản xuất này đã có yêu cầu thu hồi đang chờ duyệt.";
    private static final String MSG_NOT_PENDING = "Chỉ có thể xử lý yêu cầu ở trạng thái PENDING.";
    private static final String MSG_CANNOT_APPROVE_OWN = "Bạn không thể duyệt yêu cầu do chính mình tạo (QTN-22).";
    private static final String MSG_REJECT_REASON_REQUIRED = "Lý do từ chối không được để trống.";
    private static final String MSG_USER_NOT_FOUND = "Người dùng không tồn tại.";

    private final RecallRequestRepository recallRequestRepository;
    private final ProductionLotRepository productionLotRepository;
    private final ShipmentRepository shipmentRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ChainEventRepository chainEventRepository;
    private final NotificationService notificationService;

    @Override
    public RecallRequestResponse create(CreateRecallRequest request, CustomUserDetails currentUser) {
        ProductionLot lot = productionLotRepository.findById(request.getLotId())
                .orElseThrow(() -> new BusinessException(MSG_LOT_NOT_FOUND));

        // Lô đã bị thu hồi thì từ chối yêu cầu
        if (lot.getStatus() == ProductionLotStatus.RECALLED) {
            throw new BusinessException(MSG_LOT_ALREADY_RECALLED);
        }

        // Chỉ lô đang hiệu lực (APPROVED, HARVESTED, PACKAGED) mới được yêu cầu thu hồi
        if (lot.getStatus() != ProductionLotStatus.APPROVED
                && lot.getStatus() != ProductionLotStatus.HARVESTED
                && lot.getStatus() != ProductionLotStatus.PACKAGED) {
            throw new BusinessException(MSG_LOT_NOT_ACTIVE);
        }

        // Không cho tạo trùng yêu cầu PENDING cho cùng lô
        if (recallRequestRepository.existsByProductionLot_IdAndStatus(lot.getId(), RecallRequestStatus.PENDING)) {
            throw new BusinessException(MSG_PENDING_EXISTS);
        }

        User requester = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        RecallRequest recallRequest = new RecallRequest();
        recallRequest.setProductionLot(lot);
        recallRequest.setRequestedBy(requester);
        recallRequest.setRequestedAt(LocalDateTime.now());
        recallRequest.setReason(request.getReason());
        recallRequest.setEvidence(request.getEvidence());
        recallRequest.setStatus(RecallRequestStatus.PENDING);

        RecallRequest saved = recallRequestRepository.save(recallRequest);
        return toResponse(saved);
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
            result = recallRequestRepository.findByStatus(requestStatus, pageable);
        } else {
            result = recallRequestRepository.findAll(pageable);
        }

        List<RecallRequestResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(result, items);
    }

    @Override
    @Transactional(readOnly = true)
    public RecallRequestResponse getById(UUID id, CustomUserDetails currentUser) {
        RecallRequest recallRequest = recallRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));
        return toResponse(recallRequest);
    }

    @Override
    public RecallRequestResponse approve(UUID id, ApproveRecallRequest request, CustomUserDetails currentUser) {
        RecallRequest recallRequest = recallRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        if (recallRequest.getStatus() != RecallRequestStatus.PENDING) {
            throw new BusinessException(MSG_NOT_PENDING);
        }

        // QTN-22: không cho tự duyệt yêu cầu của chính mình
        if (recallRequest.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
            throw new BusinessException(MSG_CANNOT_APPROVE_OWN);
        }

        User approver = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        // Cập nhật trạng thái yêu cầu
        recallRequest.setStatus(RecallRequestStatus.APPROVED);
        recallRequest.setApprovedBy(approver);
        recallRequest.setApprovedAt(LocalDateTime.now());
        recallRequest.setApprovalRemarks(request != null ? request.getRemarks() : null);

        // Cascade: ProductionLot -> RECALLED
        ProductionLot lot = recallRequest.getProductionLot();
        lot.setStatus(ProductionLotStatus.RECALLED);
        productionLotRepository.save(lot);

        // Cascade: tất cả Shipment -> RECALLED
        List<UUID> affectedShipmentIds = new ArrayList<>();
        List<Shipment> shipments = shipmentRepository.findByProductionLotId(lot.getId());
        for (Shipment s : shipments) {
            if (s.getStatus() != ShipmentStatus.RECALLED) {
                s.setStatus(ShipmentStatus.RECALLED);
                affectedShipmentIds.add(s.getId());
            }
        }
        shipmentRepository.saveAll(shipments);

        // Cascade: tất cả TraceCode của các shipment -> RECALLED
        List<TraceCode> allTraceCodes = new ArrayList<>();
        for (UUID shipmentId : affectedShipmentIds) {
            List<TraceCode> codes = traceCodeRepository.findByShipmentId(shipmentId);
            codes.forEach(code -> code.setStatus(TraceCodeStatus.RECALLED));
            allTraceCodes.addAll(codes);
        }
        traceCodeRepository.saveAll(allTraceCodes);

        // Gửi thông báo cho các doanh nghiệp thu mua (người mua) của lô
        int notifiedBuyerCount = sendBuyerNotifications(lot, recallRequest.getReason());

        RecallRequest saved = recallRequestRepository.save(recallRequest);

        RecallRequestResponse response = toResponse(saved);
        response.setNotifiedBuyerCount(notifiedBuyerCount);
        return response;
    }

    @Override
    public RecallRequestResponse reject(UUID id, RejectRecallRequest request, CustomUserDetails currentUser) {
        RecallRequest recallRequest = recallRequestRepository.findById(id)
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
        recallRequest.setRejectionReason(request.getRejectionReason());

        RecallRequest saved = recallRequestRepository.save(recallRequest);
        return toResponse(saved);
    }

    /**
     * Xác định các doanh nghiệp thu mua (người mua) có liên quan đến lô sản xuất
     * và gửi thông báo thu hồi cho họ.
     *
     * <p>
     * Các doanh nghiệp thu mua được xác định qua sự kiện PROCUREMENT
     * (do người dùng VT-04 ghi) trên các lô hàng thuộc lô sản xuất.
     * </p>
     *
     * @param lot    lô sản xuất bị thu hồi
     * @param reason lý do thu hồi
     * @return số lượng người dùng đã nhận thông báo
     */
    private int sendBuyerNotifications(ProductionLot lot, String reason) {
        List<UUID> shipmentIds = shipmentRepository.findByProductionLotId(lot.getId()).stream()
                .map(Shipment::getId)
                .toList();

        if (shipmentIds.isEmpty()) {
            return 0;
        }

        // 1. Lấy các user đã ghi sự kiện PROCUREMENT cho các lô hàng này
        List<UUID> procurementRecorderIds = chainEventRepository
                .findDistinctProcurementRecorderIdsByShipmentIds(shipmentIds);

        if (procurementRecorderIds.isEmpty()) {
            return 0;
        }

        // 2. Tìm các tổ chức mà các user đó trực thuộc
        List<UUID> buyerOrgIds = new ArrayList<>();
        for (UUID recorderId : procurementRecorderIds) {
            List<OrganizationUser> memberships = organizationUserRepository.findAllByUser_UserId(recorderId);
            for (OrganizationUser ou : memberships) {
                if (ou.getStatus() == OrganizationUserStatus.ACTIVE) {
                    UUID orgId = ou.getOrganization().getOrganizationId();
                    if (!buyerOrgIds.contains(orgId)) {
                        buyerOrgIds.add(orgId);
                    }
                    break;
                }
            }
        }

        // 3. Lấy tất cả user đang hoạt động thuộc các tổ chức đó
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
        return notificationService.sendLotRecallNotification(lot.getName(), reason, recipientIds);
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
                .lotId(entity.getProductionLot().getId())
                .lotName(entity.getProductionLot().getName())
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