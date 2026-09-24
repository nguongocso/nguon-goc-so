package vn.nguongocso.farm.dto.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả kiểm tra điều kiện cách ly thu hoạch.
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarvestEligibilityResponse {
    private boolean determined;

    private LocalDate eligibleHarvestDate;

    @Builder.Default
    private List<String> unmatchedMaterials = new ArrayList<>();
}
