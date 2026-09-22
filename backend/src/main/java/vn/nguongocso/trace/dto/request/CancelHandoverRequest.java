package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Yêu cầu hủy phiếu bàn giao lô hàng. */
@Getter
@Setter
public class CancelHandoverRequest {
    @NotBlank(message = "Lý do hủy không được để trống")
    private String reason;
}
