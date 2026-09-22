package vn.nguongocso.mail.service;

/**
 * Dịch vụ gửi email thông báo và xác thực trong hệ thống.
*/
public interface EmailService {
    /**
     * Gửi thư mời tham gia tổ chức bất đồng bộ.
     */
    void sendInvitationEmail(
            String toEmail,
            String organizationName,
            String roleName,
            String joinUrl,
            int expiryDays
    );

    /**
     * Gửi email hướng dẫn đặt lại mật khẩu bất đồng bộ.
     */
    void sendPasswordResetEmail(
            String toEmail,
            String fullName,
            String resetUrl,
            int expiryMinutes
    );

    /**
     * Gửi email liên kết cổng nhập kết quả cho đơn vị kiểm nghiệm bất đồng bộ.
     */
    void sendInspectionResultEntryEmail(
            String toEmail,
            String organizationName,
            String testingUnitName,
            String lotCode,
            String entryUrl,
            int expiryDays
    );
}
