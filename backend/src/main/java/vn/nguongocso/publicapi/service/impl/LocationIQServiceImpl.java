package vn.nguongocso.publicapi.service.impl;

import java.time.Duration;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.publicapi.dto.response.LocationIQResponse;
import vn.nguongocso.publicapi.service.ReverseGeocodingService;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationIQServiceImpl implements ReverseGeocodingService {

    private final RestClient.Builder restClientBuilder;

    @Value("${locationiq.api-key}")
    private String apiKey;

    @Value("${locationiq.base-url}")
    private String baseUrl;

    @Override
    public String reverseGeocode(double latitude, double longitude) {

        try {
            log.info(
                    ">>> LOCATION IQ - START: lat={}, lon={}",
                    latitude,
                    longitude);

            SimpleClientHttpRequestFactory factory =
                    new SimpleClientHttpRequestFactory();

            // Timeout kết nối
            factory.setConnectTimeout(Duration.ofSeconds(5));

            // Timeout chờ response
            factory.setReadTimeout(Duration.ofSeconds(5));

            RestClient client = restClientBuilder
                    .requestFactory(factory)
                    .baseUrl(baseUrl)
                    .build();

            LocationIQResponse response = client
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("key", apiKey)
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("format", "json")
                            .queryParam("addressdetails", 1)
                            .queryParam("accept-language", "vi")
                            .build())
                    .retrieve()
                    .body(LocationIQResponse.class);

            if (response == null) {
                log.warn(">>> LOCATION IQ - EMPTY RESPONSE");
                return null;
            }

            String location = buildLocation(response);

            log.info(
                    ">>> LOCATION IQ - SUCCESS: {}",
                    location);

            return location;

        } catch (Exception e) {

            // LocationIQ chỉ là chức năng bổ sung.
            // Không được làm hỏng API tra cứu QR.
            log.warn(
                    ">>> LOCATION IQ - FAILED: {}",
                    e.getMessage());

            return null;
        }
    }

    private String buildLocation(LocationIQResponse response) {

        if (response.getAddress() == null) {
            return response.getDisplayName();
        }

        var address = response.getAddress();

        String village = firstNonBlank(
                address.getVillage(),
                address.getTown(),
                address.getCityDistrict(),
                address.getCity(),
                address.getMunicipality());

        String county = address.getCounty();
        String state = address.getState();
        String country = address.getCountry();

        return Stream.of(
                    village,
                    county,
                    state,
                    country)
                .filter(this::isNotBlank)
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
    }

    private String firstNonBlank(String... values) {

        for (String value : values) {
            if (isNotBlank(value)) {
                return value;
            }
        }

        return null;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}