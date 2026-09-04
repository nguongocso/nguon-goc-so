package vn.nguongocso.certification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kết quả kiểm tra lô sản xuất đã đủ mốc canh tác bắt buộc (theo loại nông sản
 * + tiêu chuẩn của lô) để ghi sự kiện đóng gói. Story: NCL-09-CN-011.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneEligibilityResponse {

    /** ID lô sản xuất được kiểm tra. */
    private UUID productionLotId;

    /** true = đã đáp ứng mọi mốc canh tác bắt buộc. */
    private boolean eligible;

    /** Danh sách mốc canh tác bắt buộc còn thiếu (rỗng khi eligible = true). */
    @Builder.Default
    private List<MissingMilestone> missingMilestones = new ArrayList<>();

    /**
     * Một mốc canh tác bắt buộc còn thiếu.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingMilestone {

        /** Tên mốc canh tác. */
        private String name;

        /** Loại hoạt động của mốc (FarmActivityType), nguyên bản từ cấu hình. */
        private String activityType;
    }
}
