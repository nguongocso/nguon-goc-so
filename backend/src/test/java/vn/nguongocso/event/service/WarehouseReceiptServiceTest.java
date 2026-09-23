package vn.nguongocso.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.WarehouseReceiptRequest;
import vn.nguongocso.event.dto.response.WarehouseReceiptResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.impl.WarehouseReceiptServiceImpl;
import vn.nguongocso.event.service.notifier.WarehouseDiscrepancyNotifier;
import vn.nguongocso.event.service.processor.WarehouseReceiptProcessor;
import vn.nguongocso.event.service.recorder.ChainEventHashRecorder;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Kiểm thử cho WarehouseReceiptService (NCL-05-CN-008).
 * Xác thực ranh giới giao dịch proxy và quy tắc non-blocking của thông báo chênh lệch nhập kho.
 */
@ExtendWith(SpringExtension.class)
class WarehouseReceiptServiceTest {

    private TraceCodeRepository traceCodeRepository;
    private ShipmentRepository shipmentRepository;
    private ShipmentHandoverRepository shipmentHandoverRepository;
    private ChainEventRepository chainEventRepository;
    private ChainEventService chainEventService;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;
    private EventValidationService eventValidationService;
    private ApplicationEventPublisher eventPublisher;
    private NotificationRepository notificationRepository;
    private OrganizationUserRepository organizationUserRepository;

    private TrackingTransactionManager txManager;
    private WarehouseReceiptService transactionalProxy;

    private CustomUserDetails vt04User;
    private WarehouseReceiptRequest request;

    @BeforeEach
    void setUp() {
        traceCodeRepository = mock(TraceCodeRepository.class);
        shipmentRepository = mock(ShipmentRepository.class);
        shipmentHandoverRepository = mock(ShipmentHandoverRepository.class);
        chainEventRepository = mock(ChainEventRepository.class);
        chainEventService = mock(ChainEventService.class);
        userRepository = mock(UserRepository.class);
        objectMapper = new ObjectMapper();
        eventValidationService = mock(EventValidationService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        notificationRepository = mock(NotificationRepository.class);
        organizationUserRepository = mock(OrganizationUserRepository.class);

        WarehouseDiscrepancyNotifier discrepancyNotifier = new WarehouseDiscrepancyNotifier(
                organizationUserRepository,
                notificationRepository
        );
        ChainEventHashRecorder chainEventHashRecorder = mock(ChainEventHashRecorder.class);
        when(chainEventHashRecorder.saveWithChainHash(any())).thenAnswer(invocation -> {
            ChainEvent e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setHash("mocked-hash");
            return e;
        });

        WarehouseReceiptProcessor processor = new WarehouseReceiptProcessor(
                traceCodeRepository,
                shipmentHandoverRepository,
                chainEventRepository,
                chainEventHashRecorder,
                userRepository,
                eventValidationService,
                eventPublisher,
                discrepancyNotifier,
                organizationUserRepository,
                objectMapper
        );

        WarehouseReceiptServiceImpl target = new WarehouseReceiptServiceImpl(
                processor,
                chainEventRepository,
                traceCodeRepository,
                organizationUserRepository,
                objectMapper
        );

        txManager = new TrackingTransactionManager();
        RuleBasedTransactionAttribute txAttr = new RuleBasedTransactionAttribute();
        txAttr.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        MatchAlwaysTransactionAttributeSource tas = new MatchAlwaysTransactionAttributeSource();
        tas.setTransactionAttribute(txAttr);

        TransactionInterceptor txInterceptor = new TransactionInterceptor(txManager, tas);

        ProxyFactory pf = new ProxyFactory();
        pf.setTarget(target);
        pf.setInterfaces(WarehouseReceiptService.class);
        pf.addAdvice(txInterceptor);

        transactionalProxy = (WarehouseReceiptService) pf.getProxy();

        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        vt04User = mock(CustomUserDetails.class);
        when(vt04User.getRoleCode()).thenReturn(RoleCode.PROCUREMENT);
        when(vt04User.getUserId()).thenReturn(userId);
        when(vt04User.getUsername()).thenReturn("procurement_user");
        when(vt04User.getFullName()).thenReturn("Nguyễn Thu Mua");
        when(vt04User.getOrganizationId()).thenReturn(orgId);

        User actor = new User();
        actor.setUserId(userId);
        actor.setFullName("Nguyễn Thu Mua");
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        Organization org = new Organization();
        org.setOrganizationId(orgId);

        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("Lô cà chua test");
        shipment.setOrganization(org);
        shipment.setRecipientOrganization(org);
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipment.setTotalQuantity(100L);

        TraceCode traceCode = new TraceCode();
        traceCode.setCodeValue("TC-123456");
        traceCode.setShipment(shipment);
        when(traceCodeRepository.findByCodeValue("TC-123456")).thenReturn(Optional.of(traceCode));

        ChainEvent procurementEvent = ChainEvent.builder()
                .eventType(ChainEventType.PROCUREMENT)
                .recordedBy(actor)
                .build();
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(List.of(procurementEvent));

        OrganizationUser actorOrgUser = new OrganizationUser();
        actorOrgUser.setUser(actor);
        when(organizationUserRepository.findByOrganization_OrganizationIdAndUser_UserId(orgId, userId))
                .thenReturn(Optional.of(actorOrgUser));

        when(chainEventService.saveWithChainHash(any())).thenAnswer(invocation -> {
            ChainEvent e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setHash("mocked-hash");
            return e;
        });

        User managerUser = new User();
        managerUser.setUserId(UUID.randomUUID());
        OrganizationUser managerOrgUser = new OrganizationUser();
        managerOrgUser.setUser(managerUser);
        when(organizationUserRepository.findAllByOrganization_OrganizationIdAndRole_Code(orgId, RoleCode.ORG_MANAGER))
                .thenReturn(List.of(managerOrgUser));

        request = new WarehouseReceiptRequest();
        request.setCodeValue("TC-123456");
        request.setReceivedQuantity(80.0);
        request.setReason("Hao hụt vận chuyển đường dài");
        request.setConditionNote("Bình thường");
        request.setReceiptDate(LocalDate.now());
    }

    @Test
    @DisplayName("Khi NotificationRepository lưu thành công: outer transaction COMMIT và notificationSent = true")
    void shouldCommitTransactionWhenNotificationSucceeds() {
        WarehouseReceiptResponse response = transactionalProxy.recordWarehouseReceipt(request, vt04User);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationSent()).isTrue();
        assertThat(txManager.commitCount).isEqualTo(1);
        assertThat(txManager.rollbackCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Khi NotificationRepository ném DataIntegrityViolationException: outer transaction VẪN COMMIT")
    void shouldStillCommitTransactionWhenNotificationThrowsDataAccessException() {
        doThrow(new DataIntegrityViolationException("Simulated DB notification error"))
                .when(notificationRepository).saveAll(any());

        WarehouseReceiptResponse response = transactionalProxy.recordWarehouseReceipt(request, vt04User);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationSent()).isFalse();
        assertThat(txManager.commitCount).isEqualTo(1);
        assertThat(txManager.rollbackCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Khi NotificationRepository ném RuntimeException: outer transaction VẪN COMMIT")
    void shouldStillCommitTransactionWhenNotificationThrowsRuntimeException() {
        doThrow(new RuntimeException("Simulated generic runtime error"))
                .when(notificationRepository).saveAll(any());

        WarehouseReceiptResponse response = transactionalProxy.recordWarehouseReceipt(request, vt04User);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationSent()).isFalse();
        assertThat(txManager.commitCount).isEqualTo(1);
        assertThat(txManager.rollbackCount).isEqualTo(0);
    }

    /** TransactionManager ghi nhận số lần commit/rollback thực tế qua Spring proxy. */
    private static class TrackingTransactionManager implements PlatformTransactionManager {
        int commitCount = 0;
        int rollbackCount = 0;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new org.springframework.transaction.support.SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            commitCount++;
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
            rollbackCount++;
        }
    }
}
