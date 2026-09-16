package vn.nguongocso.alert_reclaim_history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
        String token = UUID.randomUUID().toString();
        UUID requesterId = UUID.randomUUID();
        ActivityLogExportJob job = ActivityLogExportJob.builder()
                .id(jobId).organizationId(UUID.randomUUID()).requestedBy(requesterId)
                .requestedByUsername("manager").requestedByRole("VT-02")
                .processingToken(token)
                .status(ActivityLogExportStatus.IN_PROGRESS).recordCount(1L)
                .createdAt(LocalDateTime.of(2026, 9, 14, 10, 0)).build();
        ActivityLogExportItem item = ActivityLogExportItem.builder()
                .jobId(jobId).sequenceNo(0L).occurredAt(LocalDateTime.of(2026, 9, 14, 9, 0))
                .actorName("Nguyễn Văn An").actorUsername("manager").actorRole("VT-02")
                .actionType("UPDATE_PRODUCTION_LOT").objectType("PRODUCTION_LOT").objectIdentifier("LOT-1")
                .beforeValue("{\"password\":\"should-not-leak\",\"status\":\"OLD\"}")
                .afterValue("{\"status\":\"NEW\"}").build();
        when(jobRepository.findByIdAndProcessingTokenAndStatus(jobId, token, ActivityLogExportStatus.IN_PROGRESS))
                .thenReturn(Optional.of(job));
        when(jobRepository.renewLease(eq(jobId), eq(token), any(LocalDateTime.class),
                eq(ActivityLogExportStatus.IN_PROGRESS))).thenReturn(1);
        when(itemRepository.findByJobIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                any(UUID.class), eq(-1L), any(Pageable.class))).thenReturn(List.of(item));

        ActivityLogCsvWriter csvWriter = new ActivityLogCsvWriter(
                new ActivityLogExportValueSanitizer(new ObjectMapper()));
        ActivityLogExportWorker worker = new ActivityLogExportWorker(jobRepository, itemRepository, csvWriter,
                notificationService, Clock.fixed(Instant.parse("2026-09-14T03:30:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(worker, "storageDirectory", tempDirectory.toString());
        ReflectionTestUtils.setField(worker, "leaseSeconds", 300L);

        worker.process(jobId, token);

        assertThat(job.getStatus()).isEqualTo(ActivityLogExportStatus.SUCCESS);
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getFileName()).endsWith(".csv");
        Path output = Path.of(job.getFilePath());
        assertThat(output).exists().isRegularFile();
        String csv = Files.readString(output, StandardCharsets.UTF_8);
        assertThat(csv).contains("Cập nhật lô sản xuất", "Lô sản xuất", "OLD", "NEW", "***")
                .doesNotContain("should-not-leak");
        verify(jobRepository).save(job);
        verify(notificationService).sendActivityLogExportReadyNotification(jobId, requesterId);
    }

    @Test
    void process_shouldCompleteJobSuccessfully_evenWhenNotificationFails() throws Exception {
        UUID jobId = UUID.randomUUID();
        String token = UUID.randomUUID().toString();
        UUID requesterId = UUID.randomUUID();
        ActivityLogExportJob job = ActivityLogExportJob.builder()
                .id(jobId).organizationId(UUID.randomUUID()).requestedBy(requesterId)
                .requestedByUsername("manager").requestedByRole("VT-02")
                .processingToken(token)
                .status(ActivityLogExportStatus.IN_PROGRESS).recordCount(1L)
                .createdAt(LocalDateTime.of(2026, 9, 14, 10, 0)).build();
        ActivityLogExportItem item = ActivityLogExportItem.builder()
                .jobId(jobId).sequenceNo(0L).occurredAt(LocalDateTime.of(2026, 9, 14, 9, 0))
                .actorName("Nguyễn Văn An").actorUsername("manager").actorRole("VT-02")
                .actionType("UPDATE_LOT").objectType("LOT").objectIdentifier("LOT-1").build();
        when(jobRepository.findByIdAndProcessingTokenAndStatus(jobId, token, ActivityLogExportStatus.IN_PROGRESS))
                .thenReturn(Optional.of(job));
        when(jobRepository.renewLease(eq(jobId), eq(token), any(LocalDateTime.class),
                eq(ActivityLogExportStatus.IN_PROGRESS))).thenReturn(1);
        when(itemRepository.findByJobIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                any(UUID.class), eq(-1L), any(Pageable.class))).thenReturn(List.of(item));
        org.mockito.Mockito.doThrow(new RuntimeException("Notification service down"))
                .when(notificationService).sendActivityLogExportReadyNotification(jobId, requesterId);

        ActivityLogCsvWriter csvWriter = new ActivityLogCsvWriter(
                new ActivityLogExportValueSanitizer(new ObjectMapper()));
        ActivityLogExportWorker worker = new ActivityLogExportWorker(jobRepository, itemRepository, csvWriter,
                notificationService, Clock.fixed(Instant.parse("2026-09-14T03:30:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(worker, "storageDirectory", tempDirectory.toString());
        ReflectionTestUtils.setField(worker, "leaseSeconds", 300L);

        worker.process(jobId, token);

        assertThat(job.getStatus()).isEqualTo(ActivityLogExportStatus.SUCCESS);
        assertThat(job.getCompletedAt()).isNotNull();
        Path output = Path.of(job.getFilePath());
        assertThat(output).exists().isRegularFile();
        verify(jobRepository).save(job);
    }

    @Test
    void process_shouldIgnoreJobWhenWorkerDoesNotOwnLease() {
        UUID jobId = UUID.randomUUID();
        String token = UUID.randomUUID().toString();
        when(jobRepository.findByIdAndProcessingTokenAndStatus(jobId, token, ActivityLogExportStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        ActivityLogExportWorker worker = new ActivityLogExportWorker(jobRepository, itemRepository,
                new ActivityLogCsvWriter(new ActivityLogExportValueSanitizer(new ObjectMapper())),
                notificationService, Clock.fixed(Instant.parse("2026-09-14T03:30:00Z"), ZoneOffset.UTC));

        worker.process(jobId, token);

        verify(itemRepository, never()).findByJobIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                any(), any(), any());
        verify(jobRepository, never()).save(any());
    }
}
