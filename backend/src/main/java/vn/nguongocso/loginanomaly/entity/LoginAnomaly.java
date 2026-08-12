package vn.nguongocso.loginanomaly.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.loginanomaly.enums.LoginAnomalySeverity;

/**
 * Bản ghi một phiên đăng nhập bất thường được hệ thống phát hiện.
 *
 * <p>
 * Danh sách này được quản trị viên (VT-01) xem toàn nền tảng,
 * quản lý hợp tác xã (VT-02) chỉ xem dữ liệu thuộc tổ chức của mình.
 * </p>
 */
@Entity
@Table(name = "login_anomalies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAnomaly {

    @Id
    @Column(name = "anomaly_id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID anomalyId;

    /** ID người dùng (null nếu tài khoản không tồn tại tại thời điểm phát hiện). */
    @Column(name = "user_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    /** Tên đăng nhập của tài khoản bị nghi vấn. */
    @Column(nullable = false, length = 100)
    private String username;

    /** Họ tên người dùng (snapshot tại thời điểm phát hiện). */
    @Column(name = "full_name", length = 255)
    private String fullName;

    /** Tổ chức của tài khoản (snapshot tại thời điểm phát hiện). */
    @Column(name = "organization_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID organizationId;

    @Column(name = "organization_name", length = 255)
    private String organizationName;

    /** IP của phiên đăng nhập bất thường. */
    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    /** Vị trí suy ra từ IP. */
    @Column(nullable = false, length = 255)
    private String location;

    /** Nguyên nhân được xác định bất thường. */
    @Column(nullable = false, length = 255)
    private String reason;

    /** Mức độ bất thường. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoginAnomalySeverity severity;

    /** Thời điểm xảy ra phiên đăng nhập bất thường. */
    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    /** Thời điểm bản ghi được tạo. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (anomalyId == null) {
            anomalyId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
