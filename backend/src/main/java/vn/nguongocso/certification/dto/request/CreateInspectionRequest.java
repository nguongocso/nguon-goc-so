package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO request tạo yêu cầu kiểm nghiệm.
 */
@Getter
@Setter
public class CreateInspectionRequest {
    private UUID testingUnitId;

    private String testingUnit;

    @NotNull(message = "Ngày gửi mẫu không được để trống.")
    private LocalDate sampleSentDate;

    @NotEmpty(message = "Vui lòng chọn ít nhất một chỉ tiêu kiểm nghiệm.")
    private List<Long> criteriaIds;

    private Boolean confirmDuplicate = false;
}