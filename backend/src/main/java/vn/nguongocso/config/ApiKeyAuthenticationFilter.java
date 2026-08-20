package vn.nguongocso.config;

import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;

/**
 * Filter kiểm tra và xác thực API Key từ bên thứ ba (QTN-20).
 * <p>
 * Bẫy tất cả các request đến đường dẫn {@code /api/v1/partner/**},
 * kiểm tra Header {@code X-API-KEY}, kiểm tra hạn mức gọi API trong 1 giờ
 * và tính hợp lệ của khóa (REVOKED / EXPIRED).
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String PARTNER_PATH_PREFIX = "/api/v1/partner/";

    private final ObjectProvider<PartnerApiKeyService> partnerApiKeyServiceProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith(PARTNER_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        PartnerApiKeyService partnerApiKeyService = partnerApiKeyServiceProvider.getIfAvailable();
        if (partnerApiKeyService == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        String clientIp = getClientIp(request);

        try {
            PartnerApiKey partnerApiKey = partnerApiKeyService.validateApiKeyAndCheckRateLimit(apiKey, clientIp);
            request.setAttribute("partnerApiKey", partnerApiKey);
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            int status = HttpStatus.UNAUTHORIZED.value();
            if (ex.getMessage() != null && ex.getMessage().contains("vượt quá hạn mức")) {
                status = HttpStatus.TOO_MANY_REQUESTS.value(); // HTTP 429 (QTN-20)
            }

            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResult<Void> apiResult = ApiResult.error(status, ex.getMessage(), request.getRequestURI());
            objectMapper.writeValue(response.getWriter(), apiResult);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
