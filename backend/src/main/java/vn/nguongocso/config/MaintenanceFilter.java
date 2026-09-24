package vn.nguongocso.config;

import java.io.IOException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.nguongocso.backup.service.RestoreService;
import vn.nguongocso.common.ApiResult;

/** Bộ lọc kiểm tra trạng thái bảo trì hệ thống. */
@Component
public class MaintenanceFilter extends OncePerRequestFilter {
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    public MaintenanceFilter(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    private RestoreService getRestoreService() {
        try {
            return applicationContext.getBean(RestoreService.class);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RestoreService restoreService = getRestoreService();
        if (restoreService != null && restoreService.isMaintenanceMode()) {
            String uri = request.getRequestURI();

            boolean isPermittedPath = uri.equals("/actuator/health")
                    || uri.startsWith("/api/v1/backups")
                    || uri.startsWith("/api/v1/admin/monitoring");
            if (isPermittedPath) {
                filterChain.doFilter(request, response);
                return;
            }

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
