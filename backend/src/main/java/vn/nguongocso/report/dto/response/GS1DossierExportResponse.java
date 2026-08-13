package vn.nguongocso.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO phản hồi xuất hồ sơ theo lược đồ mô phỏng chuẩn GS1 (NCL-12-CN-003).
 *
 * <p>Đây là schema mô phỏng phục vụ học tập/demo, không phải chứng nhận GS1
 * hoặc triển khai EPCIS/Digital Link chính thức.</p>
 *
 * @author NCL-12-CN-003
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GS1DossierExportResponse {

    // Thông tin Shipment
    private ShipmentInfo shipment;

    // Danh sách sự kiện đã ánh xạ
    private List<GS1Event> events;

    // Bảng ánh xạ giữa dữ liệu hệ thống và schema mô phỏng (null nếu includeMapping=false)
    private Map<String, String> mapping;

    // Cảnh báo dữ liệu thiếu/không đầy đủ
    private List<Warning> warnings;

    // Thời điểm thực hiện export (server time)
    private LocalDateTime exportedAt;

    // Người thực hiện export (authenticated principal)
    private String exportedBy;

    // Phiên bản schema mô phỏng
    private String schemaVersion;

    // Mô tả schema mô phỏng
    private String schemaDescription;

    /**
     * Thông tin Shipment theo các field hiện có trong domain.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShipmentInfo {
        private UUID id;

        private String name;

        // Không tồn tại field codeValue trên Shipment -> null
        private String codeValue;

        private String productCategory;

        private Long totalQuantity;

        private String unit;

        private String status;

        private OrganizationInfo organization;
    }

    /**
     * Thông tin tổ chức của Shipment.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrganizationInfo {
        private UUID id;

        private String name;

        private String code;
    }
}