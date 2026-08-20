package vn.nguongocso.auth.service.impl;

import java.net.InetAddress;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.dto.response.IpCountryResponse;
import vn.nguongocso.auth.service.IpCountryResolver;

@Service
@RequiredArgsConstructor
@Slf4j
public class IpCountryResolverImpl implements IpCountryResolver {
    private final RestClient.Builder restClientBuilder;

    @Value("${app.ip-geolocation.enabled:true}")
    private boolean enabled;

    @Value("${app.ip-geolocation.base-url:https://ipwho.is}")
    private String baseUrl;

    @Override
    public String resolveCountryCode(String ipAddress) {
        if (!enabled || isPrivateOrLocalAddress(ipAddress)) {
            return null;
        }

        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(2));
            requestFactory.setReadTimeout(Duration.ofSeconds(2));

            IpCountryResponse response = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder.path("/{ip}").build(ipAddress))
                .retrieve()
                .body(IpCountryResponse.class);

            if (response == null || !response.isSuccess() || response.getCountryCode() == null) {
                return null;
            }

            return response.getCountryCode().trim().toUpperCase();
        } catch (Exception exception) {
            log.warn("Unable to resolve country for client IP {}: {}", ipAddress, exception.getMessage());
            return null;
        }
    }

    private boolean isPrivateOrLocalAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return true;
        }

        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            byte[] bytes = address.getAddress();
            return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || (bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc);
        } catch (Exception exception) {
            return true;
        }
    }
}
