package vn.nguongocso.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.annotation.Auditable;
import vn.nguongocso.alert.event.ActivityLogEvent;

import java.time.LocalDateTime;

/**
 * Lớp AuditAspect chịu trách nhiệm thu thập thông tin lưu vết (audit log) cho
 * các hành động
 * được đánh dấu bằng annotation @Auditable. Nó sử dụng AOP để tự động ghi lại
 * các sự kiện
 * quan trọng trong hệ thống, bao gồm thông tin người dùng, hành động, mô tả,
 * loại thực thể,
 * địa chỉ IP và thời gian thực hiện.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {
    private final ApplicationEventPublisher eventPublisher;
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Ghi lại hoạt động của người dùng.
     */
    @AfterReturning(value = "@annotation(auditable)", returning = "result")
    public void logActivity(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
                return; // Chỉ lưu vết khi người dùng đã được xác thực thành công
            }
            CustomUserDetails currentUser = (CustomUserDetails) auth.getPrincipal();

            // Lấy thông tin HTTP request để lấy IP
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            String ipAddress = "";
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String forwardedFor = request.getHeader("X-Forwarded-For");
                ipAddress = (forwardedFor != null && !forwardedFor.isBlank())
                        ? forwardedFor.split(",")[0].trim()
                        : request.getRemoteAddr();
            }

            // Đánh giá biểu thức SpEL để sinh mô tả động dựa trên tham số truyền vào phương
            // thức
            StandardEvaluationContext context = new StandardEvaluationContext();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
            context.setVariable("result", result); // Có thể dùng kết quả trả về của method

            String evaluatedDescription = parser.parseExpression(auditable.description()).getValue(context,
                    String.class);

            // Xây dựng sự kiện lưu log
            ActivityLogEvent event = ActivityLogEvent.builder()
                    .userId(currentUser.getUserId())
                    .username(currentUser.getUsername())
                    .fullName(currentUser.getFullName())
                    .organizationId(currentUser.getOrganizationId())
                    .action(auditable.action())
                    .description(evaluatedDescription)
                    .entityType(auditable.entityType())
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Phát hành Event bất đồng bộ
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Lỗi xảy ra trong quá trình thu thập thông tin lưu vết: {}", e.getMessage(), e);
        }
    }
}
