package vn.nguongocso.alert_reclaim_history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import vn.nguongocso.alert.enums.ActivityLogExportStatus;
import vn.nguongocso.alert.repository.ActivityLogExportJobRepository;
import vn.nguongocso.alert.service.impl.ActivityLogExportDispatcher;
import vn.nguongocso.alert.service.impl.ActivityLogExportWorker;

@ExtendWith(MockitoExtension.class)
class ActivityLogExportDispatcherTest {
    @Mock private ActivityLogExportJobRepository jobRepository;
    @Mock private ActivityLogExportWorker worker;
    @Mock private TaskExecutor taskExecutor;

    private ActivityLogExportDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new ActivityLogExportDispatcher(jobRepository, worker, taskExecutor,
                Clock.fixed(Instant.parse("2026-09-15T01:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(dispatcher, "leaseSeconds", 300L);
        ReflectionTestUtils.setField(dispatcher, "recoveryBatchSize", 20);
    }

    @Test
    void dispatch_shouldClaimBeforeSubmittingWork() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.claim(eq(jobId), any(String.class), any(LocalDateTime.class),
                any(LocalDateTime.class), eq(ActivityLogExportStatus.IN_PROGRESS))).thenReturn(1);
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);

        assertThat(dispatcher.dispatch(jobId)).isTrue();
        verify(taskExecutor).execute(task.capture());
        task.getValue().run();
        verify(worker).process(eq(jobId), any(String.class));
    }

    @Test
    void dispatch_shouldReleaseClaimWhenExecutorRejectsWork() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.claim(eq(jobId), any(String.class), any(LocalDateTime.class),
                any(LocalDateTime.class), eq(ActivityLogExportStatus.IN_PROGRESS))).thenReturn(1);
        org.mockito.Mockito.doThrow(new TaskRejectedException("Queue full"))
                .when(taskExecutor).execute(any(Runnable.class));

        assertThat(dispatcher.dispatch(jobId)).isFalse();
        verify(jobRepository).releaseClaim(eq(jobId), any(String.class));
    }

    @Test
    void recoverPendingJobs_shouldRedispatchUnclaimedAndExpiredJobs() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(jobRepository.findRecoverableJobIds(eq(ActivityLogExportStatus.IN_PROGRESS),
                any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(first, second));

        dispatcher.recoverPendingJobs();

        verify(jobRepository).claim(eq(first), any(String.class), any(LocalDateTime.class),
                any(LocalDateTime.class), eq(ActivityLogExportStatus.IN_PROGRESS));
        verify(jobRepository).claim(eq(second), any(String.class), any(LocalDateTime.class),
                any(LocalDateTime.class), eq(ActivityLogExportStatus.IN_PROGRESS));
    }
}
