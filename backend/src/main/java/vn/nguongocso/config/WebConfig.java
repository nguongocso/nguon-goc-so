package vn.nguongocso.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình cho ứng dụng web, bao gồm việc định nghĩa các đường dẫn tài nguyên tĩnh.
 * Trong trường hợp này, cấu hình để phục vụ các tệp QR từ thư mục lưu trữ.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({QrImageStorageProperties.class, UploadProperties.class})
public class WebConfig implements WebMvcConfigurer {

    private final QrImageStorageProperties qrProperties;
    private final UploadProperties uploadProperties;

    /**
     * Thêm các bộ xử lý tài nguyên để phục vụ các tệp QR và tệp tải lên (ảnh đại diện, v.v.).
     *
     * @param registry Đối tượng ResourceHandlerRegistry để đăng ký các bộ xử lý tài nguyên.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/qr/**")
                .addResourceLocations("file:" + qrProperties.getPath() + "/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadProperties.getBaseDir() + "/");
    }
}
