package vn.nguongocso.report.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chỉ tiêu kiểm nghiệm trong hồ sơ truy xuất theo lược đồ GS1. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gs1InspectionCriterion {
    @JacksonXmlProperty(localName = "criterionCode")
    private String criterionCode;

    @JacksonXmlProperty(localName = "criterionName")
    private String criterionName;

    @JacksonXmlProperty(localName = "standardName")
    private String standardName;

    @JacksonXmlProperty(localName = "passed")
    private Boolean passed;

    @JacksonXmlProperty(localName = "resultDate")
    private LocalDate resultDate;

    @JacksonXmlProperty(localName = "expiryDate")
    private LocalDate expiryDate;
}
