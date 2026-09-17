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

    /**
     * ID của chỉ tiêu kiểm nghiệm thuộc yêu cầu (snapshot).
     */
    private UUID criterionId;

    /**
     * Mã chỉ tiêu kiểm nghiệm.
     */
    private String code;

    /**
     * Tên chỉ tiêu kiểm nghiệm.
     */
    private String name;

    /**
     * Tên tiêu chuẩn áp dụng nếu có.
     */
    private String standardName;
}
