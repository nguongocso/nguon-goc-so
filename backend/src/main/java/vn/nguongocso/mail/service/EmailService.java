package vn.nguongocso.mail.service;

/**
 * Giao diện dịch vụ gửi email thông báo và xác thực trong hệ thống.
 */
public interface EmailService {

    /**
     * Gửi thư mời tham gia tổ chức bất đồng bộ qua Gmail HTML.
     *
     * @param toEmail          địa chỉ email người nhận
     * @param organizationName tên tổ chức/HTX mời
     * @param roleName         tên vai trò được phân công
     * @param joinUrl          đường dẫn xác nhận tham gia chứa token
     * @param expiryDays       thời hạn hiệu lực (ngày)
     */
    void sendInvitationEmail(
            String toEmail,
            String organizationName,
            String roleName,
            String joinUrl,
            int expiryDays
    );

    /**
     * Gửi email hướng dẫn đặt lại mật khẩu bất đồng bộ (NCL-01-CN-008).
     *
     * @param toEmail       địa chỉ email người nhận
     * @param fullName      họ và tên người nhận
     * @param resetUrl      đường dẫn đặt lại mật khẩu chứa token
     * @param expiryMinutes thời hạn hiệu lực (phút)
     */
    void sendPasswordResetEmail(
            String toEmail,
            String fullName,
            String resetUrl,
            int expiryMinutes
    );
}
