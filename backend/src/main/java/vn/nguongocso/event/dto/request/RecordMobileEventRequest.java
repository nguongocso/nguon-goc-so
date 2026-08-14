package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.event.enums.ChainEventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO ghi nhận sự kiện từ thiết bị di động ngoài đồng.
 *
 * @author Triệu Văn Đại
 */
@Getter
@Setter
public class RecordMobileEventRequest {

    @NotNull(message = "Vui lòng chọn lô sản xuất")
    private UUID productionLotId;

    @NotNull(message = "Loại sự kiện không được để trống")
    private ChainEventType eventType;

    @NotNull(message = "Thời điểm ghi nhận không được để trống")
    private LocalDateTime recordedAt;

    @NotNull(message = "Vĩ độ không được để trống")
    private Double latitude;

    @NotNull(message = "Kinh độ không được để trống")
    private Double longitude;

    @NotEmpty(message = "Sự kiện ghi nhận ngoài đồng yêu cầu tối thiểu một hình ảnh thực địa")
    private List<String> images;

    private String deviceSource = "MOBILE";

    @NotNull(message = "Dữ liệu sự kiện không được để trống")
    private Map<String, Object> eventData;
}
