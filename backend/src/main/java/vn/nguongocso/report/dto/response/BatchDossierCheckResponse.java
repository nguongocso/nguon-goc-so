package vn.nguongocso.report.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO phản hồi kiểm tra tính đầy đủ bộ hồ sơ theo lô. */
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

    /** Thông tin điều kiện của từng lô hàng. */
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

        private UUID organizationId;

        private String organizationName;
    }
}
