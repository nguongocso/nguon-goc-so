package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phản hồi xuất hồ sơ truy xuất theo lược đồ GS1. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "gs1Dossier")
public class Gs1DossierExportResponse {

    @JacksonXmlProperty(localName = "shipment")
    private Gs1ShipmentInfo shipment;

    @JacksonXmlElementWrapper(localName = "events")
    @JacksonXmlProperty(localName = "event")
    private List<Gs1Event> events;

    @JacksonXmlElementWrapper(localName = "inspections")
    @JacksonXmlProperty(localName = "inspection")
    private List<Gs1Inspection> inspections;

    @JacksonXmlProperty(localName = "mapping")
    private Map<String, String> mapping;

    @JacksonXmlElementWrapper(localName = "warnings")
    @JacksonXmlProperty(localName = "warning")
    private List<Gs1Warning> warnings;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JacksonXmlProperty(localName = "exportedAt")
    private LocalDateTime exportedAt;

    @JacksonXmlProperty(localName = "exportedBy")
    private String exportedBy;

    @JacksonXmlProperty(localName = "schemaVersion")
    private String schemaVersion;

    @JacksonXmlProperty(localName = "schemaDescription")
    private String schemaDescription;
}