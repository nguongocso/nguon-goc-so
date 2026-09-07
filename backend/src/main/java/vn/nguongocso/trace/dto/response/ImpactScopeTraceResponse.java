package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactScopeTraceResponse {
    private String rootNodeType; // PRODUCTION_LOT, SHIPMENT, TRACE_CODE
    private String searchedCode;
    private FarmAreaTraceDto farmArea;
    private ProductionLotTraceDto productionLot;
    private List<ShipmentTraceDto> shipments;
    private ImpactScopeSummaryDto summary;
}
