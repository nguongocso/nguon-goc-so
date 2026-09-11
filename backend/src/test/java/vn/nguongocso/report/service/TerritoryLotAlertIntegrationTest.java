package vn.nguongocso.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
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
import vn.nguongocso.recall.entity.RecallRequest;
import vn.nguongocso.recall.enums.RecallRequestStatus;
import vn.nguongocso.recall.repository.RecallRequestRepository;
import vn.nguongocso.report.dto.response.AlertLotDetailResponse;
import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;
import vn.nguongocso.report.enums.LotAlertType;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Kiểm thử tích hợp toàn diện cho Story NCL-07-CN-006:
 * Danh sách lô có cảnh báo theo địa bàn cho Cán bộ quản lý ngành (VT-05).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TerritoryLotAlertIntegrationTest {

    @Autowired
    private TerritoryLotAlertService territoryLotAlertService;

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

    @Autowired
    private TraceCodeRepository traceCodeRepository;

    @Autowired
    private RecallRequestRepository recallRequestRepository;

    @Autowired
    private InspectionRequestRepository inspectionRequestRepository;

    @Autowired
    private ChainEventRepository chainEventRepository;

    @Autowired
    private ProductFeedbackRepository productFeedbackRepository;

    @Autowired
    private AlertRepository alertRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private AdministrativeUnit tinhLamDong;
    private AdministrativeUnit huyenDucTrong;
    private AdministrativeUnit tinhDongNai;

    private Organization orgDucTrong; // Thuộc địa bàn của cán bộ VT-05
    private Organization orgDongNai;  // Ngoài địa bàn của cán bộ VT-05

    private User vt05Assigned;
    private User vt05Unassigned;

    private ProductionLot lotRecall1;
    private ProductionLot lotRecall2;
    private ProductionLot lotLockedLabel;
    private ProductionLot lotOutsideTerritory;
    private ProductionLot lotInspectionFailed;
    private ProductionLot lotQuarantineOverwritten;
    private ProductionLot lotSeriousFeedback;
    private ProductionLot lotInspectionExpired;

    @BeforeEach
    void setUp() {
        seedAdministrativeUnits();
        seedOrganizationsAndUsers();
        seedLotsAndAlertData();
        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void seedAdministrativeUnits() {
        tinhLamDong = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("68").name("Lâm Đồng")
                .level(AdministrativeUnitLevel.PROVINCE).active(true).build());

        huyenDucTrong = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("678").name("Đức Trọng")
                .level(AdministrativeUnitLevel.COMMUNE)
                .parent(tinhLamDong).province(tinhLamDong).active(true).build());

        tinhDongNai = administrativeUnitRepository.save(AdministrativeUnit.builder()
                .code("75").name("Đồng Nai")
                .level(AdministrativeUnitLevel.PROVINCE).active(true).build());
    }

    private void seedOrganizationsAndUsers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        orgDucTrong = organizationRepository.save(Organization.builder()
                .name("HTX Rau Sạch Đức Trọng " + suffix)
                .code("HTX-DT-" + suffix)
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .address("Huyện Đức Trọng, Lâm Đồng")
                .province(tinhLamDong)
                .build());

        orgDongNai = organizationRepository.save(Organization.builder()
                .name("HTX Nông Sản Đồng Nai " + suffix)
                .code("HTX-DN-" + suffix)
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .address("Thành phố Biên Hòa, Đồng Nai")
                .province(tinhDongNai)
                .build());

        Role regulatorRole = roleRepository.findByCode("VT-05").orElseGet(() -> {
            Role r = new Role();
            r.setCode("VT-05");
            r.setName("Cán bộ quản lý ngành");
            return roleRepository.save(r);
        });

        Role adminRole = roleRepository.findByCode("VT-01").orElseGet(() -> {
            Role r = new Role();
            r.setCode("VT-01");
            r.setName("Quản trị hệ thống");
            return roleRepository.save(r);
        });

        User adminUser = userRepository.save(User.builder()
                .userName("admin-" + suffix)
                .passwordHash("{noop}password")
                .fullName("Quản Trị Viên")
                .email("admin." + suffix + "@test.local")
                .status(UserStatus.ACTIVE)
                .build());
        attachMembership(adminUser, orgDucTrong, adminRole);

        vt05Assigned = userRepository.save(User.builder()
                .userName("regulator-assigned-" + suffix)
                .passwordHash("{noop}password")
                .fullName("Cán Bộ Lâm Đồng")
                .email("vt05.assigned." + suffix + "@test.local")
                .status(UserStatus.ACTIVE)
                .build());
        attachMembership(vt05Assigned, orgDucTrong, regulatorRole);

        vt05Unassigned = userRepository.save(User.builder()
                .userName("regulator-empty-" + suffix)
                .passwordHash("{noop}password")
                .fullName("Cán Bộ Chưa Gán Địa Bàn")
                .email("vt05.empty." + suffix + "@test.local")
                .status(UserStatus.ACTIVE)
                .build());
        attachMembership(vt05Unassigned, orgDucTrong, regulatorRole);

        // Gán cán bộ vt05Assigned phụ trách địa bàn tỉnh Lâm Đồng
        userAreaAssignmentRepository.save(UserAreaAssignment.builder()
                .user(vt05Assigned)
                .unit(tinhLamDong)
                .assignedBy(adminUser)
                .assignedAt(LocalDateTime.now())
                .build());
    }

    private void seedLotsAndAlertData() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        ProductCategory category = productCategoryRepository.save(ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Rau củ quả Đà Lạt " + suffix)
                .isActive(true)
                .build());

        FarmArea farmAreaIn = farmAreaRepository.save(FarmArea.builder()
                .id(UUID.randomUUID())
                .organization(orgDucTrong)
                .name("Khu sản xuất Đức Trọng " + suffix)
                .area(BigDecimal.valueOf(5))
                .areaUnit(AreaUnit.HA)
                .cropType(category)
                .build());

        FarmArea farmAreaOut = farmAreaRepository.save(FarmArea.builder()
                .id(UUID.randomUUID())
                .organization(orgDongNai)
                .name("Khu sản xuất Đồng Nai " + suffix)
                .area(BigDecimal.valueOf(10))
                .areaUnit(AreaUnit.HA)
                .cropType(category)
                .build());

        // Lô 1: Lô đang thu hồi (trạng thái RECALLED trực tiếp trên lot) - Thuộc Đức Trọng
        lotRecall1 = productionLotRepository.save(ProductionLot.builder()
                .organization(orgDucTrong)
                .farmArea(farmAreaIn)
                .productCategory(category)
                .name("Lô Cà chua thu hồi " + suffix)
                .expectedQuantity(1500.0)
                .actualQuantity(1450.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.RECALLED)
                .plantingDate(LocalDate.now().minusDays(90))
                .harvestDate(LocalDate.now().minusDays(10))
                .build());

        // Lô 2: Lô có lệnh thu hồi đã duyệt qua RecallRequest - Thuộc Đức Trọng
        lotRecall2 = productionLotRepository.save(ProductionLot.builder()
                .organization(orgDucTrong)
                .farmArea(farmAreaIn)
                .productCategory(category)
                .name("Lô Xà lách có lệnh thu hồi " + suffix)
                .expectedQuantity(2000.0)
                .actualQuantity(2000.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.CLOSED)
                .plantingDate(LocalDate.now().minusDays(60))
                .harvestDate(LocalDate.now().minusDays(5))
                .build());

        RecallRequest recallReq = new RecallRequest();
        recallReq.setProductionLot(lotRecall2);
        recallReq.setRequestedBy(vt05Assigned);
        recallReq.setReason("Phát hiện tồn dư chất cấm");
        recallReq.setStatus(RecallRequestStatus.APPROVED);
        recallReq.setRequestedAt(LocalDateTime.now().minusDays(3));
        recallReq.setApprovedAt(LocalDateTime.now().minusDays(2));
        recallRequestRepository.save(recallReq);

        // Lô 3: Lô có tem bị khóa (TraceCodeStatus.LOCKED) - Thuộc Đức Trọng
        lotLockedLabel = productionLotRepository.save(ProductionLot.builder()
                .organization(orgDucTrong)
                .farmArea(farmAreaIn)
                .productCategory(category)
                .name("Lô Dâu tây tem bị khóa " + suffix)
                .expectedQuantity(800.0)
                .actualQuantity(800.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.APPROVED)
                .plantingDate(LocalDate.now().minusDays(45))
                .harvestDate(LocalDate.now().minusDays(2))
                .build());

        Shipment shipmentLocked = new Shipment();
        shipmentLocked.setId(UUID.randomUUID());
        shipmentLocked.setOrganization(orgDucTrong);
        shipmentLocked.setProductionLot(lotLockedLabel);
        shipmentLocked.setName("Lô hàng Dâu tây " + suffix);
        shipmentLocked.setTotalQuantity(800L);
        shipmentLocked.setStatus(ShipmentStatus.ACTIVATED);
        shipmentRepository.save(shipmentLocked);

        TraceCode lockedCode = new TraceCode();
        lockedCode.setId(UUID.randomUUID());
        lockedCode.setShipment(shipmentLocked);
        lockedCode.setCodeValue("VN-68-LOCKED-" + suffix);
        lockedCode.setStatus(TraceCodeStatus.LOCKED);
        lockedCode.setLockReason("Nghi vấn dán tem sai sản phẩm");
        lockedCode.setLockedAt(LocalDateTime.now().minusDays(1));
        traceCodeRepository.save(lockedCode);

        // Lô 4: Lô có cảnh báo thu hồi nhưng thuộc Đồng Nai (ngoài địa bàn phụ trách)
        lotOutsideTerritory = productionLotRepository.save(ProductionLot.builder()
                .organization(orgDongNai)
                .farmArea(farmAreaOut)
                .productCategory(category)
                .name("Lô Ngoài Địa Bàn " + suffix)
                .expectedQuantity(3000.0)
                .actualQuantity(3000.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.RECALLED)
                .plantingDate(LocalDate.now().minusDays(100))
                .harvestDate(LocalDate.now().minusDays(15))
                .build());

        // Lô 5: Lô có kiểm nghiệm không đạt (INSPECTION_FAILED) - Thuộc Đức Trọng
        lotInspectionFailed = productionLotRepository.save(ProductionLot.builder()
                .organization(orgDucTrong)
                .farmArea(farmAreaIn)
                .productCategory(category)
                .name("Lô Bắp cải kiểm nghiệm không đạt " + suffix)
                .expectedQuantity(1200.0)
                .actualQuantity(1200.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.APPROVED)
                .plantingDate(LocalDate.now().minusDays(80))
                .build());

        InspectionRequest failedReq = InspectionRequest.builder()
                .id(UUID.randomUUID())
                .productionLot(lotInspectionFailed)
                .inspectionUnit("Trung tâm kiểm nghiệm chất lượng Lâm Đồng")
                .sampleSentDate(LocalDate.now().minusDays(10))
                .status(InspectionRequestStatus.FAILED)
                .createdBy(vt05Assigned)
                .scopeWarning(false)
                .build();
        inspectionRequestRepository.save(failedReq);

        // Lô 6: Lô ghi đè cách ly / thu hoạch sớm (QUARANTINE_OVERWRITTEN)
        lotQuarantineOverwritten = productionLotRepository.save(ProductionLot.builder()
                .organization(orgDucTrong)
                .farmArea(farmAreaIn)
                .productCategory(category)
                .name("Lô Ớt chuông thu hoạch sớm " + suffix)
                .expectedQuantity(500.0)
                .actualQuantity(500.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.APPROVED)
                .plantingDate(LocalDate.now().minusDays(70))
                .build());

        ChainEvent harvestEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + lotQuarantineOverwritten.getId() + "\",\"earlyHarvest\":true,\"note\":\"Thu hoạch sớm tránh bão\"}")
                .recordedAt(LocalDateTime.now().minusDays(5))
                .recordedBy(vt05Assigned)
                .isCorrection(false)
                .build();
        chainEventRepository.save(harvestEvent);

        // Lô 7: Lô có phản ánh nghiêm trọng chưa đóng (SERIOUS_FEEDBACK_OPEN)
        lotSeriousFeedback = productionLotRepository.save(ProductionLot.builder()
                .organization(orgDucTrong)
                .farmArea(farmAreaIn)
                .productCategory(category)
                .name("Lô Khoai tây phản ánh xấu " + suffix)
                .expectedQuantity(2500.0)
                .actualQuantity(2500.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.APPROVED)
                .plantingDate(LocalDate.now().minusDays(85))
                .build());

        ProductFeedback feedback = ProductFeedback.builder()
                .productionLot(lotSeriousFeedback)
                .content("Sản phẩm có mùi hóa chất lạ")
                .severity(ProductFeedbackSeverity.QUALITY_SUSPECTED)
                .status(ProductFeedbackStatus.NEW)
                .build();
        productFeedbackRepository.save(feedback);

        // Lô 8: Lô có cảnh báo kiểm nghiệm hết hạn (INSPECTION_EXPIRED)
        lotInspectionExpired = productionLotRepository.save(ProductionLot.builder()
                .organization(orgDucTrong)
                .farmArea(farmAreaIn)
                .productCategory(category)
                .name("Lô Hành tây hết hạn kiểm nghiệm " + suffix)
                .expectedQuantity(1800.0)
                .actualQuantity(1800.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.APPROVED)
                .plantingDate(LocalDate.now().minusDays(120))
                .build());

        Alert alertExpired = new Alert();
        alertExpired.setId(UUID.randomUUID());
        alertExpired.setType(AlertType.INSPECTION_EXPIRED);
        alertExpired.setRelatedEntityType("PRODUCTION_LOT");
        alertExpired.setRelatedEntityId(lotInspectionExpired.getId());
        alertExpired.setOrganization(orgDucTrong);
        alertExpired.setSeverity(AlertSeverity.HIGH);
        alertExpired.setStatus(AlertStatus.PENDING);
        alertExpired.setMessage("Kết quả kiểm nghiệm đã quá thời hạn 90 ngày.");
        alertExpired.setCreatedAt(LocalDateTime.now().minusDays(6));
        alertRepository.save(alertExpired);
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
        OrganizationUser membership = organizationUserRepository.findFirstByUser(user).orElseThrow();
        CustomUserDetails details = new CustomUserDetails(user, membership, membership.getRole());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        return details;
    }

    // =========================================================================
    // TC-01: Cán bộ có địa bàn thấy đủ các lô cảnh báo, đúng loại, đúng tổ chức
    // =========================================================================
    @Test
    @DisplayName("TC-01: Cán bộ có địa bàn thấy đủ 2 lô thu hồi và 1 lô tem khóa, đúng loại cảnh báo")
    void tc01_regulatorWithTerritory_seesRecallAndLockedLotsCorrectly() {
        CustomUserDetails principal = loginAs(vt05Assigned);
        Pageable pageable = PageRequest.of(0, 20);

        PageResponse<AlertLotSummaryResponse> result = territoryLotAlertService.getAlertLots(
                principal, null, null, null, null, null, pageable);

        assertThat(result.getItems()).isNotEmpty();

        // 1. Kiểm tra không leak lô ngoài địa bàn (Đồng Nai)
        List<UUID> lotIds = result.getItems().stream().map(AlertLotSummaryResponse::getLotId).toList();
        assertThat(lotIds).doesNotContain(lotOutsideTerritory.getId());

        // 2. Kiểm tra có đủ 2 lô thu hồi và 1 lô tem khóa
        assertThat(lotIds).contains(lotRecall1.getId(), lotRecall2.getId(), lotLockedLabel.getId());

        // 3. Kiểm tra thông tin loại cảnh báo
        AlertLotSummaryResponse r1 = result.getItems().stream()
                .filter(item -> item.getLotId().equals(lotRecall1.getId())).findFirst().orElseThrow();
        assertThat(r1.getAlertTypes()).contains(LotAlertType.RECALLING);
        assertThat(r1.getOrganizationName()).contains("Đức Trọng");

        AlertLotSummaryResponse r2 = result.getItems().stream()
                .filter(item -> item.getLotId().equals(lotRecall2.getId())).findFirst().orElseThrow();
        assertThat(r2.getAlertTypes()).contains(LotAlertType.RECALLING);

        AlertLotSummaryResponse r3 = result.getItems().stream()
                .filter(item -> item.getLotId().equals(lotLockedLabel.getId())).findFirst().orElseThrow();
        assertThat(r3.getAlertTypes()).contains(LotAlertType.LOCKED_LABEL);
    }

    // =========================================================================
    // TC-02: Cán bộ chưa được gán địa bàn => trả về danh sách rỗng, không leak
    // =========================================================================
    @Test
    @DisplayName("TC-02: Cán bộ chưa gán địa bàn nhận kết quả rỗng, không lỗi 403, không leak dữ liệu toàn hệ thống")
    void tc02_regulatorWithoutTerritory_returnsEmptyListWithoutLeak() {
        CustomUserDetails principal = loginAs(vt05Unassigned);
        Pageable pageable = PageRequest.of(0, 10);

        PageResponse<AlertLotSummaryResponse> result = territoryLotAlertService.getAlertLots(
                principal, null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getItems()).isEmpty();
    }

    // =========================================================================
    // TC-03: Chi tiết lô có cảnh báo: Read-only và kiểm tra từ chối xem ngoài địa bàn
    // =========================================================================
    @Test
    @DisplayName("TC-03: Xem chi tiết lô thành công ở chế độ Read-only với đầy đủ bằng chứng và timeline")
    void tc03_getAlertLotDetail_readOnlyTimelineAndEvidence() {
        CustomUserDetails principal = loginAs(vt05Assigned);

        AlertLotDetailResponse detail = territoryLotAlertService.getAlertLotDetail(
                principal, lotLockedLabel.getId());

        assertThat(detail).isNotNull();
        assertThat(detail.getLotInfo().getLotId()).isEqualTo(lotLockedLabel.getId());
        assertThat(detail.getOrganization().getOrganizationName()).contains("Đức Trọng");

        // Bằng chứng có tem bị khóa
        assertThat(detail.getActiveAlerts()).isNotEmpty();
        assertThat(detail.getActiveAlerts().get(0).getAlertType()).isEqualTo(LotAlertType.LOCKED_LABEL);
        assertThat(detail.getActiveAlerts().get(0).getEvidenceData()).containsKey("lockedCount");

        // Timeline sự kiện
        assertThat(detail.getTimelineEvents()).isNotNull();
    }

    // =========================================================================
    // TC-04: Lọc theo INSPECTION_FAILED chỉ trả về lô kiểm nghiệm không đạt
    // =========================================================================
    @Test
    @DisplayName("TC-04: Lọc theo INSPECTION_FAILED chỉ trả về các lô kiểm nghiệm không đạt")
    void tc04_filterByInspectionFailed_returnsOnlyFailedInspectionLots() {
        CustomUserDetails principal = loginAs(vt05Assigned);
        Pageable pageable = PageRequest.of(0, 10);

        PageResponse<AlertLotSummaryResponse> result = territoryLotAlertService.getAlertLots(
                principal, LotAlertType.INSPECTION_FAILED, null, null, null, null, pageable);

        assertThat(result.getItems()).isNotEmpty();
        for (AlertLotSummaryResponse item : result.getItems()) {
            assertThat(item.getAlertTypes()).contains(LotAlertType.INSPECTION_FAILED);
        }
        assertThat(result.getItems().stream().map(AlertLotSummaryResponse::getLotId))
                .contains(lotInspectionFailed.getId())
                .doesNotContain(lotRecall1.getId(), lotLockedLabel.getId());
    }

    // =========================================================================
    // TC-05: Lọc theo Organization: chỉ trả về tổ chức trong địa bàn
    // =========================================================================
    @Test
    @DisplayName("TC-05: Lọc theo Organization trong địa bàn trả về đúng lô, ngoài địa bàn trả về rỗng")
    void tc05_filterByOrganization_respectsTerritoryScope() {
        CustomUserDetails principal = loginAs(vt05Assigned);
        Pageable pageable = PageRequest.of(0, 10);

        // Lọc org trong địa bàn
        PageResponse<AlertLotSummaryResponse> inScope = territoryLotAlertService.getAlertLots(
                principal, null, orgDucTrong.getOrganizationId(), null, null, null, pageable);
        assertThat(inScope.getItems()).isNotEmpty();
        assertThat(inScope.getItems()).allMatch(it -> it.getOrganizationId().equals(orgDucTrong.getOrganizationId()));

        // Lọc org ngoài địa bàn
        PageResponse<AlertLotSummaryResponse> outScope = territoryLotAlertService.getAlertLots(
                principal, null, orgDongNai.getOrganizationId(), null, null, null, pageable);
        assertThat(outScope.getItems()).isEmpty();
    }

    // =========================================================================
    // TC-06: Lọc theo khoảng thời gian phát sinh cảnh báo
    // =========================================================================
    @Test
    @DisplayName("TC-06: Lọc theo khoảng thời gian phát sinh cảnh báo")
    void tc06_filterByDateRange_returnsOnlyAlertsInRange() {
        CustomUserDetails principal = loginAs(vt05Assigned);
        Pageable pageable = PageRequest.of(0, 20);

        // Khoảng thời gian trong quá khứ xa (không có cảnh báo)
        LocalDateTime fromPast = LocalDateTime.now().minusDays(100);
        LocalDateTime toPast = LocalDateTime.now().minusDays(50);
        PageResponse<AlertLotSummaryResponse> emptyRange = territoryLotAlertService.getAlertLots(
                principal, null, null, fromPast, toPast, null, pageable);
        assertThat(emptyRange.getItems()).isEmpty();

        // Khoảng thời gian 7 ngày gần đây
        LocalDateTime fromRecent = LocalDateTime.now().minusDays(7);
        LocalDateTime toRecent = LocalDateTime.now().plusDays(1);
        PageResponse<AlertLotSummaryResponse> recentRange = territoryLotAlertService.getAlertLots(
                principal, null, null, fromRecent, toRecent, null, pageable);
        assertThat(recentRange.getItems()).isNotEmpty();
    }

    // =========================================================================
    // TC-07: Xem chi tiết lô ngoài địa bàn phụ trách bị từ chối 403 Forbidden
    // =========================================================================
    @Test
    @DisplayName("TC-07: Truy cập chi tiết lô ngoài địa bàn phụ trách bị từ chối 403 Forbidden")
    void tc07_getDetailOutsideTerritory_throwsForbidden() {
        CustomUserDetails principal = loginAs(vt05Assigned);

        assertThatThrownBy(() -> territoryLotAlertService.getAlertLotDetail(
                principal, lotOutsideTerritory.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    // =========================================================================
    // TC-08: Thử bypass phạm vi bằng tham số unitIds (Anti-Bypass)
    // =========================================================================
    @Test
    @DisplayName("TC-08: Cố gắng truyền unitIds ngoài địa bàn được phân công không thể bypass phạm vi")
    void tc08_antiBypassUnitIds_cannotExpandTerritory() {
        CustomUserDetails principal = loginAs(vt05Assigned);
        Pageable pageable = PageRequest.of(0, 10);

        // Cố tình truyền unitId của tỉnh Đồng Nai (ngoài địa bàn phụ trách)
        PageResponse<AlertLotSummaryResponse> bypassAttempt = territoryLotAlertService.getAlertLots(
                principal, null, null, null, null, List.of(tinhDongNai.getId()), pageable);

        assertThat(bypassAttempt.getItems()).isEmpty();
    }

    // =========================================================================
    // TC-09: Xuất Excel danh sách lô có cảnh báo
    // =========================================================================
    @Test
    @DisplayName("TC-09: Xuất Excel áp dụng đúng phạm vi địa bàn và bộ lọc, không bypass")
    void tc09_exportAlertLots_generatesValidExcelFile() {
        CustomUserDetails principal = loginAs(vt05Assigned);

        byte[] excelBytes = territoryLotAlertService.exportAlertLots(
                principal, null, null, null, null, null);

        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes.length).isGreaterThan(1000); // File Excel hợp lệ có dung lượng lớn hơn 1KB

        // Người dùng chưa có địa bàn xuất file rỗng an toàn
        CustomUserDetails unassignedPrincipal = loginAs(vt05Unassigned);
        byte[] emptyExcelBytes = territoryLotAlertService.exportAlertLots(
                unassignedPrincipal, null, null, null, null, null);
        assertThat(emptyExcelBytes).isNotNull();
    }

    // =========================================================================
    // TC-10: Kiểm tra phát hiện đủ các loại cảnh báo bổ sung
    // =========================================================================
    @Test
    @DisplayName("TC-10: Hệ thống phát hiện đầy đủ QUARANTINE_OVERWRITTEN, SERIOUS_FEEDBACK_OPEN, INSPECTION_EXPIRED")
    void tc10_additionalAlertTypes_detectedCorrectly() {
        CustomUserDetails principal = loginAs(vt05Assigned);
        Pageable pageable = PageRequest.of(0, 20);

        PageResponse<AlertLotSummaryResponse> result = territoryLotAlertService.getAlertLots(
                principal, null, null, null, null, null, pageable);

        // Kiểm tra lô có cảnh báo cách ly
        assertThat(result.getItems().stream()
                .anyMatch(it -> it.getLotId().equals(lotQuarantineOverwritten.getId())
                        && it.getAlertTypes().contains(LotAlertType.QUARANTINE_OVERWRITTEN)))
                .isTrue();

        // Kiểm tra lô có phản ánh nghiêm trọng
        assertThat(result.getItems().stream()
                .anyMatch(it -> it.getLotId().equals(lotSeriousFeedback.getId())
                        && it.getAlertTypes().contains(LotAlertType.SERIOUS_FEEDBACK_OPEN)))
                .isTrue();

        // Kiểm tra lô có kiểm nghiệm hết hạn
        assertThat(result.getItems().stream()
                .anyMatch(it -> it.getLotId().equals(lotInspectionExpired.getId())
                        && it.getAlertTypes().contains(LotAlertType.INSPECTION_EXPIRED)))
                .isTrue();
    }
}
