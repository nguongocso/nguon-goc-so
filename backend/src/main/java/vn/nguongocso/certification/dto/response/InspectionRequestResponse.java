package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO cho yêu cầu kiểm nghiệm
 */
@Getter
@Builder
public class InspectionRequestResponse {

    @JsonProperty("testRequestId")
    private UUID testRequestId;

    @JsonProperty("lotId")
    private UUID lotId;

    @JsonProperty("lotCode")
    private String lotCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("testingUnit")
    private String testingUnit;

    /**
     * ID đơn vị kiểm nghiệm trong danh mục dùng chung (null nếu yêu cầu cũ nhập tự do).
     */
    @JsonProperty("testingUnitId")
    private UUID testingUnitId;

    /**
     * Cờ cảnh báo phạm vi công nhận (NCL-11-CN-006 Phase 2).
     * true khi yêu cầu chọn đơn vị từ danh mục nhưng có chỉ tiêu ngoài phạm vi.
     */
    @JsonProperty("hasScopeWarning")
    private Boolean hasScopeWarning;

    /**
     * Tên các chỉ tiêu ngoài phạm vi công nhận (snapshot), ngăn cách bởi dấu phẩy.
     */
    @JsonProperty("scopeWarningDetails")
    private String scopeWarningDetails;

    @JsonProperty("sampleSentDate")
    private LocalDate sampleSentDate;

    @JsonProperty("criteria")
    private List<InspectionCriterionResponse> criteria;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}