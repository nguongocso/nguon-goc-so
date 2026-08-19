package vn.nguongocso.integration.apikey.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.integration.apikey.dto.request.CreateApiKeyRequest;
import vn.nguongocso.integration.apikey.dto.response.PartnerApiKeyResponse;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;

/**
 * Service quản lý vòng đời khóa truy cập của bên thứ ba (NCL-12-CN-001).
 * <p>
 * Quản lý sinh khóa, băm SHA-256, hiển thị duy nhất 1 lần khi tạo mới,
 * thu hồi khóa và theo dõi kiểm soát hạn mức gọi API (Rate Limit per Hour).
 */
@Service
@RequiredArgsConstructor
public class PartnerApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(PartnerApiKeyService.class);
    private static final String KEY_PREFIX_CONSTANT = "nks_live_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PartnerApiKeyRepository partnerApiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    // Bộ nhớ tạm đếm số lượt gọi trong 1 giờ: Key = apiKeyId + ":" + yyyyMMddHH
    private final Map<String, AtomicInteger> hourlyRateLimitMap = new ConcurrentHashMap<>();

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

        PartnerApiKeyResponse response = mapToResponse(savedKey);
        // Gán rawApiKey DUY NHẤT LẦN NÀY
        response.setRawApiKey(rawApiKey);
        return response;
    }

    /**
     * Lấy danh sách khóa truy cập của Hợp tác xã hiện tại (phân trang).
     */
    @Transactional(readOnly = true)
    public Page<PartnerApiKeyResponse> getOrganizationApiKeys(PartnerApiKeyStatus status, Pageable pageable) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        UUID organizationId = currentUser.getOrganizationId();

        Page<PartnerApiKey> page;
        if (status != null) {
            page = partnerApiKeyRepository.findByOrganizationOrganizationIdAndStatus(organizationId, status, pageable);
        } else {
            page = partnerApiKeyRepository.findByOrganizationOrganizationId(organizationId, pageable);
        }

        return page.map(this::mapToResponse);
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
            throw new BusinessException("Khóa truy cập đã hết thời gian hiệu lực");
        }

        // 3. Kiểm tra Hạn mức số lượt gọi trong 1 giờ (Rate Limit per Hour - QTN-20)
        LocalDateTime now = LocalDateTime.now();
        String hourlyKey = apiKey.getId().toString() + ":" + String.format("%04d%02d%02d%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour());

        AtomicInteger currentCallCount = hourlyRateLimitMap.computeIfAbsent(hourlyKey, k -> new AtomicInteger(0));
        int callsInCurrentHour = currentCallCount.incrementAndGet();

        if (callsInCurrentHour > apiKey.getRateLimitPerHour()) {
            recordCallStats(apiKey, false, 429, clientIp);
            throw new BusinessException("Khóa truy cập đã vượt quá hạn mức " + apiKey.getRateLimitPerHour() + " lượt gọi/giờ");
        }

        // Gọi thành công -> Ghi nhận thống kê
        recordCallStats(apiKey, true, 200, clientIp);
        return apiKey;
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

    private PartnerApiKeyResponse mapToResponse(PartnerApiKey key) {
        return PartnerApiKeyResponse.builder()
                .id(key.getId())
                .organizationId(key.getOrganization().getOrganizationId())
                .partnerName(key.getPartnerName())
                .keyPrefix(key.getKeyPrefix())
                .rateLimitPerHour(key.getRateLimitPerHour())
                .expiresAt(key.getExpiresAt())
                .status(key.getStatus())
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
