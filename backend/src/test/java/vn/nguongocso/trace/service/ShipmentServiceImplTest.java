package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.context.ApplicationEventPublisher;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.dto.request.CreateShipmentRequest;
import vn.nguongocso.trace.dto.response.ShipmentResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.QRCodeService;
import vn.nguongocso.trace.service.impl.ShipmentServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {

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
    private PermissionChecker permissionChecker;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    private CustomUserDetails currentUser;
    private Organization organization;
    private ProductionLot productionLot;
    private CodeRange codeRange;
    private CreateShipmentRequest request;
    private UUID organizationId;
    private UUID userId;
    private UUID productionLotId;
    private UUID shipmentId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        productionLotId = UUID.randomUUID();
        shipmentId = UUID.randomUUID();

        organization = new Organization();
        organization.setOrganizationId(organizationId);
        organization.setName("HTX Nông Nghiệp Xanh");

        User user = new User();
        user.setUserId(userId);
        user.setFullName("Nguyễn Văn A");

        currentUser = mock(CustomUserDetails.class);
        lenient().when(currentUser.getUserId()).thenReturn(userId);
        lenient().when(currentUser.getOrganizationId()).thenReturn(organizationId);
        lenient().when(currentUser.getRoleCode()).thenReturn("VT-02");
        lenient().when(currentUser.getFullName()).thenReturn("Nguyễn Văn A");

        // Thiết lập SecurityContext với lenient để tránh lỗi UnnecessaryStubbing
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(currentUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        productionLot = new ProductionLot();
        productionLot.setId(productionLotId);
        productionLot.setName("Lô cà chua vụ Đông");
        productionLot.setOrganization(organization);
        productionLot.setStatus(ProductionLotStatus.PACKAGED);

        codeRange = new CodeRange();
        codeRange.setId(UUID.randomUUID());
        codeRange.setOrganization(organization);
        codeRange.setPrefix("NCL");
        codeRange.setTotalLimit(1000L);
        codeRange.setUsedCount(0L);

        request = new CreateShipmentRequest();
        request.setProductionLotId(productionLotId);
        request.setName("Lô hàng cà chua số 1");
        request.setTotalQuantity(50L);
        request.setPackagingInfo("Đóng thùng 10kg");

        lenient().when(traceCodeRepository.findMaxCodeValueByOrganization(any(), anyString())).thenReturn(null);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== TEST createShipment ====================

    @Test
    void createShipment_ShouldSuccess_WhenAllValid() {
        // Arrange
        when(productionLotRepository.findById(productionLotId))
                .thenReturn(Optional.of(productionLot));

        when(codeRangeRepository.findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId))
                .thenReturn(Optional.of(codeRange));

        when(shipmentRepository.save(any(Shipment.class)))
                .thenAnswer(invocation -> {
                    Shipment s = invocation.getArgument(0);
                    s.setId(UUID.randomUUID());
                    s.setCreatedAt(LocalDateTime.now());
                    return s;
                });

        when(traceCodeRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<TraceCode> list = invocation.getArgument(0);
                    list.forEach(tc -> {
                        tc.setId(UUID.randomUUID());
                        tc.setCreatedAt(LocalDateTime.now());
                    });
                    return list;
                });

        when(qrCodeService.generateQRCode(anyString(), any(), any(), any()))
                .thenReturn("/qr/images/code.png");

        // Act
        ShipmentResponse response = shipmentService.createShipment(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getTotalQuantity()).isEqualTo(request.getTotalQuantity());
        assertThat(response.getPackagingInfo()).isEqualTo(request.getPackagingInfo());
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.CODE_PRINTED);
        assertThat(response.getProductionLotId()).isEqualTo(productionLotId);
        assertThat(response.getProductionLotName()).isEqualTo(productionLot.getName());
        assertThat(response.getTraceCodes()).hasSize((int) request.getTotalQuantity());
        assertThat(response.getTraceCodes().get(0).getCodeValue()).startsWith("NCL");
        assertThat(codeRange.getUsedCount()).isEqualTo(50L);

        verify(shipmentRepository).save(any(Shipment.class));
        verify(traceCodeRepository).saveAll(anyList());
        verify(productionLotRepository).findById(productionLotId);
        verify(codeRangeRepository).findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId);
        verify(qrCodeService, times(50)).generateQRCode(anyString(), any(), any(), any());
    }

    @Test
    void createShipment_ShouldThrowException_WhenRoleNotManager() {
        // Arrange - override role
        when(currentUser.getRoleCode()).thenReturn("VT-01");

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không có quyền tạo lô hàng.");

        verify(productionLotRepository, never()).findById(any());
        verify(shipmentRepository, never()).save(any());
        verify(traceCodeRepository, never()).saveAll(any());
    }

    @Test
    void createShipment_ShouldThrowException_WhenProductionLotNotFound() {
        // Arrange
        when(productionLotRepository.findById(productionLotId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy lô sản xuất.");

        verify(productionLotRepository).findById(productionLotId);
        verify(codeRangeRepository, never()).findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(any());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createShipment_ShouldThrowException_WhenOrganizationMismatch() {
        // Arrange
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(UUID.randomUUID());
        productionLot.setOrganization(otherOrg);

        when(productionLotRepository.findById(productionLotId))
                .thenReturn(Optional.of(productionLot));

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không thuộc tổ chức của lô sản xuất.");

        verify(codeRangeRepository, never()).findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(any());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createShipment_ShouldThrowException_WhenProductionLotStatusNotPackaged() {
        // Arrange
        productionLot.setStatus(ProductionLotStatus.APPROVED);
        when(productionLotRepository.findById(productionLotId))
                .thenReturn(Optional.of(productionLot));

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Chỉ có thể tạo lô hàng từ lô sản xuất đã đóng gói.");

        verify(codeRangeRepository, never()).findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(any());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createShipment_ShouldThrowException_WhenCodeRangeNotFound() {
        // Arrange
        when(productionLotRepository.findById(productionLotId))
                .thenReturn(Optional.of(productionLot));

        when(codeRangeRepository.findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tổ chức chưa được cấp dải mã truy xuất.");

        verify(codeRangeRepository).findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId);
        verify(shipmentRepository, never()).save(any());
        verify(traceCodeRepository, never()).saveAll(any());
    }

    @Test
    void createShipment_ShouldThrowException_WhenCodeRangeLimitExceeded() {
        // Arrange
        when(productionLotRepository.findById(productionLotId))
                .thenReturn(Optional.of(productionLot));

        codeRange.setUsedCount(980L);
        when(codeRangeRepository.findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId))
                .thenReturn(Optional.of(codeRange));
        when(traceCodeRepository.findMaxCodeValueByOrganization(eq(organizationId), anyString()))
                .thenReturn("NCL00000980");

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.createShipment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Số lượng tem vượt quá hạn mức dải mã còn lại.");

        verify(codeRangeRepository).findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId);
        verify(shipmentRepository, never()).save(any());
        verify(traceCodeRepository, never()).saveAll(any());
    }

    @Test
    void createShipment_ShouldGenerateCorrectCodeValues_WhenMultipleCodes() {
        // Arrange
        long quantity = 3L;
        request.setTotalQuantity(quantity);
        codeRange.setUsedCount(5L);

        when(productionLotRepository.findById(productionLotId))
                .thenReturn(Optional.of(productionLot));

        when(codeRangeRepository.findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId))
                .thenReturn(Optional.of(codeRange));

        when(traceCodeRepository.findMaxCodeValueByOrganization(eq(organizationId), anyString()))
                .thenReturn("NCL00000005");

        when(shipmentRepository.save(any(Shipment.class)))
                .thenAnswer(invocation -> {
                    Shipment s = invocation.getArgument(0);
                    s.setId(UUID.randomUUID());
                    s.setCreatedAt(LocalDateTime.now());
                    return s;
                });

        when(traceCodeRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<TraceCode> list = invocation.getArgument(0);
                    list.forEach(tc -> {
                        tc.setId(UUID.randomUUID());
                        tc.setCreatedAt(LocalDateTime.now());
                    });
                    return list;
                });

        when(qrCodeService.generateQRCode(anyString(), any(), any(), any()))
                .thenReturn("/qr/images/code.png");

        // Act
        ShipmentResponse response = shipmentService.createShipment(request);

        // Assert
        assertThat(response.getTraceCodes()).hasSize((int) quantity);
        assertThat(response.getTraceCodes().get(0).getCodeValue()).isEqualTo("NCL00000006");
        assertThat(response.getTraceCodes().get(1).getCodeValue()).isEqualTo("NCL00000007");
        assertThat(response.getTraceCodes().get(2).getCodeValue()).isEqualTo("NCL00000008");
        assertThat(codeRange.getUsedCount()).isEqualTo(8L);
    }

    // ==================== TEST activateShipmentStamps ====================

    @Test
    void activateShipmentStamps_ShouldSuccess_WhenAllValid() {
        // Arrange
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(organization);
        shipment.setProductionLot(productionLot);
        shipment.setStatus(ShipmentStatus.CODE_PRINTED);

        User actor = new User();
        actor.setUserId(userId);
        actor.setFullName("Nguyễn Văn A");

        TraceCode traceCode = new TraceCode();
        traceCode.setId(UUID.randomUUID());
        traceCode.setCodeValue("NCL00000001");
        traceCode.setStatus(TraceCodeStatus.INACTIVE);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));
        when(traceCodeRepository.findByShipmentId(shipmentId)).thenReturn(List.of(traceCode));

        // Act
        ShipmentResponse response = shipmentService.activateShipmentStamps(shipmentId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.ACTIVATED);
        assertThat(traceCode.getStatus()).isEqualTo(TraceCodeStatus.ACTIVE);
        assertThat(traceCode.getActivatedBy()).isEqualTo(actor);
        assertThat(traceCode.getActivatedAt()).isNotNull();

        verify(shipmentRepository).save(shipment);
        verify(traceCodeRepository).saveAll(anyList());
    }

    @Test
    void activateShipmentStamps_ShouldThrowException_WhenRoleNotManager() {
        // Arrange
        when(currentUser.getRoleCode()).thenReturn("VT-03");

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.activateShipmentStamps(shipmentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không có quyền kích hoạt tem.");

        verify(shipmentRepository, never()).findById(any());
    }

    @Test
    void activateShipmentStamps_ShouldThrowException_WhenShipmentNotFound() {
        // Arrange
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.activateShipmentStamps(shipmentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy lô hàng.");
    }

    @Test
    void activateShipmentStamps_ShouldThrowException_WhenDifferentOrganization() {
        // Arrange
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(UUID.randomUUID());

        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(otherOrg);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.activateShipmentStamps(shipmentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không có quyền kích hoạt tem của tổ chức khác.");
    }

    @Test
    void activateShipmentStamps_ShouldThrowException_WhenProductionLotNotPackaged() {
        // Arrange
        ProductionLot unpackaged = new ProductionLot();
        unpackaged.setStatus(ProductionLotStatus.HARVESTED);

        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(organization);
        shipment.setProductionLot(unpackaged);
        shipment.setStatus(ShipmentStatus.CODE_PRINTED);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.activateShipmentStamps(shipmentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Chỉ có thể tạo lô hàng từ lô sản xuất đã đóng gói.");
    }

    @Test
    void activateShipmentStamps_ShouldThrowException_WhenAlreadyActivated() {
        // Arrange
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(organization);
        shipment.setProductionLot(productionLot);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.activateShipmentStamps(shipmentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tem đã được kích hoạt trước đó.");
    }

    @Test
    void activateShipmentStamps_ShouldThrowException_WhenNotCodePrinted() {
        // Arrange
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(organization);
        shipment.setProductionLot(productionLot);
        shipment.setStatus(ShipmentStatus.DRAFT);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.activateShipmentStamps(shipmentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô hàng chưa được cấp hoặc in mã tem.");
    }

    @Test
    void activateShipmentStamps_ShouldThrowException_WhenActorNotFound() {
        // Arrange
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(organization);
        shipment.setProductionLot(productionLot);
        shipment.setStatus(ShipmentStatus.CODE_PRINTED);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.activateShipmentStamps(shipmentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Người dùng không tồn tại.");
    }

    @Test
    void getShipmentById_ShouldSuccess_WhenUserHasPermissionAndSameOrg() {
        // Arrange
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(organization);
        shipment.setProductionLot(productionLot);
        shipment.setName("Lô hàng cà chua");
        shipment.setTotalQuantity(100L);
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipment.setCreatedAt(LocalDateTime.now());

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(traceCodeRepository.findByShipmentId(shipmentId)).thenReturn(List.of());

        // Act
        ShipmentResponse response = shipmentService.getShipmentById(shipmentId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(shipmentId);
        verify(permissionChecker, times(1)).check("shipment", "READ");
    }

    @Test
    void getShipmentById_ShouldThrowException_WhenShipmentNotFound() {
        // Arrange
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.getShipmentById(shipmentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy lô hàng với ID: " + shipmentId);
    }

    @Test
    void getShipmentById_ShouldThrowException_WhenDifferentOrg() {
        // Arrange
        Organization otherOrganization = new Organization();
        otherOrganization.setOrganizationId(UUID.randomUUID());

        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(otherOrganization);
        shipment.setProductionLot(productionLot);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.getShipmentById(shipmentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không có quyền truy cập lô hàng của tổ chức khác.");
    }

    @Test
    void getShipmentById_ShouldSuccess_WhenDifferentOrgButAdminOrAuditor() {
        // Arrange
        lenient().when(currentUser.getRoleCode()).thenReturn("VT-01"); // Admin

        Organization otherOrganization = new Organization();
        otherOrganization.setOrganizationId(UUID.randomUUID());

        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(otherOrganization);
        shipment.setProductionLot(productionLot);
        shipment.setName("Lô hàng chè");
        shipment.setTotalQuantity(50L);
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipment.setCreatedAt(LocalDateTime.now());

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(traceCodeRepository.findByShipmentId(shipmentId)).thenReturn(List.of());

        // Act
        ShipmentResponse response = shipmentService.getShipmentById(shipmentId);

        // Assert
        assertThat(response).isNotNull();
        verify(permissionChecker, times(1)).check("shipment", "READ");
    }
}