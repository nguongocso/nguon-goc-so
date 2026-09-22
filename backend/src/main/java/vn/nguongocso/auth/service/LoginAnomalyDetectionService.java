package vn.nguongocso.auth.service;

import java.util.UUID;

import vn.nguongocso.auth.entity.User;

/**
 * Service phát hiện và xử lý bất thường đăng nhập.
 */
public interface LoginAnomalyDetectionService {
    /**
     * Ghi nhận một lần đăng nhập (thành công hoặc thất bại).
     */
    void recordLoginAttempt(
            User user,
            String usernameInput,
            boolean isSuccess,
            String ipAddress,
            String countryCode);

    /**
     * Kiểm tra xem một tài khoản có đang ở trạng thái LOCKED không.
     */
    boolean isAccountLocked(UUID userId);
}
