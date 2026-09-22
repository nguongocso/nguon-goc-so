package vn.nguongocso.report.dto.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cảnh báo dữ liệu khi xuất hồ sơ GS1. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gs1Warning {

    @JacksonXmlProperty(localName = "eventId")
    private UUID eventId;

    @JacksonXmlProperty(localName = "field")
    private String field;

    @JacksonXmlProperty(localName = "message")
    private String message;
}