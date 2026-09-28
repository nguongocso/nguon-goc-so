package vn.nguongocso.farm.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Dữ liệu xem trước khi tạo lô sản xuất mới từ mẫu vụ trước.
*/
@Getter
@Setter
@Builder
public class CloneProductionLotPreviewResponse {
    private UUID sourceLotId;

    private String sourceLotName;

    private UUID farmAreaId;

    private String farmAreaName;

    private UUID productCategoryId;

    private String productCategoryName;

    private String name;

    private Double expectedQuantity;

    private String expectedQuantityUnit;

    private LocalDate plantingDate;

    private List<CloneCertificationInfo> activeCertifications;

    private List<CloneCertificationInfo> skippedCertifications;

    private List<String> warnings;
}
