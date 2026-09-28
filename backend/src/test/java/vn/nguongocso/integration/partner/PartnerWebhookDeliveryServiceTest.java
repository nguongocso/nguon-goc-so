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

import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookAttemptDto;
import vn.nguongocso.integration.partner.entity.PartnerWebhookNotification;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;
import vn.nguongocso.integration.partner.repository.PartnerLotAccessLogRepository;
import vn.nguongocso.integration.partner.repository.PartnerWebhookNotificationRepository;
import vn.nguongocso.integration.partner.service.PartnerWebhookDeliveryService;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Unit test độc lập cho PartnerWebhookDeliveryService (NCL-12-CN-006).
 */
@ExtendWith(MockitoExtension.class)
class PartnerWebhookDeliveryServiceTest {

    @Mock
    private PartnerLotAccessLogRepository partnerLotAccessLogRepository;

    @Mock
    private PartnerApiKeyRepository partnerApiKeyRepository;

    @Mock
    private PartnerWebhookNotificationRepository partnerWebhookNotificationRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private PartnerWebhookDeliveryService deliveryService;

    private Shipment shipment;
    private ProductionLot productionLot;
    private PartnerApiKey activeApiKey;
    private PartnerApiKey revokedApiKey;

    @BeforeEach
    void setUp() {
        productionLot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("LOT-LUA-ST25-001")
                .build();

        shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("SHIPMENT-ST25-HCM");
        shipment.setProductionLot(productionLot);

        activeApiKey = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .partnerName("Công ty Bán lẻ X")
                .webhookUrl("https://127.0.0.1:59999/webhook")
                .webhookSecret("whsec_secret123")
                .isWebhookActive(true)
                .status(PartnerApiKeyStatus.ACTIVE)
                .isTest(false)
                .build();

        revokedApiKey = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .partnerName("Công ty Y (Bị thu hồi)")
                .webhookUrl("https://127.0.0.1:59999/webhook")
                .webhookSecret("whsec_secret456")
                .isWebhookActive(true)
                .status(PartnerApiKeyStatus.REVOKED)
                .isTest(false)
                .build();
    }

    @Test
    @DisplayName("TC-03: Đối tác chưa từng truy xuất lô -> Bỏ qua không gửi thông báo")
    void testProcessShipmentRecall_TC03_NoCandidatePartners() {
        when(partnerLotAccessLogRepository.findDistinctPartnerApiKeyIdsByShipmentOrProductionLot(
                eq(shipment.getId()), eq(productionLot.getId()), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        deliveryService.processShipmentRecall(shipment, "RECALLING", "Thu hồi", null, LocalDateTime.now().minusDays(30));

        verify(partnerApiKeyRepository, never()).findEligibleWebhookKeys(anyList(), any());
        verify(partnerWebhookNotificationRepository, never()).save(any(PartnerWebhookNotification.class));
    }

    @Test
    @DisplayName("TC-04: Khóa đối tác bị REVOKED -> Bỏ qua không tạo bản ghi thông báo")
    void testProcessShipmentRecall_TC04_RevokedKeySkipped() {
        when(partnerLotAccessLogRepository.findDistinctPartnerApiKeyIdsByShipmentOrProductionLot(
                eq(shipment.getId()), eq(productionLot.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(revokedApiKey.getId()));

        when(partnerApiKeyRepository.findEligibleWebhookKeys(anyList(), any()))
                .thenReturn(List.of(revokedApiKey));

        deliveryService.processShipmentRecall(shipment, "RECALLING", "Thu hồi", null, LocalDateTime.now().minusDays(30));

        verify(partnerWebhookNotificationRepository, never()).save(any(PartnerWebhookNotification.class));
    }

    @Test
    @DisplayName("Idempotency: Bỏ qua tạo thông báo trùng nếu đã tồn tại bản ghi cùng trạng thái")
    void testProcessShipmentRecall_Idempotency_DuplicateSkipped() {
        when(partnerLotAccessLogRepository.findDistinctPartnerApiKeyIdsByShipmentOrProductionLot(
                eq(shipment.getId()), eq(productionLot.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(activeApiKey.getId()));

        when(partnerApiKeyRepository.findEligibleWebhookKeys(anyList(), any()))
                .thenReturn(List.of(activeApiKey));

        when(partnerWebhookNotificationRepository.existsByPartnerApiKey_IdAndShipment_IdAndNewStatus(
                activeApiKey.getId(), shipment.getId(), "RECALLING"))
                .thenReturn(true);

        deliveryService.processShipmentRecall(shipment, "RECALLING", "Thu hồi", null, LocalDateTime.now().minusDays(30));

        verify(partnerWebhookNotificationRepository, never()).save(any(PartnerWebhookNotification.class));
    }

    @Test
    @DisplayName("TC-04: Lượt gửi bị hủy ngay nếu khóa bị REVOKED lúc thực thi (CANCELLED)")
    void testExecuteWebhookDelivery_KeyRevoked_CancelsNotification_TC04() {
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

        deliveryService.executeWebhookDelivery(pendingNotification, revokedApiKey.getWebhookSecret());

        assertEquals(WebhookDeliveryStatus.CANCELLED, pendingNotification.getDeliveryStatus());
        assertNotNull(pendingNotification.getCompletedAt());
        assertTrue(pendingNotification.getLastErrorMessage().contains("TC-04"));
        verify(partnerWebhookNotificationRepository).save(pendingNotification);
    }

    @Test
    @DisplayName("Tính toán HMAC-SHA256 chuẩn xác (64 ký tự hex)")
    void testComputeHmacSha256() {
        String data = "1672531199.{\"eventId\":\"12345\"}";
        String secret = "whsec_testkey";

        String signature = PartnerWebhookDeliveryService.computeHmacSha256(data, secret);

        assertNotNull(signature);
        assertFalse(signature.isBlank());
        assertEquals(64, signature.length());
    }

    @Test
    @DisplayName("Parse nhật ký các lượt gửi Webhook từ chuỗi JSON")
    void testParseAttemptsLog() {
        List<PartnerWebhookAttemptDto> emptyList = deliveryService.parseAttemptsLog(null);
        assertTrue(emptyList.isEmpty());

        String json = "[{\"attemptNumber\":1,\"httpStatus\":500,\"durationMs\":120,\"errorMessage\":\"Server Error\"}]";
        List<PartnerWebhookAttemptDto> attempts = deliveryService.parseAttemptsLog(json);
        assertEquals(1, attempts.size());
        assertEquals(1, attempts.get(0).getAttemptNumber());
        assertEquals(500, attempts.get(0).getHttpStatus());
        assertEquals(120, attempts.get(0).getDurationMs());
    }
}
