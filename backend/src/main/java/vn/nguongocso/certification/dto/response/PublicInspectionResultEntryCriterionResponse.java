package vn.nguongocso.certification.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO thông tin chỉ tiêu kiểm nghiệm trên cổng công khai dành cho đơn vị kiểm nghiệm.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicInspectionResultEntryCriterionResponse {
    private UUID criterionId;

    private String code;

    private String name;

    private String standardName;
}
