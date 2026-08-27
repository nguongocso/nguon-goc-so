package vn.nguongocso.auth.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload {@code errors} của phản hồi 409 khi vô hiệu hóa thành viên
 * còn phân công vào lô chưa hoàn thành — FE dựa vào
 * {@code requiresReplacement} để mở bước chọn người thay thế.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplacementRequiredError {

    /** Mã mô tả cố định để FE phân nhánh (không phải error-code hệ thống mới). */
    private String code;

    private boolean requiresReplacement;

    /** Danh sách lô chưa hoàn thành đang phân công cho thành viên. */
    private List<MemberLotSummary> pendingLots;
}
