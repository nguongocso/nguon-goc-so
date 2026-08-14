package vn.nguongocso.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import vn.nguongocso.alert.service.ScanAnomalyDetectionService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.report.dto.response.AbnormalScanResponse;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Kiểm thử tích hợp luồng dữ liệu QTN-10.
 *
 * <p>
 * Xác minh toàn bộ luồng:
 * <pre>
 *   CN-001 phát hiện quét bất thường
 *        -> TraceCodeScanLog.isAbnormal = true
 *        -> lưu vào cơ sở dữ liệu
 *        -> truy vấn QTN-10 (LookupStatisticsService.getAbnormalScans)
 *        -> trả về lượt quét bất thường
 * </pre>
 *
 * Sử dụng {@code @SpringBootTest} + profile {@code test} + {@code @Transactional}
 * theo đúng quy ước hạ tầng kiểm thử hiện có (MySQL test, ddl-auto=create-drop).
 * Không mock repository truy vấn — dữ liệu được lưu và truy vấn thật.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Qtn10AbnormalScanDataFlowIntegrationTest {
    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("nguon_goc_so_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
    // Hà Nội, Đà Nẵng, TP.HCM — cách xa nhau (> 5km), đủ để CN-001 coi là 3 vị trí khác nhau.
    private static final double HN_LAT = 21.0285;
    private static final double HN_LON = 105.8542;
    private static final double DN_LAT = 16.0544;
    private static final double DN_LON = 108.2022;
    private static final double HCM_LAT = 10.7769;
    private static final double HCM_LON = 106.7009;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductionLotRepository productionLotRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private TraceCodeRepository traceCodeRepository;

    @Autowired
    private TraceCodeScanLogRepository traceCodeScanLogRepository;

    @Autowired
    private ScanAnomalyDetectionService scanAnomalyDetectionService;

    @Autowired
    private LookupStatisticsService lookupStatisticsService;

    private UUID traceCodeId;
    private UUID organizationId;
    private UUID productionLotId;

    @BeforeEach
    void setUp() {
        Organization organization = Organization.builder()
                .organizationId(UUID.randomUUID())
                .name("HTX Nông Sản Sạch Test")
                .code("HTX-TEST-QTN10")
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .build();
        organization = organizationRepository.save(organization);
        organizationId = organization.getOrganizationId();

        ProductCategory category = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Chè")
                .isActive(true)
                .build();
        category = productCategoryRepository.save(category);

        ProductionLot lot = ProductionLot.builder()
                .organization(organization)
                .productCategory(category)
                .name("Lô chè Tân Cương T8/2026")
                .expectedQuantity(1000.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.DRAFT)
                .build();
        lot = productionLotRepository.save(lot);
        productionLotId = lot.getId();

        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setProductionLot(lot);
        shipment.setOrganization(organization);
        shipment.setName("Lô hàng chè Tân Cương");
        shipment.setTotalQuantity(100L);
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipment = shipmentRepository.save(shipment);

        TraceCode traceCode = new TraceCode();
        traceCode.setId(UUID.randomUUID());
        traceCode.setCodeValue("NCL-QTN10-0001");
        traceCode.setStatus(TraceCodeStatus.ACTIVE);
        traceCode.setShipment(shipment);
        traceCode = traceCodeRepository.save(traceCode);
        traceCodeId = traceCode.getId();

        // 3 lượt quét trong cửa sổ 10 phút, tại 3 vị trí khác nhau.
        LocalDateTime now = LocalDateTime.now();
        traceCodeScanLogRepository.save(scanLog(now.minusMinutes(6), HN_LAT, HN_LON));
        traceCodeScanLogRepository.save(scanLog(now.minusMinutes(4), DN_LAT, DN_LON));
        traceCodeScanLogRepository.save(scanLog(now.minusMinutes(2), HCM_LAT, HCM_LON));

        traceCodeScanLogRepository.flush();
    }

    private TraceCodeScanLog scanLog(LocalDateTime at, double lat, double lon) {
        return TraceCodeScanLog.builder()
                .traceCode(traceCodeRepository.findById(traceCodeId).orElseThrow())
                .scannedAt(at)
                .ipAddress("127.0.0.1")
                .userAgent("integration-test")
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lon))
                .location("Test Location")
                .isAbnormal(false)
                .build();
    }

    @Test
    void qtn10_flow_whenCn001DetectsAnomaly() {
        // Khi: CN-001 phát hiện quét bất thường trên mã tem đã ghi nhận 3 lượt quét.
        scanAnomalyDetectionService.onScanRecorded(traceCodeId);

        // 1) Dữ liệu đã được lưu: isAbnormal = true và abnormalReason không rỗng.
        List<TraceCodeScanLog> persisted = traceCodeScanLogRepository
                .findByTraceCodeIdAndScannedAtGreaterThanEqualOrderByScannedAtDesc(
                        traceCodeId, LocalDateTime.now().minusMinutes(10));
        assertThat(persisted).hasSize(3);
        assertThat(persisted).allSatisfy(log -> {
            assertThat(log.getIsAbnormal()).isTrue();
            assertThat(log.getAbnormalReason()).isNotBlank();
        });

        // 2) Truy vấn qua đường đọc QTN-10 (LookupStatisticsService.getAbnormalScans)
        //    với người dùng VT-01 (chỉ cần roleCode cho logic phân quyền thật của service).
        CustomUserDetails admin = mock(CustomUserDetails.class);
        when(admin.getRoleCode()).thenReturn("VT-01");

        Page<AbnormalScanResponse> abnormalPage = lookupStatisticsService.getAbnormalScans(
                null,
                LocalDate.now().plusDays(1),
                productionLotId,
                organizationId,
                PageRequest.of(0, 20),
                admin);

        // 3) Lượt quét bất thường được trả về.
        assertThat(abnormalPage.getTotalElements()).isEqualTo(3);
        assertThat(abnormalPage.getContent()).allSatisfy(row -> {
            assertThat(row.getReason()).isNotBlank();
            assertThat(row.getCodeValue()).isEqualTo("NCL-QTN10-0001");
        });
    }

    @Test
    void qtn10_flow_whenCn001DoesNotDetectAnomaly() {
        // Chỉ 2 lượt quét (< 3) -> CN-001 không đánh dấu bất thường.
        traceCodeScanLogRepository.deleteAll();
        traceCodeScanLogRepository.flush();
        LocalDateTime now = LocalDateTime.now();
        traceCodeScanLogRepository.save(scanLog(now.minusMinutes(4), HN_LAT, HN_LON));
        traceCodeScanLogRepository.save(scanLog(now.minusMinutes(2), DN_LAT, DN_LON));
        traceCodeScanLogRepository.flush();

        scanAnomalyDetectionService.onScanRecorded(traceCodeId);

        List<TraceCodeScanLog> persisted = traceCodeScanLogRepository
                .findByTraceCodeIdAndScannedAtGreaterThanEqualOrderByScannedAtDesc(
                        traceCodeId, LocalDateTime.now().minusMinutes(10));
        assertThat(persisted).hasSize(2);
        assertThat(persisted).allSatisfy(log -> assertThat(log.getIsAbnormal()).isFalse());

        CustomUserDetails admin = mock(CustomUserDetails.class);
        when(admin.getRoleCode()).thenReturn("VT-01");

        Page<AbnormalScanResponse> abnormalPage = lookupStatisticsService.getAbnormalScans(
                null,
                LocalDate.now().plusDays(1),
                productionLotId,
                organizationId,
                PageRequest.of(0, 20),
                admin);

        assertThat(abnormalPage.getTotalElements()).isZero();
    }
}