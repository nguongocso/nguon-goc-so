package vn.nguongocso.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình Spring Security cho hệ thống.
 *
 * <p>
 * Authentication sử dụng JWT và hoàn toàn stateless.
 * </p>
 *
 * <p>
 * Hệ thống có 2 loại JWT:
 * </p>
 *
 * <ul>
 *     <li>ORG_SELECTION - dùng trong quá trình chọn organization.</li>
 *     <li>ACCESS - dùng để truy cập API sau khi đã chọn organization.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final MetricsCollectorFilter metricsCollectorFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    /**
     * Security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http

                /*
                 * =====================================================
                 * CSRF
                 * =====================================================
                 *
                 * REST API sử dụng JWT nên không sử dụng session-based
                 * CSRF protection.
                 */
                .csrf(csrf -> csrf.disable())

                /*
                 * =====================================================
                 * CORS
                 * =====================================================
                 */
                .cors(Customizer.withDefaults())

                /*
                 * =====================================================
                 * SESSION
                 * =====================================================
                 *
                 * JWT authentication là stateless.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * =====================================================
                 * AUTHORIZATION
                 * =====================================================
                 */
                .authorizeHttpRequests(auth -> auth

                        /*
                         * CORS preflight.
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        /*
                         * =================================================
                         * LOGIN & PASSWORD RESET
                         * =================================================
                         *
                         * Chưa có JWT.
                         */
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/reset-password/validate"
                        ).permitAll()

                        /*
                         * =================================================
                         * ORGANIZATION SELECTION FLOW
                         * =================================================
                         *
                         * Hai endpoint này không sử dụng Spring Security
                         * Authentication.
                         *
                         * Controller/service sẽ tự validate:
                         *
                         *     tokenType = ORG_SELECTION
                         *
                         */
                        .requestMatchers(
                                "/api/v1/auth/organizations",
                                "/api/v1/auth/select-organization"
                        ).permitAll()

                        /*
                         * =================================================
                         * PUBLIC & PARTNER API
                         * =================================================
                         */
                        .requestMatchers(
                                "/api/v1/public/**",
                                "/api/v1/partner/**"
                        ).permitAll()

                        /*
                         * =================================================
                         * HEALTH CHECK
                         * =================================================
                         */
                        .requestMatchers(
                                "/actuator/health"
                        ).permitAll()

                        /*
                         * =================================================
                         * STATIC / UPLOADED FILES
                         * =================================================
                         */
                        .requestMatchers(
                                "/files/qr/**",
                                "/uploads/**"
                        ).permitAll()

                        /*
                         * =================================================
                         * EVERYTHING ELSE
                         * =================================================
                         *
                         * Bắt buộc phải có ACCESS JWT hợp lệ.
                         */
                        .anyRequest().authenticated()
                )

                /*
                 * =====================================================
                 * API KEY FILTER & JWT FILTER
                 * =====================================================
                 *
                 * Chạy trước UsernamePasswordAuthenticationFilter.
                 */
                .addFilterBefore(
                        apiKeyAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        metricsCollectorFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Cấu hình CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        List<String> origins =
                new ArrayList<>(
                        Arrays.asList(
                                "http://localhost:3000",
                                "http://localhost:5173",
                                "http://localhost:63342",
                                "http://localhost",
                                "http://localhost:5500",
                                "http://127.0.0.1:5500",
                                "http://localhost:5501",
                                "http://127.0.0.1:5501"
                        )
                );

        /*
         * Cho phép bổ sung origin từ application.properties.
         */
        if (allowedOrigins != null
                && !allowedOrigins.isBlank()) {

            String[] split =
                    allowedOrigins.split(",");

            for (String origin : split) {

                String trimmed = origin.trim();

                if (!trimmed.isEmpty()
                        && !origins.contains(trimmed)) {

                    origins.add(trimmed);
                }
            }
        }

        configuration.setAllowedOriginPatterns(List.of("*"));

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "X-API-KEY"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Authorization",
                        "Content-Disposition"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    /**
     * AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config)
            throws Exception {

        return config.getAuthenticationManager();
    }

    /**
     * Password encoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}