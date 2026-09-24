package vn.nguongocso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Cấu hình kích hoạt xử lý tác vụ bất đồng bộ cho ứng dụng. */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool chung cho các tác vụ bất đồng bộ của ứng dụng (ActivityLog, Backup, Restore).
     */
    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    @Primary
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AppTask-");
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool chuyên dụng cho các tác vụ xuất dữ liệu dung lượng lớn (OpenData, Báo cáo).
     * Giúp giải phóng HTTP worker thread và tránh chiếm dụng toàn bộ tài nguyên hệ thống.
     */
    @Bean(name = "exportTaskExecutor")
    public ThreadPoolTaskExecutor exportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ExportTask-");
        executor.initialize();
        return executor;
    }
}
