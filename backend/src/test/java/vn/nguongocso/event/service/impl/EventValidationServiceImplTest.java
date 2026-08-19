package vn.nguongocso.event.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.event.dto.response.FailedEventLogResponse;
import vn.nguongocso.event.dto.response.LotValidationResponse;
import vn.nguongocso.event.entity.FailedEventLog;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.repository.FailedEventLogRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.report.repository.DossierExportHistoryRepository;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

@ExtendWith(MockitoExtension.class)
class EventValidationServiceImplTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private FailedEventLogRepository failedEventLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private CodeRangeRepository codeRangeRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private DossierExportHistoryRepository dossierExportHistoryRepository;

    @InjectMocks
    private EventValidationServiceImpl eventValidationService;

    private CustomUserDetails currentUser;
    private Organization organization;
    private ProductionLot productionLot;
    private Shipment shipment;
    private User user;

    @BeforeEach
    void setUp() {
        currentUser = mock(CustomUserDetails.class);
        UUID orgId = UUID.randomUUID();
        lenient().when(currentUser.getOrganizationId()).thenReturn(orgId);
        lenient().when(currentUser.getUserId()).thenReturn(UUID.randomUUID());

        organization = new Organization();
        organization.setOrganizationId(orgId);

        productionLot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô A")
                .organization(organization)
                .status(ProductionLotStatus.APPROVED)
                .build();

        shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("Lô Hàng A");
        shipment.setOrganization(organization);
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipment.setTotalQuantity(100);

        user = new User();
        user.setUserId(currentUser.getUserId());
        user.setFullName("Nguyễn Văn Ghi");
    }

    @Test
    void validateLot_harvest_success() {
        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        LotValidationResponse response = eventValidationService.validateLot(
                productionLot.getId(), ChainEventType.HARVEST, currentUser);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getLotId()).isEqualTo(productionLot.getId());
    }

    @Test
    void validateLot_harvest_fail_notApproved() {
        productionLot.setStatus(ProductionLotStatus.DRAFT);
        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        LotValidationResponse response = eventValidationService.validateLot(
                productionLot.getId(), ChainEventType.HARVEST, currentUser);

        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).contains("chưa được duyệt");
    }

    @Test
    void validateLot_packaging_success() {
        productionLot.setStatus(ProductionLotStatus.HARVESTED);
        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        LotValidationResponse response = eventValidationService.validateLot(
                productionLot.getId(), ChainEventType.PACKAGING, currentUser);

        assertThat(response.isValid()).isTrue();
    }

    @Test
    void validateLot_preprocessing_success() {
        productionLot.setStatus(ProductionLotStatus.HARVESTED);
        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        LotValidationResponse response = eventValidationService.validateLot(
                productionLot.getId(), ChainEventType.PREPROCESSING, currentUser);

        assertThat(response.isValid()).isTrue();
    }

    @Test
    void validateLot_preprocessing_fail_notHarvested() {
        productionLot.setStatus(ProductionLotStatus.APPROVED);
        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        LotValidationResponse response = eventValidationService.validateLot(
                productionLot.getId(), ChainEventType.PREPROCESSING, currentUser);

        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).contains("Chỉ được ghi nhận sự kiện sơ chế cho lô đã thu hoạch.");
    }

    @Test
    void validateLot_transport_success() {
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        LotValidationResponse response = eventValidationService.validateLot(
                shipment.getId(), ChainEventType.TRANSPORT, currentUser);

        assertThat(response.isValid()).isTrue();
    }

    @Test
    void validateLot_transport_fail_recalled() {
        shipment.setStatus(ShipmentStatus.RECALLED);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        LotValidationResponse response = eventValidationService.validateLot(
                shipment.getId(), ChainEventType.TRANSPORT, currentUser);

        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).contains("thu hồi");
    }

    @Test
    void deleteDraft_success() {
        shipment.setStatus(ShipmentStatus.DRAFT);
        CodeRange codeRange = new CodeRange();
        codeRange.setUsedCount(500L);

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(codeRangeRepository.findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(currentUser.getOrganizationId())).thenReturn(Optional.of(codeRange));

        eventValidationService.deleteDraft(shipment.getId(), currentUser);

        verify(traceCodeRepository, times(1)).deleteByShipmentId(shipment.getId());
        verify(shipmentRepository, times(1)).delete(shipment);
        assertThat(codeRange.getUsedCount()).isEqualTo(400); // 500 - 100
    }

    @Test
    void deleteDraft_fail_notDraft() {
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> eventValidationService.deleteDraft(shipment.getId(), currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể hủy bản nháp");
    }

    @Test
    void logFailedAttempt_success() {
        when(userRepository.findById(currentUser.getUserId())).thenReturn(Optional.of(user));

        eventValidationService.logFailedAttempt(
                productionLot.getId(), "LOT-A", ChainEventType.HARVEST, "Lý do lỗi", currentUser);

        verify(failedEventLogRepository, times(1)).save(any(FailedEventLog.class));
    }

    @Test
    void getFailedLogs_success() {
        Pageable pageable = PageRequest.of(0, 10);
        FailedEventLog log = FailedEventLog.builder()
                .id(UUID.randomUUID())
                .user(user)
                .eventType(ChainEventType.HARVEST)
                .lotId(UUID.randomUUID())
                .lotCode("LOT-A")
                .failureReason("Lỗi")
                .attemptedAt(LocalDateTime.now())
                .build();
        Page<FailedEventLog> page = new PageImpl<>(Collections.singletonList(log), pageable, 1);

        when(failedEventLogRepository.findAllByOrderByAttemptedAtDesc(pageable)).thenReturn(page);

        PageResponse<FailedEventLogResponse> response = eventValidationService.getFailedLogs(pageable);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getLotCode()).isEqualTo("LOT-A");
    }
}
