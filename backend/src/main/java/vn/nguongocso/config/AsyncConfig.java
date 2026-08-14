package vn.nguongocso.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Cấu hình cho các tác vụ bất đồng bộ trong ứng dụng.
 * Bật tính năng @Async để cho phép thực thi các phương thức bất đồng bộ.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Có thể tùy chỉnh Executor nếu cần
}