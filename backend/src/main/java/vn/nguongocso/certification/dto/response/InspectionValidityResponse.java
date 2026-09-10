package vn.nguongocso.certification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.certification.enums.InspectionValidityStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Khối dữ liệu hiệu lực kết quả kiểm nghiệm của lô sản xuất (NCL-11-CN-004).
 *
 * <p>
 * Bổ sung additive vào response danh sách/chi tiết lô sản xuất.
 * Toàn bộ trường được tính toán bởi backend tại thời điểm đọc.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionValidityResponse {

    /**
     * Lô thuộc loại nông sản bắt buộc kiểm nghiệm hay không.
     */
    private Boolean requiresInspection;

    /**
     * Trạng thái hiệu lực suy diễn tại thời điểm đọc.
     */
    private InspectionValidityStatus status;

    /**
     * Ngày hết hiệu lực sớm nhất của các chỉ tiêu đạt, null khi không xác định.
     */
    private LocalDate earliestExpiryDate;

    /**
     * Số ngày còn hiệu lực, null khi đã quá hạn hoặc không xác định.
     */
    private Long daysRemaining;

    /**
     * Số ngày quá hạn, null khi chưa hết hạn hoặc không xác định.
     */
    private Long daysOverdue;

    /**
     * Lô có đủ điều kiện kích hoạt tem theo QTN-21 hay không.
     */
    private Boolean canActivate;

    /**
     * Lô đang ở trạng thái cho phép tạo yêu cầu kiểm nghiệm mới hay không.
     */
    private Boolean canCreateNewRequest;

    /**
     * ID của yêu cầu kiểm nghiệm PASSED mới nhất, null nếu không có.
     */
    private UUID latestPassedRequestId;

    /**
     * Số mã tem INACTIVE thuộc các lô hàng chưa thu hồi của lô, null khi chưa có lô hàng.
     */
    private Long inactiveStampCount;

    /**
     * Tổng số mã tem thuộc các lô hàng chưa thu hồi của lô.
     */
    private Long totalStamps;

    /**
     * Helper trả về ngày hết hạn (alias cho earliestExpiryDate).
     */
    public LocalDate getExpiryDate() {
        return earliestExpiryDate;
    }

    /**
     * Helper trả về số ngày còn hiệu lực (alias cho daysRemaining).
     */
    public Long getDaysUntilExpiry() {
        return daysRemaining;
    }

    /**
     * Helper trả về tên trạng thái dạng chuỗi.
     */
    public String getInspectionValidityStatus() {
        return status != null ? status.name() : null;
    }

    /**
     * Helper trả về ngày hết hạn dạng chuỗi ISO.
     */
    public String getInspectionExpiryDate() {
        return earliestExpiryDate != null ? earliestExpiryDate.toString() : null;
    }

    /**
     * Helper trả về số ngày còn hiệu lực.
     */
    public Long getInspectionDaysRemaining() {
        return daysRemaining;
    }
}
