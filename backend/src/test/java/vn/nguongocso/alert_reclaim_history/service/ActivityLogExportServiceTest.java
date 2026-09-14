package vn.nguongocso.alert_reclaim_history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import vn.nguongocso.alert.dto.request.ActivityLogExportFilterRequest;
import vn.nguongocso.alert.dto.response.ActivityLogExportPreviewResponse;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.alert.service.impl.ActivityLogExportServiceImpl;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ActivityLogExportServiceTest {
    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private CustomUserDetails currentUser;

    private ActivityLogExportServiceImpl service;
    private UUID organizationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-09-14T03:00:00Z"),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new ActivityLogExportServiceImpl(activityLogRepository, clock);
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        when(currentUser.getRoleCode()).thenReturn("VT-02");
        lenient().when(currentUser.getOrganizationId()).thenReturn(organizationId);
    }

    @Test
    void preview_shouldCountWithSharedTenantScopedSpecification() {
        ActivityLogExportFilterRequest filter = ActivityLogExportFilterRequest.builder()
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 14))
                .action("UPDATE_PRODUCTION_LOT")
                .actorName("nguyen")
                .objectType("PRODUCTION_LOT")
                .build();
        when(activityLogRepository.count(any(Specification.class))).thenReturn(12L);

        ActivityLogExportPreviewResponse response = service.preview(filter, currentUser);

        assertThat(response.getCount()).isEqualTo(12L);
        assertThat(response.getMode()).isEqualTo("DIRECT");
        verify(activityLogRepository).count(any(Specification.class));
    }

    @Test
    void exportCsv_shouldCreateFixedSchemaAndWriteAuditAfterSnapshot() {
        when(currentUser.getUserId()).thenReturn(userId);
        when(currentUser.getUsername()).thenReturn("manager");
        when(currentUser.getFullName()).thenReturn("Quản lý HTX");

        ActivityLog log = ActivityLog.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(UUID.randomUUID())
                .username("=danger")
                .fullName("Nguyễn Văn A")
                .action("UPDATE_PRODUCTION_LOT")
                .description("Nội dung nội bộ không được xuất")
                .entityType("PRODUCTION_LOT")
                .entityId(UUID.randomUUID().toString())
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.of(2026, 9, 14, 9, 30))
                .build();
        when(activityLogRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(log));

        byte[] result = service.exportCsv(ActivityLogExportFilterRequest.builder().build(), currentUser);
        String csv = new String(result, StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\ufeffoccurredAt,actorName,actorUsername,actorRole,actionType,objectType,objectIdentifier,beforeValue,afterValue");
        assertThat(csv).contains("2026-09-14T09:30", "Nguyễn Văn A", "'=danger", "UPDATE_PRODUCTION_LOT");
        assertThat(csv).doesNotContain("127.0.0.1", "Nội dung nội bộ không được xuất");
        ArgumentCaptor<ActivityLog> auditCaptor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).saveAndFlush(auditCaptor.capture());
        ActivityLog audit = auditCaptor.getValue();
        assertThat(audit.getOrganizationId()).isEqualTo(organizationId);
        assertThat(audit.getAction()).isEqualTo("EXPORT_ACTIVITY_LOG");
        assertThat(audit.getDescription()).contains("recordCount=1", "status=SUCCESS");

        InOrder callOrder = inOrder(activityLogRepository);
        callOrder.verify(activityLogRepository).findAll(any(Specification.class), any(Sort.class));
        callOrder.verify(activityLogRepository).saveAndFlush(audit);
    }

    @Test
    void exportCsv_shouldRejectEmptyResultWithoutWritingAudit() {
        when(activityLogRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.exportCsv(
                ActivityLogExportFilterRequest.builder().build(),
                currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không có nhật ký hoạt động trong phạm vi lọc.");

        verify(activityLogRepository, never()).saveAndFlush(any(ActivityLog.class));
    }

    @Test
    void preview_shouldRejectInvalidDateRange() {
        ActivityLogExportFilterRequest filter = ActivityLogExportFilterRequest.builder()
                .startDate(LocalDate.of(2026, 9, 15))
                .endDate(LocalDate.of(2026, 9, 14))
                .build();

        assertThatThrownBy(() -> service.preview(filter, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ngày bắt đầu không được sau ngày kết thúc.");

        verify(activityLogRepository, never()).count(any(Specification.class));
    }

    @Test
    void preview_shouldRejectWrongRoleAtServiceBoundary() {
        when(currentUser.getRoleCode()).thenReturn("VT-03");

        assertThatThrownBy(() -> service.preview(
                ActivityLogExportFilterRequest.builder().build(),
                currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Chỉ Quản lý tổ chức (VT-02) mới được xuất nhật ký hoạt động.");
    }
}
