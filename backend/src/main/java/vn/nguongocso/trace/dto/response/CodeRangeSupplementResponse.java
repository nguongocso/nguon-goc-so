package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response cho một yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007).
 */
@Getter
@Setter
@Builder
public class CodeRangeSupplementResponse {

    private UUID id;
    private UUID organizationId;
    private String organizationName;
    private UserInfo requestedBy;
    private LocalDateTime requestedAt;
    private Long requestedQuantity;
    private Long approvedQuantity;
    private String status;
    private String reason;
    private List<UUID> evidenceEventIds;
    /**
     * Chi tiết bằng chứng đã resolve từ {@code evidenceEventIds} (loại sự kiện,
     * tên lô, thời điểm, người ghi) để VT-01 xem khi duyệt mà không cần gọi
     * endpoint evidence-events (vốn chỉ dành cho VT-02). Giữ đúng thứ tự ID gốc;
     * sự kiện đã bị xóa thì vắng mặt (FE fallback hiển thị ID).
     */
    private List<EvidenceEventResponse> evidenceEvents;
    private UserInfo approvedBy;
    private LocalDateTime approvedAt;
    private String approvalRemarks;
    private UserInfo rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private int notifiedCount;

    /** Thông tin người dùng gọn nhẹ (userId + fullName). */
    @Getter
    @Setter
    @Builder
    public static class UserInfo {
        private UUID userId;
        private String fullName;
    }
}
