package vn.nguongocso.recall.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response cho một yêu cầu thu hồi lô sản xuất (NCL-08-CN-008).
 */
@Getter
@Setter
@Builder
public class RecallRequestResponse {

    private UUID id;
    private UUID lotId;
    private String lotName;
    private UserInfo requestedBy;
    private LocalDateTime requestedAt;
    private String status;
    private String reason;
    private String evidence;
    private UserInfo approvedBy;
    private LocalDateTime approvedAt;
    private String approvalRemarks;
    private UserInfo rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private int notifiedBuyerCount;

    /** Thông tin người dùng gọn nhẹ (userId + fullName). */
    @Getter
    @Setter
    @Builder
    public static class UserInfo {
        private UUID userId;
        private String fullName;
    }
}