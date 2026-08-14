package vn.nguongocso.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.backup.dto.request.BackupScheduleRequest;
import vn.nguongocso.backup.dto.response.BackupHistoryResponse;
import vn.nguongocso.backup.dto.response.BackupScheduleResponse;
import vn.nguongocso.backup.entity.BackupRestoreHistory;
import vn.nguongocso.backup.entity.BackupSchedule;
import vn.nguongocso.backup.enums.BackupOperationType;
import vn.nguongocso.backup.enums.BackupStatus;
import vn.nguongocso.backup.event.BackupScheduleChangedEvent;
import vn.nguongocso.backup.repository.BackupRestoreHistoryRepository;
import vn.nguongocso.backup.repository.BackupScheduleRepository;
import vn.nguongocso.backup.service.impl.BackupServiceImpl;
import vn.nguongocso.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class BackupServiceTest {

    @Mock
    private BackupScheduleRepository backupScheduleRepository;

    @Mock
    private BackupRestoreHistoryRepository backupRestoreHistoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TaskExecutor taskExecutor;

    @InjectMocks
    private BackupServiceImpl backupService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(UUID.randomUUID())
                .userName("admin")
                .fullName("System Administrator")
                .build();

        // Inject config values using ReflectionTestUtils
        ReflectionTestUtils.setField(backupService, "dbHost", "localhost");
        ReflectionTestUtils.setField(backupService, "dbPort", "3306");
        ReflectionTestUtils.setField(backupService, "dbName", "nguon_goc_so");
        ReflectionTestUtils.setField(backupService, "dbUsername", "root");
        ReflectionTestUtils.setField(backupService, "dbPassword", "password");
        ReflectionTestUtils.setField(backupService, "backupDir", "./backups");
        ReflectionTestUtils.setField(backupService, "mysqlDumpPath", "mysqldump");
        ReflectionTestUtils.setField(backupService, "retentionCount", 30);
    }

    @Test
    void configureSchedule_shouldSaveScheduleAndPublishEvent_whenCronIsValid() {
        // Arrange
        BackupScheduleRequest request = BackupScheduleRequest.builder()
                .cronExpression("0 0 2 * * ?")
                .description("Daily backup at 2 AM")
                .isActive(true)
                .build();

        BackupSchedule existingSchedule = new BackupSchedule();
        existingSchedule.setId(1);
        existingSchedule.setCronExpression("0 0 12 * * ?");
        existingSchedule.setActive(false);

        when(backupScheduleRepository.findFirstByIsActiveTrue()).thenReturn(Optional.empty());
        when(backupScheduleRepository.findById(1)).thenReturn(Optional.of(existingSchedule));
        when(backupScheduleRepository.save(any(BackupSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BackupScheduleResponse response = backupService.configureSchedule(request, mockUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCronExpression()).isEqualTo("0 0 2 * * ?");
        assertThat(response.getDescription()).isEqualTo("Daily backup at 2 AM");
        assertThat(response.isActive()).isTrue();

        verify(backupScheduleRepository).save(any(BackupSchedule.class));
        verify(eventPublisher).publishEvent(any(BackupScheduleChangedEvent.class));
    }

    @Test
    void configureSchedule_shouldThrowException_whenCronIsInvalid() {
        // Arrange
        BackupScheduleRequest request = BackupScheduleRequest.builder()
                .cronExpression("invalid-cron-expr")
                .description("Invalid cron")
                .isActive(true)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> backupService.configureSchedule(request, mockUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Định dạng biểu thức cron không hợp lệ");

        verify(backupScheduleRepository, never()).save(any(BackupSchedule.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void triggerManualBackup_shouldCreateRecordAndRunAsync_whenNoJobIsRunning() {
        // Arrange
        when(backupRestoreHistoryRepository.existsByStatus(BackupStatus.IN_PROGRESS)).thenReturn(false);
        when(backupRestoreHistoryRepository.save(any(BackupRestoreHistory.class))).thenAnswer(invocation -> {
            BackupRestoreHistory h = invocation.getArgument(0);
            h.setId(100);
            h.setCreatedAt(LocalDateTime.now());
            return h;
        });

        // Act
        BackupHistoryResponse response = backupService.triggerManualBackup(mockUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100);
        assertThat(response.getOperationType()).isEqualTo(BackupOperationType.BACKUP.name());
        assertThat(response.getStatus()).isEqualTo(BackupStatus.IN_PROGRESS.name());

        verify(backupRestoreHistoryRepository).save(any(BackupRestoreHistory.class));
        verify(taskExecutor).execute(any(Runnable.class));
    }

    @Test
    void triggerManualBackup_shouldThrowException_whenAnotherJobIsRunning() {
        // Arrange
        when(backupRestoreHistoryRepository.existsByStatus(BackupStatus.IN_PROGRESS)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> backupService.triggerManualBackup(mockUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Hệ thống đang có một tiến trình sao lưu hoặc khôi phục khác đang diễn ra");

        verify(backupRestoreHistoryRepository, never()).save(any(BackupRestoreHistory.class));
        verify(taskExecutor, never()).execute(any());
    }
}
