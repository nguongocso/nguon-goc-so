package vn.nguongocso.auth.service;

import vn.nguongocso.auth.dto.request.ForgotPasswordRequest;
import vn.nguongocso.auth.dto.request.ResetPasswordRequest;
import vn.nguongocso.auth.dto.response.ValidateResetTokenResponse;

/**
 * Service xử lý nghiệp vụ quên mật khẩu và đặt lại mật khẩu (NCL-01-CN-008).
 */
public interface PasswordResetService {

    /**
     * Tiếp nhận yêu cầu đặt lại mật khẩu, sinh token bảo mật và gửi email.
     * Bảo vệ chống Account Enumeration (không tiết lộ tài khoản có tồn tại hay không).
     *
     * @param request chứa email hoặc username của người dùng
     */
    void requestPasswordReset(ForgotPasswordRequest request);

    /**
     * Kiểm tra tính hợp lệ và thời hạn của token đặt lại mật khẩu.
     *
     * @param token chuỗi token nhận được từ URL
     * @return kết quả kiểm tra
     */
    ValidateResetTokenResponse validateToken(String token);

    /**
     * Xác thực và cập nhật mật khẩu mới cho người dùng.
     * Đảm bảo tiêu thụ token atomic và chống race condition.
     *
     * @param request chứa token, mật khẩu mới và xác nhận mật khẩu
     */
    void resetPassword(ResetPasswordRequest request);
}
