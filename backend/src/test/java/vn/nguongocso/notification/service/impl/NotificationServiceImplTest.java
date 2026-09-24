package vn.nguongocso.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.alert.enums.NotificationType;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.notification.entity.Notification;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Kiểm thử đơn vị cho NotificationServiceImpl liên quan đến thông báo kiểm nghiệm đạt và không đạt.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID organizationId;
    private User recipientUser;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        recipientUser = User.builder()
                .userId(UUID.randomUUID())
                .fullName("Quản lý Hợp tác xã")
                .build();
    }

    @Test
    @DisplayName("Gửi thông báo ĐẠT: tạo notification INFO cho người nhận thuộc HTX")
    @SuppressWarnings("unchecked")
    void testSendInspectionPassedNotification_createsInfoNotification() {
        when(organizationUserRepository.findUsersByPermission(organizationId, "notification", "READ"))
                .thenReturn(List.of(recipientUser));

        notificationService.sendInspectionPassedNotification("Lô 01", organizationId);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        Notification n = saved.get(0);
        assertThat(n.getUser()).isEqualTo(recipientUser);
        assertThat(n.getType()).isEqualTo(NotificationType.INFO);
        assertThat(n.getTitle()).isEqualTo("Kết quả kiểm nghiệm đạt");
        assertThat(n.getContent()).contains("Lô sản xuất \"Lô 01\" đã hoàn tất kiểm nghiệm và ĐẠT yêu cầu");
        assertThat(n.getIsRead()).isFalse();
        assertThat(n.getReadAt()).isNull();
    }

    @Test
    @DisplayName("Gửi cảnh báo KHÔNG ĐẠT: tạo notification ALERT cho người nhận thuộc HTX")
    @SuppressWarnings("unchecked")
    void testSendInspectionFailedNotification_createsAlertNotification() {
        when(organizationUserRepository.findUsersByPermission(organizationId, "notification", "READ"))
                .thenReturn(List.of(recipientUser));

        notificationService.sendInspectionFailedNotification("Lô 01", organizationId);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        Notification n = saved.get(0);
        assertThat(n.getUser()).isEqualTo(recipientUser);
        assertThat(n.getType()).isEqualTo(NotificationType.ALERT);
        assertThat(n.getTitle()).isEqualTo("Kết quả kiểm nghiệm không đạt");
        assertThat(n.getContent()).contains("Lô sản xuất \"Lô 01\" có kết quả kiểm nghiệm KHÔNG ĐẠT");
        assertThat(n.getIsRead()).isFalse();
        assertThat(n.getReadAt()).isNull();
    }

    @Test
    @DisplayName("Gửi thông báo: Bỏ qua khi organizationId là null")
    void testSendInspectionNotification_whenOrgIdNull_doesNothing() {
        notificationService.sendInspectionPassedNotification("Lô 01", null);
        notificationService.sendInspectionFailedNotification("Lô 01", null);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Gửi thông báo: Bỏ qua khi không tìm thấy người nhận nào có quyền notification:READ")
    void testSendInspectionNotification_whenNoRecipients_doesNothing() {
        when(organizationUserRepository.findUsersByPermission(organizationId, "notification", "READ"))
                .thenReturn(List.of());

        notificationService.sendInspectionPassedNotification("Lô 01", organizationId);
        notificationService.sendInspectionFailedNotification("Lô 01", organizationId);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Lấy danh sách tất cả: ưu tiên thông báo chưa đọc lên trước rồi mới đến mới nhất")
    void testGetNotifications_withoutFilter_ordersUnreadFirst() {
        UUID userId = loginAs();
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 20);
        when(notificationRepository.findByUser_UserIdOrderByIsReadAscCreatedAtDesc(eq(userId), eq(pageable)))
                .thenReturn(org.springframework.data.domain.Page.empty(pageable));

        notificationService.getNotifications(null, pageable);

        verify(notificationRepository).findByUser_UserIdOrderByIsReadAscCreatedAtDesc(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("Đánh dấu tất cả đã đọc: cập nhật theo người dùng hiện tại và trả về số bản ghi")
    void testMarkAllAsRead_returnsUpdatedCount() {
        UUID userId = loginAs();

        when(notificationRepository.markAllAsRead(eq(userId), any(LocalDateTime.class)))
                .thenReturn(34);

        int markedReadCount = notificationService.markAllAsRead();

        assertThat(markedReadCount).isEqualTo(34);
        verify(permissionChecker).check("notification", "READ");
        verify(notificationRepository).markAllAsRead(eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Đánh dấu tất cả đã đọc: trả về 0 khi người dùng không còn thông báo chưa đọc")
    void testMarkAllAsRead_whenNoUnreadNotification_returnsZero() {
        UUID userId = loginAs();

        when(notificationRepository.markAllAsRead(eq(userId), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThat(notificationService.markAllAsRead()).isZero();
    }

    /** Đăng nhập ngữ cảnh bảo mật bằng người dùng có quyền đọc thông báo. */
    private UUID loginAs() {
        User currentUser = User.builder()
                .userId(UUID.randomUUID())
                .userName("managerA")
                .fullName("Quản lý HTX")
                .build();

        Organization organization = new Organization();
        organization.setOrganizationId(organizationId);

        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setOrganization(organization);

        Role role = new Role();
        role.setCode("VT-02");
        role.setName("Quản lý hợp tác xã");

        CustomUserDetails userDetails = new CustomUserDetails(currentUser, organizationUser, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        return currentUser.getUserId();
    }
}
