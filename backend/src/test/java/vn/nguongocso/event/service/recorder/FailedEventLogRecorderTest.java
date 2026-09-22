package vn.nguongocso.event.service.recorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.entity.FailedEventLog;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.FailedEventLogRepository;
import vn.nguongocso.exception.BusinessException;

/**
 * Kiểm thử đơn vị cho FailedEventLogRecorder (NCL-05-CN-008).
 * Xác thực việc ghi nhận lịch sử các lần ghi sự kiện chuỗi cung ứng thất bại.
 */
@ExtendWith(MockitoExtension.class)
class FailedEventLogRecorderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FailedEventLogRepository failedEventLogRepository;

    @Mock
    private CustomUserDetails currentUser;

    @InjectMocks
    private FailedEventLogRecorder failedEventLogRecorder;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .userId(userId)
                .userName("testuser")
                .fullName("Test User")
                .build();
    }

    @Test
    void shouldRecordFailedAttemptSuccessfullyWhenUserExists() {
        when(currentUser.getUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UUID lotId = UUID.randomUUID();
        failedEventLogRecorder.recordFailedAttempt(
                lotId, "LOT-123", ChainEventType.HARVEST, "Lý do lỗi", currentUser);

        ArgumentCaptor<FailedEventLog> captor = ArgumentCaptor.forClass(FailedEventLog.class);
        verify(failedEventLogRepository).save(captor.capture());
        FailedEventLog saved = captor.getValue();
        assertThat(saved.getLotId()).isEqualTo(lotId);
        assertThat(saved.getLotCode()).isEqualTo("LOT-123");
        assertThat(saved.getEventType()).isEqualTo(ChainEventType.HARVEST);
        assertThat(saved.getFailureReason()).isEqualTo("Lý do lỗi");
        assertThat(saved.getUser()).isEqualTo(user);
    }

    @Test
    void shouldThrowBusinessExceptionWhenUserNotFound() {
        when(currentUser.getUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UUID lotId = UUID.randomUUID();
        assertThatThrownBy(() -> failedEventLogRecorder.recordFailedAttempt(
                lotId, "LOT-123", ChainEventType.HARVEST, "Lý do lỗi", currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy người dùng");
    }
}
