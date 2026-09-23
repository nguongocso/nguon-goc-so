package vn.nguongocso.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

/** Cấu hình chia sẻ tài nguyên giữa các nguồn (CORS) cho ứng dụng. */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:63342",
            "http://localhost",
            "http://localhost:5500",
            "http://127.0.0.1:5500",
            "http://localhost:5501",
            "http://127.0.0.1:5501"
    );

    private final CorsProperties corsProperties;

    /** Khởi tạo cấu hình nguồn CORS cho toàn bộ đường dẫn API. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new ArrayList<>(DEFAULT_ALLOWED_ORIGINS);

        String allowedOrigins = corsProperties.getAllowedOrigins();
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            String[] split = allowedOrigins.split(",");
            for (String origin : split) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty() && !origins.contains(trimmed)) {
                    origins.add(trimmed);
                }
            }
        }

        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-API-KEY", "X-Api-Key"
        ));
        configuration.setExposedHeaders(List.of(
                "Authorization", "Content-Disposition"
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
