package vn.nguongocso.trace.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Yêu cầu tách lô hàng thành các lô con. */
@Data
public class SplitShipmentRequest {

    @Valid
    @NotEmpty(message = "Danh sách phân bổ lô con không được để trống")
    private List<@NotNull @Valid SplitShipmentAllocationRequest> allocations;
}
