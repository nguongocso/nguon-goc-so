package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.dto.request.CreateShipmentRequest;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.impl.ShipmentServiceImpl;
import vn.nguongocso.notification.NotificationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private CodeRangeRepository codeRangeRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QRCodeService qrCodeService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    private CustomUserDetails currentUser;
    private Organization organization;
    private ProductionLot productionLot;
    private CodeRange codeRange;
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        // Mock SecurityContextHolder
        currentUser = mock(CustomUserDetails.class);
        when(currentUser.getOrganizationId()).thenReturn(orgId);
        when(currentUser.getRoleCode()).thenReturn("VT-02");
        lenient().when(currentUser.getUserId()).thenReturn(UUID.randomUUID());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(currentUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        organization = new Organization();
        organization.setOrganizationId(orgId);
        organization.setName("HTX Xanh");

        productionLot = new ProductionLot();
        productionLot.setId(UUID.randomUUID());
        productionLot.setOrganization(organization);
        productionLot.setStatus(ProductionLotStatus.PACKAGED);
        productionLot.setName("Lô lúa vụ hè");

        codeRange = CodeRange.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .prefix("893001")
                .totalLimit(100L)
                .usedCount(0L)
                .build();
    }

    private CreateShipmentRequest createRequest(long totalQuantity) {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setProductionLotId(productionLot.getId());
        request.setName("Lô hàng 1");
        request.setTotalQuantity(totalQuantity);
        request.setPackagingInfo("Đóng thùng 20kg");
        return request;
    }

    private void mockCodeRangeFindAll(List<CodeRange> ranges) {
        when(codeRangeRepository.findAllByOrganizationOrganizationId(orgId))
                .thenReturn(ranges);
    }

    private void mockCodeRangeLock(CodeRange cr) {
        when(codeRangeRepository.findByIdAndOrganizationIdForUpdate(cr.getId(), orgId))
                .thenReturn(Optional.of(cr));
    }

    // ==================== Case 1: One CodeRange with enough capacity ====================

    @Test
    void createShipment_shouldSucceed_whenSingleCodeRangeHasCapacity() {

        // Given
        codeRange.setUsedCount(90L);
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        mockCodeRangeFindAll(List.of(codeRange));
        mockCodeRangeLock(codeRange);

        User authUser = new User();
        authUser.setUserId(UUID.randomUUID());
        when(userRepository.findById(any())).thenReturn(Optional.of(authUser));

        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(traceCodeRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(qrCodeService.generateQRCode(anyString(), any(), any(), any()))
                .thenReturn("/qr/images/code.png");
        when(codeRangeRepository.save(any(CodeRange.class))).thenReturn(codeRange);

        CreateShipmentRequest request = createRequest(10);

        // When
        shipmentService.createShipment(request);

        // Then
        verify(shipmentRepository).save(any(Shipment.class));
        verify(traceCodeRepository).saveAll(anyList());
        verify(codeRangeRepository).save(codeRange);
        assertThat(codeRange.getUsedCount()).isEqualTo(100L);
    }

    // ==================== Case 2: Multiple CodeRanges, one exhausted, one available ====================

    @Test
    void createShipment_shouldSelectCodeRangeWithRemainingCapacity_whenMultipleCodeRanges() {

        // Given
        CodeRange codeRangeA = CodeRange.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .prefix("893001")
                .totalLimit(1000L)
                .usedCount(1000L)   // exhausted
                .build();

        CodeRange codeRangeB = CodeRange.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .prefix("893002")
                .totalLimit(5000L)
                .usedCount(1000L)   // remaining = 4000
                .build();

        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        mockCodeRangeFindAll(List.of(codeRangeA, codeRangeB));
        // codeRangeB should be selected (greatest remaining = 4000)
        mockCodeRangeLock(codeRangeB);

        User authUser = new User();
        authUser.setUserId(UUID.randomUUID());
        when(userRepository.findById(any())).thenReturn(Optional.of(authUser));

        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(traceCodeRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(qrCodeService.generateQRCode(anyString(), any(), any(), any()))
                .thenReturn("/qr/images/code.png");
        when(codeRangeRepository.save(any(CodeRange.class))).thenReturn(codeRangeB);

        CreateShipmentRequest request = createRequest(500);

        // When
        shipmentService.createShipment(request);

        // Then
        verify(shipmentRepository).save(any(Shipment.class));
        verify(traceCodeRepository).saveAll(anyList());
        verify(codeRangeRepository).save(codeRangeB);
        // codeRangeA should NOT be saved (unchanged)
        verify(codeRangeRepository, never()).save(codeRangeA);
        // usedCount on B should be updated: 1000 + 500 = 1500
        assertThat(codeRangeB.getUsedCount()).isEqualTo(1500L);
        // A remains 1000
        assertThat(codeRangeA.getUsedCount()).isEqualTo(1000L);
    }

    // ==================== Case 3: All CodeRanges exhausted ====================

    @Test
    void createShipment_shouldThrow_whenAllCodeRangesExhausted() {

        // Given
        CodeRange exhaustedA = CodeRange.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .prefix("893001")
                .totalLimit(1000L)
                .usedCount(1000L)
                .build();

        CodeRange exhaustedB = CodeRange.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .prefix("893002")
                .totalLimit(500L)
                .usedCount(500L)
                .build();

        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        mockCodeRangeFindAll(List.of(exhaustedA, exhaustedB));

        CreateShipmentRequest request = createRequest(10);

        // When / Then
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Số lượng tem vượt quá hạn mức dải mã còn lại.");

        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(traceCodeRepository, never()).saveAll(any());
        verify(codeRangeRepository, never()).save(any(CodeRange.class));
    }

    // ==================== Case 4: Requested quantity exceeds available capacity ====================

    @Test
    void createShipment_shouldThrow_whenQuantityExceedsCapacity() {

        // Given
        codeRange.setUsedCount(95L);  // remaining = 5
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        mockCodeRangeFindAll(List.of(codeRange));
        mockCodeRangeLock(codeRange);

        CreateShipmentRequest request = createRequest(10);  // 10 > 5

        // When / Then
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Số lượng tem vượt quá hạn mức dải mã còn lại.");

        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(traceCodeRepository, never()).saveAll(any());
    }

    // ==================== Case 5: Organization isolation ====================

    @Test
    void createShipment_shouldThrow_whenOrganizationMismatch() {

        // Given
        UUID otherOrgId = UUID.randomUUID();
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(otherOrgId);
        productionLot.setOrganization(otherOrg);
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));

        CreateShipmentRequest request = createRequest(10);

        // When / Then
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thuộc tổ chức");

        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    // ==================== Existing tests (adapted) ====================

    @Test
    void createShipment_shouldThrow_whenCodeRangeExceed() {

        // Given
        codeRange.setUsedCount(100L);
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        mockCodeRangeFindAll(List.of(codeRange));
        // No lock mock needed: codeRange is exhausted (totalLimit=100, usedCount=100),
        // so findAvailableCodeRange throws before reaching the lock step

        CreateShipmentRequest request = createRequest(10);

        // When / Then
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Số lượng tem vượt quá hạn mức dải mã còn lại.");

        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(traceCodeRepository, never()).saveAll(any());
    }

    @Test
    void createShipment_shouldSuccess_whenCodeRangeHasRemaining() {

        // Given
        codeRange.setUsedCount(90L);
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        mockCodeRangeFindAll(List.of(codeRange));
        mockCodeRangeLock(codeRange);

        User authUser = new User();
        authUser.setUserId(UUID.randomUUID());
        when(userRepository.findById(any())).thenReturn(Optional.of(authUser));

        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(traceCodeRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(qrCodeService.generateQRCode(anyString(), any(), any(), any()))
                .thenReturn("/qr/images/code.png");
        when(codeRangeRepository.save(any(CodeRange.class))).thenReturn(codeRange);

        CreateShipmentRequest request = createRequest(10);

        // When
        shipmentService.createShipment(request);

        // Then
        verify(shipmentRepository).save(any(Shipment.class));
        verify(traceCodeRepository).saveAll(anyList());
        verify(codeRangeRepository).save(codeRange);
        assertThat(codeRange.getUsedCount()).isEqualTo(100L);
    }

    @Test
    void createShipment_shouldThrow_whenProductionLotNotPackaged() {

        // Given
        productionLot.setStatus(ProductionLotStatus.DRAFT);
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));

        CreateShipmentRequest request = createRequest(10);

        // When / Then
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã đóng gói");

        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(traceCodeRepository, never()).saveAll(any());
    }

    @Test
    void createShipment_shouldThrow_whenProductionLotNotFound() {

        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(productionLotRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setProductionLotId(nonExistentId);
        request.setName("Lô hàng 1");
        request.setTotalQuantity(10);

        // When / Then
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy lô sản xuất");

        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void createShipment_shouldThrow_whenNoCodeRange() {

        // Given
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        mockCodeRangeFindAll(List.of());  // empty list

        CreateShipmentRequest request = createRequest(10);

        // When / Then
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa được cấp dải mã");

        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void createShipment_shouldThrow_whenUserNotFound() {

        // Given
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        mockCodeRangeFindAll(List.of(codeRange));
        mockCodeRangeLock(codeRange);
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        CreateShipmentRequest request = createRequest(5);

        // When / Then
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Người dùng không tồn tại");

        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void createShipment_shouldVerifyTraceCodeGenerationAndResponseMapping() {

        // Given
        codeRange.setUsedCount(50L);
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        mockCodeRangeFindAll(List.of(codeRange));
        mockCodeRangeLock(codeRange);

        User authUser = new User();
        authUser.setUserId(UUID.randomUUID());
        when(userRepository.findById(any())).thenReturn(Optional.of(authUser));

        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });
        when(traceCodeRepository.saveAll(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<TraceCode> codes = inv.getArgument(0);
            // Simulate JPA @PrePersist — assign IDs
            for (TraceCode tc : codes) {
                if (tc.getId() == null) {
                    tc.setId(UUID.randomUUID());
                }
            }
            return codes;
        });
        when(qrCodeService.generateQRCode(anyString(), any(), any(), any()))
                .thenReturn("/files/qr/test.png");
        when(codeRangeRepository.save(any(CodeRange.class))).thenReturn(codeRange);

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setProductionLotId(productionLot.getId());
        request.setName("Lô hàng 1");
        request.setTotalQuantity(3);
        request.setPackagingInfo("Đóng thùng 20kg");

        // When
        var response = shipmentService.createShipment(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Lô hàng 1");
        assertThat(response.getTotalQuantity()).isEqualTo(3L);
        assertThat(response.getPackagingInfo()).isEqualTo("Đóng thùng 20kg");
        assertThat(response.getProductionLotId()).isEqualTo(productionLot.getId());
        assertThat(response.getProductionLotName()).isEqualTo("Lô lúa vụ hè");
        assertThat(response.getTraceCodes()).hasSize(3);

        // Verify trace codes have required fields
        for (var tc : response.getTraceCodes()) {
            assertThat(tc.getId()).isNotNull();
            assertThat(tc.getCodeValue()).isNotNull();
            assertThat(tc.getQrImage()).isEqualTo("/files/qr/test.png");
            assertThat(tc.getStatus()).isNotNull();
        }

        // Verify codeRange usedCount was updated
        assertThat(codeRange.getUsedCount()).isEqualTo(53L);

        // Verify QR service was called correct number of times
        verify(qrCodeService, times(3)).generateQRCode(anyString(), any(), any(), any());
    }
}