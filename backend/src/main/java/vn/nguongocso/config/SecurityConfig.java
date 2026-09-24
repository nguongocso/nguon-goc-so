package vn.nguongocso.config;

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

import lombok.RequiredArgsConstructor;

/** Cấu hình chuỗi bộ lọc bảo mật Spring Security cho hệ thống. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Import({CorsConfig.class, SecurityBeansConfig.class})
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final MetricsCollectorFilter metricsCollectorFilter;

    /** Cấu hình chuỗi bộ lọc bảo mật với chính sách quản lý phiên phi trạng thái. */
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

    /** Cấu hình phân quyền truy cập cho từng nhóm đường dẫn API. */
    private void configureAuthorization(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // Yêu cầu CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/reset-password/validate"
                ).permitAll()

                // Quy trình chọn tổ chức tự xác thực qua ORG_SELECTION token
                .requestMatchers(
                        "/api/v1/auth/organizations",
                        "/api/v1/auth/select-organization"
                ).permitAll()

                // API đối tác được xác thực riêng qua bộ lọc API Key
                .requestMatchers(
                        "/api/v1/public/**",
                        "/api/v1/partner/**",
                        "/api/publicapi/**"
                ).permitAll()

                .requestMatchers("/actuator/health").permitAll()

                .requestMatchers(
                        "/files/qr/**",
                        "/uploads/**"
                ).permitAll()

                .anyRequest().authenticated();
    }
}
