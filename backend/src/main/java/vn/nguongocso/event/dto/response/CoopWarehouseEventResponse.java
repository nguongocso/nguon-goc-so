package vn.nguongocso.event.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.event.enums.ChainEventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO phản hồi thông tin sự kiện kho hợp tác xã (nhập kho / xuất kho).
 *
 * @author Antigravity
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoopWarehouseEventResponse {

    private UUID id;
    private UUID productionLotId;
    private String productionLotName;
    private ChainEventType eventType;
    private String warehouseName;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String storageCondition;
    private String destination;
    private String notes;

    /**
     * Thời gian lưu kho tính theo ngày.
     */
    private Long storageDurationDays;

    /**
     * Thời gian lưu kho tính theo giờ.
     */
    private Long storageDurationHours;

    /**
     * Ngưỡng thời gian lưu kho tối đa của loại nông sản (ngày).
     */
    private Integer maxAllowedStorageDays;

    /**
     * Cờ đánh dấu thời gian lưu kho có vượt ngưỡng bảo quản cho phép hay không.
     */
    private Boolean isStorageExceeded;

    /**
     * Cảnh báo hiển thị khi thời gian lưu kho vượt ngưỡng bảo quản.
     */
    private String warningMessage;

    private Double latitude;
    private Double longitude;
    private List<String> images;
    private LocalDateTime recordedAt;
    private String recordedByName;
    private LocalDateTime createdAt;
}
