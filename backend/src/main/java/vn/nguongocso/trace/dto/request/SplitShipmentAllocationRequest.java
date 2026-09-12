package vn.nguongocso.trace.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SplitShipmentAllocationRequest {
    @NotNull private UUID recipientOrganizationId;
    @NotBlank @Size(max = 255) private String name;
    @NotNull @Positive private Long quantity;
    @NotBlank private String fromCode;
    @NotBlank private String toCode;
    @Size(max = 500) private String packagingInfo;
}
