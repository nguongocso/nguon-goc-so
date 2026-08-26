package vn.nguongocso.mail.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import vn.nguongocso.mail.service.EmailService;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void sendInvitationEmail(String toEmail, String organizationName, String roleName, String joinUrl, int expiryDays) {
        log.info("Đang xử lý gửi email bất đồng bộ tới: {}", toEmail);

        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("[MAIL FALLBACK] Chưa cấu hình spring.mail.username. Giả lập gửi mail qua log. Link: {}", joinUrl);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Nguồn Gốc Số - Hệ Thống Truy Xuất Nguồn Gốc");
            helper.setTo(toEmail);
            helper.setSubject("Lời mời tham gia tổ chức " + organizationName + " - Nguồn Gốc Số");

            String htmlContent = buildInvitationHtmlTemplate(organizationName, roleName, joinUrl, expiryDays);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi email thư mời thành công tới {}", toEmail);
        } catch (Exception e) {
            log.error("Gửi email thư mời tới {} thất bại: {}. Link truy cập thay thế: {}", toEmail, e.getMessage(), joinUrl);
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetUrl, int expiryMinutes) {
        log.info("Đang xử lý gửi email đặt lại mật khẩu bất đồng bộ tới: {}", toEmail);

        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("[MAIL FALLBACK] Chưa cấu hình spring.mail.username. Giả lập gửi mail đặt lại mật khẩu qua log. Link: {}", resetUrl);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Nguồn Gốc Số - Hệ Thống Truy Xuất Nguồn Gốc");
            helper.setTo(toEmail);
            helper.setSubject("Yêu cầu đặt lại mật khẩu - Nguồn Gốc Số");

            String htmlContent = buildPasswordResetHtmlTemplate(fullName, resetUrl, expiryMinutes);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi email đặt lại mật khẩu thành công tới {}", toEmail);
        } catch (Exception e) {
            log.error("Gửi email đặt lại mật khẩu tới {} thất bại: {}. Link truy cập thay thế: {}", toEmail, e.getMessage(), resetUrl);
        }
    }

    private String buildInvitationHtmlTemplate(String organizationName, String roleName, String joinUrl, int expiryDays) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f7f6; }
                        .container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
                        .header { background: #059669; color: #ffffff; padding: 24px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; }
                        .content { padding: 32px 24px; }
                        .info-box { background: #f0fdf4; border-left: 4px solid #059669; padding: 16px; margin: 20px 0; border-radius: 4px; }
                        .info-item { margin-bottom: 8px; }
                        .btn-container { text-align: center; margin: 32px 0; }
                        .btn { background: #059669; color: #ffffff !important; text-decoration: none; padding: 14px 28px; border-radius: 6px; font-weight: bold; display: inline-block; }
                        .footer { background: #f9fafb; padding: 16px; text-align: center; font-size: 12px; color: #6b7280; border-top: 1px solid #e5e7eb; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🌱 Nguồn Gốc Số</h1>
                        </div>
                        <div class="content">
                            <h2>Lời mời tham gia tổ chức</h2>
                            <p>Xin chào,</p>
                            <p>Bạn đã nhận được lời mời tham gia vào tổ chức trên Hệ thống Truy xuất Nguồn Gốc Số Quốc gia.</p>
                            
                            <div class="info-box">
                                <div class="info-item">🏛️ <strong>Tổ chức mời:</strong> {{organizationName}}</div>
                                <div class="info-item">👤 <strong>Vai trò được gán:</strong> {{roleName}}</div>
                                <div class="info-item">⏳ <strong>Thời hạn lời mời:</strong> {{expiryDays}} ngày</div>
                            </div>

                            <p>Vui lòng bấm vào nút bên dưới để hoàn tất xác nhận và thiết lập tài khoản của bạn:</p>
                            
                            <div class="btn-container">
                                <a href="{{joinUrl}}" target="_blank" class="btn">Xác nhận & Tham gia tổ chức</a>
                            </div>

                            <p style="font-size: 13px; color: #6b7280;">Nếu nút bấm không hoạt động, bạn có thể sao chép liên kết này vào trình duyệt:<br>
                            <a href="{{joinUrl}}" style="color: #059669; word-break: break-all;">{{joinUrl}}</a></p>
                        </div>
                        <div class="footer">
                            <p>© 2026 Nguồn Gốc Số. Mọi quyền được bảo lưu.<br>Email này được gửi tự động, vui lòng không trả lời.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .replace("{{organizationName}}", organizationName)
                .replace("{{roleName}}", roleName)
                .replace("{{expiryDays}}", String.valueOf(expiryDays))
                .replace("{{joinUrl}}", joinUrl);
    }

    private String buildPasswordResetHtmlTemplate(String fullName, String resetUrl, int expiryMinutes) {
        String greetingName = (fullName != null && !fullName.isBlank()) ? fullName : "Quý khách";
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f7f6; }
                        .container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
                        .header { background: #059669; color: #ffffff; padding: 24px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; }
                        .content { padding: 32px 24px; }
                        .info-box { background: #f0fdf4; border-left: 4px solid #059669; padding: 16px; margin: 20px 0; border-radius: 4px; }
                        .info-item { margin-bottom: 8px; }
                        .btn-container { text-align: center; margin: 32px 0; }
                        .btn { background: #059669; color: #ffffff !important; text-decoration: none; padding: 14px 28px; border-radius: 6px; font-weight: bold; display: inline-block; }
                        .warning-box { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 12px 16px; margin: 20px 0; border-radius: 4px; font-size: 13px; color: #92400e; }
                        .footer { background: #f9fafb; padding: 16px; text-align: center; font-size: 12px; color: #6b7280; border-top: 1px solid #e5e7eb; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🌱 Nguồn Gốc Số</h1>
                        </div>
                        <div class="content">
                            <h2>Yêu cầu đặt lại mật khẩu</h2>
                            <p>Xin chào <strong>{{fullName}}</strong>,</p>
                            <p>Hệ thống Nguồn Gốc Số vừa nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                            
                            <div class="info-box">
                                <div class="info-item">⏳ <strong>Thời hạn liên kết:</strong> {{expiryMinutes}} phút (dùng một lần)</div>
                            </div>

                            <p>Vui lòng bấm vào nút bên dưới để tiến hành tạo mật khẩu mới:</p>
                            
                            <div class="btn-container">
                                <a href="{{resetUrl}}" target="_blank" class="btn">Đặt lại mật khẩu ngay</a>
                            </div>

                            <p style="font-size: 13px; color: #6b7280;">Nếu nút bấm không hoạt động, bạn có thể sao chép liên kết này vào trình duyệt:<br>
                            <a href="{{resetUrl}}" style="color: #059669; word-break: break-all;">{{resetUrl}}</a></p>

                            <div class="warning-box">
                                ⚠️ <strong>Lưu ý bảo mật:</strong> Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email. Mật khẩu của bạn vẫn được giữ an toàn tuyệt đối.
                            </div>
                        </div>
                        <div class="footer">
                            <p>© 2026 Nguồn Gốc Số. Mọi quyền được bảo lưu.<br>Email này được gửi tự động, vui lòng không trả lời.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .replace("{{fullName}}", greetingName)
                .replace("{{expiryMinutes}}", String.valueOf(expiryMinutes))
                .replace("{{resetUrl}}", resetUrl);
    }
}
