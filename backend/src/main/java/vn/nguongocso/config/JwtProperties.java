package vn.nguongocso.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thuộc tính cấu hình cho mã xác thực JWT (Access token và Selection token).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Khóa bí mật dùng để ký và xác thực mã JWT. */
    private String secret;

    /** Thời gian hết hạn của access token (tính bằng mili-giây, mặc định 24 giờ). */
    private long expiration = 86400000L;
}
