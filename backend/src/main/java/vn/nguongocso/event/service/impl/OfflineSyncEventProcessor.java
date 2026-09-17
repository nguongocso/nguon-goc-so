package vn.nguongocso.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.request.RecordOfflineEventDto;
import vn.nguongocso.event.dto.request.RecordPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordTransportEventRequest;
import vn.nguongocso.event.dto.response.OfflineEventSyncResultDto;
import vn.nguongocso.event.entity.OfflineSyncLog;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.OfflineSyncLogRepository;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.FarmLogService;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Xử lý một sự kiện ngoại tuyến trong transaction riêng.
 * <p>
 * Đảm bảo logic xử lý offline nhất quán với online:
 * - Sử dụng cùng các service method (recordHarvestEvent, recordPackagingEvent,
 * recordTransportEvent, FarmLogService.create).
 * - Log thất bại vào cả failed_event_logs (qua EventValidationService) và
 * offline_sync_logs.
 * - Hỗ trợ HARVEST, PACKAGING, TRANSPORT, FARM_LOG (NCL-10-CN-012).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineSyncEventProcessor {

    private final OfflineSyncLogRepository offlineSyncLogRepository;
    private final UserRepository userRepository;
    private final ChainEventService chainEventService;
    private final EventValidationService eventValidationService;
    private final ProductionLotRepository productionLotRepository;
    private final ShipmentRepository shipmentRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final FarmLogService farmLogService;

    /**
     * Xử lý một event trong transaction riêng (REQUIRES_NEW).
     * Khi phương thức này được gọi, transaction hiện tại (nếu có) sẽ tạm dừng,
     * và một transaction mới được tạo.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OfflineEventSyncResultDto processEvent(RecordOfflineEventDto eventDto, UUID syncId,
            CustomUserDetails currentUser) {
        try {
            // Kiểm tra trùng lặp trước khi xử lý
            Optional<OfflineSyncLog> existingLog = offlineSyncLogRepository
                    .findByOfflineEventId(eventDto.getOfflineEventId());
            if (existingLog.isPresent() && "SUCCESS".equals(existingLog.get().getStatus())) {
                log.info("Event {} đã được đồng bộ trước đó, bỏ qua.", eventDto.getOfflineEventId());
                return OfflineEventSyncResultDto.builder()
                        .offlineEventId(eventDto.getOfflineEventId())
                        .status("DUPLICATE")
                        .message("Sự kiện đã được đồng bộ trước đó.")
                        .build();
            }

            // Xử lý theo loại sự kiện
            UUID createdEventId = null;
            switch (eventDto.getEventType()) {
                case HARVEST:
                    processHarvestOffline(eventDto, currentUser);
                    break;
                case PACKAGING:
                    processPackagingOffline(eventDto, currentUser);
                    break;
                case TRANSPORT:
                    processTransportOffline(eventDto, currentUser);
                    break;
                case FARM_LOG:
                    createdEventId = processFarmLogOffline(eventDto);
                    break;
                default:
                    throw new BusinessException(
                            "Loại sự kiện không hỗ trợ đồng bộ ngoại tuyến: " + eventDto.getEventType());
            }

            // Ghi log thành công vào offline_sync_logs
            saveSuccessSyncLog(eventDto, syncId, currentUser);

            return OfflineEventSyncResultDto.builder()
                    .offlineEventId(eventDto.getOfflineEventId())
                    .status("SUCCESS")
                    .eventId(createdEventId)
                    .build();

        } catch (BusinessException e) {
            // Lỗi nghiệp vụ -> ghi log vào cả hai bảng
            logFailedAttempts(eventDto, currentUser, e.getMessage());
            saveFailedSyncLog(eventDto, syncId, e.getMessage(), currentUser);
            return OfflineEventSyncResultDto.builder()
                    .offlineEventId(eventDto.getOfflineEventId())
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();

        } catch (Exception e) {
            // Lỗi hệ thống
            log.error("Lỗi xử lý event ngoại tuyến {}: {}", eventDto.getOfflineEventId(), e.getMessage(), e);
            String reason = "Lỗi hệ thống: " + e.getMessage();
            logFailedAttempts(eventDto, currentUser, reason);
            saveFailedSyncLog(eventDto, syncId, reason, currentUser);
            return OfflineEventSyncResultDto.builder()
                    .offlineEventId(eventDto.getOfflineEventId())
                    .status("FAILED")
                    .message(reason)
                    .build();
        }
    }

    private void processHarvestOffline(RecordOfflineEventDto eventDto, CustomUserDetails currentUser) {
        RecordHarvestEventRequest harvestRequest = new RecordHarvestEventRequest();
        harvestRequest.setProductionLotId(eventDto.getProductionLotId());

        Object quantityObj = eventDto.getEventData().get("quantity");
        if (quantityObj == null) {
            throw new BusinessException("Thiếu thông tin sản lượng");
        }
        harvestRequest.setQuantity(((Number) quantityObj).doubleValue());

        Object harvestDateObj = eventDto.getEventData().get("harvestDate");
        if (harvestDateObj == null) {
            throw new BusinessException("Thiếu thông tin ngày thu hoạch");
        }
        harvestRequest.setHarvestDate(LocalDate.parse(harvestDateObj.toString()));

        harvestRequest.setLatitude(eventDto.getLatitude());
        harvestRequest.setLongitude(eventDto.getLongitude());

        // Delegate to the same online service method
        chainEventService.recordHarvestEvent(harvestRequest, currentUser);
    }

    private void processPackagingOffline(RecordOfflineEventDto eventDto, CustomUserDetails currentUser) {
        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(eventDto.getProductionLotId());

        Object specObj = eventDto.getEventData().get("packagingSpecification");
        if (specObj == null) {
            throw new BusinessException("Thiếu thông tin quy cách đóng gói");
        }
        packagingRequest.setPackagingSpecification(specObj.toString());

        Object packagingDateObj = eventDto.getEventData().get("packagingDate");
        if (packagingDateObj == null) {
            throw new BusinessException("Thiếu thông tin ngày đóng gói");
        }
        packagingRequest.setPackagingDate(LocalDate.parse(packagingDateObj.toString()));

        packagingRequest.setLatitude(eventDto.getLatitude());
        packagingRequest.setLongitude(eventDto.getLongitude());

        // Delegate to the same online service method
        chainEventService.recordPackagingEvent(packagingRequest, currentUser);
    }

    private void processTransportOffline(RecordOfflineEventDto eventDto, CustomUserDetails currentUser) {
        RecordTransportEventRequest transportRequest = new RecordTransportEventRequest();

        // Sử dụng codeValue để lookup mã truy xuất (giống online endpoint)
        String codeValue = eventDto.getCodeValue();
        if (codeValue == null || codeValue.isBlank()) {
            // Fallback: thử lấy từ eventData
            Object codeValueObj = eventDto.getEventData().get("codeValue");
            if (codeValueObj != null) {
                codeValue = codeValueObj.toString();
            }
        }
        if (codeValue == null || codeValue.isBlank()) {
            throw new BusinessException("Thiếu mã truy xuất (codeValue) cho sự kiện vận chuyển.");
        }
        transportRequest.setCodeValue(codeValue);

        // Lấy fromLocation và toLocation từ eventData
        Object fromLocationObj = eventDto.getEventData().get("fromLocation");
        Object toLocationObj = eventDto.getEventData().get("toLocation");
        if (fromLocationObj == null || toLocationObj == null) {
            throw new BusinessException("Thiếu thông tin địa điểm vận chuyển (fromLocation, toLocation).");
        }
        transportRequest.setFromLocation(fromLocationObj.toString());
        transportRequest.setToLocation(toLocationObj.toString());

        // Lấy transportTime
        Object transportTimeObj = eventDto.getEventData().get("transportTime");
        if (transportTimeObj != null) {
            transportRequest.setTransportTime(LocalDateTime.parse(transportTimeObj.toString()));
        } else {
            transportRequest.setTransportTime(eventDto.getRecordedAt());
        }

        // Delegate to the same online service method
        chainEventService.recordTransportEvent(transportRequest, currentUser);
    }

    /**
     * Xử lý nhật ký canh tác ghi khi ngoại tuyến (NCL-10-CN-012).
     * <p>
     * Ánh xạ {@code eventData} sang {@code CreateFarmLogRequest} rồi delegate về
     * cùng service method với ghi trực tuyến, nên mọi kiểm tra quyền (QTN-07),
     * tổ chức (QTN-01) và trạng thái lô được giữ nguyên.
     *
     * @param eventDto sự kiện ngoại tuyến loại FARM_LOG
     * @return ID bản ghi farm_logs vừa tạo
     */
    private UUID processFarmLogOffline(RecordOfflineEventDto eventDto) {
        validateFarmLogBasics(eventDto);
        FarmActivityType activityType = extractAndValidateActivityType(eventDto.getEventData());
        LocalDate executedDate = extractAndValidateExecutedDate(eventDto.getEventData());
        CreateFarmLogRequest farmLogRequest = buildCreateFarmLogRequest(eventDto, activityType, executedDate);
        // Delegate về cùng service method với ghi trực tuyến.
        FarmLogResponse created = farmLogService.create(farmLogRequest);
        return created != null ? created.getId() : null;
    }

    /**
     * Kiểm tra điều kiện tiên quyết cho nhật ký canh tác ngoại tuyến.
     *
     * @param eventDto sự kiện ngoại tuyến loại FARM_LOG
     */
    private void validateFarmLogBasics(RecordOfflineEventDto eventDto) {
        if (eventDto.getProductionLotId() == null) {
            throw new BusinessException("Vui lòng chọn lô sản xuất");
        }
        if (eventDto.getEventData() == null) {
            throw new BusinessException("Thiếu dữ liệu nhật ký canh tác.");
        }
    }

    /**
     * Trích xuất và kiểm tra loại hoạt động canh tác.
     *
     * @param eventData dữ liệu sự kiện ngoại tuyến
     * @return loại hoạt động hợp lệ
     */
    private FarmActivityType extractAndValidateActivityType(Map<String, Object> eventData) {
        Object activityTypeObj = eventData.get("activityType");
        if (activityTypeObj == null || activityTypeObj.toString().isBlank()) {
            throw new BusinessException("Vui lòng chọn loại hoạt động");
        }
        try {
            return FarmActivityType.valueOf(activityTypeObj.toString().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Loại hoạt động không hợp lệ: " + activityTypeObj);
        }
    }

    /**
     * Trích xuất và kiểm tra ngày thực hiện.
     *
     * @param eventData dữ liệu sự kiện ngoại tuyến
     * @return ngày thực hiện hợp lệ
     */
    private LocalDate extractAndValidateExecutedDate(Map<String, Object> eventData) {
        Object executedDateObj = eventData.get("executedDate");
        if (executedDateObj == null || executedDateObj.toString().isBlank()) {
            throw new BusinessException("Vui lòng chọn ngày thực hiện");
        }
        try {
            return LocalDate.parse(executedDateObj.toString().trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("Ngày thực hiện không hợp lệ: " + executedDateObj);
        }
    }

    /**
     * Dựng request tạo nhật ký từ dữ liệu ngoại tuyến đã kiểm tra.
     *
     * @param eventDto sự kiện ngoại tuyến loại FARM_LOG
     * @param activityType loại hoạt động đã kiểm tra
     * @param executedDate ngày thực hiện đã kiểm tra
     * @return request tạo nhật ký canh tác
     */
    private CreateFarmLogRequest buildCreateFarmLogRequest(RecordOfflineEventDto eventDto,
            FarmActivityType activityType, LocalDate executedDate) {
        CreateFarmLogRequest farmLogRequest = new CreateFarmLogRequest();
        farmLogRequest.setProductionLotId(eventDto.getProductionLotId());
        farmLogRequest.setActivityType(activityType);
        farmLogRequest.setExecutedDate(executedDate);
        applyOptionalFarmLogFields(eventDto.getEventData(), farmLogRequest);
        return farmLogRequest;
    }

    /**
     * Bổ sung các trường tùy chọn (vật tư, số lượng, đơn vị, ghi chú, mốc canh tác).
     *
     * @param eventData dữ liệu sự kiện ngoại tuyến
     * @param farmLogRequest request đang dựng
     */
    private void applyOptionalFarmLogFields(Map<String, Object> eventData,
            CreateFarmLogRequest farmLogRequest) {
        String material = getOptionalText(eventData, "material");
        if (material != null) {
            farmLogRequest.setMaterial(material);
        }
        applyQuantityField(eventData, farmLogRequest);
        String unit = getOptionalText(eventData, "unit");
        if (unit != null) {
            farmLogRequest.setUnit(unit);
        }
        String notes = getOptionalText(eventData, "notes");
        if (notes != null) {
            farmLogRequest.setNotes(notes);
        }
        applyMilestoneField(eventData, farmLogRequest);
    }

    /**
     * Phân tích số lượng tùy chọn, báo lỗi nghiệp vụ khi không phải số.
     *
     * @param eventData dữ liệu sự kiện ngoại tuyến
     * @param farmLogRequest request đang dựng
     */
    private void applyQuantityField(Map<String, Object> eventData,
            CreateFarmLogRequest farmLogRequest) {
        Object quantityObj = eventData.get("quantity");
        if (quantityObj == null || quantityObj.toString().isBlank()) {
            return;
        }
        try {
            farmLogRequest.setQuantity(Double.valueOf(quantityObj.toString()));
        } catch (NumberFormatException e) {
            throw new BusinessException("Số lượng phải là số.");
        }
    }

    /**
     * Phân tích ID mốc canh tác tùy chọn, báo lỗi nghiệp vụ khi không hợp lệ.
     *
     * @param eventData dữ liệu sự kiện ngoại tuyến
     * @param farmLogRequest request đang dựng
     */
    private void applyMilestoneField(Map<String, Object> eventData,
            CreateFarmLogRequest farmLogRequest) {
        Object milestoneObj = eventData.get("milestoneId");
        if (milestoneObj == null || milestoneObj.toString().isBlank()) {
            return;
        }
        try {
            farmLogRequest.setMilestoneId(Long.valueOf(milestoneObj.toString()));
        } catch (NumberFormatException e) {
            throw new BusinessException("ID mốc canh tác không hợp lệ.");
        }
    }

    /**
     * Lấy chuỗi tùy chọn đã cắt khoảng trắng, trả về null khi trống.
     *
     * @param eventData dữ liệu sự kiện ngoại tuyến
     * @param key khóa cần lấy
     * @return chuỗi đã chuẩn hóa hoặc null
     */
    private String getOptionalText(Map<String, Object> eventData, String key) {
        Object value = eventData.get(key);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString();
    }

    /**
     * Ghi log thất bại vào failed_event_logs (cùng bảng với online).
     * Sử dụng REQUIRES_NEW để không bị ảnh hưởng bởi transaction chính.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedAttempts(RecordOfflineEventDto eventDto, CustomUserDetails currentUser, String reason) {
        try {
            UUID lotId = null;
            String lotCode = null;
            ChainEventType eventType = eventDto.getEventType();

            if (eventType == ChainEventType.TRANSPORT) {
                // Với TRANSPORT, lotId là shipmentId (giống online)
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

            // Nếu không xác định được lotId/lotCode, vẫn log với thông tin có sẵn
            if (lotId == null) {
                lotId = eventDto.getProductionLotId() != null ? eventDto.getProductionLotId()
                        : eventDto.getShipmentId();
            }
            if (lotCode == null) {
                lotCode = lotId != null ? lotId.toString() : "UNKNOWN";
            }

            eventValidationService.logFailedAttempt(lotId, lotCode, eventType, reason, currentUser);
        } catch (Exception ex) {
            log.error("Không thể ghi log thất bại vào failed_event_logs cho event {}: {}",
                    eventDto.getOfflineEventId(), ex.getMessage(), ex);
        }
    }

    /**
     * Lưu log thất bại vào offline_sync_logs với pessimistic locking.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveFailedSyncLog(RecordOfflineEventDto eventDto, UUID syncId, String reason,
            CustomUserDetails currentUser) {
        try {
            User actor = userRepository.findById(currentUser.getUserId()).orElse(null);
            if (actor == null)
                return;

            UUID lotId = null;
            UUID shipmentId = null;
            if (eventDto.getEventType() == ChainEventType.TRANSPORT
                    || eventDto.getEventType() == ChainEventType.PROCUREMENT) {
                shipmentId = eventDto.getShipmentId() != null ? eventDto.getShipmentId() : resolveShipmentId(eventDto);
                lotId = eventDto.getProductionLotId();
            } else {
                lotId = eventDto.getProductionLotId();
            }

            // Tìm và khóa bản ghi hiện có
            Optional<OfflineSyncLog> existing = offlineSyncLogRepository
                    .findByOfflineEventIdWithLock(eventDto.getOfflineEventId());

            if (existing.isPresent()) {
                OfflineSyncLog syncLog = existing.get();
                syncLog.setSyncId(syncId);
                syncLog.setFailureReason(reason);
                syncLog.setSyncedAt(LocalDateTime.now());
                offlineSyncLogRepository.save(syncLog);
                log.info("Updated existing offline sync log for event {}", eventDto.getOfflineEventId());
            } else {
                OfflineSyncLog newLog = OfflineSyncLog.builder()
                        .syncId(syncId)
                        .user(actor)
                        .offlineEventId(eventDto.getOfflineEventId())
                        .productionLotId(lotId)
                        .shipmentId(shipmentId)
                        .eventType(eventDto.getEventType())
                        .status("FAILED")
                        .failureReason(reason)
                        .build();
                offlineSyncLogRepository.save(newLog);
                log.info("Inserted new offline sync log for event {}", eventDto.getOfflineEventId());
            }
        } catch (Exception ex) {
            log.error("Không thể lưu log thất bại cho offlineEventId: {}", eventDto.getOfflineEventId(), ex);
        }
    }

    /**
     * Lưu log thành công vào offline_sync_logs.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveSuccessSyncLog(RecordOfflineEventDto eventDto, UUID syncId, CustomUserDetails currentUser) {
        try {
            User actor = userRepository.findById(currentUser.getUserId()).orElse(null);
            if (actor == null)
                return;

            UUID lotId = null;
            UUID shipmentId = null;
            if (eventDto.getEventType() == ChainEventType.TRANSPORT
                    || eventDto.getEventType() == ChainEventType.PROCUREMENT) {
                shipmentId = eventDto.getShipmentId() != null ? eventDto.getShipmentId() : resolveShipmentId(eventDto);
                lotId = eventDto.getProductionLotId();
            } else {
                lotId = eventDto.getProductionLotId();
            }

            Optional<OfflineSyncLog> existing = offlineSyncLogRepository
                    .findByOfflineEventIdWithLock(eventDto.getOfflineEventId());

            if (existing.isPresent()) {
                OfflineSyncLog syncLog = existing.get();
                syncLog.setSyncId(syncId);
                syncLog.setStatus("SUCCESS");
                syncLog.setFailureReason(null);
                syncLog.setSyncedAt(LocalDateTime.now());
                offlineSyncLogRepository.save(syncLog);
            } else {
                OfflineSyncLog newLog = OfflineSyncLog.builder()
                        .syncId(syncId)
                        .user(actor)
                        .offlineEventId(eventDto.getOfflineEventId())
                        .productionLotId(lotId)
                        .shipmentId(shipmentId)
                        .eventType(eventDto.getEventType())
                        .status("SUCCESS")
                        .build();
                offlineSyncLogRepository.save(newLog);
            }
        } catch (Exception ex) {
            log.error("Không thể lưu log thành công cho offlineEventId: {}", eventDto.getOfflineEventId(), ex);
        }
    }

    /**
     * Resolve shipmentId từ codeValue hoặc shipmentId trong DTO.
     */
    private UUID resolveShipmentId(RecordOfflineEventDto eventDto) {
        if (eventDto.getShipmentId() != null) {
            return eventDto.getShipmentId();
        }
        if (eventDto.getCodeValue() != null && !eventDto.getCodeValue().isBlank()) {
            Optional<TraceCode> traceCode = traceCodeRepository.findByCodeValue(eventDto.getCodeValue());
            if (traceCode.isPresent() && traceCode.get().getShipment() != null) {
                return traceCode.get().getShipment().getId();
            }
        }
        return null;
    }
}