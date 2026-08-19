package vn.nguongocso.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.auth.dto.response.AccountLockResponse;
import vn.nguongocso.auth.dto.response.LoginAnomalyResponse;
import vn.nguongocso.auth.dto.response.LoginHistoryResponse;
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.entity.LoginAttempt;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.AccountLockStatus;
import vn.nguongocso.auth.enums.AnomalyReasonCode;
import vn.nguongocso.auth.enums.AnomalyStatus;
import vn.nguongocso.auth.enums.LoginResult;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.AccountLockRepository;
import vn.nguongocso.auth.repository.LoginAnomalyRepository;
import vn.nguongocso.auth.repository.LoginAttemptRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.AccountLockService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.impl.LoginMonitoringServiceImpl;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.permission.service.PermissionChecker;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginMonitoringService Unit Tests")
class LoginMonitoringServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private LoginAnomalyRepository loginAnomalyRepository;

    @Mock
    private AccountLockRepository accountLockRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountLockService accountLockService;

    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private LoginMonitoringServiceImpl loginMonitoringService;

    private UUID userId;
    private UUID organizationId;
    private User user;
    private User targetUser;
    private LoginAttempt loginAttempt;
    private LoginAnomaly loginAnomaly;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        user = User.builder()
                .userId(userId)
                .userName("admin")
                .passwordHash("hashed")
                .fullName("Admin")
                .status(UserStatus.ACTIVE)
                .build();

        targetUser = User.builder()
                .userId(UUID.randomUUID())
                .userName("targetuser")
                .passwordHash("hashed")
                .fullName("Target User")
                .status(UserStatus.ACTIVE)
                .build();

        loginAttempt = LoginAttempt.builder()
                .id(UUID.randomUUID())
                .user(targetUser)
                .usernameInput("targetuser")
                .result(LoginResult.SUCCESS)
                .ipAddress("192.168.1.1")
                .countryCode("VN")
                .createdAt(OffsetDateTime.now())
                .build();

        loginAnomaly = LoginAnomaly.builder()
                .id(UUID.randomUUID())
                .user(targetUser)
                .reasonCode(AnomalyReasonCode.REPEATED_FAILED_LOGIN)
                .attemptCount(5)
                .ipAddress("192.168.1.1")
                .countryCode("VN")
                .status(AnomalyStatus.OPEN)
                .detectedAt(OffsetDateTime.now())
                .build();

        CustomUserDetails currentUser = mock(CustomUserDetails.class);
        lenient().when(currentUser.getUserId()).thenReturn(userId);
        lenient().when(currentUser.getRoleCode()).thenReturn("VT-01");

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(currentUser);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("should get login history with pagination")
    void getLoginHistory_shouldReturnPaginatedResults() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<LoginHistoryResponse> mockPage = new PageImpl<>(
                List.of(),
                pageable,
                0
        );

        // Act
        PageResponse<LoginHistoryResponse> result = loginMonitoringService.getLoginHistory(
                null,
                null,
                null,
                null,
                null,
                pageable
        );

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("should get login anomalies with pagination")
    void getLoginAnomalies_shouldReturnPaginatedResults() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PageResponse<LoginAnomalyResponse> result = loginMonitoringService.getLoginAnomalies(
                null,
                null,
                null,
                pageable
        );

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("should lock account successfully")
    void lockAccount_shouldLockAccountSuccessfully() {
        // Arrange
        UUID accountId = targetUser.getUserId();
        UUID anomalyId = loginAnomaly.getId();
        String reason = "Phát hiện đăng nhập bất thường";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountLockService.lockAccount(
                eq(accountId),
                eq(anomalyId),
                eq(reason),
                any(User.class),
                eq(0),
                eq(0),
                eq(0),
                eq(false)))
                .thenReturn(accountId);
        when(accountLockRepository.findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(
                eq(accountId), eq(AccountLockStatus.LOCKED)))
                .thenReturn(Optional.of(AccountLock.builder()
                        .user(targetUser)
                        .lockedBy(user)
                        .status(AccountLockStatus.LOCKED)
                        .lockReason(reason)
                        .lockedAt(OffsetDateTime.now())
                        .build()));

        // Act
        AccountLockResponse result = loginMonitoringService.lockAccount(accountId, anomalyId, reason);

        // Assert
        assertThat(result).isNotNull();
        verify(accountLockService, times(1)).lockAccount(
                eq(accountId),
                eq(anomalyId),
                eq(reason),
                any(User.class),
                eq(0),
                eq(0),
                eq(0),
                eq(false));
    }

    @Test
    @DisplayName("should unlock account successfully")
    void unlockAccount_shouldUnlockAccountSuccessfully() {
        // Arrange
        UUID accountId = targetUser.getUserId();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountLockService.unlockAccount(eq(accountId), any(User.class)))
                .thenReturn(accountId);
        when(accountLockRepository.findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(
                eq(accountId), eq(AccountLockStatus.UNLOCKED)))
                .thenReturn(Optional.of(AccountLock.builder()
                        .user(targetUser)
                        .lockedBy(user)
                        .unlockedBy(user)
                        .status(AccountLockStatus.UNLOCKED)
                        .lockedAt(OffsetDateTime.now().minusMinutes(1))
                        .unlockedAt(OffsetDateTime.now())
                        .build()));

        // Act
        AccountLockResponse result = loginMonitoringService.unlockAccount(accountId);

        // Assert
        assertThat(result).isNotNull();
        verify(accountLockService, times(1)).unlockAccount(eq(accountId), any(User.class));
    }

    @Test
    @DisplayName("should handle lock account with null anomaly ID")
    void lockAccount_shouldHandleNullAnomalyId() {
        // Arrange
        UUID accountId = targetUser.getUserId();
        String reason = "Manual lock";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountLockService.lockAccount(
                eq(accountId),
                eq(null),
                eq(reason),
                any(User.class),
                eq(0),
                eq(0),
                eq(0),
                eq(false)))
                .thenReturn(accountId);
        when(accountLockRepository.findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(
                eq(accountId), eq(AccountLockStatus.LOCKED)))
                .thenReturn(Optional.of(AccountLock.builder()
                        .user(targetUser)
                        .lockedBy(user)
                        .status(AccountLockStatus.LOCKED)
                        .lockReason(reason)
                        .lockedAt(OffsetDateTime.now())
                        .build()));

        // Act
        AccountLockResponse result = loginMonitoringService.lockAccount(accountId, null, reason);

        // Assert
        assertThat(result).isNotNull();
        verify(accountLockService, times(1)).lockAccount(
                eq(accountId),
                eq(null),
                eq(reason),
                any(User.class),
                eq(0),
                eq(0),
                eq(0),
                eq(false));
    }
}
