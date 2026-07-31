package vn.nguongocso.report.dto.response;

import lombok.*;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LookupResponse {
    private String codeValue;
    private TraceCodeStatus status;
    private LocalDateTime activatedAt;
    private ShipmentInfo shipment;
    private ProductionLotInfo productionLot;
    private List<FarmLogInfo> farmLogs;
    private List<ChainEventInfo> chainEvents;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShipmentInfo {
        private UUID id;
        private String name;
        private String packagingInfo;
        private long totalQuantity;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductionLotInfo {
        private UUID id;
        private String name;
        private String plantingDate;
        private String harvestDate;
        private String cropType;
        private OrgInfo organization;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrgInfo {
        private UUID id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FarmLogInfo {
        private UUID id;
        private String logDate;
        private String activityType;
        private String description;
        private List<AttachmentInfo> attachments;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentInfo {
        private UUID id;
        private String fileName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChainEventInfo {
        private UUID id;
        private String eventType;
        private LocalDateTime eventDate;
        private String eventData;
    }
}
