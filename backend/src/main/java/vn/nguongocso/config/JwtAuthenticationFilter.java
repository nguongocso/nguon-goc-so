package vn.nguongocso.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import vn.nguongocso.auth.service.CustomUserDetailsService;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT authentication filter.
 *
 * <p>
 * Hệ thống sử dụng 2 loại JWT:
 * </p>
 *
 * <ul>
 *     <li>
 *         ORG_SELECTION:
 *         token tạm thời sau khi username/password hợp lệ,
 *         dùng cho bước chọn organization.
 *     </li>
 *
 *     <li>
 *         ACCESS:
 *         token xác thực đầy đủ sau khi organization được chọn,
 *         dùng để truy cập các API được bảo vệ.
 *     </li>
 * </ul>
 *
 * <p>
 * ORG_SELECTION không được tạo Spring Security Authentication.
 * ACCESS mới được phép thiết lập SecurityContext.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Xử lý JWT trong Authorization header.
     *
     * <p>
     * Quy tắc:
     * </p>
     *
     * <ul>
     *     <li>Không có token → request tiếp tục.</li>
     *     <li>Token không hợp lệ → request tiếp tục.</li>
     *     <li>ORG_SELECTION → không authenticate.</li>
     *     <li>ACCESS → authenticate user.</li>
     * </ul>
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        /*
         * Không có JWT.
         */
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * JWT không hợp lệ hoặc hết hạn.
         */
        if (!tokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Xác định loại JWT.
         */
        String tokenType = tokenProvider.getTokenTypeFromToken(token);

        /*
         * =========================================================
         * ORG_SELECTION
         * =========================================================
         *
         * Token này KHÔNG được dùng để authenticate Spring Security.
         *
         * Endpoint /organizations và /select-organization sẽ tự
         * kiểm tra token thông qua JwtTokenProvider.
         */
        if (JwtTokenProvider.TOKEN_TYPE_SELECTION.equals(tokenType)) {

            filterChain.doFilter(request, response);
            return;
        }

        /*
         * =========================================================
         * ACCESS
         * =========================================================
         *
         * ACCESS JWT mới được tạo Authentication.
         */
        if (JwtTokenProvider.TOKEN_TYPE_ACCESS.equals(tokenType)) {

            authenticateAccessToken(
                    request,
                    token
            );

            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Không nhận diện được token type.
         *
         * Không authenticate.
         */
        filterChain.doFilter(request, response);
    }

    /**
     * Authenticate request bằng ACCESS JWT.
     */
    private void authenticateAccessToken(
            HttpServletRequest request,
            String token) {

        /*
         * Nếu SecurityContext đã có Authentication,
         * không authenticate lại.
         */
        if (SecurityContextHolder
                .getContext()
                .getAuthentication() != null) {

            return;
        }

        /*
         * Load lại đúng membership đã được ghi vào ACCESS JWT.
         * Dùng userId + organizationId tránh phụ thuộc vào việc
         * organization code có thay đổi hoặc khác format hay không.
         */
        UUID userId = tokenProvider.getUserIdFromToken(token);
        UUID organizationId = tokenProvider.getOrganizationIdFromToken(token);

        if (userId == null || organizationId == null) {
            return;
        }

        UserDetails userDetails =
                userDetailsService.loadUserByUserIdAndOrganizationId(
                        userId,
                        organizationId
                );

        /*
         * Tạo Spring Security Authentication.
         */
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        /*
         * Đưa Authentication vào SecurityContext.
         */
        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }

    /**
     * Lấy JWT từ Authorization header.
     *
     * <pre>
     * Authorization: Bearer &lt;jwt&gt;
     * </pre>
     */
    private String getTokenFromRequest(
            HttpServletRequest request) {

        String bearer =
                request.getHeader(AUTHORIZATION_HEADER);

        if (bearer != null
                && bearer.startsWith(BEARER_PREFIX)) {

            return bearer.substring(
                    BEARER_PREFIX.length()
            );
        }

        return null;
    }
}