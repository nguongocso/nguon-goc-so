package vn.nguongocso.config;

import java.io.IOException;
import java.util.List;

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
 * Bộ lọc xác thực khóa API đối tác cho các đường dẫn tích hợp (QTN-20).
 * Kiểm tra tính hợp lệ của khóa và giới hạn tần suất gọi API.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String API_KEY_HEADER_ALT = "X-Api-Key";
    private static final List<String> FILTER_PREFIXES = List.of(
            "/api/v1/partner/",
            "/api/publicapi/"
    );

    private final ObjectProvider<PartnerApiKeyService> partnerApiKeyServiceProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return FILTER_PREFIXES.stream().noneMatch(path::startsWith);
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
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = request.getHeader(API_KEY_HEADER_ALT);
        }
        String clientIp = getClientIp(request);

        try {
            PartnerApiKey partnerApiKey = partnerApiKeyService.validateApiKeyAndCheckRateLimit(apiKey, clientIp);
            request.setAttribute("partnerApiKey", partnerApiKey);
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            int status = HttpStatus.UNAUTHORIZED.value();
            if (ex.getMessage() != null && ex.getMessage().contains("vượt quá hạn mức")) {
                // QTN-20: Trả về HTTP 429 khi đối tác vượt hạn mức gọi API
                status = HttpStatus.TOO_MANY_REQUESTS.value();
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
