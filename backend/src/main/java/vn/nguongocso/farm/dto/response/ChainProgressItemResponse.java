package vn.nguongocso.farm.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import vn.nguongocso.farm.enums.ChainProgressStage;
import vn.nguongocso.farm.enums.ProductionLotStatus;

/**
 * Thông tin một lô sản xuất trên bảng theo dõi tiến độ chuỗi.
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainProgressItemResponse {
    private UUID id;

    private String name;

    private UUID farmAreaId;

    private String farmAreaName;

    private UUID productCategoryId;

    private String productCategoryName;

    private ProductionLotStatus status;

    private ChainProgressStage currentStage;

    private long daysInStage;

    private boolean isStagnant;

    private String nextActionRequired;

    private String targetScreen;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
