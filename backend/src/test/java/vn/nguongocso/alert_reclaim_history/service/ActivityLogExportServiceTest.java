package vn.nguongocso.alert_reclaim_history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.alert.dto.request.ActivityLogExportFilterRequest;
import vn.nguongocso.alert.dto.response.ActivityLogExportResult;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.entity.ActivityLogExportJob;
import vn.nguongocso.alert.enums.ActivityLogExportStatus;
import vn.nguongocso.alert.repository.ActivityLogExportItemRepository;
import vn.nguongocso.alert.repository.ActivityLogExportJobRepository;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.alert.service.impl.ActivityLogCsvWriter;
import vn.nguongocso.alert.service.impl.ActivityLogExportValueSanitizer;
import vn.nguongocso.alert.service.impl.ActivityLogExportServiceImpl;
import vn.nguongocso.alert.service.impl.ActivityLogExportDispatcher;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ActivityLogExportServiceTest {
    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private ActivityLogExportJobRepository jobRepository;
    @Mock private ActivityLogExportItemRepository itemRepository;
    @Mock private ActivityLogExportDispatcher dispatcher;
    @Mock private CustomUserDetails currentUser;

    private ActivityLogExportServiceImpl service;
    private UUID organizationId;

    @TempDir
    Path tempDirectory;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-14T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new ActivityLogExportServiceImpl(activityLogRepository, jobRepository, itemRepository,
                csvWriter(), dispatcher, clock);
        organizationId = UUID.randomUUID();
        lenient().when(currentUser.getRoleCode()).thenReturn("VT-02");
        lenient().when(currentUser.getOrganizationId()).thenReturn(organizationId);
    }

    @Test
    void preview_shouldChooseDirectOrAsyncFromCount() {
        when(activityLogRepository.count(any(Specification.class))).thenReturn(10_000L, 10_001L);
        ActivityLogExportFilterRequest filter = ActivityLogExportFilterRequest.builder().build();

        assertThat(service.preview(filter, currentUser).getMode()).isEqualTo("DIRECT");
        assertThat(service.preview(filter, currentUser).getMode()).isEqualTo("ASYNC");
    }

    @Test
    void preview_shouldRejectInvalidDateRange() {
        ActivityLogExportFilterRequest filter = ActivityLogExportFilterRequest.builder()
                .startDate(LocalDate.of(2026, 9, 15)).endDate(LocalDate.of(2026, 9, 14)).build();
        assertThatThrownBy(() -> service.preview(filter, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ngày bắt đầu không được sau ngày kết thúc.");
    }

    @Test
    void requestExport_shouldCreateDirectCsvAndAudit() {
        when(currentUser.getUserId()).thenReturn(UUID.randomUUID());
        when(currentUser.getUsername()).thenReturn("manager");
        when(currentUser.getFullName()).thenReturn("Quản lý HTX");
        when(activityLogRepository.count(any(Specification.class))).thenReturn(1L);
        ActivityLog log = log("=danger");
        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        ActivityLogExportResult result = service.requestExport(ActivityLogExportFilterRequest.builder().build(), currentUser);

        assertThat(result.getMode()).isEqualTo("DIRECT");
        String csv = new String(result.getCsvBytes(), StandardCharsets.UTF_8);
        assertThat(csv).contains("' =danger").contains("VT-03").contains("before").contains("after");
        verify(activityLogRepository).saveAndFlush(any(ActivityLog.class));
        verify(jobRepository, never()).save(any());
    }

    @Test
    void requestExport_shouldCreateAsyncSnapshotWhenOverLimit() {
        when(currentUser.getUserId()).thenReturn(UUID.randomUUID());
        when(currentUser.getUsername()).thenReturn("manager");
        when(currentUser.getFullName()).thenReturn("Quản lý HTX");
        when(activityLogRepository.count(any(Specification.class))).thenReturn(10_001L);
        when(itemRepository.snapshotFromActivityLogs(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(jobRepository.save(any(ActivityLogExportJob.class))).thenAnswer(invocation -> {
            ActivityLogExportJob job = invocation.getArgument(0);
            if (job.getId() == null) job.setId(UUID.randomUUID());
            return job;
        });

        ActivityLogExportResult result = service.requestExport(ActivityLogExportFilterRequest.builder().build(), currentUser);

        assertThat(result.getMode()).isEqualTo("ASYNC");
        assertThat(result.getJob().getExportId()).isNotNull();
        assertThat(result.getJob().getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(result.getJob().getRecordCount()).isEqualTo(1);
        verify(itemRepository).snapshotFromActivityLogs(any(), any(), any(), any(), any(), any(), any());
        verify(dispatcher).dispatch(result.getJob().getExportId());
        verify(activityLogRepository).saveAndFlush(any(ActivityLog.class));
    }

    @Test
    void requestExport_shouldReturnDirectCsv_whenCountEqualsDirectLimit() {
        when(currentUser.getUserId()).thenReturn(UUID.randomUUID());
        when(currentUser.getUsername()).thenReturn("manager");
        when(currentUser.getFullName()).thenReturn("Quản lý HTX");
        when(activityLogRepository.count(any(Specification.class))).thenReturn(10_000L);
        ActivityLog log = log("manager");
        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        ActivityLogExportResult result = service.requestExport(ActivityLogExportFilterRequest.builder().build(), currentUser);

        assertThat(result.getMode()).isEqualTo("DIRECT");
        assertThat(result.getCsvBytes()).isNotNull();
        verify(activityLogRepository).saveAndFlush(any(ActivityLog.class));
        verify(jobRepository, never()).save(any());
    }

    @Test
    void requestExport_shouldReturnAsyncJob_whenCountEqualsDirectLimitPlusOne() {
        when(currentUser.getUserId()).thenReturn(UUID.randomUUID());
        when(currentUser.getUsername()).thenReturn("manager");
        when(currentUser.getFullName()).thenReturn("Quản lý HTX");
        when(activityLogRepository.count(any(Specification.class))).thenReturn(10_001L);
        when(itemRepository.snapshotFromActivityLogs(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(10_001);
        UUID generatedJobId = UUID.randomUUID();
        when(jobRepository.save(any(ActivityLogExportJob.class))).thenAnswer(invocation -> {
            ActivityLogExportJob job = invocation.getArgument(0);
            if (job.getId() == null) job.setId(generatedJobId);
            return job;
        });

        ActivityLogExportResult result = service.requestExport(ActivityLogExportFilterRequest.builder().build(), currentUser);

        assertThat(result.getMode()).isEqualTo("ASYNC");
        assertThat(result.getJob().getExportId()).isEqualTo(generatedJobId);
        assertThat(result.getJob().getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(result.getJob().getRecordCount()).isEqualTo(10_001L);
        verify(itemRepository).snapshotFromActivityLogs(any(), any(), any(), any(), any(), any(), any());
        verify(dispatcher).dispatch(generatedJobId);
        verify(activityLogRepository).saveAndFlush(any(ActivityLog.class));
    }

    @Test
    void validateRequest_shouldPass_whenDateRangeEqualsMaxRangeDays() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = start.plusDays(365);
        ActivityLogExportFilterRequest filter = ActivityLogExportFilterRequest.builder()
                .startDate(start).endDate(end).build();
        when(activityLogRepository.count(any(Specification.class))).thenReturn(0L);

        assertThatThrownBy(() -> service.requestExport(filter, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không có nhật ký hoạt động trong phạm vi lọc.");
    }

    @Test
    void validateRequest_shouldThrowBadRequest_whenDateRangeExceedsMaxRangeDays() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = start.plusDays(366);
        ActivityLogExportFilterRequest filter = ActivityLogExportFilterRequest.builder()
                .startDate(start).endDate(end).build();

        assertThatThrownBy(() -> service.requestExport(filter, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Khoảng thời gian xuất nhật ký không được vượt quá 365 ngày.");

        verify(activityLogRepository, never()).count(any(Specification.class));
        verify(activityLogRepository, never()).saveAndFlush(any());
        verify(jobRepository, never()).save(any());
    }

    @Test
    void requestExport_shouldKeepAcceptedJobRecoverable_whenInitialDispatchCannotStart() {
        when(currentUser.getUserId()).thenReturn(UUID.randomUUID());
        when(currentUser.getUsername()).thenReturn("manager");
        when(currentUser.getFullName()).thenReturn("Quản lý HTX");
        when(activityLogRepository.count(any(Specification.class))).thenReturn(15_000L);
        when(itemRepository.snapshotFromActivityLogs(any(), any(), any(), any(), any(), any(), any())).thenReturn(15_000);
        UUID generatedJobId = UUID.randomUUID();
        when(jobRepository.save(any(ActivityLogExportJob.class))).thenAnswer(invocation -> {
            ActivityLogExportJob job = invocation.getArgument(0);
            if (job.getId() == null) job.setId(generatedJobId);
            return job;
        });
        when(dispatcher.dispatch(generatedJobId)).thenReturn(false);

        ActivityLogExportResult result = service.requestExport(ActivityLogExportFilterRequest.builder().build(), currentUser);

        assertThat(result.getMode()).isEqualTo("ASYNC");
        assertThat(result.getJob().getStatus()).isEqualTo("IN_PROGRESS");
        verify(dispatcher).dispatch(generatedJobId);
    }

    @Test
    void requestExport_shouldRejectEmptyResultWithoutAudit() {
        when(activityLogRepository.count(any(Specification.class))).thenReturn(0L);
        assertThatThrownBy(() -> service.requestExport(ActivityLogExportFilterRequest.builder().build(), currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không có nhật ký hoạt động trong phạm vi lọc.");
        verify(activityLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void preview_shouldRejectWrongRoleAndMissingTenant() {
        when(currentUser.getRoleCode()).thenReturn("VT-03");
        assertThatThrownBy(() -> service.preview(ActivityLogExportFilterRequest.builder().build(), currentUser))
                .isInstanceOf(BusinessException.class);

        when(currentUser.getRoleCode()).thenReturn("VT-02");
        when(currentUser.getOrganizationId()).thenReturn(null);
        assertThatThrownBy(() -> service.preview(ActivityLogExportFilterRequest.builder().build(), currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Người dùng không thuộc tổ chức nào.");
    }

    @Test
    void getJob_shouldHideJobFromAnotherTenant() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByIdAndOrganizationId(jobId, organizationId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getJob(jobId, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy yêu cầu xuất nhật ký.");
    }

    @Test
    void getDownload_shouldRejectInProgressJob() {
        UUID jobId = UUID.randomUUID();
        ActivityLogExportJob job = exportJob(jobId, ActivityLogExportStatus.IN_PROGRESS);
        when(jobRepository.findByIdAndOrganizationId(jobId, organizationId))
                .thenReturn(java.util.Optional.of(job));

        assertThatThrownBy(() -> service.getDownload(jobId, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tệp nhật ký đang được xử lý.");
    }

    @Test
    void getDownload_shouldReturnSuccessfulFileInsideConfiguredStorage() throws Exception {
        UUID jobId = UUID.randomUUID();
        Path file = tempDirectory.resolve("activity-logs.csv");
        Files.writeString(file, "occurredAt\r\n", StandardCharsets.UTF_8);
        ActivityLogExportJob job = exportJob(jobId, ActivityLogExportStatus.SUCCESS);
        job.setFileName(file.getFileName().toString());
        job.setFilePath(file.toString());
        job.setFileSize(Files.size(file));
        when(jobRepository.findByIdAndOrganizationId(jobId, organizationId))
                .thenReturn(java.util.Optional.of(job));
        ReflectionTestUtils.setField(service, "storageDirectory", tempDirectory.toString());

        assertThat(service.getDownload(jobId, currentUser).getPath()).isEqualTo(file.toAbsolutePath().normalize());
    }

    @Test
    void csvWriter_shouldProtectFormulaAndExcludeSensitiveFields() {
        ActivityLog log = log("=cmd");
        log.setBeforeValue("{\"status\":\"OLD\",\"password\":\"plain-text\",\"nested\":{\"access_token\":\"abc\"}}");
        log.setAfterValue("credential=my-secret;status=NEW");
        String csv = new String(csvWriter().writeActivities(List.of(log)), StandardCharsets.UTF_8);
        assertThat(csv).contains("' =cmd", "Người ghi sự kiện (VT-03)", "Dữ liệu trước", "Dữ liệu sau");
        assertThat(csv).contains("***", "OLD", "NEW");
        assertThat(csv).doesNotContain(log.getOrganizationId().toString(), log.getUserId().toString(),
                "plain-text", "my-secret", "abc");
    }

    private ActivityLogCsvWriter csvWriter() {
        return new ActivityLogCsvWriter(new ActivityLogExportValueSanitizer(new ObjectMapper()));
    }

    private ActivityLogExportJob exportJob(UUID jobId, ActivityLogExportStatus status) {
        return ActivityLogExportJob.builder().id(jobId).organizationId(organizationId)
                .requestedBy(UUID.randomUUID()).requestedByUsername("manager").requestedByRole("VT-02")
                .status(status).recordCount(1L).createdAt(LocalDateTime.now()).build();
    }

    private ActivityLog log(String username) {
        return ActivityLog.builder().id(UUID.randomUUID()).organizationId(organizationId)
                .userId(UUID.randomUUID()).username(" " + username).fullName("Nguyễn Văn A")
                .actorRole("VT-03").action("UPDATE_PRODUCTION_LOT").description("secret")
                .entityType("PRODUCTION_LOT").entityId("LOT-01")
                .beforeValue("before").afterValue("after")
                .createdAt(LocalDateTime.of(2026, 9, 14, 9, 30)).build();
    }
}
