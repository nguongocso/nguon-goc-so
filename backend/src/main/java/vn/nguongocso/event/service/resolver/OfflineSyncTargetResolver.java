package vn.nguongocso.event.service.resolver;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import vn.nguongocso.event.dto.request.RecordOfflineEventDto;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/** Tra cứu thực thể đích phục vụ xử lý và ghi log đồng bộ sự kiện ngoại tuyến. */
@Component
@RequiredArgsConstructor
public class OfflineSyncTargetResolver {
    private final ProductionLotRepository productionLotRepository;
    private final ShipmentRepository shipmentRepository;
    private final TraceCodeRepository traceCodeRepository;

    /** Tra cứu shipmentId từ DTO hoặc từ codeValue trong eventData. */
    public UUID resolveShipmentId(RecordOfflineEventDto eventDto) {
        if (eventDto.getShipmentId() != null) {
            return eventDto.getShipmentId();
        }
        String codeValue = eventDto.getCodeValue();
        if ((codeValue == null || codeValue.isBlank()) && eventDto.getEventData() != null) {
            Object cv = eventDto.getEventData().get("codeValue");
            if (cv != null) {
                codeValue = cv.toString();
            }
        }
        if (codeValue != null && !codeValue.isBlank()) {
            Optional<TraceCode> traceCode = traceCodeRepository.findByCodeValue(codeValue);
            if (traceCode.isPresent() && traceCode.get().getShipment() != null) {
                return traceCode.get().getShipment().getId();
            }
        }
        return null;
    }

    /** Tra cứu thông tin đích (lotId và lotCode) để ghi nhận failed_event_logs. */
    public SyncTargetInfo resolveTargetInfo(RecordOfflineEventDto eventDto) {
        UUID lotId = null;
        String lotCode = null;
        ChainEventType eventType = eventDto.getEventType();

        if (eventType == ChainEventType.TRANSPORT) {
            UUID shipmentId = resolveShipmentId(eventDto);
            if (shipmentId != null) {
                lotId = shipmentId;
                Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
                if (shipment != null) {
                    lotCode = shipment.getName();
                }
            }
        } else if (eventDto.getProductionLotId() != null) {
            lotId = eventDto.getProductionLotId();
            ProductionLot lot = productionLotRepository.findById(lotId).orElse(null);
            if (lot != null) {
                lotCode = lot.getName();
            }
        }

        if (lotId == null) {
            lotId = eventDto.getProductionLotId() != null ? eventDto.getProductionLotId() : eventDto.getShipmentId();
        }
        if (lotCode == null) {
            lotCode = lotId != null ? lotId.toString() : "UNKNOWN";
        }

        return new SyncTargetInfo(lotId, lotCode);
    }

    /** Bản ghi chứa định danh và mã hiển thị của đối tượng đồng bộ ngoại tuyến. */
    public record SyncTargetInfo(UUID lotId, String lotCode) {}
}
