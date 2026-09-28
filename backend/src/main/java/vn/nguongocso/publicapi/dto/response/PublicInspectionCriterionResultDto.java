package vn.nguongocso.publicapi.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.certification.enums.InspectionResultEntrySource;

/** DTO chi tiết kết quả kiểm nghiệm của từng chỉ tiêu trên cổng tra cứu công khai. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicInspectionCriterionResultDto {
    /** ID của kết quả kiểm nghiệm. */
    private String id;

    /** Tên chỉ tiêu kiểm nghiệm. */
    private String criterionName;

    /** Tên chỉ tiêu kiểm nghiệm tiếng Anh. */
    private String criterionNameEn;

    /** Ngưỡng quy định áp dụng. */
    private String standardValue;

    /** Ngưỡng quy định áp dụng tiếng Anh. */
    private String standardValueEn;

    /** Giá trị đo được thực tế. */
    private String measuredValue;

    /** Kết luận đạt chuẩn hay không. */
    private Boolean passed;

    /** Tên chuyên viên kiểm nghiệm. */
    private String inspectorName;

    /** Ngày kiểm nghiệm. */
    private LocalDate inspectionDate;

    /** Ngày hết hiệu lực. */
    private LocalDate expiryDate;

    /** Đơn vị kiểm nghiệm. */
    private String laboratoryName;

    /** Nguồn ghi nhận kết quả kiểm nghiệm. */
    private InspectionResultEntrySource entrySource;
}
