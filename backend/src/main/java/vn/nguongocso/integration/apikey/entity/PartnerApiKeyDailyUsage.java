package vn.nguongocso.integration.apikey.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Thực thể đếm lượt gọi theo ngày của khóa truy cập đối tác.
*/
@Entity
@Table(name = "partner_api_key_daily_usage", uniqueConstraints = @UniqueConstraint(name = "uq_partner_api_key_daily_usage", columnNames = {
        "api_key_id", "usage_date" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerApiKeyDailyUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "api_key_id", nullable = false)
    private UUID apiKeyId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Builder.Default
    @Column(name = "call_count", nullable = false)
    private Integer callCount = 0;

    @Column(name = "warning_sent_at")
    private LocalDateTime warningSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Thiết lập thời điểm tạo và giá trị mặc định trước khi lưu mới.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (callCount == null) {
            callCount = 0;
        }
    }

    /**
     * Cập nhật thời điểm chỉnh sửa trước khi cập nhật bản ghi.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
