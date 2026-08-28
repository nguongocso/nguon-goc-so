package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO cho đơn vị kiểm nghiệm trong danh mục dùng chung.
 */
@Getter
@Builder
public class TestingUnitResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("accreditationCode")
    private String accreditationCode;

    @JsonProperty("contactInfo")
    private String contactInfo;

    @JsonProperty("accreditationExpiryDate")
    private LocalDate accreditationExpiryDate;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}