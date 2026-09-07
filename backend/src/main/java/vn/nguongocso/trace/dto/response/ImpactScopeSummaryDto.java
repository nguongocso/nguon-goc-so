package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactScopeSummaryDto {
    private long totalShipments;
    private long totalActivatedStamps;
    private long totalReceivingOrganizations;
    private long totalRecalledShipments;
}
