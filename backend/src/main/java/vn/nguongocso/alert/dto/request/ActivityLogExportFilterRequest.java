package vn.nguongocso.alert.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;
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

    @Size(max = 100, message = "Loại thao tác không được vượt quá 100 ký tự.")
    private String action;

    @Size(max = 255, message = "Người thực hiện không được vượt quá 255 ký tự.")
    private String actorName;

    @Size(max = 50, message = "Loại đối tượng không được vượt quá 50 ký tự.")
    private String objectType;
}
