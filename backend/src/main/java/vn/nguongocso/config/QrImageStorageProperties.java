package vn.nguongocso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/** Thuộc tính cấu hình đường dẫn lưu trữ tệp mã QR. */
@Getter
@Setter
@ConfigurationProperties(prefix = "qr.image.storage")
public class QrImageStorageProperties {
    private String path = "./files/qr";
}
