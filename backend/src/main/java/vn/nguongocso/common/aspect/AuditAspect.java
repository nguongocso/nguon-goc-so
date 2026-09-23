package vn.nguongocso.common.aspect;

import java.time.LocalDateTime;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.annotation.Auditable;

/** Khía diện AOP thu thập thông tin lưu vết (Audit Log) cho các phương thức gắn @Auditable. */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final ExpressionParser parser = new SpelExpressionParser();

    /** Ghi lại nhật ký hoạt động của người dùng sau khi phương thức thực thi thành công. */
    @AfterReturning(value = "@annotation(auditable)", returning = "result")
    public void logActivity(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
                return;
            }
            CustomUserDetails currentUser = (CustomUserDetails) auth.getPrincipal();

            // Lấy thông tin HTTP request để trích xuất IP
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

            // Đánh giá biểu thức SpEL để sinh mô tả động dựa trên tham số phương thức
            StandardEvaluationContext context = new StandardEvaluationContext();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
            context.setVariable("result", result);

            String evaluatedDescription = parser.parseExpression(auditable.description())
                    .getValue(context, String.class);
            String beforeValue = evaluateOptionalExpression(auditable.beforeValue(), context);
            String afterValue = evaluateOptionalExpression(auditable.afterValue(), context);

            // Xây dựng sự kiện lưu log
            ActivityLogEvent event = ActivityLogEvent.builder()
                    .userId(currentUser.getUserId())
                    .username(currentUser.getUsername())
                    .fullName(currentUser.getFullName())
                    .actorRole(currentUser.getRoleCode())
                    .organizationId(currentUser.getOrganizationId())
                    .action(auditable.action())
                    .description(evaluatedDescription)
                    .entityType(auditable.entityType())
                    .beforeValue(beforeValue)
                    .afterValue(afterValue)
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Phát hành sự kiện bất đồng bộ
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Lỗi xảy ra trong quá trình thu thập thông tin lưu vết: {}", e.getMessage(), e);
        }
    }

    /** Đánh giá biểu thức SpEL tùy chọn của dữ liệu trước hoặc sau thay đổi. */
    private String evaluateOptionalExpression(String expression, StandardEvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = parser.parseExpression(expression).getValue(context);
        if (value == null || value instanceof String) {
            return (String) value;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Không thể chuyển dữ liệu audit thành JSON.", exception);
        }
    }
}
