package vn.nguongocso.loginanomaly.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.loginanomaly.entity.LoginAnomaly;
import vn.nguongocso.loginanomaly.enums.LoginAnomalySeverity;

/** Phản hồi một bản ghi đăng nhập bất thường trong danh sách. */
@Getter
@Builder
public class LoginAnomalyResponse {

    private UUID id;

    private String username;

    private String fullName;

    private UUID organizationId;

    private String organizationName;

    private String ipAddress;

    private String location;

    private String reason;

    private LoginAnomalySeverity severity;

    private LocalDateTime loginAt;

    /** Trạng thái tài khoản hiện tại (đọc từ bảng users, mặc định ACTIVE). */
    private UserStatus accountStatus;

    /**
     * Chuyển entity sang response, trạng thái tài khoản lấy từ {@code statusMap}
     * (bản đồ userId -> trạng thái đã nạp từ bảng users).
     */
    public static LoginAnomalyResponse from(
            LoginAnomaly anomaly,
            Map<UUID, UserStatus> statusMap) {

        UserStatus accountStatus = anomaly.getUserId() != null
                ? statusMap.getOrDefault(anomaly.getUserId(), UserStatus.ACTIVE)
                : UserStatus.ACTIVE;

        return LoginAnomalyResponse.builder()
                .id(anomaly.getAnomalyId())
                .username(anomaly.getUsername())
                .fullName(anomaly.getFullName())
                .organizationId(anomaly.getOrganizationId())
                .organizationName(anomaly.getOrganizationName())
                .ipAddress(anomaly.getIpAddress())
                .location(anomaly.getLocation())
                .reason(anomaly.getReason())
                .severity(anomaly.getSeverity())
                .loginAt(anomaly.getLoginAt())
                .accountStatus(accountStatus)
                .build();
    }
}
