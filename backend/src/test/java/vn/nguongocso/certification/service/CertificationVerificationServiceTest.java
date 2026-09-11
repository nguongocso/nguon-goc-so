package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.AttachCertificationRequest;
import vn.nguongocso.certification.dto.request.CreateCertificationRequest;
import vn.nguongocso.certification.dto.request.RejectCertificateRequest;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;
import vn.nguongocso.certification.event.CertificationRejectedEvent;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.certification.repository.StandardRepository;
import vn.nguongocso.certification.service.impl.CertificationServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;

/** Kiểm thử nghiệp vụ chính của NCL-696 và dependency tải tài liệu NCL-09-CN-003. */
@ExtendWith(MockitoExtension.class)
class CertificationVerificationServiceTest {

    @Mock private ProductionLotRepository productionLotRepository;
    @Mock private CertificationRepository certificationRepository;
    @Mock private ProductionLotCertificationRepository productionLotCertificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AlertRepository alertRepository;
    @Mock private NotificationService notificationService;
    @Mock private ObjectMapper objectMapper;
    @Mock private StandardRepository standardRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ActivityLogRepository activityLogRepository;

    @InjectMocks private CertificationServiceImpl service;

    @TempDir Path uploadDirectory;

    private UUID organizationId;
    private UUID userId;
    private CustomUserDetails organizationManager;
    private CustomUserDetails platformAdmin;
    private Organization organization;
    private Standard standard;
    private User reviewer;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organization = Organization.builder().organizationId(organizationId).name("HTX Xanh").build();
        standard = Standard.builder().id(UUID.randomUUID()).name("VietGAP").build();
        reviewer = User.builder().userId(userId).userName("admin").fullName("Quản trị viên").build();
        organizationManager = user("VT-02");
        platformAdmin = user("VT-01");

        ReflectionTestUtils.setField(service, "uploadBaseDir", uploadDirectory.toString());
        ReflectionTestUtils.setField(service, "certificationRelativePath", "certifications");
        ReflectionTestUtils.setField(service, "certificationMaxFileSize", 5L * 1024 * 1024);
        ReflectionTestUtils.setField(service, "warningThresholdDays", 30);
        lenient().when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createCertificationStoresPrivateDocumentAndStartsPending() {
        CreateCertificationRequest request = validCreateRequest();
        MockMultipartFile file = new MockMultipartFile(
                "file", "vietgap.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-test".getBytes());
        when(standardRepository.findById(standard.getId())).thenReturn(Optional.of(standard));
        when(certificationRepository.findByCode(request.getCode())).thenReturn(Optional.empty());
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(certificationRepository.save(any(Certification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createCertification(request, file, organizationManager);

        ArgumentCaptor<Certification> captor = ArgumentCaptor.forClass(Certification.class);
        verify(certificationRepository).save(captor.capture());
        Certification saved = captor.getValue();
        assertThat(saved.getVerificationStatus()).isEqualTo(CertificationVerificationStatus.PENDING);
        assertThat(saved.getDocumentFileName()).isEqualTo("vietgap.pdf");
        assertThat(saved.getDocumentContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(Files.isRegularFile(Path.of(saved.getDocumentStoragePath()))).isTrue();
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    void verifyCertificateUpdatesStateAndActivityLogTogether() throws Exception {
        Certification certification = pendingCertificationWithDocument();
        when(certificationRepository.findById(certification.getId())).thenReturn(Optional.of(certification));
        when(userRepository.findById(userId)).thenReturn(Optional.of(reviewer));
        when(certificationRepository.updateVerificationStatus(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        var response = service.verifyCertificate(certification.getId(), null, platformAdmin);

        assertThat(response.getVerificationStatus()).isEqualTo(CertificationVerificationStatus.VERIFIED);
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    void rejectCertificatePublishesNotificationOnlyAsAfterCommitEvent() {
        Certification certification = pendingCertification();
        when(certificationRepository.findById(certification.getId())).thenReturn(Optional.of(certification));
        when(userRepository.findById(userId)).thenReturn(Optional.of(reviewer));
        when(certificationRepository.updateVerificationStatus(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        service.rejectCertificate(
                certification.getId(),
                RejectCertificateRequest.builder().rejectionReason("Số hiệu tài liệu không khớp").build(),
                platformAdmin);

        verify(notificationService, never()).sendCertificationRejectionNotification(any(), any());
        verify(eventPublisher).publishEvent(any(CertificationRejectedEvent.class));
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    void attachCertificationRejectsRejectedCertificate() {
        UUID lotId = UUID.randomUUID();
        Certification rejected = pendingCertification();
        rejected.setVerificationStatus(CertificationVerificationStatus.REJECTED);
        ProductionLot lot = new ProductionLot();
        lot.setId(lotId);
        lot.setOrganization(organization);
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(certificationRepository.findByIdAndOrganizationId(rejected.getId(), organizationId))
                .thenReturn(Optional.of(rejected));

        AttachCertificationRequest request = new AttachCertificationRequest();
        request.setCertificationId(rejected.getId());

        assertThatThrownBy(() -> service.attachCertification(lotId, request, organizationManager))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã bị từ chối");
    }

    @Test
    void adminListRejectsNonPlatformAdmin() {
        assertThatThrownBy(() -> service.getAdminCertifications(
                null, null, null, "createdAt", "desc", 0, 20, organizationManager))
                .isInstanceOf(AccessDeniedException.class);
    }

    private CustomUserDetails user(String roleCode) {
        CustomUserDetails user = mock(CustomUserDetails.class, withSettings().lenient());
        when(user.getRoleCode()).thenReturn(roleCode);
        when(user.getOrganizationId()).thenReturn(organizationId);
        when(user.getUserId()).thenReturn(userId);
        when(user.getUsername()).thenReturn("admin");
        when(user.getFullName()).thenReturn("Quản trị viên");
        return user;
    }

    private CreateCertificationRequest validCreateRequest() {
        CreateCertificationRequest request = new CreateCertificationRequest();
        request.setStandardId(standard.getId());
        request.setCode("VGP-2026-001");
        request.setIssuedBy("Trung tâm Chứng nhận");
        request.setIssueDate(LocalDate.now().minusDays(1));
        request.setExpiryDate(LocalDate.now().plusYears(1));
        return request;
    }

    private Certification pendingCertification() {
        return Certification.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .standard(standard)
                .name(standard.getName())
                .code("VGP-2026-001")
                .issuedBy("Trung tâm Chứng nhận")
                .issueDate(LocalDate.now().minusDays(1))
                .expiryDate(LocalDate.now().plusYears(1))
                .verificationStatus(CertificationVerificationStatus.PENDING)
                .build();
    }

    private Certification pendingCertificationWithDocument() throws Exception {
        Certification certification = pendingCertification();
        Path directory = uploadDirectory.resolve("certifications").resolve(certification.getId().toString());
        Files.createDirectories(directory);
        Path document = Files.writeString(directory.resolve("document.pdf"), "%PDF-test");
        certification.setDocumentFileName("vietgap.pdf");
        certification.setDocumentContentType(MediaType.APPLICATION_PDF_VALUE);
        certification.setDocumentFileSize(Files.size(document));
        certification.setDocumentStoragePath(document.toAbsolutePath().toString());
        return certification;
    }
}
