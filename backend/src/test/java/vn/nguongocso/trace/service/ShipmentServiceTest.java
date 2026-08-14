package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import vn.nguongocso.trace.service.QRCodeService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.dto.request.CreateShipmentRequest;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.impl.ShipmentServiceImpl;

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
    private QRCodeService qrCodeService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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

    @Test
    void createShipment_shouldThrow_whenCodeRangeExceed() {

        // Given
        codeRange.setUsedCount(100L);
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        when(codeRangeRepository.findByOrganizationOrganizationId(orgId))
                .thenReturn(Optional.of(codeRange));
        when(traceCodeRepository.findMaxCodeValueByOrganization(orgId, codeRange.getPrefix()))
            .thenReturn(codeRange.getPrefix() + "100");
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setProductionLotId(productionLot.getId());
        request.setName("Lô hàng 1");
        request.setTotalQuantity(10);
        request.setPackagingInfo("Đóng thùng 20kg");

        // When / Then
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Số lượng tem vượt quá hạn mức dải mã còn lại.");

        // Verify không lưu Shipment hay TraceCode
        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(traceCodeRepository, never()).saveAll(any());
    }

    @Test
    void createShipment_shouldSuccess_whenCodeRangeHasRemaining() {

        // Given
        codeRange.setUsedCount(90L);
        when(productionLotRepository.findById(any())).thenReturn(Optional.of(productionLot));
        when(codeRangeRepository.findByOrganizationOrganizationId(orgId))
                .thenReturn(Optional.of(codeRange));
        when(traceCodeRepository.findMaxCodeValueByOrganization(orgId, codeRange.getPrefix()))
            .thenReturn(null);
            when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
                Shipment savedShipment = invocation.getArgument(0);
                if (savedShipment.getId() == null) {
                    savedShipment.setId(UUID.randomUUID());
                }
                return savedShipment;
            });
        when(traceCodeRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(codeRangeRepository.save(any(CodeRange.class))).thenReturn(codeRange);
        when(qrCodeService.generateQRCode(anyString(), any(), any(), any()))
            .thenReturn("qr/test.png");

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setProductionLotId(productionLot.getId());
        request.setName("Lô hàng 1");
        request.setTotalQuantity(10);
        request.setPackagingInfo("Đóng thùng 20kg");

        // When
        shipmentService.createShipment(request);

        // Then
        verify(shipmentRepository).save(any(Shipment.class));
        verify(traceCodeRepository).saveAll(anyList());
        verify(codeRangeRepository).save(codeRange);
        assertThat(codeRange.getUsedCount()).isEqualTo(10L);
    }
}
