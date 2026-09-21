package vn.nguongocso.event.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.CorrectPackagingEventRequest;
import vn.nguongocso.event.dto.request.CorrectPreprocessingEventRequest;
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.request.RecordMobileEventRequest;
import vn.nguongocso.event.dto.request.RecordPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest;
import vn.nguongocso.event.dto.request.RecordTransportEventRequest;
import vn.nguongocso.event.dto.request.RecordWarehouseEntryRequest;
import vn.nguongocso.event.dto.request.RecordWarehouseExitRequest;
import vn.nguongocso.event.dto.request.StorageConditionRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.dto.response.ChainVerificationResponse;
import vn.nguongocso.event.dto.response.CoopWarehouseEventResponse;
import vn.nguongocso.event.dto.response.ScanLookupResponse;
import vn.nguongocso.event.dto.response.StorageConditionResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.service.processor.CoopWarehouseEventProcessor;
import vn.nguongocso.event.service.processor.HarvestEventProcessor;
import vn.nguongocso.event.service.processor.StorageConditionProcessor;
import vn.nguongocso.event.service.recorder.ChainEventHashRecorder;
import vn.nguongocso.event.service.resolver.ChainScanLookupResolver;
import vn.nguongocso.event.service.verifier.ChainIntegrityVerifier;
import vn.nguongocso.exception.BusinessException;

/**
 * Service implementation đóng vai trò Facade điều phối các nghiệp vụ chuỗi sự kiện.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChainEventServiceImpl implements ChainEventService {

    private final ChainEventRepository chainEventRepository;
    private final ChainEventHashRecorder chainEventHashRecorder;
    private final HarvestEventProcessor harvestEventProcessor;
    private final CoopWarehouseEventProcessor coopWarehouseEventProcessor;
    private final StorageConditionProcessor storageConditionProcessor;
    private final ChainScanLookupResolver chainScanLookupResolver;
    private final ChainIntegrityVerifier chainIntegrityVerifier;

    @Override
    @Transactional
    public ChainEventResponse recordHarvestEvent(RecordHarvestEventRequest request, CustomUserDetails currentUser) {
        return harvestEventProcessor.recordHarvestEvent(request, currentUser);
    }

    @Override
    @Transactional
    public ChainEventResponse recordPreprocessingEvent(RecordPreprocessingEventRequest request, CustomUserDetails currentUser) {
        return coopWarehouseEventProcessor.recordPreprocessingEvent(request, currentUser);
    }

    @Override
    @Transactional
    public ChainEventResponse correctPreprocessingEvent(
            UUID originalEventId, CorrectPreprocessingEventRequest request, CustomUserDetails currentUser) {
        return coopWarehouseEventProcessor.correctPreprocessingEvent(originalEventId, request, currentUser);
    }

    @Override
    @Transactional
    public ChainEventResponse recordPackagingEvent(RecordPackagingEventRequest request, CustomUserDetails currentUser) {
        return coopWarehouseEventProcessor.recordPackagingEvent(request, currentUser);
    }

    @Override
    @Transactional
    public ChainEventResponse correctPackagingEvent(
            UUID originalEventId, CorrectPackagingEventRequest request, CustomUserDetails currentUser) {
        return coopWarehouseEventProcessor.correctPackagingEvent(originalEventId, request, currentUser);
    }

    @Override
    @Transactional
    public ChainEventResponse recordTransportEvent(RecordTransportEventRequest request, CustomUserDetails currentUser) {
        return coopWarehouseEventProcessor.recordTransportEvent(request, currentUser);
    }

    @Override
    @Transactional
    public StorageConditionResponse recordStorageCondition(StorageConditionRequest request, CustomUserDetails currentUser) {
        return storageConditionProcessor.recordStorageCondition(request, currentUser);
    }

    @Override
    @Transactional
    public CoopWarehouseEventResponse recordWarehouseEntryEvent(
            RecordWarehouseEntryRequest request, CustomUserDetails currentUser) {
        return coopWarehouseEventProcessor.recordWarehouseEntryEvent(request, currentUser);
    }

    @Override
    @Transactional
    public CoopWarehouseEventResponse recordWarehouseExitEvent(
            RecordWarehouseExitRequest request, CustomUserDetails currentUser) {
        return coopWarehouseEventProcessor.recordWarehouseExitEvent(request, currentUser);
    }

    @Override
    @Transactional
    public ChainEventResponse recordMobileEvent(RecordMobileEventRequest request, CustomUserDetails currentUser) {
        if (request.getEventType() == ChainEventType.HARVEST) {
            return harvestEventProcessor.recordMobileHarvestEvent(request, currentUser);
        } else if (request.getEventType() == ChainEventType.PACKAGING) {
            return coopWarehouseEventProcessor.recordMobilePackagingEvent(request, currentUser);
        } else {
            throw new BusinessException("Loại sự kiện không được hỗ trợ ghi nhận từ thiết bị di động.");
        }
    }

    @Override
    public List<ChainEventResponse> getShipmentTimeline(UUID shipmentId) {
        return chainScanLookupResolver.getShipmentTimeline(shipmentId);
    }

    @Override
    public ScanLookupResponse scanLookup(String codeValue, CustomUserDetails currentUser) {
        return chainScanLookupResolver.scanLookup(codeValue, currentUser);
    }

    @Override
    public ChainVerificationResponse verifyChainIntegrity(UUID shipmentId, CustomUserDetails currentUser) {
        return chainIntegrityVerifier.verifyChainIntegrity(shipmentId, currentUser);
    }

    @Override
    @Transactional
    public ChainEvent saveWithChainHash(ChainEvent event) {
        return chainEventHashRecorder.saveWithChainHash(event);
    }
}
