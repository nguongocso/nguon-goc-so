package vn.nguongocso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/** Thuộc tính cấu hình thời gian và múi giờ nghiệp vụ của hệ thống. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppTimeProperties {

    private String timezone = "Asia/Ho_Chi_Minh";
}
