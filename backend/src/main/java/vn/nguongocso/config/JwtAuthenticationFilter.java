package vn.nguongocso.config;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.service.CustomUserDetailsService;

/** Bộ lọc xác thực JWT cho các yêu cầu HTTP. */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);
        if (token == null || !tokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenType = tokenProvider.getTokenTypeFromToken(token);

        // ORG_SELECTION không đưa vào SecurityContext; các API chọn tổ chức tự xác thực riêng
        if (JwtTokenProvider.TOKEN_TYPE_SELECTION.equals(tokenType)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (JwtTokenProvider.TOKEN_TYPE_ACCESS.equals(tokenType)) {
            authenticateAccessToken(request, token);
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Xác thực người dùng từ mã ACCESS JWT và thiết lập SecurityContext. */
    private void authenticateAccessToken(HttpServletRequest request, String token) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        // Xác định người dùng theo userId và organizationId để không phụ thuộc vào mã tổ chức có thể thay đổi
        UUID userId = tokenProvider.getUserIdFromToken(token);
        UUID organizationId = tokenProvider.getOrganizationIdFromToken(token);
        if (userId == null || organizationId == null) {
            return;
        }

        // QTN-32: Không xác thực thành viên bị vô hiệu hóa để yêu cầu được xử lý như chưa đăng nhập.
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUserIdAndOrganizationId(userId, organizationId);
        } catch (Exception e) {
            log.warn("Không xác thực được ACCESS token: userId={}, organizationId={}, reason={}",
                    userId, organizationId, e.getMessage());
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** Trích xuất JWT từ tiêu đề Authorization hoặc tham số truy vấn token. */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION_HEADER);
        if (bearer != null && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }

        // Dự phòng lấy token từ tham số truy vấn khi tải tệp hoặc xem tài nguyên không thể gửi tiêu đề Authorization
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken.trim();
        }

        return null;
    }
}
