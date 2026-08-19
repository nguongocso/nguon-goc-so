package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO ghi nhận sự kiện sơ chế và phân loại.
 *
 * @author NGUON-GOC-SO Team
 */

@Getter
@Setter
public class RecordPreprocessingEventRequest {

    @NotNull(message = "Vui lòng chọn lô sản xuất")
    private UUID productionLotId;

    @NotNull(message = "Vui lòng nhập khối lượng đưa vào sơ chế")
    @Positive(message = "Khối lượng vào sơ chế phải lớn hơn 0")
    private Double inputQuantity;

    @NotNull(message = "Vui lòng nhập khối lượng sau sơ chế")
    @PositiveOrZero(message = "Khối lượng sau sơ chế phải lớn hơn hoặc bằng 0")
    private Double outputQuantity;

    @Size(max = 100, message = "Hạng phân loại không được vượt quá 100 ký tự")
    private String grade;

    @Size(max = 500, message = "Mô tả cách sơ chế không được vượt quá 500 ký tự")
    private String processingMethod;

    @NotNull(message = "Vui lòng chọn ngày sơ chế")
    private LocalDate preprocessingDate;

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
