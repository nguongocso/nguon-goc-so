package vn.nguongocso.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IpCountryResponse {
    private boolean success;

    @JsonProperty("country_code")
    private String countryCode;
}
