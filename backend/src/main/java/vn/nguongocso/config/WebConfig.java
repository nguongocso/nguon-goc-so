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
	
    /**
     * Thêm các bộ xử lý tài nguyên để phục vụ các tệp QR từ thư mục lưu trữ.
     * Đường dẫn URL /files/qr/** sẽ được ánh xạ tới thư mục lưu trữ QR.
     *
     * @param registry Đối tượng ResourceHandlerRegistry để đăng ký các bộ xử lý tài nguyên.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/qr/**")
                .addResourceLocations("file:" + qrStoragePath + "/");
    }
}
