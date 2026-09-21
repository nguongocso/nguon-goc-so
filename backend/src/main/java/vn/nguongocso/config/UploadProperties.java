package vn.nguongocso.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thuộc tính cấu hình thư mục lưu trữ tải lên tập trung.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** Thư mục gốc chứa các tệp tải lên (ảnh đại diện, hóa đơn...). */
    private String baseDir = "./uploads";
}
