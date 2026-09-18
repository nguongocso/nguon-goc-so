package vn.nguongocso.certification.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import vn.nguongocso.exception.BusinessException;

/**
 * Dịch vụ giới hạn tần suất truy cập (rate limiting) cho cổng nhập kết quả công khai (QTN-20).
 *
 * <p>
 * Áp dụng giới hạn trong bộ nhớ:
 * - Tối đa 60 yêu cầu/giờ đối với mỗi token hợp lệ.
 * - Tối đa 30 yêu cầu không hợp lệ/giờ đối với mỗi địa chỉ IP.
 * </p>
 */
@Service
public class InspectionResultPortalRateLimitService {

    private static final String MSG_RATE_LIMIT_EXCEEDED =
            "Bạn thao tác quá nhanh. Vui lòng thử lại sau.";

    private static final int MAX_REQUESTS_PER_TOKEN_PER_HOUR = 60;
    private static final int MAX_INVALID_ATTEMPTS_PER_IP_PER_HOUR = 30;
    private static final long ONE_HOUR_SECONDS = 3600L;

    private final ConcurrentHashMap<String, RateLimitBucket> tokenBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateLimitBucket> ipInvalidBuckets = new ConcurrentHashMap<>();

    /**
     * Kiểm tra tần suất gọi cho một token hợp lệ.
     *
     * @param tokenHash Chuỗi token hoặc mã băm của token.
     */
    public void checkTokenRateLimit(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            return;
        }

        long currentWindow = Instant.now().getEpochSecond() / ONE_HOUR_SECONDS;
        RateLimitBucket bucket = tokenBuckets.compute(tokenHash, (k, existing) -> {
            if (existing == null || existing.window() != currentWindow) {
                return new RateLimitBucket(currentWindow, new AtomicInteger(1));
            }
            existing.counter().incrementAndGet();
            return existing;
        });

        if (bucket.counter().get() > MAX_REQUESTS_PER_TOKEN_PER_HOUR) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, MSG_RATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * Kiểm tra tần suất gọi từ địa chỉ IP trước khi xử lý.
     *
     * @param ipAddress Địa chỉ IP của client.
     */
    public void checkIpRateLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        long currentWindow = Instant.now().getEpochSecond() / ONE_HOUR_SECONDS;
        RateLimitBucket bucket = ipInvalidBuckets.get(ipAddress);
        if (bucket != null && bucket.window() == currentWindow
                && bucket.counter().get() >= MAX_INVALID_ATTEMPTS_PER_IP_PER_HOUR) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, MSG_RATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * Ghi nhận một lần thử không hợp lệ từ địa chỉ IP.
     *
     * @param ipAddress Địa chỉ IP của client.
     */
    public void recordInvalidTokenAttempt(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        long currentWindow = Instant.now().getEpochSecond() / ONE_HOUR_SECONDS;
        ipInvalidBuckets.compute(ipAddress, (k, existing) -> {
            if (existing == null || existing.window() != currentWindow) {
                return new RateLimitBucket(currentWindow, new AtomicInteger(1));
            }
            existing.counter().incrementAndGet();
            return existing;
        });
    }

    private record RateLimitBucket(long window, AtomicInteger counter) {}
}
