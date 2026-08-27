package vn.nguongocso.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.AdministrativeUnit;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.entity.UserAreaAssignment;
import vn.nguongocso.organization.enums.AdministrativeUnitLevel;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.AdministrativeUnitRepository;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.organization.repository.UserAreaAssignmentRepository;
import vn.nguongocso.report.dto.response.CropAreaAnalysisResponse;
import vn.nguongocso.report.dto.response.IndustryReportResponse;
import vn.nguongocso.report.dto.response.SeasonYieldComparisonResponse;
import vn.nguongocso.report.service.ReportAccessLogService;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Kiểm thử tích hợp rule bảo mật số 1 của NCL-670: báo cáo đa tổ chức LUÔN
 * giao trong phạm vi địa bàn đã gán của VT-05.
 *
 * <p>
 * TC-01: cán bộ được gán 2 địa bàn => chỉ thấy dữ liệu tổ chức mapped trong 2
 * đơn vị đó; TC-02 (release blocker): cán bộ CHƯA được gán => dữ liệu rỗng +
 * thông báo chuẩn, tuyệt đối không fallback toàn bộ dữ liệu.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AreaScopeReportIntegrationTest {

    private static final String UNASSIGNED_MESSAGE = "Bạn chưa được phân công địa bàn quản lý nào.";

    @Autowired
    private ReportService reportService;

    @Autowired
    private CropAreaAnalysisService cropAreaAnalysisService;

    @Autowired
    private OpenDataExportService openDataExportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationUserRepository organizationUserRepository;

    @Autowired
    private AdministrativeUnitRepository administrativeUnitRepository;

    @Autowired
    private UserAreaAssignmentRepository userAreaAssignmentRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private FarmAreaRepository farmAreaRepository;

    @Autowired
    private ProductionLotRepository productionLotRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private ReportAccessLogService reportAccessLogService;

    private AdministrativeUnit tinhNinhBinh;
    private AdministrativeUnit xaHoaLau;
    private AdministrativeUnit xaGiaVien;
    private AdministrativeUnit tinhThanhHoa;

    private Organization orgA1;
    private Organization orgA2;
    private Organization orgB;
    private Organization orgNone;

    private User adminUser;
    private User vt05Assigned;
    private User vt05Unassigned;

    private String catNameA1;
    private String catNameA2;
    private String catNameB;

    private String areaNameA1;
    private String areaNameA2;
    private String areaNameB;

    private long shipQtyA1 = 100;
    private long shipQtyB = 50;

    @BeforeEach
    void setUp() {
        seedUnits();
        seedOrganizationsAndUsers();
        seedLotsAndShipments();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== fixtures ====================

    private void seedUnits() {
        tinhNinhBinh = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("36").name("Ninh Bình")
                .level(AdministrativeUnitLevel.PROVINCE).active(true).build());
        xaHoaLau = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("04098").name("Hoa Lư")
                .level(AdministrativeUnitLevel.COMMUNE)
                .parent(tinhNinhBinh).province(tinhNinhBinh).active(true).build());
        xaGiaVien = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("04099").name("Gia Viễn")
                .level(AdministrativeUnitLevel.COMMUNE)
                .parent(tinhNinhBinh).province(tinhNinhBinh).active(true).build());
        tinhThanhHoa = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("38").name("Thanh Hóa")
                .level(AdministrativeUnitLevel.PROVINCE).active(true).build());
    }

    private void seedOrganizationsAndUsers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        orgA1 = organizationRepository.save(Organization.builder()
                .name("HTX Hoa Lư " + suffix).code("HTX-A1-" + suffix)
                .type(OrganizationType.COOPERATIVE).status(OrganizationStatus.ACTIVE)
                .address("Xã Hoa Lư, Ninh Bình").province(xaHoaLau.getProvince())
                .commune(xaHoaLau).build());

        orgA2 = organizationRepository.save(Organization.builder()
                .name("HTX Gia Viễn " + suffix).code("HTX-A2-" + suffix)
                .type(OrganizationType.COOPERATIVE).status(OrganizationStatus.ACTIVE)
                .address("Xã Gia Viễn, Ninh Bình").province(xaGiaVien.getProvince())
                .commune(xaGiaVien).build());

        orgB = organizationRepository.save(Organization.builder()
                .name("HTX Thanh Hóa " + suffix).code("HTX-B-" + suffix)
                .type(OrganizationType.COOPERATIVE).status(OrganizationStatus.ACTIVE)
                .address("Thành phố Thanh Hóa").province(tinhThanhHoa).build());

        orgNone = organizationRepository.save(Organization.builder()
                .name("HTX Chưa Map " + suffix).code("HTX-NONE-" + suffix)
                .type(OrganizationType.COOPERATIVE).status(OrganizationStatus.ACTIVE)
                .address("Không xác định").build());

        Role adminRole = role("VT-01", "Quản trị hệ thống");
        Role regulatorRole = role("VT-05", "Cán bộ quản lý ngành");

        adminUser = createUser("admin-" + suffix, "Trần Quản Trị");
        attachMembership(adminUser, orgNone, adminRole);

        vt05Assigned = createUser("vt05-assigned-" + suffix, "Nguyễn Được Gán");
        attachMembership(vt05Assigned, orgA1, regulatorRole);

        vt05Unassigned = createUser("vt05-empty-" + suffix, "Phạm Chưa Gán");
        attachMembership(vt05Unassigned, orgA2, regulatorRole);

        // TC-01: gán đúng 2 đơn vị — một cấp xã (Hoa Lư) và một cấp tỉnh (Thanh Hóa).
        userAreaAssignmentRepository.save(UserAreaAssignment.builder()
                .user(vt05Assigned).unit(xaHoaLau)
                .assignedBy(adminUser).assignedAt(LocalDateTime.now().minusMinutes(5)).build());
        userAreaAssignmentRepository.save(UserAreaAssignment.builder()
                .user(vt05Assigned).unit(tinhThanhHoa)
                .assignedBy(adminUser).assignedAt(LocalDateTime.now()).build());

        // Flush to make all seeded data visible to REQUIRES_NEW transactions
        // (e.g. ReportAccessLogServiceImpl.logAccess).
        entityManager.flush();
    }

    private void seedLotsAndShipments() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        catNameA1 = "Chè A1-" + suffix;
        catNameA2 = "Rau A2-" + suffix;
        catNameB = "Lúa B-" + suffix;

        ProductCategory catA1 = productCategoryRepository.save(ProductCategory.builder()
                .id(UUID.randomUUID()).name(catNameA1).isActive(true).build());
        ProductCategory catA2 = productCategoryRepository.save(ProductCategory.builder()
                .id(UUID.randomUUID()).name(catNameA2).isActive(true).build());
        ProductCategory catB = productCategoryRepository.save(ProductCategory.builder()
                .id(UUID.randomUUID()).name(catNameB).isActive(true).build());

        areaNameA1 = "Vùng A1-" + suffix;
        areaNameA2 = "Vùng A2-" + suffix;
        areaNameB = "Vùng B-" + suffix;

        FarmArea areaA1 = farmAreaRepository.save(FarmArea.builder()
                .id(UUID.randomUUID()).organization(orgA1).name(areaNameA1)
                .area(BigDecimal.TEN).areaUnit(AreaUnit.HA).cropType(catA1).build());
        FarmArea areaA2 = farmAreaRepository.save(FarmArea.builder()
                .id(UUID.randomUUID()).organization(orgA2).name(areaNameA2)
                .area(BigDecimal.TEN).areaUnit(AreaUnit.HA).cropType(catA2).build());
        FarmArea areaB = farmAreaRepository.save(FarmArea.builder()
                .id(UUID.randomUUID()).organization(orgB).name(areaNameB)
                .area(BigDecimal.TEN).areaUnit(AreaUnit.HA).cropType(catB).build());

        ProductionLot lotA1 = productionLotRepository.save(ProductionLot.builder()
                .organization(orgA1).productCategory(catA1).farmArea(areaA1)
                .name("Lô A1-" + suffix).expectedQuantity(1000.0).actualQuantity(900.0)
                .expectedQuantityUnit("kg")
                .plantingDate(LocalDate.of(2026, 3, 15))
                .harvestDate(LocalDate.of(2026, 6, 20))
                .status(ProductionLotStatus.CLOSED).build());
        ProductionLot lotA2 = productionLotRepository.save(ProductionLot.builder()
                .organization(orgA2).productCategory(catA2).farmArea(areaA2)
                .name("Lô A2-" + suffix).expectedQuantity(5000.0).actualQuantity(4900.0)
                .expectedQuantityUnit("kg")
                .plantingDate(LocalDate.of(2026, 4, 10))
                .harvestDate(LocalDate.of(2026, 7, 10))
                .status(ProductionLotStatus.CLOSED).build());
        ProductionLot lotB = productionLotRepository.save(ProductionLot.builder()
                .organization(orgB).productCategory(catB).farmArea(areaB)
                .name("Lô B-" + suffix).expectedQuantity(2000.0).actualQuantity(1800.0)
                .expectedQuantityUnit("kg")
                .plantingDate(LocalDate.of(2026, 7, 1))
                .harvestDate(LocalDate.of(2026, 10, 1))
                .status(ProductionLotStatus.CLOSED).build());

        shipment(orgA1, lotA1, shipQtyA1);
        shipment(orgA2, lotA2, 999);
        shipment(orgB, lotB, shipQtyB);
    }

    private void shipment(Organization organization, ProductionLot lot, long totalQuantity) {
        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setOrganization(organization);
        shipment.setProductionLot(lot);
        shipment.setName("Lô hàng " + lot.getName());
        shipment.setTotalQuantity(totalQuantity);
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipmentRepository.save(shipment);
    }

    private Role role(String code, String name) {
        return roleRepository.findByCode(code).orElseGet(() -> {
            Role entity = new Role();
            entity.setCode(code);
            entity.setName(name);
            return roleRepository.save(entity);
        });
    }

    private User createUser(String userName, String fullName) {
        return userRepository.save(User.builder()
                .userName(userName).passwordHash("{noop}mat-khau").fullName(fullName)
                .email(userName + "@test.local").status(UserStatus.ACTIVE).build());
    }

    private void attachMembership(User user, Organization organization, Role roleEntity) {
        OrganizationUser membership = new OrganizationUser();
        membership.setUser(user);
        membership.setOrganization(organization);
        membership.setRole(roleEntity);
        membership.setStatus(OrganizationUserStatus.ACTIVE);
        membership.setJoinedAt(LocalDateTime.now());
        organizationUserRepository.save(membership);
    }

    private CustomUserDetails loginAs(User user) {
        OrganizationUser membership =
                organizationUserRepository.findFirstByUser(user).orElseThrow();
        CustomUserDetails details =
                new CustomUserDetails(user, membership, membership.getRole());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        return details;
    }

    // ==================== TC-01 ====================

    @Test
    void tc01_vt05WithTwoAssignedUnits_seesOnlyMappedOrganizations() {
        CustomUserDetails principal = loginAs(vt05Assigned);

        List<UUID> assignedUnits = List.of(xaHoaLau.getId(), tinhThanhHoa.getId());
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2030, 12, 31);

        IndustryReportResponse summary =
                reportService.getIndustrySummary(null, assignedUnits, from, to);
        assertThat(summary.getTotalOrganizations()).isEqualTo(2); // A1 + B
        assertThat(summary.getTotalShipments()).isEqualTo(2);
        assertThat(summary.getTotalQuantity()).isEqualTo((double) (shipQtyA1 + shipQtyB));
        assertThat(summary.getProductBreakdown())
                .extracting(item -> item.getProductCategoryName())
                .contains(catNameA1, catNameB)
                .doesNotContain(catNameA2);
        assertThat(summary.getHasData()).isTrue();

        // Region string chỉ thu hẹp thêm (address LIKE ∩ phạm vi đã gán).
        IndustryReportResponse narrowed =
                reportService.getIndustrySummary("ninh bình", assignedUnits, from, to);
        assertThat(narrowed.getTotalOrganizations()).isEqualTo(1); // chỉ A1
        assertThat(narrowed.getTotalShipments()).isEqualTo(1);
        assertThat(narrowed.getTotalQuantity()).isEqualTo((double) shipQtyA1);

        // Phân tích diện tích canh tác cũng bị giới hạn tương tự.
        CropAreaAnalysisResponse analysis = cropAreaAnalysisService.getAnalysis(
                2026, null, null, null, assignedUnits, principal, "127.0.0.1");
        assertThat(analysis.getByArea())
                .extracting(CropAreaAnalysisResponse.AreaAnalysisStats::getFarmAreaName)
                .containsExactlyInAnyOrder(areaNameA1, areaNameB);
        assertThat(analysis.getSummary().getTotalLots()).isEqualTo(2L);
        assertThat(analysis.getMessage()).isNull();

        SeasonYieldComparisonResponse seasonYield = cropAreaAnalysisService.compareSeasonYield(
                List.of(2026), null, null, null, assignedUnits, principal, "127.0.0.1");
        int totalLotsInScope = seasonYield.getSeasons().stream()
                .mapToInt(item -> item.getLotCount().intValue()).sum();
        assertThat(totalLotsInScope).isEqualTo(2); // lô A2 ngoài phạm vi không bao giờ xuất hiện
    }

    @Test
    void tc01b_vt05CannotWidenScopeByRequestingUnassignedUnits() {
        CustomUserDetails principal = loginAs(vt05Assigned);

        // P1 (Ninh Bình) KHÔNG nằm trong tập đã gán [Hoa Lư, Thanh Hóa] =>
        // giao nhau rỗng => kết quả phải rỗng hoàn toàn, kể cả dữ liệu Hoa Lư.
        IndustryReportResponse response = reportService.getIndustrySummary(
                null, List.of(tinhNinhBinh.getId()),
                LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31));

        assertThat(response.getHasData()).isFalse();
        assertThat(response.getTotalOrganizations()).isZero();
        assertThat(response.getTotalShipments()).isZero();
        assertThat(response.getTotalQuantity()).isZero();
        assertThat(response.getProductBreakdown()).isEmpty();
    }

    // ==================== TC-02 (release blocker) ====================

    @Test
    void tc02_vt05WithoutAssignment_getsEmptyDataNeverFullData() {
        CustomUserDetails principal = loginAs(vt05Unassigned);
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2030, 12, 31);

        // 1. industry-summary
        IndustryReportResponse summary = reportService.getIndustrySummary(null, null, from, to);
        assertThat(summary.getMessage()).isEqualTo(UNASSIGNED_MESSAGE);
        assertThat(summary.getHasData()).isFalse();
        assertThat(summary.getTotalOrganizations()).isZero();
        assertThat(summary.getTotalShipments()).isZero();
        assertThat(summary.getTotalQuantity()).isZero();
        assertThat(summary.getProductBreakdown()).isEmpty();

        // Kể cả khi truyền region trùng địa chỉ tổ chức thật vẫn rỗng.
        IndustryReportResponse withRegion =
                reportService.getIndustrySummary("Gia Viễn", null, from, to);
        assertThat(withRegion.getMessage()).isEqualTo(UNASSIGNED_MESSAGE);
        assertThat(withRegion.getTotalOrganizations()).isZero();

        // 2. crop-area-analysis
        CropAreaAnalysisResponse analysis = cropAreaAnalysisService.getAnalysis(
                2026, null, null, null, null, principal, "127.0.0.1");
        assertThat(analysis.getMessage()).isEqualTo(UNASSIGNED_MESSAGE);
        assertThat(analysis.getSummary().getTotalLots()).isZero();
        assertThat(analysis.getSummary().getTotalExpectedYield()).isZero();
        assertThat(analysis.getSummary().getTotalActualYield()).isZero();
        assertThat(analysis.getSummary().getTotalArea()).isZero();
        assertThat(analysis.getByArea()).isEmpty();
        assertThat(analysis.getBySeason()).isEmpty();

        // 3. season-yield-comparison
        SeasonYieldComparisonResponse seasonYield = cropAreaAnalysisService.compareSeasonYield(
                List.of(2026), null, null, null, null, principal, "127.0.0.1");
        assertThat(seasonYield.getMessage()).isEqualTo(UNASSIGNED_MESSAGE);
        assertThat(seasonYield.getHasData()).isFalse();
        assertThat(seasonYield.getSeasons()).isEmpty();
        assertThat(seasonYield.getBaselineYear()).isNull();

        // 4. open-data export: dataset rỗng đúng cấu trúc, không lộ dữ liệu.
        byte[] jsonPayload = openDataExportService.exportOpenData(
                null, null, from, to, "JSON", principal, "127.0.0.1");
        assertThat(new String(jsonPayload, StandardCharsets.UTF_8)).isEqualTo("[]");

        byte[] csvPayload = openDataExportService.exportOpenData(
                null, null, from, to, "CSV", principal, "127.0.0.1");
        String csv = new String(csvPayload, StandardCharsets.UTF_8);
        assertThat(csv).contains("lotId,lotCode");
        assertThat(csv.lines().filter(line -> !line.isBlank())).hasSize(1); // BOM+header on one line

        byte[] xmlPayload = openDataExportService.exportOpenData(
                null, null, from, to, "XML", principal, "127.0.0.1");
        String xml = new String(xmlPayload, StandardCharsets.UTF_8);
        assertThat(xml).startsWith("<?xml");
        assertThat(xml).doesNotContain("<ProductionLot>");
    }

    // ==================== VT-01 không bị ràng buộc, unitIds lọc như bộ lọc thường ====================

    @Test
    void admin_seesEverything_withoutAssignments_andFiltersByUnitIds() {
        loginAs(adminUser);
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2030, 12, 31);

        // Giữ nguyên hành vi cũ: VT-01 không truyền unitIds thì bắt buộc có region.
        assertThatThrownBy(() -> reportService.getIndustrySummary(null, null, from, to))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Địa bàn không được để trống.");

        // VT-01 + unitIds => lọc theo mapping tổ chức, không phụ thuộc assignment.
        IndustryReportResponse filtered = reportService.getIndustrySummary(
                null,
                List.of(tinhNinhBinh.getId(), xaHoaLau.getId(), tinhThanhHoa.getId()),
                from, to);
        assertThat(filtered.getTotalOrganizations()).isEqualTo(3); // A1 + A2 + B, không gồm orgNone
        assertThat(filtered.getTotalShipments()).isEqualTo(3);

        // Thu hẹp xuống 1 tỉnh.
        IndustryReportResponse oneProvince = reportService.getIndustrySummary(
                null, List.of(tinhThanhHoa.getId()), from, to);
        assertThat(oneProvince.getTotalOrganizations()).isEqualTo(1);
        assertThat(oneProvince.getTotalQuantity()).isEqualTo((double) shipQtyB);
    }
}
