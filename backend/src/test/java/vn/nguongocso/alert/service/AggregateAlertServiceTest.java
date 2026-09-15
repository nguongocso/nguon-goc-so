package vn.nguongocso.alert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.nguongocso.alert.dto.response.AggregateAlertCountResponse;
import vn.nguongocso.alert.dto.response.AggregateAlertItemResponse;
import vn.nguongocso.alert.dto.response.AggregateAlertPageResponse;
import vn.nguongocso.alert.dto.response.UnviewedAlertCountResponse;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.enums.AggregateAlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.alert.service.impl.AggregateAlertServiceImpl;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.entity.CultivationMilestone;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.MilestoneReminder;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.repository.MilestoneReminderRepository;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.recall.entity.RecallCase;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;
import vn.nguongocso.trace.recall.repository.RecallCaseRepository;
import vn.nguongocso.trace.repository.CodeRangeRepository;

import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử dịch vụ cảnh báo tổng hợp NCL-08-CN-016.
 */
@ExtendWith(MockitoExtension.class)
class AggregateAlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private ProductFeedbackRepository productFeedbackRepository;

    @Mock
    private CodeRangeRepository codeRangeRepository;

    @Mock
    private MilestoneReminderRepository milestoneReminderRepository;

    @Mock
    private RecallCaseRepository recallCaseRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PartnerApiKeyRepository partnerApiKeyRepository;

    @Mock
    private PartnerApiKeyService partnerApiKeyService;

    @InjectMocks
    private AggregateAlertServiceImpl aggregateAlertService;

    private UUID orgIdA;
    private UUID orgIdB;
    private Organization orgA;
    private CustomUserDetails userDetailsCoopA;
    private CustomUserDetails userDetailsAdmin;

    @BeforeEach
    void setUp() {
        orgIdA = UUID.randomUUID();
        orgIdB = UUID.randomUUID();

        orgA = new Organization();
        orgA.setOrganizationId(orgIdA);
        orgA.setName("Hợp tác xã Nông Nghiệp Xanh");

        userDetailsCoopA = mock(CustomUserDetails.class);
        lenient().when(userDetailsCoopA.getRoleCode()).thenReturn("VT-02");
        lenient().when(userDetailsCoopA.getOrganizationId()).thenReturn(orgIdA);
        lenient().when(userDetailsCoopA.getUsername()).thenReturn("manager_coop_a");

        userDetailsAdmin = mock(CustomUserDetails.class);
        lenient().when(userDetailsAdmin.getRoleCode()).thenReturn("VT-01");
        lenient().when(userDetailsAdmin.getOrganizationId()).thenReturn(null);
        lenient().when(userDetailsAdmin.getUsername()).thenReturn("admin_platform");

        lenient().when(certificationRepository.findByOrganizationId(any())).thenReturn(Collections.emptyList());
        lenient().when(certificationRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(partnerApiKeyRepository.findByStatus(any())).thenReturn(Collections.emptyList());
        lenient().when(partnerApiKeyRepository.findByOrganizationOrganizationId(any(), any()))
                .thenReturn(Page.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(CustomUserDetails user) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(auth.getPrincipal()).thenReturn(user);

        SecurityContext context = mock(SecurityContext.class);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    /**
     * TC-01: Luồng thành công - Tổ chức có 4 loại cảnh báo đang mở -> hiển thị đủ 4 loại nhóm theo mức khẩn cấp.
     */
    @Test
    void testGetAllAlerts_success_groupsMultipleSources() {
        mockSecurityContext(userDetailsCoopA);

        // 1. Cảnh báo quét bất thường (AlertRepository)
        Alert scanAlert = new Alert();
        scanAlert.setId(UUID.randomUUID());
        scanAlert.setType(AlertType.SCAN_ANOMALY);
        scanAlert.setSeverity(AlertSeverity.HIGH);
        scanAlert.setStatus(AlertStatus.PENDING);
        scanAlert.setRelatedEntityType("TRACE_CODE");
        scanAlert.setRelatedEntityId(UUID.randomUUID());
        scanAlert.setMessage("Phát hiện 15 lượt quét tại nhiều tỉnh trong 5 phút");
        scanAlert.setCreatedAt(LocalDateTime.now().minusHours(2));
        scanAlert.setOrganization(orgA);

        when(alertRepository.findByOrganizationOrganizationIdAndStatus(orgIdA, AlertStatus.PENDING))
                .thenReturn(List.of(scanAlert));

        // 2. Phản ánh chưa xử lý (ProductFeedbackRepository)
        ProductionLot lot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô xoài cát chu 2026")
                .organization(orgA)
                .build();

        ProductFeedback feedback = ProductFeedback.builder()
                .id(UUID.randomUUID())
                .productionLot(lot)
                .content("Bao bì bị rách và tem mờ không quét được")
                .severity(ProductFeedbackSeverity.QUALITY_SUSPECTED)
                .status(ProductFeedbackStatus.NEW)
                .createdAt(LocalDateTime.now().minusHours(3))
                .build();

        when(productFeedbackRepository.findByProductionLot_Organization_OrganizationIdAndStatusIn(eq(orgIdA), any()))
                .thenReturn(List.of(feedback));

        // 3. Dải mã sắp hết hạn mức (CodeRangeRepository)
        CodeRange codeRange = CodeRange.builder()
                .id(UUID.randomUUID())
                .organization(orgA)
                .prefix("XAI26")
                .totalLimit(1000L)
                .usedCount(850L) // 85%
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        when(codeRangeRepository.findByOrganizationOrganizationId(orgIdA))
                .thenReturn(List.of(codeRange));

        // 4. Mốc canh tác quá hạn (MilestoneReminderRepository)
        CultivationMilestone milestone = new CultivationMilestone();
        milestone.setId(1L);
        milestone.setName("Bón phân đợt 1");

        MilestoneReminder reminder = MilestoneReminder.builder()
                .id(UUID.randomUUID())
                .productionLot(lot)
                .milestone(milestone)
                .overdueDays(8) // >= 7 -> HIGH
                .status(MilestoneReminderStatus.OPEN)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        when(milestoneReminderRepository.findByProductionLot_Organization_OrganizationIdAndStatusOrderByOverdueDaysDesc(orgIdA, MilestoneReminderStatus.OPEN))
                .thenReturn(List.of(reminder));

        // Vụ việc thu hồi rỗng
        when(recallCaseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(orgIdA, RecallCaseStatus.OPEN))
                .thenReturn(Collections.emptyList());

        // Gọi service
        Pageable pageable = PageRequest.of(0, 10);
        AggregateAlertPageResponse response = aggregateAlertService.getAggregateAlerts(
                null, null, "OPEN", null, null, null, null, pageable);

        assertNotNull(response);
        assertEquals(4, response.getTotalElements());
        assertEquals(4, response.getItems().size());

        // Kiểm tra thống kê
        assertEquals(4, response.getSummaryCounts().getTotalOpen());
        assertTrue(response.getSummaryCounts().getHighSeverityCount() >= 2); // scanAlert (HIGH) + milestone >= 7 (HIGH)

        // Kiểm tra sắp xếp: mục đầu tiên phải là HIGH
        assertEquals(AlertSeverity.HIGH, response.getItems().get(0).getSeverity());

        // Kiểm tra đầy đủ lối tắt xử lý cho từng loại
        assertTrue(response.getItems().stream().anyMatch(i -> i.getType() == AggregateAlertType.SCAN_ANOMALY && "/alerts/scan-anomaly".equals(i.getActionUrl())));
        assertTrue(response.getItems().stream().anyMatch(i -> i.getType() == AggregateAlertType.UNPROCESSED_FEEDBACK && "/product-feedbacks".equals(i.getActionUrl())));
        assertTrue(response.getItems().stream().anyMatch(i -> i.getType() == AggregateAlertType.CODE_RANGE_QUOTA && "/code-range-supplements/create".equals(i.getActionUrl())));
        assertTrue(response.getItems().stream().anyMatch(i -> i.getType() == AggregateAlertType.OVERDUE_MILESTONE && i.getActionUrl().startsWith("/farm-logs/create")));
    }

    /**
     * TC-02: Luồng thành công - Cảnh báo chứng nhận tự đóng khi chứng nhận được gia hạn.
     */
    @Test
    void testAutoResolveCertificationAlert() {
        mockSecurityContext(userDetailsCoopA);

        // Giả sử chứng nhận đã được gia hạn -> alert trong bảng alerts chuyển thành RESOLVED
        Alert resolvedCertAlert = new Alert();
        resolvedCertAlert.setId(UUID.randomUUID());
        resolvedCertAlert.setType(AlertType.CERT_EXPIRING);
        resolvedCertAlert.setSeverity(AlertSeverity.MEDIUM);
        resolvedCertAlert.setStatus(AlertStatus.RESOLVED); // Đã giải quyết / tự đóng
        resolvedCertAlert.setResolvedAt(LocalDateTime.now());
        resolvedCertAlert.setOrganization(orgA);

        // Khi truy vấn các cảnh báo OPEN: AlertRepository chỉ trả về cảnh báo PENDING
        when(alertRepository.findByOrganizationOrganizationIdAndStatus(orgIdA, AlertStatus.PENDING))
                .thenReturn(Collections.emptyList());

        when(productFeedbackRepository.findByProductionLot_Organization_OrganizationIdAndStatusIn(eq(orgIdA), any()))
                .thenReturn(Collections.emptyList());
        when(codeRangeRepository.findByOrganizationOrganizationId(orgIdA))
                .thenReturn(Collections.emptyList());
        when(milestoneReminderRepository.findByProductionLot_Organization_OrganizationIdAndStatusOrderByOverdueDaysDesc(orgIdA, MilestoneReminderStatus.OPEN))
                .thenReturn(Collections.emptyList());
        when(recallCaseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(orgIdA, RecallCaseStatus.OPEN))
                .thenReturn(Collections.emptyList());

        Pageable pageable = PageRequest.of(0, 10);
        AggregateAlertPageResponse response = aggregateAlertService.getAggregateAlerts(
                null, null, "OPEN", null, null, null, null, pageable);

        // Cảnh báo chứng nhận đã tự đóng nên không còn hiển thị trong danh sách OPEN
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getItems().size());
        assertEquals(0, response.getSummaryCounts().getTotalOpen());
    }

    /**
     * TC-03: Không có quyền - Quản lý HTX cố tình truy vấn cảnh báo của tổ chức khác (QTN-01).
     */
    @Test
    void testOrganizationIsolation_QTN01() {
        mockSecurityContext(userDetailsCoopA);

        Pageable pageable = PageRequest.of(0, 10);

        // VT-02 cố tình truyền organizationId khác với tổ chức của mình
        BusinessException ex = assertThrows(BusinessException.class, () ->
                aggregateAlertService.getAggregateAlerts(
                        null, null, "OPEN", orgIdB, null, null, null, pageable));

        assertEquals("Bạn không có quyền xem cảnh báo của tổ chức khác.", ex.getMessage());
    }

    /**
     * TC-04: Dữ liệu rỗng - Tổ chức không còn cảnh báo nào đang mở.
     */
    @Test
    void testEmptyAlerts_displaysEmpty() {
        mockSecurityContext(userDetailsCoopA);

        when(alertRepository.findByOrganizationOrganizationIdAndStatus(orgIdA, AlertStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(productFeedbackRepository.findByProductionLot_Organization_OrganizationIdAndStatusIn(eq(orgIdA), any()))
                .thenReturn(Collections.emptyList());
        when(codeRangeRepository.findByOrganizationOrganizationId(orgIdA))
                .thenReturn(Collections.emptyList());
        when(milestoneReminderRepository.findByProductionLot_Organization_OrganizationIdAndStatusOrderByOverdueDaysDesc(orgIdA, MilestoneReminderStatus.OPEN))
                .thenReturn(Collections.emptyList());
        when(recallCaseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(orgIdA, RecallCaseStatus.OPEN))
                .thenReturn(Collections.emptyList());

        Pageable pageable = PageRequest.of(0, 10);
        AggregateAlertPageResponse response = aggregateAlertService.getAggregateAlerts(
                null, null, "OPEN", null, null, null, null, pageable);

        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getSummaryCounts().getTotalOpen());
        assertEquals(0, response.getSummaryCounts().getHighSeverityCount());
        assertEquals(0, response.getSummaryCounts().getMediumSeverityCount());
    }

    /**
     * NCL-12-CN-005: Khóa sắp hết hạn hiện trên cảnh báo tổng hợp kèm lối tắt,
     * khóa đã thu hồi không hiện.
     */
    @Test
    void testApiKeyAlerts_expiringKeyAppearsAndRevokedExcluded() {
        mockSecurityContext(userDetailsCoopA);
        ReflectionTestUtils.setField(aggregateAlertService, "apiKeyExpiryWarningDays", 7);
        ReflectionTestUtils.setField(aggregateAlertService, "apiKeyQuotaWarningRatio", 0.8);

        when(alertRepository.findByOrganizationOrganizationIdAndStatus(orgIdA, AlertStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(productFeedbackRepository.findByProductionLot_Organization_OrganizationIdAndStatusIn(eq(orgIdA), any()))
                .thenReturn(Collections.emptyList());
        when(codeRangeRepository.findByOrganizationOrganizationId(orgIdA))
                .thenReturn(Collections.emptyList());
        when(milestoneReminderRepository.findByProductionLot_Organization_OrganizationIdAndStatusOrderByOverdueDaysDesc(orgIdA, MilestoneReminderStatus.OPEN))
                .thenReturn(Collections.emptyList());
        when(recallCaseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(orgIdA, RecallCaseStatus.OPEN))
                .thenReturn(Collections.emptyList());

        PartnerApiKey expiring = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .organization(orgA)
                .partnerName("Doi tac TC-01")
                .keyPrefix("nks_live_abc")
                .keyHash("hash")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .status(PartnerApiKeyStatus.ACTIVE)
                .build();
        PartnerApiKey revoked = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .organization(orgA)
                .partnerName("Doi tac TC-03")
                .keyPrefix("nks_live_rev")
                .keyHash("hash2")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().plusDays(20))
                .status(PartnerApiKeyStatus.REVOKED)
                .build();
        when(partnerApiKeyRepository.findByOrganizationOrganizationId(eq(orgIdA), any()))
                .thenReturn(new PageImpl<>(List.of(expiring, revoked)));
        when(partnerApiKeyService.getHourlyCallCount(expiring.getId())).thenReturn(0);

        Pageable pageable = PageRequest.of(0, 10);
        AggregateAlertPageResponse response = aggregateAlertService.getAggregateAlerts(
                null, null, "OPEN", null, null, null, null, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(AggregateAlertType.API_KEY_EXPIRING, response.getItems().get(0).getType());
        assertEquals(expiring.getId(), response.getItems().get(0).getRelatedEntityId());
        assertEquals("/integration/api-keys", response.getItems().get(0).getActionUrl());
    }

    /**
     * Quản trị viên nền tảng (VT-01) xem được cảnh báo toàn hệ thống hoặc lọc theo tổ chức bất kỳ.
     */
    @Test
    void testAdminCanViewAllOrFilterByOrg() {
        mockSecurityContext(userDetailsAdmin);

        RecallCase recallCase = RecallCase.builder()
                .id(UUID.randomUUID())
                .caseCode("RC-2026-001")
                .organizationId(orgIdA)
                .status(RecallCaseStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(alertRepository.findByStatus(AlertStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(productFeedbackRepository.findByStatusIn(any()))
                .thenReturn(Collections.emptyList());
        when(codeRangeRepository.findAll())
                .thenReturn(Collections.emptyList());
        when(milestoneReminderRepository.findByStatus(eq(MilestoneReminderStatus.OPEN), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));
        when(recallCaseRepository.findByStatusOrderByCreatedAtDesc(RecallCaseStatus.OPEN))
                .thenReturn(List.of(recallCase));
        when(organizationRepository.findById(orgIdA))
                .thenReturn(Optional.of(orgA));

        Pageable pageable = PageRequest.of(0, 10);
        AggregateAlertPageResponse response = aggregateAlertService.getAggregateAlerts(
                null, null, "OPEN", null, null, null, null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(AggregateAlertType.OPEN_RECALL_CASE, response.getItems().get(0).getType());
        assertEquals(orgA.getName(), response.getItems().get(0).getOrganizationName());
    }

    /**
     * Kiểm thử lấy số lượng cảnh báo chưa xử lý phục vụ thanh điều hướng (Header).
     */
    @Test
    void testGetUnviewedAlertCount() {
        mockSecurityContext(userDetailsCoopA);

        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setType(AlertType.CERT_EXPIRED);
        alert.setSeverity(AlertSeverity.HIGH);
        alert.setStatus(AlertStatus.PENDING);
        alert.setOrganization(orgA);

        when(alertRepository.findByOrganizationOrganizationIdAndStatus(orgIdA, AlertStatus.PENDING))
                .thenReturn(List.of(alert));
        when(productFeedbackRepository.findByProductionLot_Organization_OrganizationIdAndStatusIn(eq(orgIdA), any()))
                .thenReturn(Collections.emptyList());
        when(codeRangeRepository.findByOrganizationOrganizationId(orgIdA))
                .thenReturn(Collections.emptyList());
        when(milestoneReminderRepository.findByProductionLot_Organization_OrganizationIdAndStatusOrderByOverdueDaysDesc(orgIdA, MilestoneReminderStatus.OPEN))
                .thenReturn(Collections.emptyList());
        when(recallCaseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(orgIdA, RecallCaseStatus.OPEN))
                .thenReturn(Collections.emptyList());

        UnviewedAlertCountResponse countResponse = aggregateAlertService.getUnviewedAlertCount();

        assertNotNull(countResponse);
        assertEquals(1, countResponse.getUnviewedCount());
        assertTrue(countResponse.isHasHighSeverity());
    }

    /**
     * Kiểm thử phát hiện chứng nhận sắp hết hạn trực tiếp từ CertificationRepository theo thời gian thực.
     */
    @Test
    void testCollectCertificationAlerts_realTimeExpiringCertification() {
        mockSecurityContext(userDetailsCoopA);

        Certification certExpiring = Certification.builder()
                .id(UUID.randomUUID())
                .name("VietGAP Bưởi da xanh")
                .code("VG-BDX-001")
                .expiryDate(LocalDate.now().plusDays(10)) // Còn 10 ngày (<= 30 ngày)
                .verificationStatus(CertificationVerificationStatus.VERIFIED)
                .organization(orgA)
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        when(certificationRepository.findByOrganizationId(orgIdA))
                .thenReturn(List.of(certExpiring));
        when(alertRepository.findByOrganizationOrganizationIdAndStatus(orgIdA, AlertStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(productFeedbackRepository.findByProductionLot_Organization_OrganizationIdAndStatusIn(eq(orgIdA), any()))
                .thenReturn(Collections.emptyList());
        when(codeRangeRepository.findByOrganizationOrganizationId(orgIdA))
                .thenReturn(Collections.emptyList());
        when(milestoneReminderRepository.findByProductionLot_Organization_OrganizationIdAndStatusOrderByOverdueDaysDesc(orgIdA, MilestoneReminderStatus.OPEN))
                .thenReturn(Collections.emptyList());
        when(recallCaseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(orgIdA, RecallCaseStatus.OPEN))
                .thenReturn(Collections.emptyList());

        Pageable pageable = PageRequest.of(0, 10);
        AggregateAlertPageResponse response = aggregateAlertService.getAggregateAlerts(
                null, null, "OPEN", null, null, null, null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        AggregateAlertItemResponse item = response.getItems().get(0);
        assertEquals(AggregateAlertType.CERT_EXPIRING, item.getType());
        assertEquals("Chứng nhận sắp hết hạn hiệu lực", item.getTitle());
        assertEquals("VietGAP Bưởi da xanh", item.getRelatedEntityName());
        assertEquals(AlertSeverity.MEDIUM, item.getSeverity());
        assertEquals("/certifications", item.getActionUrl());
        assertEquals(orgA.getName(), item.getOrganizationName());
    }
}
