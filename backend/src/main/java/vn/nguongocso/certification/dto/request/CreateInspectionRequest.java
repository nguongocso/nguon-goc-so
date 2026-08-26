package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateInspectionRequest {

    /**
     * ID đơn vị kiểm nghiệm trong danh mục dùng chung (NCL-11-CN-006 Phase 1).
     * <p>
     * Ưu tiên khi có giá trị: hệ thống tra cứu danh mục, kiểm tra trạng thái
     * hiệu lực/ngày hết hạn công nhận và lưu tên snapshot vào inspection_unit.
     * <p>
     * Nếu không truyền, hệ thống fallback về tên tự do trong {@code testingUnit}
     * để tương thích ngược với client cũ.
     */
    private UUID testingUnitId;

    /**
     * Tên đơn vị kiểm nghiệm nhập tự do (tương thích ngược).
     * Bắt buộc khi không có testingUnitId; được kiểm tra ở tầng service.
     */
    private String testingUnit;

    @NotNull(message = "Ngày gửi mẫu không được để trống.")
    private LocalDate sampleSentDate;

    @NotEmpty(message = "Vui lòng chọn ít nhất một chỉ tiêu kiểm nghiệm.")
    private List<Integer> criteriaIds;

    private Boolean confirmDuplicate = false;
}