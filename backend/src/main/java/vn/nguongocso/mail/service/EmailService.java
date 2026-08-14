package vn.nguongocso.mail.service;

public interface EmailService {
    /**
     * Gửi thư mời tham gia tổ chức bất đồng bộ qua Gmail HTML.
     *
     * @param toEmail        địa chỉ email người nhận
     * @param organizationName tên tổ chức/HTX mời
     * @param roleName       tên vai trò được phân công
     * @param joinUrl        đường dẫn xác nhận tham gia chứa token
     * @param expiryDays     thời hạn hiệu lực (ngày)
     */
    void sendInvitationEmail(String toEmail, String organizationName, String roleName, String joinUrl, int expiryDays);
}
