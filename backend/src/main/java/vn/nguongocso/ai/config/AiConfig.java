package vn.nguongocso.ai.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

/**
 * Cấu hình kết nối và khởi tạo RestClient cho dịch vụ AI.
 */
@Configuration
@RequiredArgsConstructor
public class AiConfig {
    private final AiProperties aiProperties;

    /**
     * Khởi tạo RestClient chuyên biệt cho AI với cấu hình timeout an toàn.
     */
    @Bean("aiRestClient")
    public RestClient aiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(aiProperties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(aiProperties.getReadTimeoutSeconds()));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
