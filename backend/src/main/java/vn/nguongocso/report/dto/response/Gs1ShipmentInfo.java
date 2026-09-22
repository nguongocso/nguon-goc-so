package vn.nguongocso.report.dto.response;

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

/** Thông tin lô hàng theo lược đồ GS1. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gs1ShipmentInfo {

    @JacksonXmlProperty(localName = "id")
    private UUID id;

    @JacksonXmlProperty(localName = "name")
    private String name;

    @JacksonXmlElementWrapper(localName = "codeValues")
    @JacksonXmlProperty(localName = "codeValue")
    private List<String> codeValues;

    @JacksonXmlProperty(localName = "productCategory")
    private String productCategory;

    @JacksonXmlProperty(localName = "totalQuantity")
    private Long totalQuantity;

    @JacksonXmlProperty(localName = "unit")
    private String unit;

    @JacksonXmlProperty(localName = "status")
    private String status;

    @JacksonXmlProperty(localName = "organization")
    private OrganizationInfo organization;

    /** Thông tin tổ chức sở hữu lô hàng. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrganizationInfo {

        @JacksonXmlProperty(localName = "id")
        private UUID id;

        @JacksonXmlProperty(localName = "name")
        private String name;

        @JacksonXmlProperty(localName = "code")
        private String code;
    }
}