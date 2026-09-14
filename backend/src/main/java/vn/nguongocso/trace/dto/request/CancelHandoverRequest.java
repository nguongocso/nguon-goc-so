package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO để hủy phiếu bàn giao.
 */
@Getter
@Setter
public class CancelHandoverRequest {

    @NotBlank(message = "Lý do hủy không được để trống")
    private String reason;
}
