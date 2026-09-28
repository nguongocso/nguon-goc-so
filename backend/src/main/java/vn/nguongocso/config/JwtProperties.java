package vn.nguongocso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/** Thuộc tính cấu hình mã xác thực JWT của hệ thống. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private long expiration = 86400000L;
}
