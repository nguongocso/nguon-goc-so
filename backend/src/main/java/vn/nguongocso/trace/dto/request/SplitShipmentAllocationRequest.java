package vn.nguongocso.trace.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Yêu cầu phân bổ lô con trong thao tác tách lô hàng. */
@Data
public class SplitShipmentAllocationRequest {

    @NotNull(message = "Tổ chức nhận không được để trống")
    private UUID recipientOrganizationId;

    @NotBlank(message = "Tên lô con không được để trống")
    @Size(max = 255, message = "Tên lô con không quá 255 ký tự")
    private String name;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Long quantity;

    @NotBlank(message = "Mã từ không được để trống")
    private String fromCode;

    @NotBlank(message = "Mã đến không được để trống")
    private String toCode;

    @Size(max = 500, message = "Thông tin đóng gói không quá 500 ký tự")
    private String packagingInfo;
}
