package vn.nguongocso.alert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi kết quả ước lượng tác động của ngưỡng dự thảo (NCL-08-CN-014).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpactEstimationResponse {

    /** Số lượng mã tem / lượt quét dự kiến sẽ bị gắn cờ bất thường. */
    private long estimatedAnomaliesCount;

    /** Tổng số lượt quét được phân tích trong 30 ngày qua. */
    private long totalScansAnalyzed;

    /** Tổng số mã truy xuất duy nhất được phân tích trong 30 ngày qua. */
    private long totalTraceCodesAnalyzed;

    /** Số trường hợp vi phạm tần suất quét. */
    private long highFrequencyCount;

    /** Số trường hợp vi phạm khoảng cách di chuyển bất hợp lý. */
    private long impossibleTravelCount;

    /** Số trường hợp vi phạm thời hạn kích hoạt bình thường. */
    private long activationAgeCount;

    /** Khoảng thời gian phân tích (ngày, mặc định 30). */
    private int analysisPeriodDays;

    /** Thông điệp tóm tắt kết quả ước lượng. */
    private String message;
}
