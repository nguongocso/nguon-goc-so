package vn.nguongocso.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import vn.nguongocso.alert.enums.NotificationType;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.notification.entity.Notification;
import vn.nguongocso.notification.repository.NotificationRepository;
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
}
