package vn.nguongocso.auth.service;

import java.util.UUID;

import vn.nguongocso.auth.entity.User;

/**
 * Service phát hiện và xử lý bất thường đăng nhập.
 * 
 * <p>
 * Service này chịu trách nhiệm:
 * - Ghi nhận mỗi lần đăng nhập vào bảng login_attempts
 * - Phát hiện bất thường (sai password liên tiếp, đăng nhập từ quốc gia lạ)
 * - Tạo bản ghi LoginAnomaly và gửi thông báo nếu phát hiện bất thường
 * </p>
 */
public interface LoginAnomalyDetectionService {
    
    /**
     * Ghi nhận một lần đăng nhập (thành công hoặc thất bại).
     * 
     * @param user             người dùng (null nếu username không tìm thấy)
     * @param usernameInput    username được nhập vào
     * @param isSuccess        true nếu đăng nhập thành công
     * @param ipAddress        địa chỉ IP của request
     * @param countryCode      mã quốc gia suy ra từ IP (nullable)
     */
    void recordLoginAttempt(
        User user,
        String usernameInput,
        boolean isSuccess,
        String ipAddress,
        String countryCode
    );
    
    /**
     * Kiểm tra xem một tài khoản có đang ở trạng thái LOCKED không.
     * 
     * @param userId ID tài khoản
     * @return true nếu tài khoản đang bị khóa
     */
    boolean isAccountLocked(UUID userId);
}
