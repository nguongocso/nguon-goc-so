package vn.nguongocso.auth.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO để trả về một bản ghi lịch sử đăng nhập.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryResponse {
    
    /**
     * ID bản ghi đăng nhập.
     */
    private UUID id;
    
    /**
     * ID người dùng (null nếu username không tìm thấy).
     */
    private UUID userId;
    
    /**
     * Username được nhập vào.
     */
    private String usernameInput;
    
    /**
     * Mã vai trò của người dùng (VT-01, VT-02, ...).
     * Null nếu username không tìm thấy.
     */
    private String roleCode;
    
    /**
     * Kết quả: SUCCESS hoặc FAILED.
     */
    private String result;
    
    /**
     * Địa chỉ IP.
     */
    private String ipAddress;
    
    /**
     * Mã quốc gia.
     */
    private String countryCode;
    
    /**
     * Đánh dấu nếu đây là quốc gia lạ (chưa từng SUCCESS từ quốc gia này).
     */
    private Boolean isNewCountry;
    
    /**
     * Thời điểm xảy ra.
     */
    private OffsetDateTime createdAt;
}
