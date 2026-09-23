package vn.nguongocso.event.service.resolver;

import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;

/** Component xác thực quyền và trạng thái lô hàng cho quy trình ghi nhận sự kiện thu mua. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcurementShipmentResolver {
    private final ShipmentRepository shipmentRepository;
    private final ShipmentHandoverRepository shipmentHandoverRepository;
    private final ChainEventRepository chainEventRepository;
    private final EventValidationService eventValidationService;

    /**
     * Tra cứu lô hàng và kiểm tra quyền tiếp nhận của tổ chức người dùng.
     *
     * @param shipmentId định danh lô hàng cần thu mua
     * @param userOrgId  định danh tổ chức của người dùng thực hiện
     * @return lô hàng hợp lệ
     */
    public Shipment resolveAndValidateShipment(UUID shipmentId, UUID userOrgId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng."));

        boolean isRecipient = shipment.getRecipientOrganization() != null
                && userOrgId != null
                && userOrgId.equals(shipment.getRecipientOrganization().getOrganizationId());

        boolean hasAcceptedHandover = userOrgId != null
                && shipmentHandoverRepository.existsByShipmentIdAndToOrganizationOrganizationIdAndStatus(
                        shipment.getId(), userOrgId, ShipmentHandoverStatus.ACCEPTED);

        boolean hasRecordedEvent = userOrgId != null
                && chainEventRepository.existsByShipmentIdAndRecordedOrganizationIdAndEventType(
                        shipment.getId(), userOrgId, ChainEventType.PROCUREMENT);

        if (!isRecipient && !hasAcceptedHandover && !hasRecordedEvent) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Lô hàng không được giao cho tổ chức của bạn.", Map.of("code", "RECIPIENT_MISMATCH"));
        }

        return shipment;
    }

    /**
     * Kiểm tra trạng thái hoạt động của lô hàng thu mua và ghi log thất bại nếu không hợp lệ.
     *
     * @param shipment    lô hàng cần kiểm tra
     * @param currentUser thông tin người dùng thực hiện
     */
    public void validateShipmentStatus(Shipment shipment, CustomUserDetails currentUser) {
        try {
            if (shipment.getStatus() == ShipmentStatus.RECALLED || shipment.getStatus() == ShipmentStatus.RECALLING) {
                throw new BusinessException("Lô hàng đã bị thu hồi, không thể ghi sự kiện.");
            }
            if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
                throw new BusinessException("Lô hàng chưa được kích hoạt, không thể ghi sự kiện thu mua.");
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(
                    shipment.getId(), shipment.getName(), ChainEventType.PROCUREMENT, e.getMessage(), currentUser);
            throw e;
        }
    }
}
