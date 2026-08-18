package vn.nguongocso.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.entity.LoginAttempt;
import vn.nguongocso.auth.entity.SuspiciousCase;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.AnomalyReasonCode;
import vn.nguongocso.auth.enums.AnomalyStatus;
import vn.nguongocso.auth.enums.LoginResult;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.AccountLockRepository;
import vn.nguongocso.auth.repository.LoginAnomalyRepository;
import vn.nguongocso.auth.repository.LoginAttemptRepository;
import vn.nguongocso.auth.repository.SuspiciousCaseRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.impl.LoginAnomalyDetectionServiceImpl;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAnomalyDetectionService Unit Tests")
class LoginAnomalyDetectionServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private LoginAnomalyRepository loginAnomalyRepository;

    @Mock
    private SuspiciousCaseRepository suspiciousCaseRepository;

    @Mock
    private AccountLockRepository accountLockRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @InjectMocks
    private LoginAnomalyDetectionServiceImpl loginAnomalyDetectionService;

    private UUID userId;
    private UUID organizationId;
    private User user;
    private Organization organization;
    private OrganizationUser organizationUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        organization = Organization.builder()
                .organizationId(organizationId)
                .name("Test Organization")
                .code("ORG-001")
                .build();

        user = User.builder()
                .userId(userId)
                .userName("testuser")
                .passwordHash("hashed-password")
                .fullName("Test User")
                .status(UserStatus.ACTIVE)
                .build();

        Mockito.lenient().when(loginAnomalyRepository.save(any(LoginAnomaly.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.lenient().when(suspiciousCaseRepository.existsByUser_UserIdAndStatusAndLastDetectedAtAfter(
                eq(userId), eq(AnomalyStatus.OPEN), any(OffsetDateTime.class)))
                .thenReturn(false);
        Mockito.lenient().when(loginAnomalyRepository.findByUser_UserIdAndDetectedAtAfterOrderByDetectedAtDesc(
                eq(userId), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        organizationUser = new OrganizationUser();
        organizationUser.setUser(user);
        organizationUser.setOrganization(organization);
    }

    @Test
    @DisplayName("should record successful login attempt")
    void recordLoginAttempt_shouldRecordSuccess() {
        // Arrange
        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        loginAnomalyDetectionService.recordLoginAttempt(
                user,
                "testuser",
                true,
                "192.168.1.1",
                "VN"
        );

        // Assert
        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository, times(1)).save(captor.capture());

        LoginAttempt attempt = captor.getValue();
        assertThat(attempt.getUser()).isEqualTo(user);
        assertThat(attempt.getUsernameInput()).isEqualTo("testuser");
        assertThat(attempt.getResult()).isEqualTo(LoginResult.SUCCESS);
        assertThat(attempt.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(attempt.getCountryCode()).isEqualTo("VN");
    }

    @Test
    @DisplayName("should record failed login attempt")
    void recordLoginAttempt_shouldRecordFailed() {
        // Arrange
        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(loginAttemptRepository
                .findByUser_UserIdAndResultAndCreatedAtAfterOrderByCreatedAtDesc(
                        any(UUID.class),
                        any(LoginResult.class),
                        any(OffsetDateTime.class)))
                .thenReturn(List.of());

        // Act
        loginAnomalyDetectionService.recordLoginAttempt(
                user,
                "testuser",
                false,
                "192.168.1.1",
                "VN"
        );

        // Assert
        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository, times(1)).save(captor.capture());

        LoginAttempt attempt = captor.getValue();
        assertThat(attempt.getResult()).isEqualTo(LoginResult.FAILED);
    }

    @Test
    @DisplayName("should detect repeated failed login (5+ failures in 2 minutes)")
    void recordLoginAttempt_shouldDetectRepeatedFailedLogin() {
        // Arrange
        List<LoginAttempt> failedAttempts = List.of(
                createFailedAttempt(userId, 1),
                createFailedAttempt(userId, 2),
                createFailedAttempt(userId, 3),
                createFailedAttempt(userId, 4),
                createFailedAttempt(userId, 5)
        );

        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(loginAttemptRepository
                .findByUser_UserIdAndResultAndCreatedAtAfterOrderByCreatedAtDesc(
                        eq(userId),
                        eq(LoginResult.FAILED),
                        any(OffsetDateTime.class)))
                .thenReturn(failedAttempts);        when(loginAnomalyRepository.findByUser_UserIdAndDetectedAtAfterOrderByDetectedAtDesc(
                eq(userId), any(OffsetDateTime.class)))
                .thenReturn(List.of(
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(23)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(22)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(20)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(18)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(1))
                ));
        when(suspiciousCaseRepository.existsByUser_UserIdAndStatusAndLastDetectedAtAfter(
                eq(userId), eq(AnomalyStatus.OPEN), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(organizationUserRepository.findFirstByUser(user))
                .thenReturn(Optional.of(organizationUser));

        // Act
        loginAnomalyDetectionService.recordLoginAttempt(
                user,
                "testuser",
                false,
                "192.168.1.1",
                "VN"
        );

        // Assert
        ArgumentCaptor<LoginAnomaly> anomalyCaptor = ArgumentCaptor.forClass(LoginAnomaly.class);
        verify(loginAnomalyRepository, times(1)).save(anomalyCaptor.capture());
        verify(notificationService, times(1)).sendLoginAnomalyNotification(any(LoginAnomaly.class));

        LoginAnomaly anomaly = anomalyCaptor.getValue();
        assertThat(anomaly.getUser()).isEqualTo(user);
        assertThat(anomaly.getReasonCode()).isEqualTo(AnomalyReasonCode.REPEATED_FAILED_LOGIN);
        assertThat(anomaly.getAttemptCount()).isEqualTo(5);
        verify(suspiciousCaseRepository, times(1)).save(any(SuspiciousCase.class));
    }

    @Test
    @DisplayName("should create a suspicious case when user reaches threshold with no OPEN case in the 24h window")
    void recordLoginAttempt_shouldCreateSuspiciousCaseWhenThresholdReached() {
        List<LoginAttempt> failedAttempts = List.of(
                createFailedAttempt(userId, 1),
                createFailedAttempt(userId, 2),
                createFailedAttempt(userId, 3),
                createFailedAttempt(userId, 4),
                createFailedAttempt(userId, 5)
        );

        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(loginAttemptRepository.findByUser_UserIdAndResultAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(userId), eq(LoginResult.FAILED), any(OffsetDateTime.class)))
                .thenReturn(failedAttempts);
        when(loginAnomalyRepository.findByUser_UserIdAndDetectedAtAfterOrderByDetectedAtDesc(
                eq(userId), any(OffsetDateTime.class)))
                .thenReturn(List.of(
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(23)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(22)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(20)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(18)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(1))
                ));
        when(suspiciousCaseRepository.existsByUser_UserIdAndStatusAndLastDetectedAtAfter(
                eq(userId), eq(AnomalyStatus.OPEN), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(organizationUserRepository.findFirstByUser(user))
                .thenReturn(Optional.of(organizationUser));

        loginAnomalyDetectionService.recordLoginAttempt(user, "testuser", false, "192.168.1.1", "VN");

        verify(suspiciousCaseRepository, times(1)).save(any(SuspiciousCase.class));
    }

    @Test
    @DisplayName("should not create a duplicate suspicious case while an OPEN case already exists in the same 24h window")
    void recordLoginAttempt_shouldNotCreateDuplicateSuspiciousCaseWhenOpenCaseExists() {
        List<LoginAttempt> failedAttempts = List.of(
                createFailedAttempt(userId, 1),
                createFailedAttempt(userId, 2),
                createFailedAttempt(userId, 3),
                createFailedAttempt(userId, 4),
                createFailedAttempt(userId, 5)
        );

        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(loginAttemptRepository.findByUser_UserIdAndResultAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(userId), eq(LoginResult.FAILED), any(OffsetDateTime.class)))
                .thenReturn(failedAttempts);
        when(loginAnomalyRepository.findByUser_UserIdAndDetectedAtAfterOrderByDetectedAtDesc(
                eq(userId), any(OffsetDateTime.class)))
                .thenReturn(List.of(
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(23)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(22)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(20)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(18)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(1))
                ));
        when(suspiciousCaseRepository.existsByUser_UserIdAndStatusAndLastDetectedAtAfter(
                eq(userId), eq(AnomalyStatus.OPEN), any(OffsetDateTime.class)))
                .thenReturn(true);
        when(organizationUserRepository.findFirstByUser(user))
                .thenReturn(Optional.of(organizationUser));

        loginAnomalyDetectionService.recordLoginAttempt(user, "testuser", false, "192.168.1.1", "VN");

        verify(suspiciousCaseRepository, times(0)).save(any(SuspiciousCase.class));
    }

    @Test
    @DisplayName("should not reopen the same suspicious case when the latest case was DISMISSED in the same 24h window")
    void recordLoginAttempt_shouldNotCreateCaseWhenLatestDismissedCaseStillCoversWindow() {
        OffsetDateTime now = OffsetDateTime.now();
        List<LoginAttempt> failedAttempts = List.of(
                createFailedAttempt(userId, 1),
                createFailedAttempt(userId, 2),
                createFailedAttempt(userId, 3),
                createFailedAttempt(userId, 4),
                createFailedAttempt(userId, 5)
        );

        SuspiciousCase dismissedCase = SuspiciousCase.builder()
                .user(user)
                .organization(organization)
                .status(AnomalyStatus.DISMISSED)
                .anomalyCount(5)
                .firstDetectedAt(now.minusHours(23))
                .lastDetectedAt(now.minusMinutes(10))
                .createdAt(now.minusMinutes(10))
                .build();

        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(loginAttemptRepository.findByUser_UserIdAndResultAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(userId), eq(LoginResult.FAILED), any(OffsetDateTime.class)))
                .thenReturn(failedAttempts);
        when(loginAnomalyRepository.findByUser_UserIdAndDetectedAtAfterOrderByDetectedAtDesc(
                eq(userId), any(OffsetDateTime.class)))
                .thenAnswer(invocation -> {
                    OffsetDateTime windowStart = invocation.getArgument(1);
                    if (windowStart.isAfter(dismissedCase.getLastDetectedAt())) {
                        return List.of();
                    }
                    return List.of(
                            createAnomalyForThreshold(user, organization, now.minusHours(23)),
                            createAnomalyForThreshold(user, organization, now.minusHours(22)),
                            createAnomalyForThreshold(user, organization, now.minusHours(20)),
                            createAnomalyForThreshold(user, organization, now.minusHours(18)),
                            createAnomalyForThreshold(user, organization, now.minusHours(12))
                    );
                });
        when(suspiciousCaseRepository.findByUser_UserIdOrderByLastDetectedAtDesc(eq(userId)))
                .thenReturn(List.of(dismissedCase));
        when(organizationUserRepository.findFirstByUser(user))
                .thenReturn(Optional.of(organizationUser));

        loginAnomalyDetectionService.recordLoginAttempt(user, "testuser", false, "192.168.1.1", "VN");

        verify(suspiciousCaseRepository, times(0)).save(any(SuspiciousCase.class));
    }

    @Test
    @DisplayName("should create a new suspicious case after the previous one is DISMISSED and a fresh threshold is reached")
    void recordLoginAttempt_shouldCreateNewSuspiciousCaseAfterDismissedCase() {
        List<LoginAttempt> failedAttempts = List.of(
                createFailedAttempt(userId, 1),
                createFailedAttempt(userId, 2),
                createFailedAttempt(userId, 3),
                createFailedAttempt(userId, 4),
                createFailedAttempt(userId, 5)
        );

        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(loginAttemptRepository.findByUser_UserIdAndResultAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(userId), eq(LoginResult.FAILED), any(OffsetDateTime.class)))
                .thenReturn(failedAttempts);
        when(loginAnomalyRepository.findByUser_UserIdAndDetectedAtAfterOrderByDetectedAtDesc(
                eq(userId), any(OffsetDateTime.class)))
                .thenReturn(List.of(
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(23)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(22)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(20)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(18)),
                        createAnomalyForThreshold(user, organization, OffsetDateTime.now().minusHours(1))
                ));
        when(suspiciousCaseRepository.findByUser_UserIdOrderByLastDetectedAtDesc(eq(userId)))
                .thenReturn(List.of(SuspiciousCase.builder()
                        .user(user)
                        .organization(organization)
                        .status(AnomalyStatus.DISMISSED)
                        .anomalyCount(5)
                        .firstDetectedAt(OffsetDateTime.now().minusHours(25))
                        .lastDetectedAt(OffsetDateTime.now().minusHours(10))
                        .createdAt(OffsetDateTime.now().minusHours(10))
                        .build()));
        when(suspiciousCaseRepository.existsByUser_UserIdAndStatusAndLastDetectedAtAfter(
                eq(userId), eq(AnomalyStatus.OPEN), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(organizationUserRepository.findFirstByUser(user))
                .thenReturn(Optional.of(organizationUser));

        loginAnomalyDetectionService.recordLoginAttempt(user, "testuser", false, "192.168.1.1", "VN");

        verify(suspiciousCaseRepository, times(1)).save(any(SuspiciousCase.class));
    }

    @Test
    @DisplayName("should detect unusual country login")
    void recordLoginAttempt_shouldDetectUnusualCountry() {
        // Arrange
        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(loginAttemptRepository.existsByUser_UserIdAndResultAndCountryCode(
                userId,
                LoginResult.SUCCESS,
                "US"))
                .thenReturn(false); // Never logged in from US before
        when(loginAttemptRepository.findByUser_UserIdOrderByCreatedAtDesc(
                eq(userId),
                any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(createFailedAttempt(userId, 1)),
                        org.springframework.data.domain.PageRequest.of(0, 1),
                        1)); // already had prior login before unusual country check
        when(organizationUserRepository.findFirstByUser(user))
                .thenReturn(Optional.of(organizationUser));

        // Act
        loginAnomalyDetectionService.recordLoginAttempt(
                user,
                "testuser",
                true,
                "1.2.3.4",
                "US"
        );

        // Assert
        ArgumentCaptor<LoginAnomaly> anomalyCaptor = ArgumentCaptor.forClass(LoginAnomaly.class);
        verify(loginAnomalyRepository, times(1)).save(anomalyCaptor.capture());

        LoginAnomaly anomaly = anomalyCaptor.getValue();
        assertThat(anomaly.getReasonCode()).isEqualTo(AnomalyReasonCode.UNUSUAL_COUNTRY);
        assertThat(anomaly.getCountryCode()).isEqualTo("US");
    }

    @Test
    @DisplayName("should not create anomaly if user has previous success in same country")
    void recordLoginAttempt_shouldNotCreateAnomalyForKnownCountry() {
        // Arrange
        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(loginAttemptRepository.existsByUser_UserIdAndResultAndCountryCode(
                userId,
                LoginResult.SUCCESS,
                "VN"))
                .thenReturn(true); // Already logged in from VN

        // Act
        loginAnomalyDetectionService.recordLoginAttempt(
                user,
                "testuser",
                true,
                "192.168.1.1",
                "VN"
        );

        // Assert
        verify(loginAnomalyRepository, times(0)).save(any(LoginAnomaly.class));
    }

    @Test
    @DisplayName("should handle null country code gracefully")
    void recordLoginAttempt_shouldHandleNullCountryCode() {
        // Arrange
        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        loginAnomalyDetectionService.recordLoginAttempt(
                user,
                "testuser",
                true,
                "192.168.1.1",
                null // No country code
        );

        // Assert
        verify(loginAnomalyRepository, times(0)).save(any(LoginAnomaly.class));
    }

    @Test
    @DisplayName("should check if account is locked")
    void isAccountLocked_shouldReturnTrue() {
        // Arrange
        when(accountLockRepository.existsByUser_UserIdAndStatus(
                userId,
                vn.nguongocso.auth.enums.AccountLockStatus.LOCKED))
                .thenReturn(true);

        // Act
        boolean isLocked = loginAnomalyDetectionService.isAccountLocked(userId);

        // Assert
        assertThat(isLocked).isTrue();
        verify(accountLockRepository, times(1)).existsByUser_UserIdAndStatus(
                userId,
                vn.nguongocso.auth.enums.AccountLockStatus.LOCKED);
    }

    @Test
    @DisplayName("should return false if account is not locked")
    void isAccountLocked_shouldReturnFalse() {
        // Arrange
        when(accountLockRepository.existsByUser_UserIdAndStatus(
                userId,
                vn.nguongocso.auth.enums.AccountLockStatus.LOCKED))
                .thenReturn(false);

        // Act
        boolean isLocked = loginAnomalyDetectionService.isAccountLocked(userId);

        // Assert
        assertThat(isLocked).isFalse();
    }

    // Helper method
    private LoginAttempt createFailedAttempt(UUID userId, int index) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setUser(user);
        attempt.setUsernameInput("testuser");
        attempt.setResult(LoginResult.FAILED);
        attempt.setIpAddress("192.168.1." + index);
        attempt.setCountryCode("VN");
        attempt.setCreatedAt(OffsetDateTime.now().minusMinutes(5 - index));
        return attempt;
    }

    private LoginAnomaly createAnomalyForThreshold(User user, Organization organization, OffsetDateTime detectedAt) {
        return LoginAnomaly.builder()
                .id(UUID.randomUUID())
                .user(user)
                .organization(organization)
                .reasonCode(AnomalyReasonCode.REPEATED_FAILED_LOGIN)
                .attemptCount(5)
                .ipAddress("192.168.1.10")
                .countryCode("VN")
                .detectedAt(detectedAt)
                .status(AnomalyStatus.OPEN)
                .build();
    }
}
