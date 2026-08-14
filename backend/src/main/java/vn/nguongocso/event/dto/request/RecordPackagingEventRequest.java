package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO ghi nhận sự kiện đóng gói.
 *
 * @author Team WEB 1
 */

@Getter
@Setter
public class RecordPackagingEventRequest {

    @NotNull(message = "Vui lòng chọn lô sản xuất")
    private UUID productionLotId;

    @NotBlank(message = "Quy cách đóng gói không được để trống")
    @Size(max = 255, message = "Quy cách đóng gói không được vượt quá 255 ký tự")
    private String packagingSpecification;

    @NotNull(message = "Vui lòng chọn ngày đóng gói")
    private LocalDate packagingDate;

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