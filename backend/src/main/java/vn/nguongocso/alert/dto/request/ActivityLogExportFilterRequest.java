package vn.nguongocso.alert.dto.request;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bộ lọc dùng chung cho xem trước và xuất nhật ký hoạt động.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogExportFilterRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String action;
    private String actorName;
    private String objectType;
}
