package vn.nguongocso.auth.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.enums.AnomalyReasonCode;
import vn.nguongocso.auth.enums.AnomalyStatus;
import vn.nguongocso.organization.entity.Organization;

/**
 * Ghi nhận các lần phát hiện bất thường đăng nhập của một tài khoản.
 * 
 * <p>
 * Bản ghi này được tạo khi:
 * - Tài khoản sai mật khẩu ≥ 5 lần liên tiếp trong cửa sổ 2 phút, HOẶC
 * - Tài khoản đăng nhập thành công từ quốc gia chưa từng ghi nhận SUCCESS
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
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;
    
    /**
     * Tài khoản nghi vấn (bất kể vai trò VT-01..VT-05).
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Tổ chức chứa tài khoản này. Dùng để lọc theo phạm vi quản lý.
     */
    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    /**
     * Nguyên nhân phát hiện bất thường.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyReasonCode reasonCode;
    
    /**
     * Số lần sai liên tiếp (khi reasonCode = REPEATED_FAILED_LOGIN).
     * Nullable khi reasonCode = UNUSUAL_COUNTRY.
     */
    @Column(nullable = true)
    private Integer attemptCount;
    
    /**
     * Địa chỉ IP tại thời điểm phát hiện bất thường.
     */
    @Column(nullable = false, length = 45)
    private String ipAddress;
    
    /**
     * Mã quốc gia tại thời điểm phát hiện bất thường.
     */
    @Column(nullable = true, length = 2)
    private String countryCode;
    
    /**
     * Thời điểm hệ thống đánh dấu bất thường.
     */
    @Column(nullable = false)
    private OffsetDateTime detectedAt;
    
    /**
     * Trạng thái xử lý bất thường: OPEN, ACCOUNT_LOCKED, DISMISSED.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyStatus status = AnomalyStatus.OPEN;
    
    /**
     * ID thông báo đã tạo qua NotificationService cho sự kiện này.
     * Dùng để theo dõi khi thông báo được tạo.
     */
    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID notificationId;
    
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (detectedAt == null) {
            detectedAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = AnomalyStatus.OPEN;
        }
    }
}
