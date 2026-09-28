package vn.nguongocso.farm.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import vn.nguongocso.farm.enums.ChainProgressStage;

/**
 * Thông tin một cột giai đoạn trên bảng theo dõi tiến độ chuỗi.
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainProgressStageGroupResponse {
    private ChainProgressStage stage;

    private String stageName;

    private long count;

    @Builder.Default
    private List<ChainProgressItemResponse> items = new ArrayList<>();
}
