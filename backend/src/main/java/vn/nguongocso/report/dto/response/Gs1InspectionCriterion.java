package vn.nguongocso.report.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả kiểm nghiệm của một chỉ tiêu trong lịch sử kiểm nghiệm của hồ sơ
 * truy xuất theo lược đồ GS1 mô phỏng.
 *
 * <p>
 * {@code passed} mang ba ngữ nghĩa: {@code true} = đạt, {@code false} = không
 * đạt, {@code null} = chỉ tiêu chưa được ghi kết quả.
 * </p>
 *
 * @author Triệu Văn Đại
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gs1InspectionCriterion {

    /** Mã chỉ tiêu kiểm nghiệm (ví dụ: PESTICIDE_RESIDUE, HEAVY_METAL). */
    @JacksonXmlProperty(localName = "criterionCode")
    private String criterionCode;

    /** Tên chỉ tiêu kiểm nghiệm hiển thị. */
    @JacksonXmlProperty(localName = "criterionName")
    private String criterionName;

    /** Tên tiêu chuẩn làm căn cứ cho chỉ tiêu (nếu có). */
    @JacksonXmlProperty(localName = "standardName")
    private String standardName;

    /** Kết quả kiểm nghiệm: true = đạt, false = không đạt, null = chưa có kết quả. */
    @JacksonXmlProperty(localName = "passed")
    private Boolean passed;

    /** Ngày cấp kết quả kiểm nghiệm (null khi chưa có kết quả). */
    @JacksonXmlProperty(localName = "resultDate")
    private LocalDate resultDate;

    /** Ngày hết hiệu lực của kết quả kiểm nghiệm (null khi chưa có kết quả). */
    @JacksonXmlProperty(localName = "expiryDate")
    private LocalDate expiryDate;
}
