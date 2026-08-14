package vn.nguongocso.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.nguongocso.backup.service.RestoreService;
import vn.nguongocso.common.ApiResult;

import java.io.IOException;

/**
 * Lớp MaintenanceFilter là một bộ lọc (filter) trong ứng dụng Spring Boot, chịu
 * trách nhiệm kiểm tra trạng thái bảo trì của hệ thống.
 * Khi hệ thống đang ở chế độ bảo trì, bộ lọc này sẽ chặn các yêu cầu HTTP không
 * được phép và trả về mã lỗi 503 (Service Unavailable).
 * Chỉ có các yêu cầu từ người dùng có vai trò ADMIN (VT-01) hoặc các endpoint
 * đặc biệt như /actuator/health và /api/v1/backups mới được phép tiếp tục.
 */
@Component
public class MaintenanceFilter extends OncePerRequestFilter {
    private final org.springframework.context.ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    /**
     * Khởi tạo MaintenanceFilter với ApplicationContext và ObjectMapper.
     *
     * @param applicationContext Context của ứng dụng Spring, dùng để lấy bean
     *                           RestoreService.
     * @param objectMapper       Dùng để chuyển đổi đối tượng thành JSON khi trả về
     *                           phản hồi lỗi.
     */
    public MaintenanceFilter(org.springframework.context.ApplicationContext applicationContext,
            ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    private RestoreService getRestoreService() {
        try {
            return applicationContext.getBean(RestoreService.class);
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    /**
     * Thực hiện lọc các yêu cầu HTTP dựa trên trạng thái bảo trì của hệ thống.
     * Nếu hệ thống đang ở chế độ bảo trì, chỉ cho phép các yêu cầu từ người dùng
     * có vai trò ADMIN (VT-01) hoặc các endpoint đặc biệt như /actuator/health và
     * /api/v1/backups.
     * Các yêu cầu khác sẽ nhận được phản hồi lỗi 503 (Service Unavailable).
     *
     * @param request     Yêu cầu HTTP.
     * @param response    Phản hồi HTTP.
     * @param filterChain Chuỗi bộ lọc tiếp theo trong pipeline.
     * @throws ServletException Nếu có lỗi trong quá trình xử lý bộ lọc.
     * @throws IOException      Nếu có lỗi I/O trong quá trình xử lý bộ lọc.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        RestoreService restoreService = getRestoreService();
        if (restoreService != null && restoreService.isMaintenanceMode()) {
            String uri = request.getRequestURI();

            // Allow Actuator health endpoint and Backup APIs
            if (uri.equals("/actuator/health") || uri.startsWith("/api/v1/backups")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check if authenticated user has ADMIN (VT-01) role
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_VT-01")
                            || a.getAuthority().equals("VT-01")
                            || a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ADMIN"));

            if (isAdmin) {
                filterChain.doFilter(request, response);
                return;
            }

            // Return 503 Service Unavailable for other requests
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType("application/json;charset=UTF-8");

            ApiResult<Void> apiResult = ApiResult.error(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "Hệ thống đang tiến hành bảo trì phục hồi dữ liệu. Vui lòng quay lại sau.",
                    request.getRequestURI());

            response.getWriter().write(objectMapper.writeValueAsString(apiResult));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
