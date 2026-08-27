package vn.nguongocso.auth.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả precheck các lô chưa hoàn thành đang phân công cho thành viên
 * trước khi vô hiệu hóa (API GET /members/{userId}/unfinished-lots).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnfinishedLotsResponse {

    private UUID userId;

    /**
     * {@code true} nếu còn lô chưa hoàn thành → bắt buộc chọn người
     * thay thế khi vô hiệu hóa.
     */
    private boolean hasUnfinishedLots;

    /** Số lô chưa hoàn thành đang phân công. */
    private long total;

    /** Trường tường minh cho FE — bằng {@code hasUnfinishedLots}. */
    private boolean replacementRequired;

    /** Danh sách lô chưa hoàn thành đang phân công. */
    private List<MemberLotSummary> lots;
}
