package vn.nguongocso.event.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.event.dto.response.FailedEventLogResponse;
import vn.nguongocso.event.dto.response.LotValidationResponse;
import vn.nguongocso.event.entity.FailedEventLog;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.FailedEventLogRepository;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.event.service.processor.ShipmentDraftCleaner;
import vn.nguongocso.event.service.recorder.FailedEventLogRecorder;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Implementation xác thực tính hợp lệ của lô hàng/lô sản xuất trước khi ghi sự kiện.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventValidationServiceImpl implements EventValidationService {

    private final ProductionLotRepository productionLotRepository;
    private final ShipmentRepository shipmentRepository;
    private final FailedEventLogRepository failedEventLogRepository;
    private final FailedEventLogRecorder failedEventLogRecorder;
    private final ShipmentDraftCleaner shipmentDraftCleaner;

    @Override
    public LotValidationResponse validateLot(UUID lotId, ChainEventType eventType, CustomUserDetails currentUser) {
        if (eventType == ChainEventType.HARVEST || eventType == ChainEventType.PREPROCESSING || eventType == ChainEventType.PACKAGING) {
            return validateProductionLot(lotId, eventType, currentUser);
        } else if (eventType == ChainEventType.TRANSPORT || eventType == ChainEventType.PROCUREMENT) {
            return validateShipment(lotId, eventType, currentUser);
        }
        throw new BusinessException("Loại sự kiện không được hỗ trợ để xác thực lô.");
    }

    private LotValidationResponse validateProductionLot(UUID lotId, ChainEventType eventType, CustomUserDetails currentUser) {
        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        boolean valid = false;
        String message;

        if (!lot.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            message = "Bạn không thuộc tổ chức quản lý của lô sản xuất này.";
        } else if (eventType == ChainEventType.HARVEST && lot.getStatus() != ProductionLotStatus.APPROVED) {
            message = "Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.";
        } else if (eventType == ChainEventType.PREPROCESSING && lot.getStatus() != ProductionLotStatus.HARVESTED) {
            message = "Chỉ được ghi nhận sự kiện sơ chế cho lô đã thu hoạch.";
        } else if (eventType == ChainEventType.PACKAGING && (lot.getStatus() != ProductionLotStatus.HARVESTED && lot.getStatus() != ProductionLotStatus.PREPROCESSED)) {
            message = "Chỉ được ghi nhận sự kiện đóng gói cho lô đã thu hoạch hoặc đã sơ chế.";
        } else if (lot.getStatus() == ProductionLotStatus.CANCELLED) {
            message = "Lô sản xuất đã bị hủy, không thể ghi sự kiện.";
        } else {
            valid = true;
            message = "Lô sản xuất hợp lệ.";
        }

        return LotValidationResponse.builder()
                .lotId(lotId)
                .eventType(eventType.name())
                .valid(valid)
                .message(message)
                .details(LotValidationResponse.LotDetails.builder()
                        .lotType("PRODUCTION_LOT")
                        .currentStatus(lot.getStatus().name())
                        .organizationId(lot.getOrganization().getOrganizationId())
                        .build())
                .build();
    }

    private LotValidationResponse validateShipment(UUID lotId, ChainEventType eventType, CustomUserDetails currentUser) {
        Shipment shipment = shipmentRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng."));

        boolean valid = false;
        String message;

        if (eventType == ChainEventType.TRANSPORT
                && !shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            message = "Bạn không thuộc tổ chức quản lý của lô hàng này.";
        } else if (shipment.getStatus() == ShipmentStatus.RECALLED || shipment.getStatus() == ShipmentStatus.RECALLING) {
            message = "Lô hàng đã bị thu hồi, không thể ghi sự kiện.";
        } else if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
            message = "Lô hàng chưa được kích hoạt, không thể ghi sự kiện.";
        } else {
            valid = true;
            message = "Lô hàng hợp lệ.";
        }

        return LotValidationResponse.builder()
                .lotId(lotId)
                .eventType(eventType.name())
                .valid(valid)
                .message(message)
                .details(LotValidationResponse.LotDetails.builder()
                        .lotType("SHIPMENT")
                        .currentStatus(shipment.getStatus().name())
                        .organizationId(shipment.getOrganization().getOrganizationId())
                        .build())
                .build();
    }

    @Override
    @Transactional
    public void deleteDraft(UUID draftId, CustomUserDetails currentUser) {
        shipmentDraftCleaner.deleteDraft(draftId, currentUser);
    }

    @Override
    public PageResponse<FailedEventLogResponse> getFailedLogs(Pageable pageable) {
        Page<FailedEventLog> logs = failedEventLogRepository.findAllByOrderByAttemptedAtDesc(pageable);
        List<FailedEventLogResponse> items = logs.getContent().stream()
                .map(logItem -> FailedEventLogResponse.builder()
                        .id(logItem.getId())
                        .userId(logItem.getUser().getUserId())
                        .userFullName(logItem.getUser().getFullName())
                        .eventType(logItem.getEventType().name())
                        .lotId(logItem.getLotId())
                        .lotCode(logItem.getLotCode())
                        .failureReason(logItem.getFailureReason())
                        .attemptedAt(logItem.getAttemptedAt())
                        .build())
                .toList();

        return PageResponse.from(logs, items);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedAttempt(UUID lotId, String lotCode, ChainEventType eventType, String reason,
            CustomUserDetails currentUser) {
        failedEventLogRecorder.recordFailedAttempt(lotId, lotCode, eventType, reason, currentUser);
    }
}
