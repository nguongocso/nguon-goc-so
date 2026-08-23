package vn.nguongocso.event.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response tra cứu mã truy xuất khi quét mã để mở biểu mẫu ghi sự kiện.
 *
 * API này chỉ phục vụ bước tra cứu trước khi ghi nhận sự kiện,
 * không tạo mới ChainEvent.
 */
@Getter
@Builder
public class ScanLookupResponse {

    /**
     * Mã có hợp lệ và có thể mở biểu mẫu ghi sự kiện hay không.
     */
    private Boolean valid;

    /**
     * Thông báo lỗi nếu không thể mở biểu mẫu.
     * Null khi valid = true.
     */
    private String message;

    /**
     * Giá trị mã truy xuất đã quét.
     */
    private String traceCode;

    /**
     * ID lô hàng.
     */
    private UUID shipmentId;

    /**
     * Tên lô hàng.
     */
    private String shipmentName;

    /**
     * Trạng thái hiện tại của lô hàng.
     */
    private String shipmentStatus;

    /**
     * ID lô sản xuất.
     */
    private UUID productionLotId;

    /**
     * Tên loại nông sản.
     */
    private String productCategoryName;

    /**
     * Tên vùng trồng.
     */
    private String farmAreaName;

    /**
     * ID tổ chức sở hữu lô hàng.
     */
    private UUID organizationId;

    /**
     * Tên tổ chức sở hữu lô hàng.
     */
    private String organizationName;

    /**
     * Danh sách loại sự kiện có thể ghi tiếp theo.
     */
    private List<String> allowedEventTypes;

    /**
     * Loại sự kiện gần nhất đã ghi nhận.
     */
    private String lastEventType;

    /**
     * Thời điểm ghi nhận sự kiện gần nhất.
     */
    private LocalDateTime lastEventRecordedAt;

    /**
     * Tổng số lượng khai báo của lô hàng.
     */
    private Long totalQuantity;

    /**
     * Ngưỡng bảo quản của loại nông sản (nhiệt độ/độ ẩm cho phép).
     * Null khi loại nông sản chưa được cấu hình ngưỡng.
     */
    private ThresholdInfo thresholds;
}