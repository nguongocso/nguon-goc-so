package vn.nguongocso.auth.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO để trả về thông tin chi tiết một bản ghi bất thường đăng nhập.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAnomalyResponse {
    
    /**
     * ID bản ghi bất thường.
     */
    private UUID id;
    
    /**
     * ID tài khoản bị nghi vấn.
     */
    private UUID userId;
    
    /**
     * Username của tài khoản.
     */
    private String username;
    
    /**
     * Tên đầy đủ.
     */
    private String fullName;
    
    /**
     * Mã vai trò (VT-01, VT-02, VT-03, ...).
     */
    private String roleCode;
    
    /**
     * ID tổ chức chứa tài khoản.
     */
    private UUID organizationId;
    
    /**
     * Tên tổ chức.
     */
    private String organizationName;
    
    /**
     * Nguyên nhân: REPEATED_FAILED_LOGIN, UNUSUAL_COUNTRY.
     */
    private String reasonCode;
    
    /**
     * Số lần sai liên tiếp (nếu reasonCode = REPEATED_FAILED_LOGIN).
     */
    private Integer attemptCount;
    
    /**
     * Địa chỉ IP tại thời điểm phát hiện.
     */
    private String ipAddress;
    
    /**
     * Mã quốc gia.
     */
    private String countryCode;
    
    /**
     * Thời điểm phát hiện bất thường.
     */
    private OffsetDateTime detectedAt;
    
    /**
     * Trạng thái: OPEN, ACCOUNT_LOCKED, DISMISSED.
     */
    private String status;
    
    /**
     * ID thông báo đã tạo cho sự kiện này (nếu có).
     */
    private UUID notificationId;
}
