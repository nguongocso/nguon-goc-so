package vn.nguongocso.mail.service;

/**
 * Dịch vụ gửi email thông báo và xác thực trong hệ thống.
 *
 * <p>
 * Mọi phương thức đều gửi bất đồng bộ, bên gọi không chờ kết quả; gửi thất bại
 * chỉ được ghi log mà không ném exception để không chặn luồng nghiệp vụ chính.
 * Khi chưa cấu hình địa chỉ gửi thì bỏ qua gửi thực tế và ghi log thay thế.
 * </p>
 */
public interface EmailService {

    /**
     * Gửi thư mời tham gia tổ chức bất đồng bộ qua Gmail HTML.
     *
     * @param toEmail địa chỉ email người nhận; không được null hoặc rỗng, phải đúng định dạng email
     * @param organizationName tên tổ chức/HTX mời; không được null hoặc rỗng
     * @param roleName tên vai trò được phân công; không được null hoặc rỗng
     * @param joinUrl đường dẫn xác nhận tham gia chứa token; không được null hoặc rỗng
     * @param expiryDays thời hạn hiệu lực, đơn vị ngày, luôn lớn hơn 0
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
     * @param toEmail địa chỉ email người nhận; không được null hoặc rỗng, phải đúng định dạng email
     * @param fullName họ và tên người nhận; null thì hiển thị tên chung
     * @param resetUrl đường dẫn đặt lại mật khẩu chứa token; không được null hoặc rỗng
     * @param expiryMinutes thời hạn hiệu lực, đơn vị phút, luôn lớn hơn 0
     */
    void sendPasswordResetEmail(
            String toEmail,
            String fullName,
            String resetUrl,
            int expiryMinutes
    );

    /**
     * Gửi email liên kết cổng nhập kết quả cho đơn vị kiểm nghiệm bất đồng bộ
     * (NCL-11-CN-007).
     *
     * @param toEmail địa chỉ email đơn vị kiểm nghiệm nhận liên kết; không được null
     *     hoặc rỗng, phải đúng định dạng email
     * @param organizationName tên hợp tác xã yêu cầu; không được null hoặc rỗng
     * @param testingUnitName tên đơn vị kiểm nghiệm; null thì hiển thị tên chung
     * @param lotCode mã lô sản xuất; không được null hoặc rỗng
     * @param entryUrl đường dẫn cổng nhập kết quả chứa token bí mật; không được null hoặc rỗng
     * @param expiryDays thời hạn hiệu lực, đơn vị ngày, luôn lớn hơn 0
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
