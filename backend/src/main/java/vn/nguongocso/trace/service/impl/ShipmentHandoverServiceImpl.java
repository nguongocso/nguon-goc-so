package vn.nguongocso.trace.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.request.CancelHandoverRequest;
import vn.nguongocso.trace.dto.request.CreateHandoverRequest;
import vn.nguongocso.trace.dto.response.HandoverResponse;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.ShipmentHandover;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.ShipmentHandoverService;

@Service
@Transactional
@RequiredArgsConstructor
public class ShipmentHandoverServiceImpl implements ShipmentHandoverService {

    private final ShipmentHandoverRepository handoverRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final ChainEventService chainEventService;
    private final NotificationService notificationService;
    private final PermissionChecker permissionChecker;

    @Value("${app.handover.expiry-hours:48}")
    private int handoverExpiryHours;

    @Override
    public HandoverResponse create(CreateHandoverRequest request) {
        CustomUserDetails currentUser = getCurrentUser();
        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng"));

        validateShipmentForHandover(shipment);
        validateOwnership(shipment, currentUser);

        Organization toOrganization = organizationRepository.findById(request.getToOrganizationId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy tổ chức nhận"));
        if (toOrganization.getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException("Không thể bàn giao cho chính tổ chức của mình");
        }

        long remaining = calculateRemainingQuantity(shipment.getId(), shipment.getTotalQuantity());
        if (request.getQuantity() > remaining) {
            throw new BusinessException(String.format("Lô hàng chỉ còn %s kg có thể bàn giao", remaining));
        }

        ShipmentHandover handover = ShipmentHandover.builder()
                .shipment(shipment)
                .fromOrganization(shipment.getOrganization())
                .toOrganization(toOrganization)
                .quantity(request.getQuantity())
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .plannedAt(request.getPlannedAt())
                .vehicleInfo(request.getVehicleInfo())
                .carrierName(request.getCarrierName())
                .note(request.getNote())
                .expiresAt(LocalDateTime.now().plusHours(handoverExpiryHours))
                .createdBy(currentUser.getUser())
                .build();

        handover = handoverRepository.save(handover);
        notifyReceiverOrganization(handover);
        return mapToResponse(handover);
    }

    @Override
    public HandoverResponse cancel(UUID id, CancelHandoverRequest request) {
        CustomUserDetails currentUser = getCurrentUser();
        ShipmentHandover handover = handoverRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu bàn giao"));

        if (!handover.getFromOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy phiếu bàn giao");
        }
        if (handover.getStatus() != ShipmentHandoverStatus.PENDING_CONFIRMATION) {
            throw new BusinessException("Chỉ có thể hủy phiếu đang chờ xác nhận");
        }

        handover.setStatus(ShipmentHandoverStatus.CANCELLED);
        handover.setCancelReason(request.getReason());
        handover.setCancelledBy(currentUser.getUser());
        handover.setCancelledAt(LocalDateTime.now());
        handover = handoverRepository.save(handover);
        notifyCancellation(handover);
        return mapToResponse(handover);
    }

    @Override
    public HandoverResponse accept(UUID id) {
        CustomUserDetails currentUser = getCurrentUser();
        ShipmentHandover handover = handoverRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu bàn giao"));

        if (!handover.getToOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền xác nhận phiếu bàn giao");
        }
        if (handover.getStatus() != ShipmentHandoverStatus.PENDING_CONFIRMATION) {
            throw new BusinessException("Chỉ có thể xác nhận phiếu đang chờ");
        }
        if (handover.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Phiếu bàn giao đã hết hạn");
        }

        handover.setStatus(ShipmentHandoverStatus.ACCEPTED);
        handover.setConfirmedBy(currentUser.getUser());
        handover.setConfirmedAt(LocalDateTime.now());
        handover = handoverRepository.save(handover);

        ChainEvent event = ChainEvent.builder()
                .shipment(handover.getShipment())
                .eventType(ChainEventType.HANDOVER)
                .eventData(buildHandoverEventData(handover, "ACCEPTED"))
                .recordedAt(LocalDateTime.now())
                .recordedBy(currentUser.getUser())
                .recordedOrganizationId(currentUser.getOrganizationId())
                .build();
        chainEventService.saveWithChainHash(event);

        notifySenderAccepted(handover);
        return mapToResponse(handover);
    }

    @Override
    public HandoverResponse reject(UUID id, CancelHandoverRequest request) {
        CustomUserDetails currentUser = getCurrentUser();
        ShipmentHandover handover = handoverRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu bàn giao"));

        if (!handover.getToOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền từ chối phiếu bàn giao");
        }
        if (handover.getStatus() != ShipmentHandoverStatus.PENDING_CONFIRMATION) {
            throw new BusinessException("Chỉ có thể từ chối phiếu đang chờ");
        }

        handover.setStatus(ShipmentHandoverStatus.REJECTED);
        handover.setRejectedBy(currentUser.getUser());
        handover.setRejectedAt(LocalDateTime.now());
        handover.setCancelReason(request.getReason());
        handover = handoverRepository.save(handover);

        notifySenderRejected(handover, request.getReason());
        return mapToResponse(handover);
    }

    @Override
    public HandoverResponse getById(UUID id) {
        CustomUserDetails currentUser = getCurrentUser();
        ShipmentHandover handover = handoverRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu bàn giao"));

        boolean isFromOrg = handover.getFromOrganization().getOrganizationId().equals(currentUser.getOrganizationId());
        boolean isToOrg = handover.getToOrganization().getOrganizationId().equals(currentUser.getOrganizationId());
        if (!isFromOrg && !isToOrg) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem phiếu bàn giao này");
        }
        return mapToResponse(handover);
    }

    @Override
    public List<HandoverResponse> getSentHandovers() {
        CustomUserDetails currentUser = getCurrentUser();
        List<ShipmentHandover> handovers = handoverRepository.findByFromOrganizationOrganizationId(currentUser.getOrganizationId());
        return handovers.stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<HandoverResponse> getReceivedHandovers() {
        CustomUserDetails currentUser = getCurrentUser();
        List<ShipmentHandover> handovers = handoverRepository.findByToOrganizationOrganizationId(currentUser.getOrganizationId());
        return handovers.stream().map(this::mapToResponse).toList();
    }

    @Override
    public Long getRemainingQuantity(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng"));
        return calculateRemainingQuantity(shipment.getId(), shipment.getTotalQuantity());
    }

    @Override
    public boolean hasPendingHandover(UUID shipmentId) {
        return handoverRepository.existsByShipmentIdAndStatus(shipmentId, ShipmentHandoverStatus.PENDING_CONFIRMATION);
    }

    private CustomUserDetails getCurrentUser() {
        return SecurityUtils.getCurrentUserDetails();
    }

    private void validateShipmentForHandover(Shipment shipment) {
        if (shipment.getStatus() == ShipmentStatus.RECALLED) {
            throw new BusinessException("Lô hàng đang bị thu hồi, không thể tạo phiếu bàn giao");
        }
        boolean hasLockedCodes = traceCodeRepository.existsByShipmentIdAndStatus(
                shipment.getId(), TraceCodeStatus.LOCKED);
        if (hasLockedCodes) {
            throw new BusinessException("Lô hàng có tem bị khóa, không thể tạo phiếu bàn giao");
        }
    }

    private void validateOwnership(Shipment shipment, CustomUserDetails currentUser) {
        if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền tạo phiếu bàn giao");
        }
    }

    private long calculateRemainingQuantity(UUID shipmentId, long totalQuantity) {
        Long committedSum = handoverRepository.sumQuantityByShipmentIdAndStatusIn(
                shipmentId,
                List.of(ShipmentHandoverStatus.PENDING_CONFIRMATION, ShipmentHandoverStatus.ACCEPTED)
        );
        return totalQuantity - (committedSum != null ? committedSum : 0L);
    }

    private HandoverResponse mapToResponse(ShipmentHandover handover) {
        return HandoverResponse.builder()
                .id(handover.getId())
                .shipmentId(handover.getShipment().getId())
                .shipmentName(handover.getShipment().getName())
                .fromOrganizationId(handover.getFromOrganization().getOrganizationId())
                .fromOrganizationName(handover.getFromOrganization().getName())
                .toOrganizationId(handover.getToOrganization().getOrganizationId())
                .toOrganizationName(handover.getToOrganization().getName())
                .quantity(handover.getQuantity())
                .status(handover.getStatus())
                .plannedAt(handover.getPlannedAt())
                .vehicleInfo(handover.getVehicleInfo())
                .carrierName(handover.getCarrierName())
                .note(handover.getNote())
                .expiresAt(handover.getExpiresAt())
                .createdAt(handover.getCreatedAt())
                .confirmedBy(handover.getConfirmedBy() != null ? handover.getConfirmedBy().getUserId() : null)
                .confirmedAt(handover.getConfirmedAt())
                .rejectedBy(handover.getRejectedBy() != null ? handover.getRejectedBy().getUserId() : null)
                .rejectedAt(handover.getRejectedAt())
                .cancelReason(handover.getCancelReason())
                .cancelledBy(handover.getCancelledBy() != null ? handover.getCancelledBy().getUserId() : null)
                .cancelledAt(handover.getCancelledAt())
                .build();
    }

    private String buildHandoverEventData(ShipmentHandover handover, String action) {
        return String.format("{\"action\":\"%s\",\"quantity\":%d,\"fromOrgId\":\"%s\",\"toOrgId\":\"%s\"}",
                action, handover.getQuantity(),
                handover.getFromOrganization().getOrganizationId(),
                handover.getToOrganization().getOrganizationId());
    }

    private void notifyReceiverOrganization(ShipmentHandover handover) {}
    private void notifyCancellation(ShipmentHandover handover) {}
    private void notifySenderAccepted(ShipmentHandover handover) {}
    private void notifySenderRejected(ShipmentHandover handover, String reason) {}
}
