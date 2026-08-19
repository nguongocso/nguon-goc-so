package vn.nguongocso.report.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO chứa lược đồ thông tin dữ liệu mở cho Cán bộ quản lý ngành.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenDataExportDto {
    private UUID lotId;

    private String lotCode;

    private String productCategory;

    private Double expectedQuantity;

    private String expectedQuantityUnit;

    private Double actualQuantity;

    private LocalDate plantingDate;

    private LocalDate harvestDate;

    private String status;

    private OrganizationDto organization;

    private FarmAreaDto farmArea;

    private List<FarmLogDto> farmLogs;

    private List<ShipmentDto> shipments;

    /**
     * Thông tin tổ chức.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrganizationDto {
        private UUID organizationId;

        private String organizationName;

        private String organizationAddress;
    }

    /**
     * Thông tin khu vực nông trại.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FarmAreaDto {
        private UUID farmAreaId;

        private String farmAreaName;

        private BigDecimal farmAreaSize;

        private LocationDto farmAreaLocation;
    }

    /**
     * Thông tin vị trí địa lý.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationDto {
        private Double latitude;

        private Double longitude;
    }

    /**
     * Thông tin nhật ký nông trại.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FarmLogDto {
        private UUID logId;

        private String activityType;

        private String material;

        private Double quantity;

        private String unit;

        private LocalDate executedDate;

        private String notes;

        private List<String> attachments;
    }

    /**
     * Thông tin lô hàng.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShipmentDto {
        private UUID shipmentId;

        private String shipmentName;

        private Long totalQuantity;

        private LocalDateTime shippedAt;

        private List<JourneyEventDto> journeyEvents;
    }

    /**
     * Thông tin sự kiện hành trình.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JourneyEventDto {
        private UUID eventId;

        private String eventType;

        private LocalDateTime recordedAt;

        private String actorName;

        private LocationDto eventLocation;
    }
}
