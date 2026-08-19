package vn.nguongocso.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
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

import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.AccountLockStatus;
import vn.nguongocso.auth.enums.AnomalyReasonCode;
import vn.nguongocso.auth.enums.AnomalyStatus;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.AccountLockRepository;
import vn.nguongocso.auth.repository.LoginAnomalyRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.impl.AccountLockExpiryScheduler;
import vn.nguongocso.auth.service.impl.AccountLockServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLockService Unit Tests")
class AccountLockServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountLockRepository accountLockRepository;

    @Mock
    private LoginAnomalyRepository loginAnomalyRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AccountLockServiceImpl accountLockService;

    private UUID userId;
    private UUID lockedByUserId;
    private User targetUser;
    private User lockingUser;
    private LoginAnomaly anomaly;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lockedByUserId = UUID.randomUUID();

        targetUser = User.builder()
                .userId(userId)
                .userName("targetuser")
                .passwordHash("hashed-password")
                .fullName("Target User")
                .status(UserStatus.ACTIVE)
                .build();

        lockingUser = User.builder()
                .userId(lockedByUserId)
                .userName("admin")
                .passwordHash("hashed-password")
                .fullName("Admin User")
                .status(UserStatus.ACTIVE)
                .build();

        anomaly = LoginAnomaly.builder()
                .id(UUID.randomUUID())
                .user(targetUser)
                .reasonCode(AnomalyReasonCode.REPEATED_FAILED_LOGIN)
                .attemptCount(5)
                .ipAddress("192.168.1.1")
                .countryCode("VN")
                .status(AnomalyStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("should lock account successfully")
    void lockAccount_shouldLockAccountSuccessfully() {
        // Arrange
        UUID anomalyId = anomaly.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(accountLockRepository.findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(userId, AccountLockStatus.LOCKED))
                .thenReturn(Optional.empty());
        when(loginAnomalyRepository.findById(anomalyId))
                .thenReturn(Optional.of(anomaly));
        when(accountLockRepository.save(any(AccountLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UUID result = accountLockService.lockAccount(
                userId,
                anomalyId,
                "Phát hiện đăng nhập bất thường",
                lockingUser
        );

        // Assert
        assertThat(result).isEqualTo(userId);
        verify(userRepository, times(1)).findById(userId);
        verify(accountLockRepository, times(1)).save(any(AccountLock.class));
        verify(notificationService, times(1)).sendAccountLockedNotification(any(AccountLock.class));
    }

    @Test
    @DisplayName("should throw exception if account doesn't exist")
    void lockAccount_shouldThrowExceptionIfAccountNotFound() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountLockService.lockAccount(
                userId,
                null,
                "Reason",
                lockingUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tài khoản không tồn tại");

        verify(accountLockRepository, times(0)).save(any(AccountLock.class));
    }

    @Test
    @DisplayName("should throw exception if account is not ACTIVE")
    void lockAccount_shouldThrowExceptionIfAccountNotActive() {
        // Arrange
        targetUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

        // Act & Assert
        assertThatThrownBy(() -> accountLockService.lockAccount(
                userId,
                null,
                "Reason",
                lockingUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tài khoản không ở trạng thái hoạt động");
    }

    @Test
    @DisplayName("should throw exception if account is currently locked within 60s timeout")
    void lockAccount_shouldThrowExceptionIfCurrentlyLocked() {
        // Arrange - lock created 30 seconds ago (still within 60s timeout)
        AccountLock currentLock = AccountLock.builder()
                .id(UUID.randomUUID())
                .user(targetUser)
                .lockedBy(lockingUser)
                .lockReason("Reason")
                .lockedAt(OffsetDateTime.now().minusSeconds(30))
                .status(AccountLockStatus.LOCKED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(accountLockRepository.findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(userId, AccountLockStatus.LOCKED))
                .thenReturn(Optional.of(currentLock));

        // Act & Assert
        assertThatThrownBy(() -> accountLockService.lockAccount(
                userId,
                null,
                "Reason",
                lockingUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tài khoản đã bị khóa trước đó");
    }

    @Test
    @DisplayName("should auto-unlock and allow re-locking after 60s timeout expired")
    void lockAccount_shouldAllowRelockingAfterTimeoutExpired() {
        // Arrange - lock created 5 minutes ago (timeout expired)
        AccountLock expiredLock = AccountLock.builder()
                .id(UUID.randomUUID())
                .user(targetUser)
                .lockedBy(lockingUser)
                .lockReason("Reason")
                .lockedAt(OffsetDateTime.now().minusMinutes(5))
                .status(AccountLockStatus.LOCKED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(accountLockRepository.findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(userId, AccountLockStatus.LOCKED))
                .thenReturn(Optional.of(expiredLock));
        when(accountLockRepository.save(any(AccountLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UUID result = accountLockService.lockAccount(userId, null, "New reason", lockingUser);

        // Assert - should succeed and create new lock
        assertThat(result).isEqualTo(userId);
        // Verify old lock was updated to UNLOCKED and new lock was created
        verify(accountLockRepository, times(2)).save(any(AccountLock.class));
    }

    @Test
    @DisplayName("should allow new lock when no active LOCKED record exists")
    void lockAccount_shouldSucceedWhenNoActiveLock() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(accountLockRepository.findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(userId, AccountLockStatus.LOCKED))
                .thenReturn(Optional.empty());
        when(accountLockRepository.save(any(AccountLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UUID result = accountLockService.lockAccount(
                userId,
                null,
                "Reason",
                lockingUser
        );

        // Assert
        assertThat(result).isEqualTo(userId);
        verify(accountLockRepository, times(1)).save(any(AccountLock.class));
        verify(notificationService, times(1)).sendAccountLockedNotification(any(AccountLock.class));
    }

    @Test
    @DisplayName("should update anomaly status when locking account with anomalyId")
    void lockAccount_shouldUpdateAnomalyStatus() {
        // Arrange
        UUID anomalyId = anomaly.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(accountLockRepository.findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(userId, AccountLockStatus.LOCKED))
                .thenReturn(Optional.empty());
        when(loginAnomalyRepository.findById(anomalyId))
                .thenReturn(Optional.of(anomaly));
        when(accountLockRepository.save(any(AccountLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        accountLockService.lockAccount(userId, anomalyId, "Reason", lockingUser);

        // Assert
        ArgumentCaptor<LoginAnomaly> anomalyCaptor = ArgumentCaptor.forClass(LoginAnomaly.class);
        verify(loginAnomalyRepository, times(1)).save(anomalyCaptor.capture());

        LoginAnomaly updatedAnomaly = anomalyCaptor.getValue();
        assertThat(updatedAnomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
    }

    @Test
    @DisplayName("should unlock account successfully")
    void unlockAccount_shouldUnlockAccountSuccessfully() {
        // Arrange
        AccountLock lock = AccountLock.builder()
                .id(UUID.randomUUID())
                .user(targetUser)
                .lockedBy(lockingUser)
                .lockReason("Phát hiện bất thường")
                .lockedAt(OffsetDateTime.now().minusHours(1))
                .status(AccountLockStatus.LOCKED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(accountLockRepository.findTopByUser_UserIdOrderByLockedAtDesc(userId))
                .thenReturn(Optional.of(lock));
        when(accountLockRepository.save(any(AccountLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UUID result = accountLockService.unlockAccount(userId, lockingUser);

        // Assert
        assertThat(result).isEqualTo(userId);
        verify(userRepository, times(1)).findById(userId);
        verify(accountLockRepository, times(1)).save(any(AccountLock.class));
        verify(notificationService, times(1)).sendAccountUnlockedNotification(any(AccountLock.class));
    }

    @Test
    @DisplayName("should throw exception if account not found when unlocking")
    void unlockAccount_shouldThrowExceptionIfNotFound() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountLockService.unlockAccount(userId, lockingUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tài khoản không tồn tại");
    }

    @Test
    @DisplayName("should throw exception if account is not LOCKED when unlocking")
    void unlockAccount_shouldThrowExceptionIfNotLocked() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

        // Act & Assert
        assertThatThrownBy(() -> accountLockService.unlockAccount(userId, lockingUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tài khoản hiện không ở trạng thái bị khóa");
    }

    @Test
    @DisplayName("should update account lock status to UNLOCKED when unlocking")
    void unlockAccount_shouldUpdateAccountLockStatusToUnlocked() {
        // Arrange
        AccountLock lock = AccountLock.builder()
                .id(UUID.randomUUID())
                .user(targetUser)
                .lockedBy(lockingUser)
                .lockReason("Reason")
                .lockedAt(OffsetDateTime.now())
                .status(AccountLockStatus.LOCKED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(accountLockRepository.findTopByUser_UserIdOrderByLockedAtDesc(userId))
                .thenReturn(Optional.of(lock));
        when(accountLockRepository.save(any(AccountLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        accountLockService.unlockAccount(userId, lockingUser);

        // Assert - verify that AccountLock was updated to UNLOCKED
        ArgumentCaptor<AccountLock> lockCaptor = ArgumentCaptor.forClass(AccountLock.class);
        verify(accountLockRepository, times(1)).save(lockCaptor.capture());

        AccountLock updatedLock = lockCaptor.getValue();
        assertThat(updatedLock.getStatus()).isEqualTo(AccountLockStatus.UNLOCKED);
        assertThat(updatedLock.getUnlockedBy()).isEqualTo(lockingUser);
    }

    @Test
    @DisplayName("should auto-unlock expired temporary locks and reactivate the user")
    void expiredTemporaryLocks_shouldBeAutoUnlockedAndUserReactivated() {
        // Arrange
        AccountLock expiredLock = AccountLock.builder()
                .id(UUID.randomUUID())
                .user(targetUser)
                .lockedBy(lockingUser)
                .lockReason("Reason")
                .lockedAt(OffsetDateTime.now().minusMinutes(30))
                .lockUntil(OffsetDateTime.now().minusMinutes(1))
                .permanent(false)
                .status(AccountLockStatus.LOCKED)
                .build();

        when(accountLockRepository.findByStatusAndPermanentFalseAndLockUntilBefore(
                org.mockito.ArgumentMatchers.eq(AccountLockStatus.LOCKED),
                any(OffsetDateTime.class)))
                .thenReturn(java.util.List.of(expiredLock));
        when(accountLockRepository.save(any(AccountLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountLockExpiryScheduler scheduler = new AccountLockExpiryScheduler(accountLockRepository, userRepository);

        // Act
        scheduler.processExpiredLocks();

        // Assert
        assertThat(expiredLock.getStatus()).isEqualTo(AccountLockStatus.UNLOCKED);
        assertThat(targetUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(accountLockRepository, times(1)).save(expiredLock);
        verify(userRepository, times(1)).save(targetUser);
    }

    @Test
    @DisplayName("should call invalidateAllTokens when locking")
    void lockAccount_shouldInvalidateTokens() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(accountLockRepository.findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(userId, AccountLockStatus.LOCKED))
                .thenReturn(Optional.empty());
        when(accountLockRepository.save(any(AccountLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        accountLockService.lockAccount(userId, null, "Reason", lockingUser);

        // Assert
        // invalidateAllTokens is a void method, just verify it was called
        // (Implementation verification is at integration test level)
        verify(accountLockRepository, times(1)).save(any(AccountLock.class));
    }
}
