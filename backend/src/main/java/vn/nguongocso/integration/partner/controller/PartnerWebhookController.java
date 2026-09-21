package vn.nguongocso.integration.partner.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import vn.nguongocso.common.ApiResult;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.partner.dto.request.PartnerWebhookRegistrationRequest;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookNotificationResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookResponse;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;
import vn.nguongocso.integration.partner.service.PartnerWebhookService;

/**
 * Controller cổng dữ liệu đối tác cho phép bên thứ ba tự đăng ký webhook và xem lịch sử thông báo (NCL-12-CN-006).
 * <p>
 * Yêu cầu đối tác gửi Header {@code X-API-KEY}. Đã qua xác thực từ {@code ApiKeyAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/v1/partner")
@RequiredArgsConstructor
public class PartnerWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PartnerWebhookController.class);

    private final PartnerWebhookService partnerWebhookService;

    /**
     * Đối tác tự đăng ký hoặc cập nhật địa chỉ nhận thông báo Webhook qua Header X-API-KEY.
     */
    @PutMapping("/webhook")
    public ResponseEntity<ApiResult<PartnerWebhookResponse>> updatePartnerWebhook(
            @Valid @RequestBody PartnerWebhookRegistrationRequest request,
            HttpServletRequest httpRequest) {

        PartnerApiKey partnerApiKey = (PartnerApiKey) httpRequest.getAttribute("partnerApiKey");
        if (partnerApiKey == null) {
            throw new BusinessException("Thiếu hoặc không xác thực được khóa truy cập Header X-API-KEY");
        }

        log.info("Đối tác '{}' (keyId={}) tự cấu hình Webhook URL: {}",
                partnerApiKey.getPartnerName(), partnerApiKey.getId(), request.getWebhookUrl());

        PartnerWebhookResponse response = partnerWebhookService.registerWebhookForPartnerKey(partnerApiKey, request);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Đối tác tra cứu lịch sử thông báo thu hồi đã gửi tới mình qua Header X-API-KEY.
     */
    @GetMapping("/notifications")
    public ResponseEntity<ApiResult<Page<PartnerWebhookNotificationResponse>>> getPartnerNotifications(
            @RequestParam(required = false) WebhookDeliveryStatus deliveryStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        PartnerApiKey partnerApiKey = (PartnerApiKey) httpRequest.getAttribute("partnerApiKey");
        if (partnerApiKey == null) {
            throw new BusinessException("Thiếu hoặc không xác thực được khóa truy cập Header X-API-KEY");
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<PartnerWebhookNotificationResponse> response = partnerWebhookService
                .getNotificationsForPartnerKey(partnerApiKey, deliveryStatus, pageable);

        return ResponseEntity.ok(ApiResult.success(response));
    }
}
