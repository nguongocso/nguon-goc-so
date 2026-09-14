package vn.nguongocso.alert_reclaim_history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.alert.entity.ActivityLogExportItem;
import vn.nguongocso.alert.entity.ActivityLogExportJob;
import vn.nguongocso.alert.enums.ActivityLogExportStatus;
import vn.nguongocso.alert.repository.ActivityLogExportItemRepository;
import vn.nguongocso.alert.repository.ActivityLogExportJobRepository;
import vn.nguongocso.alert.service.impl.ActivityLogCsvWriter;
import vn.nguongocso.alert.service.impl.ActivityLogExportValueSanitizer;
import vn.nguongocso.alert.service.impl.ActivityLogExportWorker;
import vn.nguongocso.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class ActivityLogExportWorkerTest {
    @Mock private ActivityLogExportJobRepository jobRepository;
    @Mock private ActivityLogExportItemRepository itemRepository;
    @Mock private NotificationService notificationService;

    @TempDir
    Path tempDirectory;

    @Test
    void process_shouldCreateFileCompleteJobAndNotifyRequester() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ActivityLogExportJob job = ActivityLogExportJob.builder()
                .id(jobId).organizationId(UUID.randomUUID()).requestedBy(requesterId)
                .requestedByUsername("manager").requestedByRole("VT-02")
                .status(ActivityLogExportStatus.IN_PROGRESS).recordCount(1L)
                .createdAt(LocalDateTime.of(2026, 9, 14, 10, 0)).build();
        ActivityLogExportItem item = ActivityLogExportItem.builder()
                .jobId(jobId).sequenceNo(0L).occurredAt(LocalDateTime.of(2026, 9, 14, 9, 0))
                .actorName("Nguyễn Văn An").actorUsername("manager").actorRole("VT-02")
                .actionType("UPDATE_LOT").objectType("LOT").objectIdentifier("LOT-1")
                .beforeValue("{\"password\":\"should-not-leak\",\"status\":\"OLD\"}")
                .afterValue("{\"status\":\"NEW\"}").build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(itemRepository.findByJobId(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        ActivityLogCsvWriter csvWriter = new ActivityLogCsvWriter(
                new ActivityLogExportValueSanitizer(new ObjectMapper()));
        ActivityLogExportWorker worker = new ActivityLogExportWorker(jobRepository, itemRepository, csvWriter,
                notificationService, Clock.fixed(Instant.parse("2026-09-14T03:30:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(worker, "storageDirectory", tempDirectory.toString());

        worker.process(jobId);

        assertThat(job.getStatus()).isEqualTo(ActivityLogExportStatus.SUCCESS);
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getFileName()).endsWith(".csv");
        Path output = Path.of(job.getFilePath());
        assertThat(output).exists().isRegularFile();
        String csv = Files.readString(output, StandardCharsets.UTF_8);
        assertThat(csv).contains("UPDATE_LOT", "OLD", "NEW", "***")
                .doesNotContain("should-not-leak");
        verify(jobRepository).save(job);
        verify(notificationService).sendActivityLogExportReadyNotification(jobId, requesterId);
    }

    @Test
    void process_shouldCompleteJobSuccessfully_evenWhenNotificationFails() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ActivityLogExportJob job = ActivityLogExportJob.builder()
                .id(jobId).organizationId(UUID.randomUUID()).requestedBy(requesterId)
                .requestedByUsername("manager").requestedByRole("VT-02")
                .status(ActivityLogExportStatus.IN_PROGRESS).recordCount(1L)
                .createdAt(LocalDateTime.of(2026, 9, 14, 10, 0)).build();
        ActivityLogExportItem item = ActivityLogExportItem.builder()
                .jobId(jobId).sequenceNo(0L).occurredAt(LocalDateTime.of(2026, 9, 14, 9, 0))
                .actorName("Nguyễn Văn An").actorUsername("manager").actorRole("VT-02")
                .actionType("UPDATE_LOT").objectType("LOT").objectIdentifier("LOT-1").build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(itemRepository.findByJobId(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));
        org.mockito.Mockito.doThrow(new RuntimeException("Notification service down"))
                .when(notificationService).sendActivityLogExportReadyNotification(jobId, requesterId);

        ActivityLogCsvWriter csvWriter = new ActivityLogCsvWriter(
                new ActivityLogExportValueSanitizer(new ObjectMapper()));
        ActivityLogExportWorker worker = new ActivityLogExportWorker(jobRepository, itemRepository, csvWriter,
                notificationService, Clock.fixed(Instant.parse("2026-09-14T03:30:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(worker, "storageDirectory", tempDirectory.toString());

        worker.process(jobId);

        assertThat(job.getStatus()).isEqualTo(ActivityLogExportStatus.SUCCESS);
        assertThat(job.getCompletedAt()).isNotNull();
        Path output = Path.of(job.getFilePath());
        assertThat(output).exists().isRegularFile();
        verify(jobRepository).save(job);
    }

    @Test
    void markDispatchFailed_shouldCompleteInProgressJobAsFailed() {
        UUID jobId = UUID.randomUUID();
        ActivityLogExportJob job = ActivityLogExportJob.builder()
                .id(jobId).status(ActivityLogExportStatus.IN_PROGRESS).recordCount(1L).build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        ActivityLogExportWorker worker = new ActivityLogExportWorker(jobRepository, itemRepository,
                new ActivityLogCsvWriter(new ActivityLogExportValueSanitizer(new ObjectMapper())),
                notificationService, Clock.fixed(Instant.parse("2026-09-14T03:30:00Z"), ZoneOffset.UTC));

        worker.markDispatchFailed(jobId);

        assertThat(job.getStatus()).isEqualTo(ActivityLogExportStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Không thể khởi tạo xử lý tệp nhật ký nền.");
        verify(jobRepository).save(job);
    }
}
