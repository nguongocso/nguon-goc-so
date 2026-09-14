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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

        lenient().when(currentUser.getRoleCode()).thenReturn("VT-02");
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
    void exportCsv_shouldUseBoundedPageableAndWriteAuditAfterSnapshot() {
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

        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        byte[] result = service.exportCsv(ActivityLogExportFilterRequest.builder().build(), currentUser);
        String csv = new String(result, StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\ufeffoccurredAt,actorName,actorUsername,actorRole,actionType,objectType,objectIdentifier,beforeValue,afterValue");
        assertThat(csv).contains("2026-09-14T09:30", "Nguyễn Văn A", "'=danger", "UPDATE_PRODUCTION_LOT");
        assertThat(csv).doesNotContain("127.0.0.1", "Nội dung nội bộ không được xuất");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(activityLogRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageSize()).isEqualTo(ActivityLogExportServiceImpl.MAX_DIRECT_EXPORT_RECORDS + 1);
        assertThat(capturedPageable.getPageNumber()).isZero();

        ArgumentCaptor<ActivityLog> auditCaptor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).saveAndFlush(auditCaptor.capture());
        ActivityLog audit = auditCaptor.getValue();
        assertThat(audit.getOrganizationId()).isEqualTo(organizationId);
        assertThat(audit.getAction()).isEqualTo("EXPORT_ACTIVITY_LOG");
        assertThat(audit.getDescription()).contains("recordCount=1", "status=SUCCESS");

        InOrder callOrder = inOrder(activityLogRepository);
        callOrder.verify(activityLogRepository).findAll(any(Specification.class), any(Pageable.class));
        callOrder.verify(activityLogRepository).saveAndFlush(audit);
    }

    @Test
    void exportCsv_shouldRejectEmptyResultWithoutWritingAudit() {
        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        assertThatThrownBy(() -> service.exportCsv(
                ActivityLogExportFilterRequest.builder().build(),
                currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không có nhật ký hoạt động trong phạm vi lọc.");

        verify(activityLogRepository, never()).saveAndFlush(any(ActivityLog.class));
    }

    @Test
    void exportCsv_shouldRejectWhenCountExceedsMaxLimit_10001Records() {
        List<ActivityLog> overLimitList = new ArrayList<>();
        for (int i = 0; i <= ActivityLogExportServiceImpl.MAX_DIRECT_EXPORT_RECORDS; i++) {
            overLimitList.add(ActivityLog.builder()
                    .id(UUID.randomUUID())
                    .organizationId(organizationId)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        assertThat(overLimitList).hasSize(10_001);

        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(overLimitList));

        assertThatThrownBy(() -> service.exportCsv(
                ActivityLogExportFilterRequest.builder().build(),
                currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Số lượng bản ghi vượt quá giới hạn xuất trực tiếp (tối đa 10.000 bản ghi). Vui lòng thu hẹp khoảng thời gian hoặc điều kiện lọc.");

        verify(activityLogRepository, never()).saveAndFlush(any(ActivityLog.class));
    }

    @Test
    void exportCsv_shouldSucceedWhenCountIsExactlyMaxLimit_10000Records() {
        when(currentUser.getUserId()).thenReturn(userId);
        when(currentUser.getUsername()).thenReturn("manager");
        when(currentUser.getFullName()).thenReturn("Quản lý");

        List<ActivityLog> exactlyMaxList = new ArrayList<>();
        for (int i = 0; i < ActivityLogExportServiceImpl.MAX_DIRECT_EXPORT_RECORDS; i++) {
            exactlyMaxList.add(ActivityLog.builder()
                    .id(UUID.randomUUID())
                    .organizationId(organizationId)
                    .username("user" + i)
                    .fullName("Tên " + i)
                    .action("ACTION")
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        assertThat(exactlyMaxList).hasSize(10_000);

        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(exactlyMaxList));

        byte[] result = service.exportCsv(ActivityLogExportFilterRequest.builder().build(), currentUser);
        assertThat(result).isNotNull().isNotEmpty();

        ArgumentCaptor<ActivityLog> auditCaptor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).saveAndFlush(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getDescription()).contains("recordCount=10000");
    }

    @Test
    void exportCsv_shouldSucceedWhenCountIsUnderLimit_9999Records() {
        when(currentUser.getUserId()).thenReturn(userId);
        when(currentUser.getUsername()).thenReturn("manager");
        when(currentUser.getFullName()).thenReturn("Quản lý");

        List<ActivityLog> underLimitList = new ArrayList<>();
        for (int i = 0; i < 9_999; i++) {
            underLimitList.add(ActivityLog.builder()
                    .id(UUID.randomUUID())
                    .organizationId(organizationId)
                    .username("user" + i)
                    .fullName("Tên " + i)
                    .action("ACTION")
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        assertThat(underLimitList).hasSize(9_999);

        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(underLimitList));

        byte[] result = service.exportCsv(ActivityLogExportFilterRequest.builder().build(), currentUser);
        assertThat(result).isNotNull().isNotEmpty();

        ArgumentCaptor<ActivityLog> auditCaptor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).saveAndFlush(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getDescription()).contains("recordCount=9999");
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

    @Test
    void protectFormula_shouldSanitizeFormulaInjectionCases() {
        assertThat(service.protectFormula("=SUM(A1:A10)")).isEqualTo("'=SUM(A1:A10)");
        assertThat(service.protectFormula("+123")).isEqualTo("'+123");
        assertThat(service.protectFormula("-123")).isEqualTo("'-123");
        assertThat(service.protectFormula("@cmd")).isEqualTo("'@cmd");
        assertThat(service.protectFormula(" =SUM(A1:A10)")).isEqualTo("' =SUM(A1:A10)");
        assertThat(service.protectFormula("\t=cmd")).isEqualTo("'\t=cmd");
        assertThat(service.protectFormula("\r+123")).isEqualTo("'\r+123");
        assertThat(service.protectFormula("normal value")).isEqualTo("normal value");
        assertThat(service.protectFormula(" Nguyễn Văn A")).isEqualTo(" Nguyễn Văn A");
    }

    @Test
    void validateRequest_shouldThrowForbidden_whenUserHasNoOrganizationId() {
        when(currentUser.getRoleCode()).thenReturn("VT-02");
        when(currentUser.getOrganizationId()).thenReturn(null);

        assertThatThrownBy(() -> service.preview(
                ActivityLogExportFilterRequest.builder().build(),
                currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Người dùng không thuộc tổ chức nào.");

        assertThatThrownBy(() -> service.exportCsv(
                ActivityLogExportFilterRequest.builder().build(),
                currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Người dùng không thuộc tổ chức nào.");
    }

    @Test
    void exportCsv_shouldNotExposeSensitiveFields_userId_organizationId_ipAddress_description() {
        UUID randomUserId = UUID.randomUUID();
        String secretIp = "192.168.1.99";
        String secretDesc = "Secret internal description";

        ActivityLog log = ActivityLog.builder()
                .organizationId(organizationId)
                .userId(randomUserId)
                .username("test_user")
                .fullName("Người dùng thử nghiệm")
                .action("ACTION_TEST")
                .description(secretDesc)
                .entityType("ENTITY_TEST")
                .entityId("ID-999")
                .ipAddress(secretIp)
                .createdAt(LocalDateTime.now())
                .build();

        when(currentUser.getRoleCode()).thenReturn("VT-02");
        when(currentUser.getOrganizationId()).thenReturn(organizationId);
        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        byte[] csv = service.exportCsv(ActivityLogExportFilterRequest.builder().build(), currentUser);
        String csvContent = new String(csv, StandardCharsets.UTF_8);

        // Kiểm tra tuyệt đối không xuất các trường nhạy cảm
        assertThat(csvContent).doesNotContain(randomUserId.toString());
        assertThat(csvContent).doesNotContain(organizationId.toString());
        assertThat(csvContent).doesNotContain(secretIp);
        assertThat(csvContent).doesNotContain(secretDesc);
    }
}

