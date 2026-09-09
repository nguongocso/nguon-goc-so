package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO yêu cầu ghi sự kiện nhập kho tại hợp tác xã (HTX).
 *
 * @author Antigravity
 */

@Getter
@Setter
public class RecordWarehouseEntryRequest {

    @NotNull(message = "Vui lòng chọn lô sản xuất")
    private UUID productionLotId;

    @NotNull(message = "Vui lòng nhập thời điểm nhập kho")
    private LocalDateTime entryTime;

    @NotBlank(message = "Tên kho lưu trữ không được để trống")
    @Size(max = 255, message = "Tên kho lưu trữ không được vượt quá 255 ký tự")
    private String warehouseName;

    @Size(max = 500, message = "Điều kiện bảo quản không được vượt quá 500 ký tự")
    private String storageCondition;

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
