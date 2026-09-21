package vn.nguongocso.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thuộc tính cấu hình chia sẻ tài nguyên giữa các nguồn (CORS).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /** Danh sách nguồn gốc được phép truy cập, phân cách bằng dấu phẩy. */
    private String allowedOrigins = "http://localhost:5173";
}
