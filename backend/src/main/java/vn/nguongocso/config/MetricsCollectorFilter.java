package vn.nguongocso.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.nguongocso.report.service.MetricsBufferService;

import java.io.IOException;

/**
 * Filter thu thập số liệu tự động từ các request HTTP phục vụ giám sát hệ thống.
 */
@Component
@RequiredArgsConstructor
public class MetricsCollectorFilter extends OncePerRequestFilter {

    private final MetricsBufferService metricsBufferService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String uri = request.getRequestURI();

        boolean isPublicTrace = uri.startsWith("/api/v1/public/") || uri.startsWith("/public/");
        boolean isDataGateway = uri.startsWith("/api/v1/partner/") 
                || uri.startsWith("/api/v1/export/") 
                || uri.startsWith("/api/v1/integration/");

        if (isDataGateway) {
            metricsBufferService.recordDataGatewayCall();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            if (isPublicTrace) {
                metricsBufferService.recordPublicTraceLatency(duration);
            }

            if (response.getStatus() >= 500) {
                metricsBufferService.recordServerError();
            }
        }
    }
}
