package vn.nguongocso.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Cấu hình phục vụ tài nguyên tệp tĩnh cho ứng dụng web.
 * Định tuyến đường dẫn mã QR và tệp người dùng tải lên từ hệ thống tệp.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({QrImageStorageProperties.class, UploadProperties.class})
public class WebConfig implements WebMvcConfigurer {
    private final QrImageStorageProperties qrProperties;
    private final UploadProperties uploadProperties;

    /** Đăng ký đường dẫn phục vụ tệp mã QR và tệp tải lên. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/qr/**")
                .addResourceLocations("file:" + qrProperties.getPath() + "/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadProperties.getBaseDir() + "/");
    }
}
