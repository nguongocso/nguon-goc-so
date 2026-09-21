package vn.nguongocso.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình chuỗi lọc bảo mật Spring Security cho hệ thống.
 * <p>
 * Hệ thống sử dụng JWT hoàn toàn stateless và tích hợp các filter xác thực theo thứ tự.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Import({CorsConfig.class, SecurityBeansConfig.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final MetricsCollectorFilter metricsCollectorFilter;

    /**
     * Cấu hình chuỗi bộ lọc bảo mật SecurityFilterChain.
     *
     * @param http đối tượng HttpSecurity
     * @return chuỗi bộ lọc đã cấu hình
     * @throws Exception nếu xảy ra lỗi cấu hình
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(this::configureAuthorization)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(metricsCollectorFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cấu hình phân quyền truy cập cho từng nhóm endpoint.
     */
    private void configureAuthorization(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Xác thực tài khoản & Quên mật khẩu
                .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/reset-password/validate"
                ).permitAll()

                // Quy trình lựa chọn tổ chức (tự xác thực qua ORG_SELECTION token)
                .requestMatchers(
                        "/api/v1/auth/organizations",
                        "/api/v1/auth/select-organization"
                ).permitAll()

                // API công khai và đối tác tích hợp (xác thực qua API Key filter)
                .requestMatchers(
                        "/api/v1/public/**",
                        "/api/v1/partner/**",
                        "/api/publicapi/**"
                ).permitAll()

                // Health check giám sát hệ thống
                .requestMatchers("/actuator/health").permitAll()

                // Tài nguyên tệp tĩnh và ảnh tải lên
                .requestMatchers(
                        "/files/qr/**",
                        "/uploads/**"
                ).permitAll()

                // Toàn bộ các yêu cầu còn lại bắt buộc có ACCESS JWT hợp lệ
                .anyRequest().authenticated();
    }
}