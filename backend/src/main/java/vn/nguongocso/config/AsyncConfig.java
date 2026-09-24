package vn.nguongocso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/** Cấu hình kích hoạt xử lý tác vụ bất đồng bộ cho ứng dụng. */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool chuyên dụng cho các tác vụ xuất dữ liệu dung lượng lớn (OpenData, Báo cáo).
     * Giúp giải phóng HTTP worker thread và tránh chiếm dụng toàn bộ tài nguyên hệ thống.
     */
    @Bean(name = "exportTaskExecutor")
    public Executor exportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ExportTask-");
        executor.initialize();
        return executor;
    }
}
