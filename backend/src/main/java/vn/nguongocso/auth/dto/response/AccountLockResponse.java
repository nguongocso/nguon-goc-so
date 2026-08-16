package vn.nguongocso.auth.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO để trả về kết quả khoá/mở khoá tài khoản.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLockResponse {
    
    /**
     * ID tài khoản bị khoá.
     */
    private UUID accountId;
    
    /**
     * Trạng thái hiện tại: LOCKED hoặc ACTIVE (unlocked).
     */
    private String status;
    
    /**
     * Username của người thực hiện khoá.
     */
    private String lockedBy;
    
    /**
     * Thời điểm khoá.
     */
    private OffsetDateTime lockedAt;
    
    /**
     * Username của người thực hiện mở khoá (nếu có).
     */
    private String unlockedBy;
    
    /**
     * Thời điểm mở khoá (nếu có).
     */
    private OffsetDateTime unlockedAt;
    
    /**
     * Lý do khoá được ghi lại.
     */
    private String reason;
    
    /**
     * Cờ xác nhận thông báo đã được gửi cho tài khoản chủ.
     */
    private Boolean notificationSent;
}
