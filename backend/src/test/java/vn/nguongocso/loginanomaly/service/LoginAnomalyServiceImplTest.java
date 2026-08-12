package vn.nguongocso.loginanomaly.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.alert.enums.NotificationType;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.loginanomaly.dto.response.LockLoginAnomalyResponse;
import vn.nguongocso.loginanomaly.dto.response.LoginAnomalyResponse;
import vn.nguongocso.loginanomaly.entity.LoginAnomaly;
import vn.nguongocso.loginanomaly.entity.LoginAttempt;
import vn.nguongocso.loginanomaly.enums.LoginAnomalySeverity;
import vn.nguongocso.loginanomaly.repository.LoginAnomalyRepository;
import vn.nguongocso.loginanomaly.repository.LoginAttemptRepository;
import vn.nguongocso.loginanomaly.service.impl.LoginAnomalyServiceImpl;
import vn.nguongocso.notification.entity.Notification;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/** Unit test cho dịch vụ theo dõi đăng nhập bất thường. */
@ExtendWith(MockitoExtension.class)
class LoginAnomalyServiceImplTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private LoginAnomalyRepository loginAnomalyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LoginAnomalyServiceImpl service;

    private User user;

    private UUID orgId;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(UUID.randomUUID())
                .userName("nguyenvanan")
                .fullName("Nguyễn Văn An")
                .status(UserStatus.ACTIVE)
                .build();

        orgId = UUID.randomUUID();
    }

    // =========================================================
    // TC-01: 5 lần đăng nhập sai trong 2 phút -> bất thường
    // =========================================================

    @Test
    void recordFailedLogin_whenReachingThreshold_createsAnomalyOnce() {
        when(userRepository.findByUserName("nguyenvanan")).thenReturn(Optional.of(user));
        when(loginAttemptRepository.countByUsernameAndFailedAtAfter(eq("nguyenvanan"), any()))
                .thenReturn(5L);

        OrganizationUser adminMembership = new OrganizationUser();
        adminMembership.setUser(user);
        when(organizationUserRepository.findAllByRole_Code("VT-01"))
                .thenReturn(List.of(adminMembership));

        service.recordFailedLogin("nguyenvanan", "103.75.200.11");

        verify(loginAttemptRepository).save(any(LoginAttempt.class));

        ArgumentCaptor<LoginAnomaly> captor = ArgumentCaptor.forClass(LoginAnomaly.class);
        verify(loginAnomalyRepository).save(captor.capture());

        LoginAnomaly anomaly = captor.getValue();
        assertEquals("nguyenvanan", anomaly.getUsername());
        assertEquals(LoginAnomalySeverity.HIGH, anomaly.getSeverity());
        assertEquals(user.getUserId(), anomaly.getUserId());
        assertNotNull(anomaly.getLocation());

        // Quản trị viên được thông báo (TC-01)
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void recordFailedLogin_belowThreshold_doesNotCreateAnomaly() {
        when(userRepository.findByUserName("nguyenvanan")).thenReturn(Optional.of(user));
        when(loginAttemptRepository.countByUsernameAndFailedAtAfter(eq("nguyenvanan"), any()))
                .thenReturn(4L);

        service.recordFailedLogin("nguyenvanan", "103.75.200.11");

        verify(loginAttemptRepository).save(any(LoginAttempt.class));
        verify(loginAnomalyRepository, never()).save(any(LoginAnomaly.class));
    }

    @Test
    void recordFailedLogin_beyondThreshold_doesNotDuplicateAnomaly() {
        when(userRepository.findByUserName("nguyenvanan")).thenReturn(Optional.of(user));
        when(loginAttemptRepository.countByUsernameAndFailedAtAfter(eq("nguyenvanan"), any()))
                .thenReturn(7L);

        service.recordFailedLogin("nguyenvanan", "103.75.200.11");

        verify(loginAnomalyRepository, never()).save(any(LoginAnomaly.class));
    }

    // =========================================================
    // TC-02 + TC-04: khóa tạm -> không đăng nhập được,
    // ghi lịch sử + thông báo chủ tài khoản
    // =========================================================

    @Test
    void lockAnomaly_locksAccount_logsActivity_andNotifiesOwner() {
        LoginAnomaly anomaly = anomalyFor(user);
        when(loginAnomalyRepository.findById(anomaly.getAnomalyId()))
                .thenReturn(Optional.of(anomaly));
        when(userRepository.findByUserName("nguyenvanan")).thenReturn(Optional.of(user));
        loginAs("admin", "Quản trị viên", orgId, "VT-01");

        LockLoginAnomalyResponse response = service.lockAnomaly(anomaly.getAnomalyId());

        assertEquals(UserStatus.LOCKED, response.getAccountStatus());
        assertEquals("nguyenvanan", response.getUsername());
        assertEquals("Quản trị viên", response.getLockedBy());

        verify(userRepository).save(user);
        assertEquals(UserStatus.LOCKED, user.getStatus());

        // TC-04: ghi lịch sử hoạt động kèm người thực hiện
        ArgumentCaptor<ActivityLogEvent> eventCaptor =
                ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ActivityLogEvent event = eventCaptor.getValue();
        assertEquals("LOCK_ACCOUNT", event.getAction());
        assertEquals("Quản trị viên", event.getFullName());

        // TC-02: chủ tài khoản nhận thông báo
        ArgumentCaptor<Notification> notificationCaptor =
                ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertEquals(NotificationType.ALERT, notification.getType());
        assertEquals(user, notification.getUser());
    }

    @Test
    void lockAnomaly_whenAlreadyLocked_throwsBusinessException() {
        user.setStatus(UserStatus.LOCKED);

        LoginAnomaly anomaly = anomalyFor(user);
        when(loginAnomalyRepository.findById(anomaly.getAnomalyId()))
                .thenReturn(Optional.of(anomaly));
        when(userRepository.findByUserName("nguyenvanan")).thenReturn(Optional.of(user));
        loginAs("admin", "Quản trị viên", orgId, "VT-01");

        assertThrows(BusinessException.class,
                () -> service.lockAnomaly(anomaly.getAnomalyId()));

        verify(userRepository, never()).save(user);
    }

    // =========================================================
    // TC-03: quản lý HTX chỉ thấy dữ liệu tổ chức của mình
    // =========================================================

    @Test
    void getAnomalies_vt02_onlySeesOwnOrganizationData() {
        UUID cooperativeOrgId = UUID.randomUUID();
        loginAs("manager", "Quản lý HTX", cooperativeOrgId, "VT-02");

        LoginAnomaly anomaly = anomalyFor(user);
        Page<LoginAnomaly> page = new PageImpl<>(List.of(anomaly));

        when(loginAnomalyRepository.findWithFilters(
                eq(cooperativeOrgId),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(PageRequest.class)))
                .thenReturn(page);
        when(userRepository.findAllById(List.of(user.getUserId())))
                .thenReturn(List.of(user));

        PageResponse<LoginAnomalyResponse> result = service.getAnomalies(
                null, null, null, null, null, 0, 20);

        assertEquals(1, result.getTotalElements());

        // Kiểm tra scope org VT-02 được truyền vào repository
        ArgumentCaptor<UUID> orgCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(loginAnomalyRepository).findWithFilters(
                orgCaptor.capture(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(PageRequest.class));
        assertEquals(cooperativeOrgId, orgCaptor.getValue());
    }

    @Test
    void getAnomalies_vt01_seesAllOrganizations() {
        loginAs("admin", "Quản trị viên", orgId, "VT-01");

        when(loginAnomalyRepository.findWithFilters(
                isNull(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<LoginAnomalyResponse> result = service.getAnomalies(
                null, null, null, null, null, 0, 20);

        assertNotNull(result);
        ArgumentCaptor<UUID> orgCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(loginAnomalyRepository).findWithFilters(
                orgCaptor.capture(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(PageRequest.class));
        assertEquals(null, orgCaptor.getValue());
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private LoginAnomaly anomalyFor(User user) {
        return LoginAnomaly.builder()
                .anomalyId(UUID.randomUUID())
                .userId(user.getUserId())
                .username(user.getUserName())
                .fullName(user.getFullName())
                .organizationId(orgId)
                .organizationName("Hợp tác xã Nông sản Xanh")
                .ipAddress("103.75.200.11")
                .location("Mạng nội bộ/VPN")
                .reason("Đăng nhập sai 5 lần liên tiếp trong 2 phút")
                .severity(LoginAnomalySeverity.HIGH)
                .loginAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void loginAs(String username, String fullName, UUID orgId, String roleCode) {
        Role role = new Role(0, roleCode, "ROLE");

        User actor = User.builder()
                .userId(UUID.randomUUID())
                .userName(username)
                .fullName(fullName)
                .status(UserStatus.ACTIVE)
                .build();

        Organization organization = Organization.builder()
                .organizationId(orgId)
                .name("Tổ chức")
                .code("ORG")
                .build();

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setUser(actor);
        orgUser.setOrganization(organization);
        orgUser.setRole(role);
        orgUser.setStatus(OrganizationUserStatus.ACTIVE);

        CustomUserDetails details = new CustomUserDetails(actor, orgUser, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
