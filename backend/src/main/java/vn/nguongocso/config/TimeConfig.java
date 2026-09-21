package vn.nguongocso.config;

import java.time.Clock;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình thời gian nghiệp vụ cho hệ thống.
 * <p>
 * Cung cấp bean {@link Clock} dựa theo múi giờ cấu hình (${app.timezone}),
 * đảm bảo an toàn đa luồng (thread-safe) và hỗ trợ giả lập thời gian trong kiểm thử.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppTimeProperties.class)
public class TimeConfig {

    private final AppTimeProperties appTimeProperties;

    /**
     * Khởi tạo bean Clock nghiệp vụ theo múi giờ hệ thống.
     *
     * @return bean Clock
     */
    @Bean
    public Clock businessClock() {
        return Clock.system(ZoneId.of(appTimeProperties.getTimezone()));
    }
}