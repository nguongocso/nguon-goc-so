package vn.nguongocso.certification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kết quả kiểm tra lô sản xuất đã đủ mốc canh tác bắt buộc để ghi sự kiện đóng gói.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneEligibilityResponse {
    private UUID productionLotId;

    private boolean eligible;

    @Builder.Default
    private List<MissingMilestone> missingMilestones = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingMilestone {
        private String name;

        private String activityType;
    }
}
