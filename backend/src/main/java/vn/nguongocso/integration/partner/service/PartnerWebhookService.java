package vn.nguongocso.integration.partner.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.partner.dto.request.PartnerWebhookRegistrationRequest;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookAttemptDto;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookNotificationResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookResponse;
import vn.nguongocso.integration.partner.dto.response.WebhookTestPingResponse;
import vn.nguongocso.integration.partner.entity.PartnerWebhookNotification;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;
import vn.nguongocso.integration.partner.repository.PartnerWebhookNotificationRepository;

/**
 * Service quản lý địa chỉ nhận thông báo (Webhook) và lịch sử gửi thông báo tới đối tác (NCL-12-CN-006).
 */
@Service
@RequiredArgsConstructor
public class PartnerWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PartnerWebhookService.class);

    private final PartnerApiKeyRepository partnerApiKeyRepository;
    private final PartnerWebhookNotificationRepository partnerWebhookNotificationRepository;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Xác thực địa chỉ URL nhận thông báo phải sử dụng giao thức an toàn HTTPS (hoặc localhost khi dev/test).
     */
    public static void validateSecureUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException("Địa chỉ nhận thông báo không được để trống.");
        }
        String trimmed = url.trim();
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new BusinessException("Định dạng URL địa chỉ nhận thông báo không hợp lệ.");
            }
            boolean isHttps = "https".equalsIgnoreCase(scheme);
            boolean isLocalDev = "http".equalsIgnoreCase(scheme) &&
                    ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
            if (!isHttps && !isLocalDev) {
                throw new BusinessException("Địa chỉ nhận thông báo phải sử dụng giao thức bảo mật HTTPS (https://).");
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Định dạng URL địa chỉ nhận thông báo không hợp lệ.");
        }
    }

    /**
     * Đăng ký hoặc cập nhật địa chỉ nhận thông báo webhook cho khóa API (dành cho Quản lý HTX / Quản trị viên).
     */
    @Transactional
    public PartnerWebhookResponse registerWebhookForOrganizationKey(
            UUID apiKeyId,
            PartnerWebhookRegistrationRequest request,
            CustomUserDetails currentUser) {

        PartnerApiKey apiKey = partnerApiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin khóa truy cập đối tác."));

        // Kiểm tra quyền tổ chức: Chỉ quản lý của tổ chức sở hữu khóa hoặc VT-01 mới được cấu hình
        if (!"VT-01".equals(currentUser.getRoleCode()) &&
                !apiKey.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException("Bạn không có quyền quản lý khóa truy cập của tổ chức khác.");
        }

        return applyWebhookRegistration(apiKey, request);
    }

    /**
     * Lấy thông tin cấu hình webhook chi tiết của một khóa API (bao gồm webhookSecret).
     */
    public PartnerWebhookResponse getWebhookForOrganizationKey(UUID apiKeyId, CustomUserDetails currentUser) {
        PartnerApiKey apiKey = partnerApiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin khóa truy cập đối tác."));

        if (!"VT-01".equals(currentUser.getRoleCode()) &&
                !apiKey.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException("Bạn không có quyền truy cập khóa của tổ chức khác.");
        }

        return mapToWebhookResponse(apiKey);
    }

    /**
     * Đăng ký hoặc cập nhật địa chỉ nhận thông báo webhook trực tiếp qua API đối tác (Header X-API-KEY).
     */
    @Transactional
    public PartnerWebhookResponse registerWebhookForPartnerKey(
            PartnerApiKey apiKey,
            PartnerWebhookRegistrationRequest request) {

        if (apiKey == null) {
            throw new BusinessException("Khóa truy cập không hợp lệ.");
        }

        return applyWebhookRegistration(apiKey, request);
    }

    private PartnerWebhookResponse applyWebhookRegistration(
            PartnerApiKey apiKey,
            PartnerWebhookRegistrationRequest request) {

        if (request.getWebhookUrl() == null || request.getWebhookUrl().isBlank()) {
            apiKey.setWebhookUrl(null);
            apiKey.setIsWebhookActive(false);
            PartnerApiKey saved = partnerApiKeyRepository.save(apiKey);
            return mapToWebhookResponse(saved);
        }

        validateSecureUrl(request.getWebhookUrl());

        apiKey.setWebhookUrl(request.getWebhookUrl().trim());
        apiKey.setIsWebhookActive(request.getIsActive() == null || request.getIsActive());

        // Nếu chưa có khóa bí mật ký số, sinh mới
        if (apiKey.getWebhookSecret() == null || apiKey.getWebhookSecret().isBlank()) {
            apiKey.setWebhookSecret("sec_wh_" + UUID.randomUUID().toString().replace("-", ""));
        }

        PartnerApiKey saved = partnerApiKeyRepository.save(apiKey);
        log.info("Đã cập nhật Webhook URL cho đối tác '{}' (keyId={}): {}",
                saved.getPartnerName(), saved.getId(), saved.getWebhookUrl());

        return mapToWebhookResponse(saved);
    }

    /**
     * Bắn thử nghiệm webhook (Test Ping) kiểm tra kết nối tới máy chủ đối tác.
     */
    public WebhookTestPingResponse sendTestPing(UUID apiKeyId, CustomUserDetails currentUser) {
        PartnerApiKey apiKey = partnerApiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin khóa truy cập đối tác."));

        if (!"VT-01".equals(currentUser.getRoleCode()) &&
                !apiKey.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException("Bạn không có quyền thao tác trên khóa truy cập này.");
        }

        String webhookUrl = apiKey.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new BusinessException("Khóa này chưa được đăng ký địa chỉ nhận thông báo Webhook.");
        }

        return executePingRequest(webhookUrl, apiKey.getWebhookSecret());
    }

    /**
     * Thực hiện gửi HTTP POST Ping.
     */
    public WebhookTestPingResponse executePingRequest(String targetUrl, String secret) {
        long startTime = System.currentTimeMillis();
        String pingPayload = "{\"event\":\"PING\",\"timestamp\":\"" + LocalDateTime.now() + "\",\"message\":\"NguonGocSo Webhook Test Ping\"}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("User-Agent", "NguonGocSo-Webhook/1.0")
                    .header("X-Webhook-Event", "PING")
                    .POST(HttpRequest.BodyPublishers.ofString(pingPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;
            int status = response.statusCode();
            boolean success = status >= 200 && status < 300;

            String body = response.body();
            if (body != null && body.length() > 500) {
                body = body.substring(0, 500) + "...";
            }

            return WebhookTestPingResponse.builder()
                    .targetUrl(targetUrl)
                    .httpStatus(status)
                    .durationMs(duration)
                    .isSuccess(success)
                    .responseBody(body)
                    .errorMessage(success ? null : "Phản hồi mã trạng thái HTTP " + status)
                    .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Lỗi kết nối khi bắn thử webhook tới {}: {}", targetUrl, e.getMessage());
            return WebhookTestPingResponse.builder()
                    .targetUrl(targetUrl)
                    .httpStatus(null)
                    .durationMs(duration)
                    .isSuccess(false)
                    .responseBody(null)
                    .errorMessage("Không thể kết nối tới máy chủ đối tác: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Lấy lịch sử thông báo thu hồi của một khóa API (dành cho màn hình quản lý khóa).
     */
    @Transactional(readOnly = true)
    public Page<PartnerWebhookNotificationResponse> getNotificationsForOrganizationKey(
            UUID apiKeyId,
            WebhookDeliveryStatus status,
            Pageable pageable,
            CustomUserDetails currentUser) {

        PartnerApiKey apiKey = partnerApiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin khóa truy cập đối tác."));

        if (!"VT-01".equals(currentUser.getRoleCode()) &&
                !apiKey.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException("Bạn không có quyền xem thông báo của khóa này.");
        }

        Page<PartnerWebhookNotification> pageData = (status != null)
                ? partnerWebhookNotificationRepository.findByPartnerApiKey_IdAndDeliveryStatusOrderByCreatedAtDesc(apiKeyId, status, pageable)
                : partnerWebhookNotificationRepository.findByPartnerApiKey_IdOrderByCreatedAtDesc(apiKeyId, pageable);

        List<PartnerWebhookNotificationResponse> dtoList = pageData.getContent().stream()
                .map(this::mapToNotificationResponse)
                .toList();

        return new PageImpl<>(dtoList, pageable, pageData.getTotalElements());
    }

    /**
     * Lấy lịch sử thông báo thu hồi của khóa API đối tác hiện tại (qua Cổng đối tác).
     */
    @Transactional(readOnly = true)
    public Page<PartnerWebhookNotificationResponse> getNotificationsForPartnerKey(
            PartnerApiKey apiKey,
            WebhookDeliveryStatus status,
            Pageable pageable) {

        if (apiKey == null) {
            throw new BusinessException("Khóa truy cập không hợp lệ.");
        }

        Page<PartnerWebhookNotification> pageData = (status != null)
                ? partnerWebhookNotificationRepository.findByPartnerApiKey_IdAndDeliveryStatusOrderByCreatedAtDesc(apiKey.getId(), status, pageable)
                : partnerWebhookNotificationRepository.findByPartnerApiKey_IdOrderByCreatedAtDesc(apiKey.getId(), pageable);

        List<PartnerWebhookNotificationResponse> dtoList = pageData.getContent().stream()
                .map(this::mapToNotificationResponse)
                .toList();

        return new PageImpl<>(dtoList, pageable, pageData.getTotalElements());
    }

    private PartnerWebhookResponse mapToWebhookResponse(PartnerApiKey key) {
        return PartnerWebhookResponse.builder()
                .id(key.getId())
                .partnerName(key.getPartnerName())
                .keyPrefix(key.getKeyPrefix())
                .webhookUrl(key.getWebhookUrl())
                .isWebhookActive(key.getIsWebhookActive())
                .webhookSecret(key.getWebhookSecret())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public PartnerWebhookNotificationResponse mapToNotificationResponse(PartnerWebhookNotification notification) {
        List<PartnerWebhookAttemptDto> attempts = parseAttemptsLog(notification.getAttemptsLog());

        return PartnerWebhookNotificationResponse.builder()
                .id(notification.getId())
                .partnerApiKeyId(notification.getPartnerApiKey().getId())
                .partnerName(notification.getPartnerApiKey().getPartnerName())
                .shipmentId(notification.getShipment().getId())
                .lotCode(notification.getLotCode())
                .newStatus(notification.getNewStatus())
                .targetUrl(notification.getTargetUrl())
                .publicReason(notification.getPublicReason())
                .deliveryStatus(notification.getDeliveryStatus())
                .attemptCount(notification.getAttemptCount())
                .maxAttempts(notification.getMaxAttempts())
                .nextRetryAt(notification.getNextRetryAt())
                .lastHttpStatus(notification.getLastHttpStatus())
                .lastErrorMessage(notification.getLastErrorMessage())
                .createdAt(notification.getCreatedAt())
                .completedAt(notification.getCompletedAt())
                .attempts(attempts)
                .build();
    }

    public List<PartnerWebhookAttemptDto> parseAttemptsLog(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<PartnerWebhookAttemptDto>>() {});
        } catch (Exception e) {
            log.warn("Không thể parse attemptsLog JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
