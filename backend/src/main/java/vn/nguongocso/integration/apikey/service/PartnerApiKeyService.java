package vn.nguongocso.integration.apikey.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.integration.apikey.dto.request.CreateApiKeyRequest;
import vn.nguongocso.integration.apikey.dto.request.CreateTestApiKeyRequest;
import vn.nguongocso.integration.apikey.dto.response.PartnerApiKeyPageResponse;
import vn.nguongocso.integration.apikey.dto.response.PartnerApiKeyResponse;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.event.ApiKeyQuotaThresholdEvent;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;

/**
 * Service quản lý vòng đời khóa truy cập của bên thứ ba (NCL-12-CN-001, NCL-12-CN-004).
 * <p>
 * Quản lý sinh khóa thật (Live Key) và khóa thử nghiệm (Test Key), băm SHA-256, hiển thị duy nhất 1 lần khi tạo mới,
 * thu hồi khóa và theo dõi kiểm soát hạn mức gọi API (Rate Limit per Hour - QTN-20).
 */
@Service
@RequiredArgsConstructor
public class PartnerApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(PartnerApiKeyService.class);
    private static final String KEY_PREFIX_CONSTANT = "nks_live_";
    private static final String TEST_KEY_PREFIX_CONSTANT = "nks_test_";
    private static final int MAX_TEST_RATE_LIMIT = 100;
    private static final int MAX_TEST_EXPIRE_DAYS = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PartnerApiKeyRepository partnerApiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PartnerApiKeyUsageService partnerApiKeyUsageService;
    private final ApiKeyQuotaPolicy apiKeyQuotaPolicy;

    // Bộ nhớ tạm đếm số lượt gọi trong 1 giờ: Key = apiKeyId + ":" + yyyyMMddHH
    private final Map<String, AtomicInteger> hourlyRateLimitMap = new ConcurrentHashMap<>();

    // Ghi chú (NCL-12-CN-005): bộ đếm lượt gọi THEO NGÀY không còn ở bộ nhớ tạm mà
    // được lưu ở DB qua PartnerApiKeyUsageService: không mất khi khởi động lại và
    // không gửi trùng khi chạy nhiều instance. Bộ đếm THEO GIỜ ở trên vẫn giữ cho
    // rate limit QTN-20 (trả 429 khi vượt rateLimitPerHour trong 1 giờ).

    /**
     * Tạo mới khóa truy cập cho đối tác.
     * <p>
     * Trả về DTO chứa {@code rawApiKey} duy nhất một lần.
     */
    @Transactional
    public PartnerApiKeyResponse createApiKey(CreateApiKeyRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        UUID organizationId = currentUser.getOrganizationId();
        UUID userId = currentUser.getUserId();

        // Kiểm tra ngày hết hạn phải ở tương lai (TC-03)
        if (request.getExpiresAt() == null || !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Ngày hết hạn của khóa truy cập phải ở thời điểm tương lai");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tổ chức"));
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));

        // 1. Sinh chuỗi ngẫu nhiên 32-byte an toàn -> Hex string
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomHex = bytesToHex(randomBytes);

        // 2. Tạo rawApiKey và keyPrefix, keyHash
        String rawApiKey = KEY_PREFIX_CONSTANT + randomHex;
        String keyPrefix = KEY_PREFIX_CONSTANT + randomHex.substring(0, 8);
        String keyHash = hashSha256(rawApiKey);

        // 3. Khởi tạo Entity
        PartnerApiKey apiKey = PartnerApiKey.builder()
                .organization(organization)
                .partnerName(request.getPartnerName().trim())
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .rateLimitPerHour(request.getRateLimitPerHour())
                .expiresAt(request.getExpiresAt())
                .status(PartnerApiKeyStatus.ACTIVE)
                .totalCalls(0L)
                .failedCalls(0L)
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .build();

        PartnerApiKey savedKey = partnerApiKeyRepository.save(apiKey);
        log.info("Đã cấp khóa truy cập cho đối tác '{}', orgId={}, keyPrefix={}",
                savedKey.getPartnerName(), organizationId, keyPrefix);

        // Ghi nhật ký hoạt động (TASK-27): không ghi rawApiKey/keyHash vì là dữ liệu nhạy cảm
        publishActivityLog(currentUser, "CREATE_API_KEY",
                "Cấp khóa truy cập cho đối tác '" + savedKey.getPartnerName()
                        + "' (mã khóa " + savedKey.getKeyPrefix() + "...)",
                "PARTNER_API_KEY", savedKey.getId().toString());

        PartnerApiKeyResponse response = mapToResponse(savedKey);
        // Gán rawApiKey DUY NHẤT LẦN NÀY
        response.setRawApiKey(rawApiKey);
        return response;
    }

    /**
     * Tạo mới khóa thử nghiệm (Sandbox) cho đối tác (TC-01, TC-04).
     * <p>
     * Khóa thử nghiệm có tiền tố {@code nks_test_}, gắn cờ {@code isTest = true},
     * giới hạn thời hạn tối đa 30 ngày và hạn mức tối đa 100 lượt/giờ.
     * Trả về DTO chứa {@code rawApiKey} duy nhất một lần.
     */
    @Transactional
    public PartnerApiKeyResponse createTestApiKey(CreateTestApiKeyRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        UUID organizationId = currentUser.getOrganizationId();
        UUID userId = currentUser.getUserId();

        if (request.getPartnerName() == null || request.getPartnerName().isBlank()) {
            throw new BusinessException("Tên đối tác hoặc tên khóa thử nghiệm không được để trống");
        }
        if (request.getRateLimitPerHour() == null) {
            request.setRateLimitPerHour(60);
        }
        if (request.getExpiresAt() == null) {
            request.setExpiresAt(LocalDateTime.now().plusDays(7));
        }

        // 1. Kiểm tra ngày hết hạn phải ở tương lai
        if (!request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Ngày hết hạn của khóa truy cập phải ở thời điểm tương lai");
        }

        // 2. Ràng buộc thời hạn ngắn cho khóa thử nghiệm: tối đa 30 ngày
        if (request.getExpiresAt().isAfter(LocalDateTime.now().plusDays(MAX_TEST_EXPIRE_DAYS))) {
            throw new BusinessException("Thời hạn khóa thử nghiệm không được vượt quá " + MAX_TEST_EXPIRE_DAYS + " ngày");
        }

        // 3. Ràng buộc hạn mức thấp cho khóa thử nghiệm: tối đa 100 lượt/giờ
        if (request.getRateLimitPerHour() != null && request.getRateLimitPerHour() > MAX_TEST_RATE_LIMIT) {
            throw new BusinessException("Hạn mức số lượt gọi thử nghiệm không vượt quá " + MAX_TEST_RATE_LIMIT + " lượt/giờ");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tổ chức"));
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));

        // 4. Sinh chuỗi ngẫu nhiên 32-byte an toàn -> Hex string
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomHex = bytesToHex(randomBytes);

        // 5. Tạo rawApiKey với tiền tố nks_test_
        String rawApiKey = TEST_KEY_PREFIX_CONSTANT + randomHex;
        String keyPrefix = TEST_KEY_PREFIX_CONSTANT + randomHex.substring(0, 8);
        String keyHash = hashSha256(rawApiKey);

        // 6. Khởi tạo Entity với isTest = true
        PartnerApiKey apiKey = PartnerApiKey.builder()
                .organization(organization)
                .partnerName(request.getPartnerName().trim())
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .rateLimitPerHour(request.getRateLimitPerHour())
                .expiresAt(request.getExpiresAt())
                .status(PartnerApiKeyStatus.ACTIVE)
                .isTest(true)
                .totalCalls(0L)
                .failedCalls(0L)
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .build();

        PartnerApiKey savedKey = partnerApiKeyRepository.save(apiKey);
        log.info("Đã cấp khóa thử nghiệm (Sandbox) cho đối tác '{}', orgId={}, keyPrefix={}",
                savedKey.getPartnerName(), organizationId, keyPrefix);

        // Ghi nhật ký hoạt động
        publishActivityLog(currentUser, "CREATE_TEST_API_KEY",
                "Cấp khóa thử nghiệm cho đối tác '" + savedKey.getPartnerName()
                        + "' (mã khóa " + savedKey.getKeyPrefix() + "...)",
                "PARTNER_API_KEY", savedKey.getId().toString());

        PartnerApiKeyResponse response = mapToResponse(savedKey);
        response.setRawApiKey(rawApiKey);
        return response;
    }

    /**
     * Lấy danh sách khóa truy cập của Hợp tác xã hiện tại (phân trang).
     * <p>
     * Trả DTO phân trang tường minh thay cho Spring {@code Page} để FE đọc đúng
     * {@code totalElements/totalPages} (NCL-12-CN-005).
     */
    @Transactional(readOnly = true)
    public PartnerApiKeyPageResponse getOrganizationApiKeys(PartnerApiKeyStatus status, Pageable pageable) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        UUID organizationId = currentUser.getOrganizationId();

        Page<PartnerApiKey> page;
        if (status != null) {
            page = partnerApiKeyRepository.findByOrganizationOrganizationIdAndStatus(organizationId, status, pageable);
        } else {
            page = partnerApiKeyRepository.findByOrganizationOrganizationId(organizationId, pageable);
        }

        return toPageResponse(page);
    }

    /**
     * Đóng gói kết quả phân trang khóa truy cập thành DTO tường minh.
     */
    private PartnerApiKeyPageResponse toPageResponse(Page<PartnerApiKey> page) {
        List<PartnerApiKeyResponse> content = page.map(this::mapToResponse).getContent();

        // NCL-12-CN-005: bổ sung số lượt gọi hôm nay + ngưỡng cảnh báo hạn mức bằng
        // một truy vấn duy nhất (tránh N+1) để FE hiển thị mức dùng mà không tự
        // hardcode tỷ lệ cấu hình.
        List<UUID> keyIds = content.stream().map(PartnerApiKeyResponse::getId).toList();
        Map<UUID, Integer> usedCallsToday = partnerApiKeyUsageService.getDailyCallCounts(keyIds);
        for (PartnerApiKeyResponse item : content) {
            item.setUsedCallsToday(usedCallsToday.getOrDefault(item.getId(), 0));
            item.setQuotaWarningThreshold(item.getRateLimitPerHour() == null
                    ? null
                    : apiKeyQuotaPolicy.warningThreshold(item.getRateLimitPerHour()));
        }

        return PartnerApiKeyPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    /**
     * Thu hồi khóa truy cập (vô hiệu hóa ngay lập tức - TC-02).
     */
    @Transactional
    public PartnerApiKeyResponse revokeApiKey(UUID apiKeyId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        UUID organizationId = currentUser.getOrganizationId();
        UUID userId = currentUser.getUserId();

        PartnerApiKey apiKey = partnerApiKeyRepository.findByIdAndOrganizationId(apiKeyId, organizationId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy khóa truy cập trong tổ chức"));

        if (apiKey.getStatus() == PartnerApiKeyStatus.REVOKED) {
            throw new BusinessException("Khóa truy cập này đã bị thu hồi trước đó");
        }

        User revoker = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người thực hiện thu hồi"));

        apiKey.setStatus(PartnerApiKeyStatus.REVOKED);
        apiKey.setRevokedBy(revoker);
        apiKey.setRevokedAt(LocalDateTime.now());

        PartnerApiKey updatedKey = partnerApiKeyRepository.save(apiKey);
        log.info("Đã thu hồi khóa truy cập id={}, partnerName={}, orgId={}",
                apiKeyId, updatedKey.getPartnerName(), organizationId);

        // Ghi nhật ký hoạt động (TASK-27)
        publishActivityLog(currentUser, "REVOKE_API_KEY",
                "Thu hồi khóa truy cập của đối tác '" + updatedKey.getPartnerName()
                        + "' (mã khóa " + updatedKey.getKeyPrefix() + "...)",
                "PARTNER_API_KEY", updatedKey.getId().toString());

        return mapToResponse(updatedKey);
    }

    /**
     * Kiểm tra tính hợp lệ và hạn mức của rawApiKey từ đối tác.
     * <p>
     * Trả về {@link PartnerApiKey} nếu hợp lệ, ném ngoại lệ tương ứng nếu vi phạm (QTN-20).
     */
    @Transactional
    public PartnerApiKey validateApiKeyAndCheckRateLimit(String rawApiKey, String clientIp) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new BusinessException("Thiếu Header X-API-KEY");
        }

        String keyHash = hashSha256(rawApiKey.trim());
        Optional<PartnerApiKey> apiKeyOpt = partnerApiKeyRepository.findByKeyHash(keyHash);

        if (apiKeyOpt.isEmpty()) {
            throw new BusinessException("Khóa truy cập không hợp lệ");
        }

        PartnerApiKey apiKey = apiKeyOpt.get();

        // 1. Kiểm tra trạng thái REVOKED
        if (apiKey.getStatus() == PartnerApiKeyStatus.REVOKED) {
            recordCallStats(apiKey, false, 401, clientIp);
            throw new BusinessException("Khóa truy cập đã bị thu hồi và không còn hiệu lực");
        }

        // 2. Kiểm tra ngày hết hạn
        if (LocalDateTime.now().isAfter(apiKey.getExpiresAt())) {
            if (apiKey.getStatus() != PartnerApiKeyStatus.EXPIRED) {
                apiKey.setStatus(PartnerApiKeyStatus.EXPIRED);
                partnerApiKeyRepository.save(apiKey);
            }
            recordCallStats(apiKey, false, 401, clientIp);
            if (Boolean.TRUE.equals(apiKey.getIsTest())) {
                throw new BusinessException("Khóa thử nghiệm đã hết hạn");
            }
            throw new BusinessException("Khóa truy cập đã hết thời gian hiệu lực");
        }

        // 3. Kiểm tra Hạn mức số lượt gọi trong 1 giờ (Rate Limit per Hour - QTN-20)
        LocalDateTime now = LocalDateTime.now();
        String hourlyKey = buildHourlyKey(apiKey.getId(), now);

        AtomicInteger currentCallCount = hourlyRateLimitMap.computeIfAbsent(hourlyKey, k -> new AtomicInteger(0));
        int callsInCurrentHour = currentCallCount.incrementAndGet();

        if (callsInCurrentHour > apiKey.getRateLimitPerHour()) {
            recordCallStats(apiKey, false, 429, clientIp);
            throw new BusinessException("Khóa truy cập đã vượt quá hạn mức " + apiKey.getRateLimitPerHour() + " lượt gọi/giờ");
        }

        // 4. Chạm ngưỡng cảnh báo hạn mức (NCL-12-CN-005): đếm lượt gọi trong NGÀY ở
        // DB (không còn dùng bộ nhớ tạm) để số liệu bền vững qua restart và đúng khi
        // chạy nhiều instance. Dùng ">=" để tự chữa khi bộ đếm đã vượt ngưỡng;
        // listener chống trùng bằng cờ warning_sent_at. Chặn 429 vẫn theo cửa sổ giờ.
        int callsInCurrentDay = partnerApiKeyUsageService.recordCallAndGetDailyCount(apiKey.getId());
        int warningThreshold = apiKeyQuotaPolicy.warningThreshold(apiKey.getRateLimitPerHour());
        if (warningThreshold > 0 && callsInCurrentDay >= warningThreshold) {
            publishQuotaThresholdEvent(apiKey, callsInCurrentDay, warningThreshold);
        }

        // Gọi thành công -> Ghi nhận thống kê
        recordCallStats(apiKey, true, 200, clientIp);
        return apiKey;
    }

    /**
     * Dựng khóa đếm theo giờ cho bộ nhớ tạm rate-limit.
     */
    private String buildHourlyKey(UUID apiKeyId, LocalDateTime time) {
        return apiKeyId.toString() + ":" + String.format("%04d%02d%02d%02d",
                time.getYear(), time.getMonthValue(), time.getDayOfMonth(), time.getHour());
    }

    /**
     * Phát sự kiện chạm ngưỡng hạn mức. Không bao giờ ném lỗi để tránh chặn
     * request của đối tác.
     */
    private void publishQuotaThresholdEvent(PartnerApiKey apiKey, int usedCalls, int warningThreshold) {
        try {
            eventPublisher.publishEvent(ApiKeyQuotaThresholdEvent.builder()
                    .apiKeyId(apiKey.getId())
                    .organizationId(apiKey.getOrganization().getOrganizationId())
                    .partnerName(apiKey.getPartnerName())
                    .rateLimitPerHour(apiKey.getRateLimitPerHour())
                    .usedCalls(usedCalls)
                    .build());
        } catch (Exception e) {
            log.warn("Bỏ qua lỗi phát sự kiện chạm ngưỡng hạn mức cho khóa {}", apiKey.getId(), e);
        }
    }

    /**
     * Ghi nhận chỉ số thống kê lượt gọi trực tiếp vào bảng {@code partner_api_keys}.
     */
    private void recordCallStats(PartnerApiKey apiKey, boolean success, int httpStatus, String clientIp) {
        try {
            if (success) {
                apiKey.setTotalCalls((apiKey.getTotalCalls() == null ? 0 : apiKey.getTotalCalls()) + 1);
            } else {
                apiKey.setFailedCalls((apiKey.getFailedCalls() == null ? 0 : apiKey.getFailedCalls()) + 1);
            }
            apiKey.setLastCalledAt(LocalDateTime.now());
            apiKey.setLastCallStatus(httpStatus);
            apiKey.setLastCallIp(clientIp);
            partnerApiKeyRepository.save(apiKey);
        } catch (Exception e) {
            log.error("Lỗi cập nhật thống kê lượt gọi cho apiKeyId={}", apiKey.getId(), e);
        }
    }

    /**
     * Băm chuỗi bằng SHA-256.
     */
    public static String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi thuật toán mã hóa SHA-256", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Ghi nhật ký hoạt động theo convention của hệ thống (TASK-27).
     * <p>
     * Actor lấy từ security context hiện tại (không tin dữ liệu client),
     * organization lấy từ organization của người thực hiện.
     */
    private void publishActivityLog(CustomUserDetails currentUser, String action, String description,
            String entityType, String entityId) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private PartnerApiKeyResponse mapToResponse(PartnerApiKey key) {
        PartnerApiKeyStatus status = key.getStatus();
        if (status == PartnerApiKeyStatus.ACTIVE && key.getExpiresAt() != null && LocalDateTime.now().isAfter(key.getExpiresAt())) {
            status = PartnerApiKeyStatus.EXPIRED;
        }

        return PartnerApiKeyResponse.builder()
                .id(key.getId())
                .organizationId(key.getOrganization().getOrganizationId())
                .partnerName(key.getPartnerName())
                .keyPrefix(key.getKeyPrefix())
                .rateLimitPerHour(key.getRateLimitPerHour())
                .expiresAt(key.getExpiresAt())
                .status(status)
                .isTest(key.getIsTest())
                .totalCalls(key.getTotalCalls())
                .failedCalls(key.getFailedCalls())
                .lastCalledAt(key.getLastCalledAt())
                .lastCallStatus(key.getLastCallStatus())
                .lastCallIp(key.getLastCallIp())
                .createdByName(key.getCreatedBy() != null ? key.getCreatedBy().getFullName() : null)
                .createdAt(key.getCreatedAt())
                .revokedByName(key.getRevokedBy() != null ? key.getRevokedBy().getFullName() : null)
                .revokedAt(key.getRevokedAt())
                .build();
    }
}
