package vn.nguongocso.farm.dto.response;

import java.time.Instant;
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
    private UUID importHistoryId; // ID lịch sử nhập dữ liệu.

    private String status; // Trạng thái: SUCCESS, PARTIAL_SUCCESS hoặc FAILED.

    private String fileName; // Tên tệp đã nhập.

    private Integer totalRows; // Tổng số dòng dữ liệu.

    private Integer successCount; // Số dòng được lưu thành công.

    private Integer failedCount; // Số dòng bị lỗi.

    private List<UUID> savedLotIds; // Danh sách ID các lô đã được tạo.

    private List<ProductionLotImportRowError> errors; // Danh sách dòng lỗi.

    private Instant importedAt; // Thời điểm hoàn tất nhập dữ liệu.
}