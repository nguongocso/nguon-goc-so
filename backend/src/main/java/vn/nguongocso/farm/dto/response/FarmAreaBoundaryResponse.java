package vn.nguongocso.farm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.farm.dto.request.LatLngDto;
import vn.nguongocso.farm.enums.AreaUnit;

/** Dữ liệu ranh giới và diện tích của một vùng trồng. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmAreaBoundaryResponse {
    private UUID id;
    private String name;
    private UUID organizationId;
    private BigDecimal declaredArea;
    private AreaUnit declaredAreaUnit;
    private BigDecimal calculatedArea;
    private List<LatLngDto> points;
    private BigDecimal areaDeviationPercentage;
    private LocalDateTime updatedAt;
}
