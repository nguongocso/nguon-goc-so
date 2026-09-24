package vn.nguongocso.ai.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
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
     * Khởi tạo RestClient chuyên biệt cho AI với cấu hình timeout và JdkClientHttpRequestFactory.
     */
    @Bean("aiRestClient")
    public RestClient aiRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(aiProperties.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(aiProperties.getReadTimeoutSeconds()));

        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
        stringConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN,
                MediaType.APPLICATION_OCTET_STREAM,
                MediaType.ALL));

        return RestClient.builder()
                .requestFactory(factory)
                .messageConverters(converters -> converters.add(0, stringConverter))
                .build();
    }
}
