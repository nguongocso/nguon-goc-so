package vn.nguongocso.trace.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SplitShipmentRequest {
    @Valid
    @NotEmpty
    private List<@NotNull @Valid SplitShipmentAllocationRequest> allocations;
}
