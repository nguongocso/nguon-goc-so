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

/**
 * Lịch sử kiểm nghiệm của lô sản xuất trong hồ sơ truy xuất theo lược đồ GS1
 * mô phỏng.
 *
 * <p>
 * Một lô sản xuất có thể có nhiều yêu cầu kiểm nghiệm; mỗi yêu cầu chứa danh
 * sách chỉ tiêu và kết quả tương ứng. Dữ liệu được nạp best-effort từ module
 * certification và không làm thay đổi bất kỳ dữ liệu nghiệp vụ nào.
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
public class Gs1Inspection {

    /** ID yêu cầu kiểm nghiệm. */
    @JacksonXmlProperty(localName = "requestId")
    private UUID requestId;

    /** Tên đơn vị kiểm nghiệm (snapshot tại thời điểm tạo yêu cầu). */
    @JacksonXmlProperty(localName = "inspectionUnit")
    private String inspectionUnit;

    /** Ngày gửi mẫu đi kiểm. */
    @JacksonXmlProperty(localName = "sampleSentDate")
    private LocalDate sampleSentDate;

    /** Trạng thái yêu cầu kiểm nghiệm (PENDING_RESULT / PASSED / FAILED / CANCELLED). */
    @JacksonXmlProperty(localName = "status")
    private String status;

    /** Danh sách chỉ tiêu kiểm nghiệm và kết quả tương ứng. */
    @JacksonXmlElementWrapper(localName = "criteria")
    @JacksonXmlProperty(localName = "criterion")
    private List<Gs1InspectionCriterion> criteria;
}
