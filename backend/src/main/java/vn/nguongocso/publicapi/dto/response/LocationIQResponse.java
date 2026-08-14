package vn.nguongocso.publicapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Response từ LocationIQ API.
 */
@Getter
@Setter
public class LocationIQResponse {

    @JsonProperty("display_name")
    private String displayName;

    private Address address;

    @Getter
    @Setter
    public static class Address {

        private String village;
        private String town;
        private String city;
        private String county;
        private String state;
        private String country;

        @JsonProperty("city_district")
        private String cityDistrict;

        private String municipality;
    }
}