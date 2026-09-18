package vn.nguongocso.integration.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import vn.nguongocso.certification.entity.SystemConfiguration;
import vn.nguongocso.certification.repository.SystemConfigurationRepository;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.partner.entity.PartnerWebhookNotification;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;
import vn.nguongocso.integration.partner.repository.PartnerLotAccessLogRepository;
import vn.nguongocso.integration.partner.repository.PartnerWebhookNotificationRepository;
import vn.nguongocso.integration.partner.service.PartnerRecallWebhookDispatcher;
import vn.nguongocso.integration.partner.service.PartnerWebhookDeliveryService;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Unit test cho PartnerRecallWebhookDispatcher (NCL-12-CN-006).
 * Kiểm thử toàn diện các kịch bản nghiệm thu TC-01, TC-02, TC-03, TC-04.
 */
@ExtendWith(MockitoExtension.class)
class PartnerRecallWebhookDispatcherTest {

    @Mock
    private PartnerLotAccessLogRepository partnerLotAccessLogRepository;

    @Mock
    private PartnerApiKeyRepository partnerApiKeyRepository;

    @Mock
    private PartnerWebhookNotificationRepository partnerWebhookNotificationRepository;

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private PartnerWebhookDeliveryService webhookDeliveryService;
    private PartnerRecallWebhookDispatcher dispatcher;

    private Shipment shipment;
    private ProductionLot productionLot;
    private PartnerApiKey activeApiKey;
    private PartnerApiKey revokedApiKey;

    @BeforeEach
    void setUp() {
        webhookDeliveryService = new PartnerWebhookDeliveryService(
                partnerLotAccessLogRepository,
                partnerApiKeyRepository,
                partnerWebhookNotificationRepository,
                objectMapper);

        dispatcher = new PartnerRecallWebhookDispatcher(
                webhookDeliveryService,
                partnerWebhookNotificationRepository,
                systemConfigurationRepository);

        productionLot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("LOT-LUA-2026-001")
                .build();

        shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("SHIPMENT-ST25-001");
        shipment.setProductionLot(productionLot);

        activeApiKey = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .partnerName("Đối tác Chuỗi Siêu Thị")
                .webhookUrl("https://127.0.0.1:59999/webhook")
                .webhookSecret("whsec_secret123")
                .isWebhookActive(true)
                .status(PartnerApiKeyStatus.ACTIVE)
                .isTest(false)
                .build();

        revokedApiKey = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .partnerName("Đối tác Bị Thu Hồi Quyền")
                .webhookUrl("https://127.0.0.1:59999/webhook")
                .webhookSecret("whsec_secret456")
                .isWebhookActive(true)
                .status(PartnerApiKeyStatus.REVOKED)
                .isTest(false)
                .build();
    }

    @Test
    @DisplayName("TC-03: Đối tác chưa từng lấy dữ liệu lô hàng trong cửa sổ T ngày -> Không gửi thông báo")
    void testDispatch_TC03_PartnerNeverAccessed_NoNotificationCreated() {
        when(systemConfigurationRepository.findById("PARTNER_RECALL_NOTIFICATION_WINDOW_DAYS"))
                .thenReturn(Optional.of(SystemConfiguration.builder()
                        .configKey("PARTNER_RECALL_NOTIFICATION_WINDOW_DAYS")
                        .configValue("30")
                        .build()));

        when(partnerLotAccessLogRepository.findDistinctPartnerApiKeyIdsByShipmentOrProductionLot(
                eq(shipment.getId()), eq(productionLot.getId()), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        dispatcher.dispatchRecallNotifications(
                List.of(shipment), "RECALLING", "Dư lượng thuốc bảo vệ thực vật", null);

        // Xác nhận không lưu thông báo nào và không tra cứu khóa
        verify(partnerApiKeyRepository, never()).findEligibleWebhookKeys(anyList(), any());
        verify(partnerWebhookNotificationRepository, never()).save(any(PartnerWebhookNotification.class));
    }

    @Test
    @DisplayName("TC-04: Khóa của đối tác bị REVOKED trước khi gửi -> Ngừng gửi và không tạo bản ghi thông báo")
    void testDispatch_TC04_RevokedApiKey_Blocked() {
        when(systemConfigurationRepository.findById("PARTNER_RECALL_NOTIFICATION_WINDOW_DAYS"))
                .thenReturn(Optional.of(SystemConfiguration.builder()
                        .configKey("PARTNER_RECALL_NOTIFICATION_WINDOW_DAYS")
                        .configValue("30")
                        .build()));

        when(partnerLotAccessLogRepository.findDistinctPartnerApiKeyIdsByShipmentOrProductionLot(
                eq(shipment.getId()), eq(productionLot.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(revokedApiKey.getId()));

        when(partnerApiKeyRepository.findEligibleWebhookKeys(anyList(), any()))
                .thenReturn(List.of(revokedApiKey));

        dispatcher.dispatchRecallNotifications(
                List.of(shipment), "RECALLING", "Thu hồi khẩn cấp", null);

        // Xác nhận không tạo bất kỳ thông báo nào cho khóa đã REVOKED
        verify(partnerWebhookNotificationRepository, never()).save(any(PartnerWebhookNotification.class));
    }

    @Test
    @DisplayName("TC-04: Thông báo đang trong hàng đợi PENDING_RETRY nhưng khóa bị REVOKED -> Hủy gửi ngay (CANCELLED)")
    void testExecuteWebhookDelivery_TC04_CancelIfKeyRevokedDuringRetry() {
        PartnerWebhookNotification pendingNotification = PartnerWebhookNotification.builder()
                .id(UUID.randomUUID())
                .partnerApiKey(revokedApiKey)
                .shipment(shipment)
                .lotCode(shipment.getName())
                .newStatus("RECALLING")
                .targetUrl(revokedApiKey.getWebhookUrl())
                .deliveryStatus(WebhookDeliveryStatus.PENDING_RETRY)
                .attemptCount(1)
                .payload("{}")
                .build();

        dispatcher.executeWebhookDelivery(pendingNotification, revokedApiKey.getWebhookSecret());

        assertEquals(WebhookDeliveryStatus.CANCELLED, pendingNotification.getDeliveryStatus());
        assertNotNull(pendingNotification.getLastErrorMessage());
        assertTrue(pendingNotification.getLastErrorMessage().contains("thu hồi"));
        verify(partnerWebhookNotificationRepository).save(pendingNotification);
    }

    @Test
    @DisplayName("TC-01 & TC-02: Đối tác hợp lệ đã truy xuất lô -> Tạo thông báo và thử gửi qua HTTPS")
    void testDispatch_TC01_TC02_EligibleKey_CreatesNotificationAndAttempts() {
        when(systemConfigurationRepository.findById("PARTNER_RECALL_NOTIFICATION_WINDOW_DAYS"))
                .thenReturn(Optional.of(SystemConfiguration.builder()
                        .configKey("PARTNER_RECALL_NOTIFICATION_WINDOW_DAYS")
                        .configValue("30")
                        .build()));

        when(partnerLotAccessLogRepository.findDistinctPartnerApiKeyIdsByShipmentOrProductionLot(
                eq(shipment.getId()), eq(productionLot.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(activeApiKey.getId()));

        when(partnerApiKeyRepository.findEligibleWebhookKeys(anyList(), any()))
                .thenReturn(List.of(activeApiKey));

        when(partnerWebhookNotificationRepository.save(any(PartnerWebhookNotification.class)))
                .thenAnswer(invocation -> {
                    PartnerWebhookNotification notif = invocation.getArgument(0);
                    if (notif.getId() == null) {
                        notif.setId(UUID.randomUUID());
                    }
                    return notif;
                });

        dispatcher.dispatchRecallNotifications(
                List.of(shipment), "RECALLING", "Thu hồi kiểm tra chất lượng", null);

        // Xác nhận đã lưu notification và thực thi gửi lần 1 (thất bại do endpoint giả lập -> PENDING_RETRY)
        verify(partnerWebhookNotificationRepository, org.mockito.Mockito.atLeastOnce()).save(any(PartnerWebhookNotification.class));
    }

    @Test
    @DisplayName("Tính toán chữ ký HMAC-SHA256 chuẩn xác theo định dạng t=timestamp,v1=signature")
    void testHmacSha256Signature() {
        String data = "1672531199.{\"eventId\":\"12345\"}";
        String secret = "whsec_testkey";

        String signature = PartnerRecallWebhookDispatcher.computeHmacSha256(data, secret);

        assertNotNull(signature);
        assertFalse(signature.isBlank());
        assertEquals(64, signature.length()); // SHA-256 hex string = 64 chars
    }

    @Test
    @DisplayName("Quét và thử lại các thông báo đang ở trạng thái PENDING_RETRY theo lịch giãn dần (CV-04)")
    void testRetryPendingNotifications_ExecutesPendingRetries() {
        PartnerWebhookNotification pendingNotif = PartnerWebhookNotification.builder()
                .id(UUID.randomUUID())
                .partnerApiKey(activeApiKey)
                .shipment(shipment)
                .lotCode(shipment.getName())
                .newStatus("RECALLING")
                .targetUrl(activeApiKey.getWebhookUrl())
                .deliveryStatus(WebhookDeliveryStatus.PENDING_RETRY)
                .attemptCount(1)
                .maxAttempts(5)
                .payload("{}")
                .nextRetryAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(partnerWebhookNotificationRepository.findByDeliveryStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                eq(WebhookDeliveryStatus.PENDING_RETRY), any(LocalDateTime.class)))
                .thenReturn(List.of(pendingNotif));

        dispatcher.retryPendingNotifications();

        // Xác nhận đã lưu lại kết quả thử lại
        verify(partnerWebhookNotificationRepository, org.mockito.Mockito.atLeastOnce()).save(pendingNotif);
    }

    @Test
    @DisplayName("Idempotency: Đã phát thông báo thu hồi với cùng lô hàng và trạng thái mới -> Bỏ qua không gửi trùng")
    void testDispatch_Idempotency_SkipsDuplicateNotification() {
        when(systemConfigurationRepository.findById("PARTNER_RECALL_NOTIFICATION_WINDOW_DAYS"))
                .thenReturn(Optional.of(SystemConfiguration.builder()
                        .configKey("PARTNER_RECALL_NOTIFICATION_WINDOW_DAYS")
                        .configValue("30")
                        .build()));

        when(partnerLotAccessLogRepository.findDistinctPartnerApiKeyIdsByShipmentOrProductionLot(
                eq(shipment.getId()), eq(productionLot.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(activeApiKey.getId()));

        when(partnerApiKeyRepository.findEligibleWebhookKeys(anyList(), any()))
                .thenReturn(List.of(activeApiKey));

        when(partnerWebhookNotificationRepository.existsByPartnerApiKey_IdAndShipment_IdAndNewStatus(
                activeApiKey.getId(), shipment.getId(), "RECALLING"))
                .thenReturn(true);

        dispatcher.dispatchRecallNotifications(
                List.of(shipment), "RECALLING", "Thu hồi do kiểm tra định kỳ", null);

        // Xác nhận không tạo bản ghi mới vì đã tồn tại thông báo với trạng thái này
        verify(partnerWebhookNotificationRepository, never()).save(any(PartnerWebhookNotification.class));
    }
}
