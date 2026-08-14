package vn.nguongocso.export.schema;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lược đồ dữ liệu công khai được sử dụng để xuất dữ liệu.
 */
@Getter
@Setter
@Builder
public class OpenDataSchema {
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime exportedAt;

    private ExporterInfo exporter;

    private List<ShipmentData> shipments;

    /** Lớp con biểu thị thông tin về người xuất dữ liệu. */
    @Getter
    @Setter
    @Builder
    public static class ExporterInfo {
        private UUID userId;
        private String fullName;
        private UUID organizationId;
        private String organizationName;
    }

    /** Lớp con biểu thị thông tin về lô hàng. */
    @Getter
    @Setter
    @Builder
    public static class ShipmentData {
        private UUID id;
        private String name;
        private String productionLotName;
        private String productCategory;
        private Double totalQuantity;
        private String unit;
        private String status;
        private List<TimelineEvent> timeline;
        private List<CertificationInfo> certifications;
    }

    /** Lớp con biểu thị thông tin về sự kiện trong dòng thời gian của lô hàng. */
    @Getter
    @Setter
    @Builder
    public static class TimelineEvent {
        private String eventType; // HARVEST, PACKAGING, TRANSPORT, PROCUREMENT
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime recordedAt;
        private String recordedBy;
        private Location location;
        private Object data; // Map<String, Object> để chứa dữ liệu đặc thù từng loại sự kiện
    }

    /** Lớp con biểu thị thông tin về vị trí địa lý. */
    @Getter
    @Setter
    @Builder
    public static class Location {
        private Double latitude;
        private Double longitude;
    }

    /** Lớp con biểu thị thông tin về chứng nhận của lô hàng. */
    @Getter
    @Setter
    @Builder
    public static class CertificationInfo {
        private String standardName;
        private String certificationCode;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime issueDate;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime expiryDate;
        private String attachedFileUrl;
    }
}