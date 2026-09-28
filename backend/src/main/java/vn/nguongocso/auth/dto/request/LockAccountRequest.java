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
    private UUID anomalyId;

    @Size(max = 500, message = "Lý do khoá không được vượt quá 500 ký tự")
    private String reason;

    private boolean permanent;

    private Integer days;

    private Integer hours;

    private Integer minutes;
}
