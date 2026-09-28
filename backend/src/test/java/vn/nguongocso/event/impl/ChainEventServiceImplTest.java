package vn.nguongocso.event.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import vn.nguongocso.event.service.impl.ChainEventServiceImpl;
import vn.nguongocso.event.service.processor.CoopWarehouseEventProcessor;
import vn.nguongocso.event.service.processor.HarvestEventProcessor;
import vn.nguongocso.event.service.processor.StorageConditionProcessor;
import vn.nguongocso.event.service.recorder.ChainEventHashRecorder;
import vn.nguongocso.event.service.resolver.ChainScanLookupResolver;
import vn.nguongocso.event.service.verifier.ChainIntegrityVerifier;
import vn.nguongocso.exception.BusinessException;

/**
 * Kiểm thử tầng Facade của ChainEventServiceImpl:
 * Xác nhận việc ủy quyền chính xác cho các processor, resolver, verifier và recorder.
 */
@ExtendWith(MockitoExtension.class)
class ChainEventServiceImplTest {

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private ChainEventHashRecorder chainEventHashRecorder;

    @Mock
    private HarvestEventProcessor harvestEventProcessor;

    @Mock
    private CoopWarehouseEventProcessor coopWarehouseEventProcessor;

    @Mock
    private StorageConditionProcessor storageConditionProcessor;

    @Mock
    private ChainScanLookupResolver chainScanLookupResolver;

    @Mock
    private ChainIntegrityVerifier chainIntegrityVerifier;

    @InjectMocks
    private ChainEventServiceImpl chainEventService;

    private CustomUserDetails user;

    @BeforeEach
    void setUp() {
        user = mock(CustomUserDetails.class);
    }

    @Test
    void recordHarvestEvent_ShouldDelegateToHarvestEventProcessor() {
        RecordHarvestEventRequest req = new RecordHarvestEventRequest();
        ChainEventResponse expected = ChainEventResponse.builder().id(UUID.randomUUID()).build();
        when(harvestEventProcessor.recordHarvestEvent(req, user)).thenReturn(expected);

        ChainEventResponse actual = chainEventService.recordHarvestEvent(req, user);

        assertThat(actual).isSameAs(expected);
        verify(harvestEventProcessor).recordHarvestEvent(req, user);
    }

    @Test
    void recordPreprocessingEvent_ShouldDelegateToCoopWarehouseEventProcessor() {
        RecordPreprocessingEventRequest req = new RecordPreprocessingEventRequest();
        ChainEventResponse expected = ChainEventResponse.builder().id(UUID.randomUUID()).build();
        when(coopWarehouseEventProcessor.recordPreprocessingEvent(req, user)).thenReturn(expected);

        ChainEventResponse actual = chainEventService.recordPreprocessingEvent(req, user);

        assertThat(actual).isSameAs(expected);
        verify(coopWarehouseEventProcessor).recordPreprocessingEvent(req, user);
    }

    @Test
    void correctPreprocessingEvent_ShouldDelegateToCoopWarehouseEventProcessor() {
        UUID eventId = UUID.randomUUID();
        CorrectPreprocessingEventRequest req = new CorrectPreprocessingEventRequest();
        ChainEventResponse expected = ChainEventResponse.builder().id(UUID.randomUUID()).build();
        when(coopWarehouseEventProcessor.correctPreprocessingEvent(eventId, req, user)).thenReturn(expected);

        ChainEventResponse actual = chainEventService.correctPreprocessingEvent(eventId, req, user);

        assertThat(actual).isSameAs(expected);
        verify(coopWarehouseEventProcessor).correctPreprocessingEvent(eventId, req, user);
    }

    @Test
    void recordPackagingEvent_ShouldDelegateToCoopWarehouseEventProcessor() {
        RecordPackagingEventRequest req = new RecordPackagingEventRequest();
        ChainEventResponse expected = ChainEventResponse.builder().id(UUID.randomUUID()).build();
        when(coopWarehouseEventProcessor.recordPackagingEvent(req, user)).thenReturn(expected);

        ChainEventResponse actual = chainEventService.recordPackagingEvent(req, user);

        assertThat(actual).isSameAs(expected);
        verify(coopWarehouseEventProcessor).recordPackagingEvent(req, user);
    }

    @Test
    void correctPackagingEvent_ShouldDelegateToCoopWarehouseEventProcessor() {
        UUID eventId = UUID.randomUUID();
        CorrectPackagingEventRequest req = new CorrectPackagingEventRequest();
        ChainEventResponse expected = ChainEventResponse.builder().id(UUID.randomUUID()).build();
        when(coopWarehouseEventProcessor.correctPackagingEvent(eventId, req, user)).thenReturn(expected);

        ChainEventResponse actual = chainEventService.correctPackagingEvent(eventId, req, user);

        assertThat(actual).isSameAs(expected);
        verify(coopWarehouseEventProcessor).correctPackagingEvent(eventId, req, user);
    }

    @Test
    void recordTransportEvent_ShouldDelegateToCoopWarehouseEventProcessor() {
        RecordTransportEventRequest req = new RecordTransportEventRequest();
        ChainEventResponse expected = ChainEventResponse.builder().id(UUID.randomUUID()).build();
        when(coopWarehouseEventProcessor.recordTransportEvent(req, user)).thenReturn(expected);

        ChainEventResponse actual = chainEventService.recordTransportEvent(req, user);

        assertThat(actual).isSameAs(expected);
        verify(coopWarehouseEventProcessor).recordTransportEvent(req, user);
    }

    @Test
    void recordWarehouseEntryEvent_ShouldDelegateToCoopWarehouseEventProcessor() {
        RecordWarehouseEntryRequest req = new RecordWarehouseEntryRequest();
        CoopWarehouseEventResponse expected = CoopWarehouseEventResponse.builder().id(UUID.randomUUID()).build();
        when(coopWarehouseEventProcessor.recordWarehouseEntryEvent(req, user)).thenReturn(expected);

        CoopWarehouseEventResponse actual = chainEventService.recordWarehouseEntryEvent(req, user);

        assertThat(actual).isSameAs(expected);
        verify(coopWarehouseEventProcessor).recordWarehouseEntryEvent(req, user);
    }

    @Test
    void recordWarehouseExitEvent_ShouldDelegateToCoopWarehouseEventProcessor() {
        RecordWarehouseExitRequest req = new RecordWarehouseExitRequest();
        CoopWarehouseEventResponse expected = CoopWarehouseEventResponse.builder().id(UUID.randomUUID()).build();
        when(coopWarehouseEventProcessor.recordWarehouseExitEvent(req, user)).thenReturn(expected);

        CoopWarehouseEventResponse actual = chainEventService.recordWarehouseExitEvent(req, user);

        assertThat(actual).isSameAs(expected);
        verify(coopWarehouseEventProcessor).recordWarehouseExitEvent(req, user);
    }

    @Test
    void recordMobileEvent_Harvest_ShouldDelegateToHarvestEventProcessor() {
        RecordMobileEventRequest req = new RecordMobileEventRequest();
        req.setEventType(ChainEventType.HARVEST);
        ChainEventResponse expected = ChainEventResponse.builder().id(UUID.randomUUID()).build();
        when(harvestEventProcessor.recordMobileHarvestEvent(req, user)).thenReturn(expected);

        ChainEventResponse actual = chainEventService.recordMobileEvent(req, user);

        assertThat(actual).isSameAs(expected);
        verify(harvestEventProcessor).recordMobileHarvestEvent(req, user);
    }

    @Test
    void recordMobileEvent_Packaging_ShouldDelegateToCoopWarehouseEventProcessor() {
        RecordMobileEventRequest req = new RecordMobileEventRequest();
        req.setEventType(ChainEventType.PACKAGING);
        ChainEventResponse expected = ChainEventResponse.builder().id(UUID.randomUUID()).build();
        when(coopWarehouseEventProcessor.recordMobilePackagingEvent(req, user)).thenReturn(expected);

        ChainEventResponse actual = chainEventService.recordMobileEvent(req, user);

        assertThat(actual).isSameAs(expected);
        verify(coopWarehouseEventProcessor).recordMobilePackagingEvent(req, user);
    }

    @Test
    void recordMobileEvent_UnsupportedType_ShouldThrowException() {
        RecordMobileEventRequest req = new RecordMobileEventRequest();
        req.setEventType(ChainEventType.TRANSPORT);

        assertThatThrownBy(() -> chainEventService.recordMobileEvent(req, user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Loại sự kiện không được hỗ trợ ghi nhận từ thiết bị di động.");
    }

    @Test
    void recordStorageCondition_ShouldDelegateToStorageConditionProcessor() {
        StorageConditionRequest req = new StorageConditionRequest();
        StorageConditionResponse expected = StorageConditionResponse.builder().id(UUID.randomUUID()).build();
        when(storageConditionProcessor.recordStorageCondition(req, user)).thenReturn(expected);

        StorageConditionResponse actual = chainEventService.recordStorageCondition(req, user);

        assertThat(actual).isSameAs(expected);
        verify(storageConditionProcessor).recordStorageCondition(req, user);
    }

    @Test
    void getShipmentTimeline_ShouldDelegateToChainScanLookupResolver() {
        UUID shipmentId = UUID.randomUUID();
        List<ChainEventResponse> expected = List.of(ChainEventResponse.builder().id(UUID.randomUUID()).build());
        when(chainScanLookupResolver.getShipmentTimeline(shipmentId)).thenReturn(expected);

        List<ChainEventResponse> actual = chainEventService.getShipmentTimeline(shipmentId);

        assertThat(actual).isSameAs(expected);
        verify(chainScanLookupResolver).getShipmentTimeline(shipmentId);
    }

    @Test
    void scanLookup_ShouldDelegateToChainScanLookupResolver() {
        String code = "NKS-TEST-001";
        ScanLookupResponse expected = ScanLookupResponse.builder().traceCode(code).build();
        when(chainScanLookupResolver.scanLookup(code, user)).thenReturn(expected);

        ScanLookupResponse actual = chainEventService.scanLookup(code, user);

        assertThat(actual).isSameAs(expected);
        verify(chainScanLookupResolver).scanLookup(code, user);
    }

    @Test
    void verifyChainIntegrity_ShouldDelegateToChainIntegrityVerifier() {
        UUID shipmentId = UUID.randomUUID();
        ChainVerificationResponse expected = ChainVerificationResponse.builder().isIntegrityVerified(true).build();
        when(chainIntegrityVerifier.verifyChainIntegrity(shipmentId, user)).thenReturn(expected);

        ChainVerificationResponse actual = chainEventService.verifyChainIntegrity(shipmentId, user);

        assertThat(actual).isSameAs(expected);
        verify(chainIntegrityVerifier).verifyChainIntegrity(shipmentId, user);
    }

    @Test
    void saveWithChainHash_ShouldDelegateToChainEventHashRecorder() {
        ChainEvent event = ChainEvent.builder().id(UUID.randomUUID()).build();
        when(chainEventHashRecorder.saveWithChainHash(event)).thenReturn(event);

        ChainEvent actual = chainEventService.saveWithChainHash(event);

        assertThat(actual).isSameAs(event);
        verify(chainEventHashRecorder).saveWithChainHash(event);
    }
}
