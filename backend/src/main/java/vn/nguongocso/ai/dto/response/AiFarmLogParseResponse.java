package vn.nguongocso.ai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả phân tích thông tin nhật ký canh tác trích xuất từ giọng nói của nông dân.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiFarmLogParseResponse {
    /**
     * Mã định danh lô sản xuất được nhận diện (nếu khớp).
     */
    private String productionLotId;

    /**
     * Tên lô sản xuất được nhận diện.
     */
    private String productionLotName;

    /**
     * Mã loại hoạt động (PLANTING, WATERING, FERTILIZING, PESTICIDE, WEEDING, HARVESTING, OTHER).
     */
    private String activityType;

    /**
     * Nhãn hiển thị tiếng Việt của hoạt động (Bón phân, Tưới nước, Phun thuốc...).
     */
    private String activityLabel;

    /**
     * Tên vật tư nông nghiệp sử dụng (phân bón, thuốc bảo vệ thực vật, hạt giống...).
     */
    private String material;

    /**
     * Số lượng vật tư sử dụng.
     */
    private BigDecimal quantity;

    /**
     * Đơn vị tính (kg, lít, bao, bình, chai...).
     */
    private String unit;

    /**
     * Ngày thực hiện (định dạng YYYY-MM-DD). Mặc định là ngày hôm nay.
     */
    private LocalDate executedDate;

    /**
     * Ghi chú bổ sung hoặc chi tiết hiện trường trích xuất từ lời nói.
     */
    private String notes;

    /**
     * Câu tóm tắt nghiệp vụ bằng tiếng Việt tự nhiên để đọc lại cho nông dân nghe (Text-to-Speech).
     */
    private String summaryText;

    /**
     * Văn bản giọng nói gốc từ đầu vào.
     */
    private String rawVoiceText;

    /**
     * Độ tin cậy của việc nhận diện (0.0 đến 1.0).
     */
    @Builder.Default
    private Double confidence = 1.0;
}
