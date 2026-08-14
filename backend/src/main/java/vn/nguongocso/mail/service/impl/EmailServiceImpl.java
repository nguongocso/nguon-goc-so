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

    private String buildInvitationHtmlTemplate(String organizationName, String roleName, String joinUrl, int expiryDays) {
        return """
                ...
                <div class="info-item">🏛️ <strong>Tổ chức mời:</strong> {{organizationName}}</div>
                <div class="info-item">👤 <strong>Vai trò được gán:</strong> {{roleName}}</div>
                <div class="info-item">⏳ <strong>Thời hạn lời mời:</strong> {{expiryDays}} ngày</div>
                
                ...
                
                <a href="{{joinUrl}}" target="_blank" class="btn">Xác nhận & Tham gia tổ chức</a>
                
                ...
                
                <a href="{{joinUrl}}" style="color: #059669;">{{joinUrl}}</a>
                ...
                """
                .replace("{{organizationName}}", organizationName)
                .replace("{{roleName}}", roleName)
                .replace("{{expiryDays}}", String.valueOf(expiryDays))
                .replace("{{joinUrl}}", joinUrl);
    }
}
