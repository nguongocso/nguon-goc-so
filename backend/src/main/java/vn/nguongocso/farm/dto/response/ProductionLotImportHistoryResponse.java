package vn.nguongocso.farm.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Thông tin lịch sử nhập dữ liệu lô sản xuất (dùng cho frontend).
 */
@Getter
@Builder
public class ProductionLotImportHistoryResponse {
    private UUID id;

    private String fileName;

    private Integer totalRows;

    private Integer successCount;

    private Integer failedCount;

    private String status;

    private Instant importedAt;

}