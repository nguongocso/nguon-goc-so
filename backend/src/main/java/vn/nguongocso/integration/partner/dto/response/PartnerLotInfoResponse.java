package vn.nguongocso.integration.partner.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.farm.enums.ProductionLotStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerLotInfoResponse {
    private String lotId;
    private String lotName;
    private String productCategoryName;
    private Double expectedQuantity;
    private Double actualQuantity;
    private String quantityUnit;
    private LocalDate plantingDate;
    private LocalDate harvestDate;
    private ProductionLotStatus status;
}
