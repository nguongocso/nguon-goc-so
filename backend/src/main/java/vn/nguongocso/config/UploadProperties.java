package vn.nguongocso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/** Thuộc tính cấu hình thư mục lưu trữ tải lên tập trung. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    private String baseDir = "./uploads";
}
