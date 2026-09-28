package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** DTO response thông tin phạm vi ảnh hưởng khi truy xuất. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ImpactScopeTraceResponse {
    private String rootNodeType;

    private String searchedCode;

    private FarmAreaTraceDto farmArea;

    private ProductionLotTraceDto productionLot;

    private List<ShipmentTraceDto> shipments;

    private ImpactScopeSummaryDto summary;
}
