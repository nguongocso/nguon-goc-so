package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.farm.enums.ProductionLotStatus;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionLotTraceDto {
    private UUID id;
    private String code;
    private String name;
    private ProductionLotStatus status;
    private Double expectedQuantity;
    private String expectedQuantityUnit;
    private Double actualQuantity;
    private LocalDate plantingDate;
    private LocalDate harvestDate;
}
