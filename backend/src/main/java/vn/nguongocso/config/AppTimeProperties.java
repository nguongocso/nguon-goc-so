package vn.nguongocso.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thuộc tính cấu hình thời gian và múi giờ nghiệp vụ của hệ thống.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppTimeProperties {

    /** Múi giờ mặc định áp dụng cho toàn bộ hoạt động nghiệp vụ (mặc định Asia/Ho_Chi_Minh). */
    private String timezone = "Asia/Ho_Chi_Minh";
}
