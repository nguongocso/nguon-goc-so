package vn.nguongocso.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thuộc tính cấu hình đường dẫn lưu trữ tệp mã QR.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "qr.image.storage")
public class QrImageStorageProperties {

    /** Đường dẫn thư mục lưu trữ ảnh mã QR. */
    private String path = "./files/qr";
}
