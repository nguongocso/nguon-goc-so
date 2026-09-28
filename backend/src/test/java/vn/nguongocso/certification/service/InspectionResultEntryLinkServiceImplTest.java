package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.IssueInspectionResultEntryLinkRequest;
import vn.nguongocso.certification.dto.response.InspectionResultEntryLinkResponse;
import vn.nguongocso.certification.dto.response.PublicInspectionResultEntryResponse;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.InspectionResultEntryLink;
import vn.nguongocso.certification.entity.TestingUnit;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.enums.InspectionResultEntryLinkStatus;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.InspectionResultEntryLinkRepository;
import vn.nguongocso.certification.repository.TestingUnitRepository;
import vn.nguongocso.certification.service.impl.InspectionResultEntryLinkServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.mail.service.EmailService;
import vn.nguongocso.organization.entity.Organization;

/**
 * Kiểm thử đơn vị cho dịch vụ quản lý liên kết nhập kết quả kiểm nghiệm (InspectionResultEntryLinkServiceImpl).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InspectionResultEntryLinkServiceImplTest {

    @Mock
    private InspectionResultEntryLinkRepository linkRepository;

    @Mock
    private InspectionRequestRepository requestRepository;

    @Mock
    private TestingUnitRepository testingUnitRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private InspectionResultPortalRateLimitService rateLimitService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InspectionResultEntryLinkServiceImpl service;

    private UUID requestId;
    private UUID orgId;
    private UUID otherOrgId;
    private UUID testingUnitId;
    private User user;
    private CustomUserDetails currentUser;
    private Organization organization;
    private ProductionLot lot;
    private TestingUnit testingUnit;
    private InspectionRequest inspectionRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "frontendUrl", "https://nguongocso.vn");

        requestId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        otherOrgId = UUID.randomUUID();
        testingUnitId = UUID.randomUUID();

        organization = Organization.builder()
                .organizationId(orgId)
                .name("Hợp tác xã Nông sản Sạch")
                .build();

        user = User.builder()
                .userId(UUID.randomUUID())
                .userName("manager_vt02")
                .fullName("Nguyễn Văn Quản Lý")
                .build();

        currentUser = mock(CustomUserDetails.class);
        when(currentUser.getOrganizationId()).thenReturn(orgId);
        when(currentUser.getUserId()).thenReturn(user.getUserId());
        when(currentUser.getUser()).thenReturn(user);

        lot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô Xoài Cát Chu 2026")
                .organization(organization)
                .build();

        testingUnit = TestingUnit.builder()
                .id(testingUnitId)
                .name("Trung tâm Kiểm nghiệm Quốc gia")
                .isActive(true)
                .build();

        inspectionRequest = InspectionRequest.builder()
                .id(requestId)
                .productionLot(lot)
                .testingUnitId(testingUnitId)
                .inspectionUnit(testingUnit.getName())
                .sampleSentDate(LocalDate.now())
                .status(InspectionRequestStatus.PENDING_RESULT)
                .createdBy(user)
                .criteria(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Cấp liên kết thành công (TC-01): sinh token an toàn, lưu băm SHA-256, gửi email và trả URL duy nhất 1 lần")
    void testIssueLink_Success() {
        IssueInspectionResultEntryLinkRequest request = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@example.vn")
                .expiryDays(7)
                .build();

        when(requestRepository.findByIdAndOrganizationIdForUpdate(requestId, currentUser.getOrganizationId()))
                .thenReturn(Optional.of(inspectionRequest));
        when(testingUnitRepository.findById(testingUnitId)).thenReturn(Optional.of(testingUnit));
        when(linkRepository.revokeActiveLinksByRequestId(any(), any(), any(), any(), any()))
                .thenReturn(0);
        when(linkRepository.save(any(InspectionResultEntryLink.class))).thenAnswer(inv -> inv.getArgument(0));

        InspectionResultEntryLinkResponse response = service.issueLink(requestId, request, currentUser);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InspectionResultEntryLinkStatus.ACTIVE);
        assertThat(response.getRecipientEmail()).isEqualTo("lab@example.vn");
        assertThat(response.getEntryUrl()).startsWith("https://nguongocso.vn/inspection-result-entry/");
        assertThat(response.getTokenPrefix()).isNotBlank();

        // Kiểm tra lưu băm SHA-256 vào database, không lưu raw token
        ArgumentCaptor<InspectionResultEntryLink> linkCaptor = ArgumentCaptor.forClass(InspectionResultEntryLink.class);
        verify(linkRepository).save(linkCaptor.capture());
        InspectionResultEntryLink savedLink = linkCaptor.getValue();
        assertThat(savedLink.getTokenHash()).hasSize(64); // Độ dài chuẩn chuỗi SHA-256 hex
        assertThat(savedLink.getTokenHash()).doesNotContain("https://");

        // Kiểm tra gửi email và log activity
        verify(emailService).sendInspectionResultEntryEmail(
                eq("lab@example.vn"),
                eq("Hợp tác xã Nông sản Sạch"),
                eq("Trung tâm Kiểm nghiệm Quốc gia"),
                eq("Lô Xoài Cát Chu 2026"),
                anyString(),
                eq(7));
        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    @DisplayName("Cấp lại liên kết (QTN-14): thu hồi liên kết ACTIVE cũ trước khi sinh liên kết mới")
    void testIssueLink_Reissue_RevokesOldActiveLinks() {
        IssueInspectionResultEntryLinkRequest request = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@example.vn")
                .expiryDays(5)
                .build();

        when(requestRepository.findByIdAndOrganizationIdForUpdate(requestId, currentUser.getOrganizationId()))
                .thenReturn(Optional.of(inspectionRequest));
        when(testingUnitRepository.findById(testingUnitId)).thenReturn(Optional.of(testingUnit));
        when(linkRepository.revokeActiveLinksByRequestId(any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(linkRepository.save(any(InspectionResultEntryLink.class))).thenAnswer(inv -> inv.getArgument(0));

        InspectionResultEntryLinkResponse response = service.issueLink(requestId, request, currentUser);

        assertThat(response).isNotNull();
        verify(linkRepository).revokeActiveLinksByRequestId(
                eq(requestId),
                eq(InspectionResultEntryLinkStatus.ACTIVE),
                eq(InspectionResultEntryLinkStatus.REVOKED),
                any(),
                eq(user));
    }

    @Test
    @DisplayName("Cấp liên kết thất bại khi yêu cầu thuộc tổ chức khác (Tenant Isolation)")
    void testIssueLink_CrossTenant_ThrowsNotFound() {
        IssueInspectionResultEntryLinkRequest request = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@example.vn")
                .expiryDays(7)
                .build();

        when(requestRepository.findByIdAndOrganizationIdForUpdate(requestId, currentUser.getOrganizationId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issueLink(requestId, request, currentUser))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(linkRepository, never()).save(any());
        verify(emailService, never()).sendInspectionResultEntryEmail(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Cấp liên kết thất bại khi yêu cầu không còn PENDING_RESULT -> 409 Conflict")
    void testIssueLink_NotPending_ThrowsConflict() {
        inspectionRequest.setStatus(InspectionRequestStatus.PASSED);

        IssueInspectionResultEntryLinkRequest request = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@example.vn")
                .build();

        when(requestRepository.findByIdAndOrganizationIdForUpdate(requestId, currentUser.getOrganizationId()))
                .thenReturn(Optional.of(inspectionRequest));

        assertThatThrownBy(() -> service.issueLink(requestId, request, currentUser))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(linkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cấp liên kết thất bại khi yêu cầu chưa gán đơn vị kiểm nghiệm -> 400 Bad Request")
    void testIssueLink_MissingTestingUnit_ThrowsBadRequest() {
        inspectionRequest.setTestingUnitId(null);

        IssueInspectionResultEntryLinkRequest request = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@example.vn")
                .build();

        when(requestRepository.findByIdAndOrganizationIdForUpdate(requestId, currentUser.getOrganizationId()))
                .thenReturn(Optional.of(inspectionRequest));

        assertThatThrownBy(() -> service.issueLink(requestId, request, currentUser))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Lấy liên kết mới nhất (VT-02): trả về thông tin metadata mà không trả URL hay secret token")
    void testGetLatestLink_Success() {
        InspectionResultEntryLink link = InspectionResultEntryLink.builder()
                .id(UUID.randomUUID())
                .inspectionRequest(inspectionRequest)
                .organization(organization)
                .testingUnit(testingUnit)
                .recipientEmail("lab@example.vn")
                .tokenPrefix("abc12345")
                .tokenHash("hash123")
                .status(InspectionResultEntryLinkStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(requestRepository.findByIdAndProductionLot_Organization_OrganizationId(requestId, currentUser.getOrganizationId()))
                .thenReturn(Optional.of(inspectionRequest));
        when(linkRepository.findFirstByInspectionRequest_IdOrderByCreatedAtDesc(requestId)).thenReturn(Optional.of(link));

        InspectionResultEntryLinkResponse response = service.getLatestLink(requestId, currentUser);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InspectionResultEntryLinkStatus.ACTIVE);
        assertThat(response.getTokenPrefix()).isEqualTo("abc12345");
        assertThat(response.getEntryUrl()).isNull(); // Tuyệt đối không trả URL/secret
    }

    @Test
    @DisplayName("Lấy liên kết mới nhất đã quá hạn: trả trạng thái EXPIRED dù bản ghi vẫn ACTIVE")
    void testGetLatestLink_ExpiredActiveLink_ReturnsEffectiveExpiredStatus() {
        InspectionResultEntryLink link = InspectionResultEntryLink.builder()
                .id(UUID.randomUUID())
                .inspectionRequest(inspectionRequest)
                .organization(organization)
                .testingUnit(testingUnit)
                .recipientEmail("lab@example.vn")
                .tokenPrefix("expired1")
                .tokenHash("expired-hash")
                .status(InspectionResultEntryLinkStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .createdAt(LocalDateTime.now().minusDays(7))
                .build();

        when(requestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                requestId,
                currentUser.getOrganizationId()))
                .thenReturn(Optional.of(inspectionRequest));
        when(linkRepository.findFirstByInspectionRequest_IdOrderByCreatedAtDesc(requestId))
                .thenReturn(Optional.of(link));

        InspectionResultEntryLinkResponse response = service.getLatestLink(requestId, currentUser);

        assertThat(response.getStatus()).isEqualTo(InspectionResultEntryLinkStatus.EXPIRED);
        assertThat(link.getStatus()).isEqualTo(InspectionResultEntryLinkStatus.ACTIVE);
    }

    @Test
    @DisplayName("Lấy dữ liệu cổng public thành công (TC-01): chỉ trả dữ liệu tối thiểu, không lộ ID nội bộ")
    void testGetPublicPortalData_Success() {
        String rawToken = "my_secret_token_123456789012345678";
        String tokenHash = service.hashToken(rawToken);

        InspectionCriterion criterion = InspectionCriterion.builder()
                .id(UUID.randomUUID())
                .criterionCode("RESIDUE_PESTICIDE")
                .criterionName("Dư lượng thuốc BVTV")
                .build();
        inspectionRequest.getCriteria().add(criterion);

        InspectionResultEntryLink link = InspectionResultEntryLink.builder()
                .id(UUID.randomUUID())
                .inspectionRequest(inspectionRequest)
                .organization(organization)
                .testingUnit(testingUnit)
                .tokenHash(tokenHash)
                .status(InspectionResultEntryLinkStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        when(linkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(link));

        PublicInspectionResultEntryResponse response = service.getPublicPortalData(rawToken, "192.168.1.10");

        assertThat(response).isNotNull();
        assertThat(response.getTestingUnit()).isEqualTo("Trung tâm Kiểm nghiệm Quốc gia");
        assertThat(response.getLotCode()).isEqualTo("Lô Xoài Cát Chu 2026");
        assertThat(response.getCriteria()).hasSize(1);
        assertThat(response.getCriteria().get(0).getCode()).isEqualTo("RESIDUE_PESTICIDE");
    }

    @Test
    @DisplayName("Mở cổng public khi link đã hết hạn (TC-02): trả về lỗi 410 GONE hướng dẫn liên hệ HTX")
    void testGetPublicPortalData_Expired_ThrowsGone() {
        String rawToken = "expired_token";
        String tokenHash = service.hashToken(rawToken);

        InspectionResultEntryLink link = InspectionResultEntryLink.builder()
                .id(UUID.randomUUID())
                .inspectionRequest(inspectionRequest)
                .organization(organization)
                .testingUnit(testingUnit)
                .tokenHash(tokenHash)
                .status(InspectionResultEntryLinkStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Đã quá hạn
                .build();

        when(linkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> service.getPublicPortalData(rawToken, "192.168.1.10"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getStatus()).isEqualTo(HttpStatus.GONE);
                    assertThat(bex.getMessage()).contains("Liên kết đã hết hạn");
                });
    }

    @Test
    @DisplayName("Mở cổng public khi link đã USED hoặc REVOKED (TC-03): trả về lỗi 410 GONE")
    void testGetPublicPortalData_Used_ThrowsGone() {
        String rawToken = "used_token";
        String tokenHash = service.hashToken(rawToken);

        InspectionResultEntryLink link = InspectionResultEntryLink.builder()
                .id(UUID.randomUUID())
                .inspectionRequest(inspectionRequest)
                .organization(organization)
                .testingUnit(testingUnit)
                .tokenHash(tokenHash)
                .status(InspectionResultEntryLinkStatus.USED)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .build();

        when(linkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> service.getPublicPortalData(rawToken, "192.168.1.10"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getStatus()).isEqualTo(HttpStatus.GONE);
                    assertThat(bex.getMessage()).contains("đã được sử dụng");
                });
    }

    @Test
    @DisplayName("Token không tồn tại: ghi nhận IP vi phạm và trả về 404 NOT_FOUND")
    void testGetPublicPortalData_InvalidToken_ThrowsNotFound() {
        String rawToken = "invalid_token";
        String tokenHash = service.hashToken(rawToken);

        when(linkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicPortalData(rawToken, "192.168.1.10"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(rateLimitService).recordInvalidTokenAttempt("192.168.1.10");
    }
}
