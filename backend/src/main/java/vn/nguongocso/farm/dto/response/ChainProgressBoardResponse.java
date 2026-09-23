package vn.nguongocso.farm.dto.response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thông tin bảng theo dõi tiến độ chuỗi của từng lô.
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainProgressBoardResponse {
    private UUID organizationId;

    private String organizationName;

    private long totalOpenLots;

    private long stagnantLotsCount;

    private int stagnantThresholdDays;

    @Builder.Default
    private List<ChainProgressStageGroupResponse> stages = new ArrayList<>();
}
