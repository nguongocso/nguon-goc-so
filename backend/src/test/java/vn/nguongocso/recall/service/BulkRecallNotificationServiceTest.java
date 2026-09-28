package vn.nguongocso.recall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.recall.entity.BulkRecallRequest;

/**
 * Kiểm tra recipient của thông báo workflow yêu cầu thu hồi hàng loạt.
 *
 * <p>
 * Bao phủ các case nghiệp vụ:
 * <ul>
 * <li>Manager A tạo: B/C nhận, A và NGSK không nhận.</li>
 * <li>Manager B approve/reject: các Manager khác nhận, B và NGSK không nhận.</li>
 * <li>Chỉ 1 Manager: không phát sinh notification.</li>
 * <li>Nhiều Manager + nhiều NGSK: chỉ Manager khác nhận.</li>
 * </ul>
 * </p>
 */
class BulkRecallNotificationServiceTest {

    private NotificationService notificationService;
    private OrganizationUserRepository organizationUserRepository;
    private BulkRecallNotificationService service;

    private UUID organizationId;
    private UUID managerAId;
    private UUID managerBId;
    private UUID managerCId;
    private UUID ngskDId;
    private UUID ngskEId;

    private User managerA;
    private User managerB;
    private User managerC;
    private User ngskD;
    private User ngskE;

    private Role managerRole;
    private Role ngskRole;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        organizationUserRepository = mock(OrganizationUserRepository.class);
        service = new BulkRecallNotificationService(notificationService, organizationUserRepository);

        organizationId = UUID.randomUUID();
        managerAId = UUID.randomUUID();
        managerBId = UUID.randomUUID();
        managerCId = UUID.randomUUID();
        ngskDId = UUID.randomUUID();
        ngskEId = UUID.randomUUID();

        managerA = user(managerAId, "Manager A");
        managerB = user(managerBId, "Manager B");
        managerC = user(managerCId, "Manager C");
        ngskD = user(ngskDId, "NGSK D");
        ngskE = user(ngskEId, "NGSK E");

        managerRole = new Role(2, "VT-02", "ORG_MANAGER");
        ngskRole = new Role(3, "VT-03", "EVENT_RECORDER");
    }

    @Test
    @DisplayName("Case 1: Manager A tạo yêu cầu - chỉ Manager B/C nhận")
    void create_managerA_shouldNotifyOnlyOtherManagers() {
        stubMembers(
                membership(managerB, managerRole),
                membership(managerC, managerRole),
                membership(managerA, managerRole),
                membership(ngskD, ngskRole));

        service.sendBulkRecallWorkflowNotification(actor(managerAId), "CREATE", request(), UUID.randomUUID());

        List<User> recipients = captureRecipients();
        assertThat(recipients).extracting(User::getUserId)
                .containsExactlyInAnyOrder(managerBId, managerCId);
    }

    @Test
    @DisplayName("Case 2: Manager B phê duyệt - chỉ Manager A/C nhận")
    void approve_managerB_shouldNotifyOnlyOtherManagers() {
        stubMembers(
                membership(managerA, managerRole),
                membership(managerC, managerRole),
                membership(managerB, managerRole),
                membership(ngskD, ngskRole));

        service.sendBulkRecallWorkflowNotification(actor(managerBId), "APPROVE", request(), UUID.randomUUID());

        List<User> recipients = captureRecipients();
        assertThat(recipients).extracting(User::getUserId)
                .containsExactlyInAnyOrder(managerAId, managerCId);
    }

    @Test
    @DisplayName("Case 3: Manager B từ chối - chỉ Manager A/C nhận")
    void reject_managerB_shouldNotifyOnlyOtherManagers() {
        stubMembers(
                membership(managerA, managerRole),
                membership(managerC, managerRole),
                membership(managerB, managerRole),
                membership(ngskD, ngskRole));

        BulkRecallRequest req = request();
        req.setRejectionReason("Chưa đủ bằng chứng");
        service.sendBulkRecallWorkflowNotification(actor(managerBId), "REJECT", req, UUID.randomUUID());

        List<User> recipients = captureRecipients();
        assertThat(recipients).extracting(User::getUserId)
                .containsExactlyInAnyOrder(managerAId, managerCId);
    }

    @Test
    @DisplayName("Case 5: chỉ 1 Manager - không phát sinh notification")
    void singleManager_shouldNotSendNotification() {
        stubMembers(membership(managerA, managerRole), membership(ngskD, ngskRole));

        service.sendBulkRecallWorkflowNotification(actor(managerAId), "CREATE", request(), UUID.randomUUID());

        verify(notificationService, never()).sendBulkRecallWorkflowNotification(
                anyString(), anyString(), any(UUID.class), anyString(), anyList());
    }

    @Test
    @DisplayName("Case 6: nhiều Manager + nhiều NGSK - tuyệt đối không gửi cho NGSK")
    void multipleManagersAndNgsks_shouldNotifyOnlyOtherManagers() {
        stubMembers(
                membership(managerA, managerRole),
                membership(managerB, managerRole),
                membership(managerC, managerRole),
                membership(ngskD, ngskRole),
                membership(ngskE, ngskRole));

        service.sendBulkRecallWorkflowNotification(actor(managerAId), "CREATE", request(), UUID.randomUUID());

        List<User> recipients = captureRecipients();
        assertThat(recipients).extracting(User::getUserId)
                .containsExactlyInAnyOrder(managerBId, managerCId);
        assertThat(recipients).extracting(User::getUserId)
                .doesNotContain(ngskDId, ngskEId, managerAId);
    }

    private void stubMembers(OrganizationUser... members) {
        when(organizationUserRepository.findByOrganization_OrganizationIdAndStatus(
                organizationId, OrganizationUserStatus.ACTIVE)).thenReturn(List.of(members));
    }

    @SuppressWarnings("unchecked")
    private List<User> captureRecipients() {
        ArgumentCaptor<List<User>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).sendBulkRecallWorkflowNotification(
                anyString(), anyString(), any(UUID.class), anyString(), captor.capture());
        return captor.getValue();
    }

    private CustomUserDetails actor(UUID userId) {
        CustomUserDetails actor = mock(CustomUserDetails.class);
        when(actor.getUserId()).thenReturn(userId);
        when(actor.getOrganizationId()).thenReturn(organizationId);
        when(actor.getFullName()).thenReturn("Actor");
        return actor;
    }

    private BulkRecallRequest request() {
        BulkRecallRequest req = new BulkRecallRequest();
        req.setId(UUID.randomUUID());
        req.setReason("Lý do thu hồi");
        ProductionLot lot = new ProductionLot();
        lot.setName("Lô sản xuất kiểm thử");
        req.setProductionLot(lot);
        return req;
    }

    private User user(UUID id, String fullName) {
        User u = new User();
        u.setUserId(id);
        u.setUserName("user-" + id.toString().substring(0, 8));
        u.setFullName(fullName);
        return u;
    }

    private OrganizationUser membership(User user, Role role) {
        Organization org = new Organization();
        org.setOrganizationId(organizationId);
        OrganizationUser ou = new OrganizationUser();
        ou.setId(UUID.randomUUID());
        ou.setOrganization(org);
        ou.setUser(user);
        ou.setRole(role);
        ou.setStatus(OrganizationUserStatus.ACTIVE);
        return ou;
    }
}
