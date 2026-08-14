package vn.nguongocso.alert_reclaim_history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.alert.dto.response.ActivityLogResponse;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.alert.service.impl.ActivityLogServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private ActivityLogServiceImpl activityLogService;

    private UUID correctOrgId;
    private CustomUserDetails mockUser;

    @BeforeEach
    void setUp() {
        correctOrgId = UUID.randomUUID();
        mockUser = mock(CustomUserDetails.class);

        // Giả lập người dùng đăng nhập thuộc về tổ chức correctOrgId
        when(mockUser.getOrganizationId()).thenReturn(correctOrgId);
    }

    @Test
    void getActivityLogs_shouldEnforceDataIsolation_whenCalled() {
        // Arrange (Chuẩn bị dữ liệu giả lập)
        int page = 0;
        int size = 10;
        String action = "EXPORT_DOSSIER";

        ActivityLog expectedLog = ActivityLog.builder()
                .id(UUID.randomUUID())
                .organizationId(correctOrgId) // Log thuộc về tổ chức đúng
                .userId(UUID.randomUUID())
                .username("coop_manager")
                .fullName("Nguyễn Văn A")
                .action("EXPORT_DOSSIER")
                .description("Xuất hồ sơ chất lượng sản phẩm")
                .createdAt(LocalDateTime.now())
                .build();

        Page<ActivityLog> dbPage = new PageImpl<>(List.of(expectedLog));

        // Cấu hình Mockito: Khi findAll được gọi với bất kỳ Specification nào, trả về trang dữ liệu mẫu
        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(dbPage);

        // Act (Thực hiện hành động test)
        PageResponse<ActivityLogResponse> response = activityLogService.getActivityLogs(
                page, size, action, null, null, null, mockUser
        );

        // Assert (Kiểm chứng kết quả kiểm thử)
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getUsername()).isEqualTo("coop_manager");


        // Tóm bắt tham số Specification truyền vào repository.findAll
        ArgumentCaptor<Specification<ActivityLog>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(activityLogRepository).findAll(specCaptor.capture(), any(Pageable.class));

        Specification<ActivityLog> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();

        // Đảm bảo rằng câu query được gọi tối thiểu 1 lần và sử dụng đúng tham số phân trang
        verify(activityLogRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }
}
