package vn.nguongocso.auth.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO để khoá tạm một tài khoản nghi vấn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LockAccountRequest {
    
    /**
     * ID bản ghi bất thường dẫn tới khoá (optional).
     * Nếu có, hệ thống liên kết khoá này với anomaly.
     */
    private UUID anomalyId;
    
    /**
     * Ghi chú/lý do khoá tạm từ người quản lý (tối đa 500 ký tự).
     */
    @Size(max = 500, message = "Lý do khoá không được vượt quá 500 ký tự")
    private String reason;
}
