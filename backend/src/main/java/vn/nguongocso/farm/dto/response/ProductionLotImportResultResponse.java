package vn.nguongocso.farm.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Kết quả sau khi hoàn tất nhập dữ liệu lô sản xuất.
*/
@Getter
@Builder
public class ProductionLotImportResultResponse {
    private UUID importHistoryId;

    private String status;

    private String fileName;

    private Integer totalRows;

    private Integer successCount;

    private Integer failedCount;

    private List<UUID> savedLotIds;

    private List<ProductionLotImportRowError> errors;

    private LocalDateTime importedAt;
}