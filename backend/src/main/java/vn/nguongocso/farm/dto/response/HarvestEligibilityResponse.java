package vn.nguongocso.farm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả tính toán điều kiện cách ly thu hoạch (NCL-681 / NCL-843).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarvestEligibilityResponse {
    /**
     * Xác định được ngày thu hoạch hợp lệ hay chưa.
     * true: Không có PESTICIDE hoặc tất cả PESTICIDE đều match vật tư hợp lệ và có ngày thực hiện.
     * false: Có ít nhất một PESTICIDE không match được vật tư trong danh mục hoặc thiếu ngày thực hiện.
     */
    private boolean determined;

    /**
     * Ngày sớm nhất đủ điều kiện thu hoạch (MAX(executedDate + quarantineDays)).
     * null nếu determined = false hoặc lô không có hoạt động PESTICIDE nào.
     */
    private LocalDate eligibleHarvestDate;

    /**
     * Danh sách tên vật tư trong nhật ký PESTICIDE chưa xác định được trong danh mục (distinct).
     */
    @Builder.Default
    private List<String> unmatchedMaterials = new ArrayList<>();
}
