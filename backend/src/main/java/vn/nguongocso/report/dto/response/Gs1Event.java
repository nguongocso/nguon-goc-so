package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sự kiện chuỗi cung ứng được ánh xạ theo lược đồ GS1. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gs1Event {

    @JacksonXmlProperty(localName = "eventId")
    private UUID eventId;

    @JacksonXmlProperty(localName = "eventType")
    private String eventType;

    @JacksonXmlProperty(localName = "eventTypeLabel")
    private String eventTypeLabel;

    @JacksonXmlProperty(localName = "recordedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recordedAt;

    @JacksonXmlProperty(localName = "recordedBy")
    private String recordedBy;

    @JacksonXmlProperty(localName = "location")
    private Gs1EventLocation location;

    @JacksonXmlProperty(localName = "details")
    private Map<String, Object> details;
}