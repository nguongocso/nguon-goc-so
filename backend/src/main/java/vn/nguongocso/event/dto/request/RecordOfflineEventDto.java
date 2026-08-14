package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.event.enums.ChainEventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO ghi nhận sự kiện ngoại tuyến.
 */
@Getter
@Setter
public class RecordOfflineEventDto {

    @NotNull(message = "ID sự kiện ngoại tuyến không được để trống")
    private UUID offlineEventId;

    /**
     * ID của lô sản xuất (dùng cho HARVEST, PACKAGING).
     * Đối với TRANSPORT/PROCUREMENT, sử dụng shipmentId hoặc codeValue.
     */
    private UUID productionLotId;

    /**
     * ID của lô hàng (dùng cho TRANSPORT, PROCUREMENT).
     */
    private UUID shipmentId;

    /**
     * Mã truy xuất (dùng cho TRANSPORT để lookup shipment thay vì dùng shipmentId
     * trực tiếp).
     */
    private String codeValue;

    @NotNull(message = "Loại sự kiện không được để trống")
    private ChainEventType eventType;

    @NotNull(message = "Thời điểm ghi nhận không được để trống")
    private LocalDateTime recordedAt;

    private Double latitude;

    private Double longitude;

    private List<String> images;

    private String deviceSource = "MOBILE";

    @NotNull(message = "Dữ liệu sự kiện không được để trống")
    private Map<String, Object> eventData;
}
