package vn.nguongocso.trace.service.impl;

import java.time.LocalDateTime;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
import vn.nguongocso.organization.enums.OrganizationStatus;
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
import vn.nguongocso.trace.service.HandoverExpiryService;
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
    private final HandoverExpiryService handoverExpiryService;

    @Value("${app.handover.expiry-hours:48}")
    private int handoverExpiryHours;

    /** Các loại file chứng từ giao hàng được chấp nhận. */
    private static final Set<String> ALLOWED_ATTACHMENT_TYPES = Set.of(
            "image/jpeg", "image/png", "application/pdf");

    @Value("${app.upload.base-dir}")
    private String baseDir;

    @Value("${app.upload.handover.relative-path:handovers}")
    private String handoverRelativePath;

    @Value("${app.upload.handover.max-size:5242880}")
    private long handoverAttachmentMaxSize;

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
        if (toOrganization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new BusinessException("Tổ chức nhận không còn hoạt động, không thể tạo phiếu bàn giao");
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
                .attachmentPath(request.getAttachmentPath())
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
        if (isExpired(handover)) {
            handoverExpiryService.expireOverdueHandovers();
            throw new BusinessException("Phiếu bàn giao đã hết hạn");
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
        if (isExpired(handover)) {
            handoverExpiryService.expireOverdueHandovers();
            throw new BusinessException("Phiếu bàn giao đã hết hạn");
        }

        handover.setStatus(ShipmentHandoverStatus.ACCEPTED);
        handover.setConfirmedBy(currentUser.getUser());
        handover.setConfirmedAt(LocalDateTime.now());
        handover = handoverRepository.save(handover);

        // QTN-31: Chuyển quyền sở hữu lô hàng sang tổ chức nhận sau khi xác nhận.
        // Từ thời điểm này, chỉ thành viên tổ chức nhận mới được ghi sự kiện tiếp theo.
        Shipment shipment = handover.getShipment();
        shipment.setOrganization(handover.getToOrganization());
        shipmentRepository.save(shipment);

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
        if (isExpired(handover)) {
            handoverExpiryService.expireOverdueHandovers();
            throw new BusinessException("Phiếu bàn giao đã hết hạn");
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
        if (isExpired(handover)) {
            handoverExpiryService.expireOverdueHandovers();
            // Đặt trạng thái EXPIRED tường minh cho response trả về ngay lập tức.
            // Scheduler REQUIRES_NEW đã lưu DB, nhưng entity trong transaction
            // hiện tại vẫn mang status cũ nên cần cập nhật thủ công.
            handover.setStatus(ShipmentHandoverStatus.EXPIRED);
            handoverRepository.save(handover);
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

    @Override
    public String uploadAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File không được để trống");
        }

        if (file.getSize() > handoverAttachmentMaxSize) {
            throw new BusinessException(
                    "File vượt quá dung lượng cho phép ("
                            + handoverAttachmentMaxSize / 1024 / 1024 + "MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_ATTACHMENT_TYPES.contains(contentType)) {
            throw new BusinessException("Loại file không hỗ trợ. Chỉ chấp nhận JPG, PNG, PDF");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        String uploadDir = Paths.get(baseDir, handoverRelativePath).toString();
        String filePath = Paths.get(uploadDir, newFileName).toString();

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Files.copy(file.getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("Lỗi hệ thống khi lưu file");
        }

        return filePath;
    }

    private CustomUserDetails getCurrentUser() {
        return SecurityUtils.getCurrentUserDetails();
    }

    private void validateShipmentForHandover(Shipment shipment) {
        if (shipment.getStatus() == ShipmentStatus.RECALLED) {
            throw new BusinessException("Lô hàng đang bị thu hồi, không thể tạo phiếu bàn giao");
        }
        if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
            throw new BusinessException("Chỉ có thể tạo phiếu bàn giao cho lô hàng đã kích hoạt tem");
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

    /**
     * Kiểm tra phiếu PENDING đã quá thời hạn xác nhận (dù scheduler chưa kịp chạy).
     */
    private boolean isExpired(ShipmentHandover handover) {
        return handover.getExpiresAt() != null
                && handover.getStatus() == ShipmentHandoverStatus.PENDING_CONFIRMATION
                && handover.getExpiresAt().isBefore(LocalDateTime.now());
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
                .attachmentPath(handover.getAttachmentPath())
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

    /**
     * Thông báo phiếu mới tới tổ chức nhận (AC NCL-05-CN-008: tổ chức nhận
     * đã nhận thông báo ngay khi phiếu được tạo).
     */
    private void notifyReceiverOrganization(ShipmentHandover handover) {
        notificationService.sendHandoverNotification(
                "Phiếu bàn giao mới cần xác nhận",
                String.format("Lô hàng \"%s\" có phiếu bàn giao %s kg từ %s đang chờ xác nhận.",
                        handover.getShipment().getName(),
                        handover.getQuantity(),
                        handover.getFromOrganization().getName()),
                handover.getId(),
                handover.getToOrganization().getOrganizationId());
    }

    /**
     * Thông báo hủy phiếu tới tổ chức nhận.
     */
    private void notifyCancellation(ShipmentHandover handover) {
        notificationService.sendHandoverNotification(
                "Phiếu bàn giao đã bị hủy",
                String.format("Phiếu bàn giao %s kg lô hàng \"%s\" đã bị bên giao hủy. Lý do: %s.",
                        handover.getQuantity(),
                        handover.getShipment().getName(),
                        handover.getCancelReason()),
                handover.getId(),
                handover.getToOrganization().getOrganizationId());
    }

    /**
     * Thông báo xác nhận tới tổ chức giao.
     */
    private void notifySenderAccepted(ShipmentHandover handover) {
        notificationService.sendHandoverNotification(
                "Phiếu bàn giao đã được xác nhận",
                String.format("Tổ chức %s đã xác nhận nhận %s kg lô hàng \"%s\".",
                        handover.getToOrganization().getName(),
                        handover.getQuantity(),
                        handover.getShipment().getName()),
                handover.getId(),
                handover.getFromOrganization().getOrganizationId());
    }

    /**
     * Thông báo từ chối tới tổ chức giao.
     */
    private void notifySenderRejected(ShipmentHandover handover, String reason) {
        notificationService.sendHandoverNotification(
                "Phiếu bàn giao đã bị từ chối",
                String.format("Tổ chức %s đã từ chối nhận %s kg lô hàng \"%s\". Lý do: %s.",
                        handover.getToOrganization().getName(),
                        handover.getQuantity(),
                        handover.getShipment().getName(),
                        reason),
                handover.getId(),
                handover.getFromOrganization().getOrganizationId());
    }
}
