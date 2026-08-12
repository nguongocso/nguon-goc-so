package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO yêu cầu ghi nhận nhập kho và đối chiếu số lượng.
 *
 * @author Team
 */
@Getter
@Setter
public class WarehouseReceiptRequest {

    @NotBlank(message = "Mã truy xuất không được để trống")
    private String codeValue;

    @NotNull(message = "Số lượng thực nhận không được để trống")
    @Positive(message = "Số lượng thực nhận phải lớn hơn 0")
    private Double receivedQuantity;

    private String conditionNote;

    private LocalDate receiptDate;

    @Size(max = 500, message = "Lý do chênh lệch không được vượt quá 500 ký tự")
    private String reason;
}