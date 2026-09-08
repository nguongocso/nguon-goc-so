package vn.nguongocso.farm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.farm.enums.ChainProgressStage;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO đại diện cho một cột giai đoạn trên bảng theo dõi tiến độ chuỗi (NCL-10-CN-013).
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
