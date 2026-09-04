package vn.nguongocso.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình cho ứng dụng web, bao gồm việc định nghĩa các đường dẫn tài nguyên tĩnh.
 * Trong trường hợp này, cấu hình để phục vụ các tệp QR từ thư mục lưu trữ.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer{
    @Value("${qr.image.storage.path:./files/qr}")
    private String qrStoragePath;

    @Value("${app.upload.base-dir:./uploads}")
    private String uploadBaseDir;

    /**
     * Thêm các bộ xử lý tài nguyên để phục vụ các tệp QR và tệp tải lên (ảnh đại diện, v.v.).
     *
     * @param registry Đối tượng ResourceHandlerRegistry để đăng ký các bộ xử lý tài nguyên.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/qr/**")
                .addResourceLocations("file:" + qrStoragePath + "/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadBaseDir + "/");
    }
}
