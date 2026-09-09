package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO yêu cầu ghi sự kiện xuất kho tại hợp tác xã (HTX).
 *
 * @author Antigravity
 */

@Getter
@Setter
public class RecordWarehouseExitRequest {

    @NotNull(message = "Vui lòng chọn lô hàng")
    private UUID shipmentId;

    @NotNull(message = "Vui lòng nhập thời điểm xuất kho")
    private LocalDateTime exitTime;

    @Size(max = 255, message = "Nơi chuyển đến không được vượt quá 255 ký tự")
    private String destination;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String notes;

    private Double latitude;
    private Double longitude;

    /**
     * Danh sách ảnh thực địa (base64 hoặc URL), tùy chọn.
     */
    private List<String> images;

    /**
     * Nguồn thiết bị ghi sự kiện, mặc định "WEB".
     */
    private String deviceSource = "WEB";
}
