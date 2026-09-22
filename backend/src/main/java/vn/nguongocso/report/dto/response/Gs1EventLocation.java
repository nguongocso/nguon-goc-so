package vn.nguongocso.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Vị trí địa lý của sự kiện trong lược đồ GS1. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gs1EventLocation {

    @JacksonXmlProperty(localName = "latitude")
    private Double latitude;

    @JacksonXmlProperty(localName = "longitude")
    private Double longitude;

    @JacksonXmlProperty(localName = "address")
    private String address;
}