package vn.nguongocso.report.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDossierCheckResponse {

    private int totalSelected;
    private int totalEligible;
    private int totalIneligible;

    private List<ShipmentEligibilityItem> eligibleShipments;
    private List<ShipmentEligibilityItem> ineligibleShipments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShipmentEligibilityItem {
        private UUID shipmentId;
        private String shipmentName;
        private boolean eligible;
        private List<String> missingDocuments;
    }
}
