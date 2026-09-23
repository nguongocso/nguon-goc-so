package vn.nguongocso.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

/**
 * Cấu hình thời gian nghiệp vụ cho hệ thống.
 * Cung cấp bean {@link Clock} dựa theo múi giờ cấu hình (${app.timezone}).
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppTimeProperties.class)
public class TimeConfig {
    private final AppTimeProperties appTimeProperties;

    /** Khởi tạo bean Clock nghiệp vụ theo múi giờ hệ thống. */
    @Bean
    public Clock businessClock() {
        return Clock.system(ZoneId.of(appTimeProperties.getTimezone()));
    }
}
