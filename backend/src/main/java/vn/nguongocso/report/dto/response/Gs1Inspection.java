package vn.nguongocso.report.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lịch sử kiểm nghiệm trong hồ sơ truy xuất theo lược đồ GS1. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gs1Inspection {

    @JacksonXmlProperty(localName = "requestId")
    private UUID requestId;

    @JacksonXmlProperty(localName = "inspectionUnit")
    private String inspectionUnit;

    @JacksonXmlProperty(localName = "sampleSentDate")
    private LocalDate sampleSentDate;

    @JacksonXmlProperty(localName = "status")
    private String status;

    @JacksonXmlElementWrapper(localName = "criteria")
    @JacksonXmlProperty(localName = "criterion")
    private List<Gs1InspectionCriterion> criteria;
}
