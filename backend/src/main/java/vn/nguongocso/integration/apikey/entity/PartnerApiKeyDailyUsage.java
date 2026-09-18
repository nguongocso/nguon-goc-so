package vn.nguongocso.integration.apikey.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

/**
 * Thực thể đếm lượt gọi theo ngày của khóa truy cập đối tác (NCL-12-CN-005).
 * <p>
 * Độ mịn: một dòng cho mỗi khóa × ngày. Đây là nguồn sự thật của ngưỡng cảnh
 * báo hạn mức nên cảnh báo không mất khi khởi động lại và không gửi trùng khi
 * chạy nhiều instance. Cột {@code warningSentAt} đóng vai trò cờ claim chống
 * trùng cảnh báo trong ngày.
 */
@Entity
@Table(name = "partner_api_key_daily_usage",
        // Trùng khớp ràng buộc UNIQUE trong migration V20260916085531 để schema do
        // Hibernate sinh ra ở profile test (create-drop) cũng chặn trùng như production.
        uniqueConstraints = @UniqueConstraint(
                name = "uq_partner_api_key_daily_usage",
                columnNames = { "api_key_id", "usage_date" }))
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

    /**
     * ID khóa truy cập.
     * <p>
     * Cố ý lưu dưới dạng cột UUID (không dùng {@code @ManyToOne}) vì đường gọi
     * API của đối tác chỉ cần ID, tránh join thừa trên luồng nóng.
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "api_key_id", nullable = false)
    private UUID apiKeyId;

    /** Ngày nghiệp vụ ghi nhận lượt gọi. */
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    /** Số lượt gọi đã xác thực thành công trong ngày. */
    @Builder.Default
    @Column(name = "call_count", nullable = false)
    private Integer callCount = 0;

    /** Mốc gửi cảnh báo hạn mức (null = chưa gửi). */
    @Column(name = "warning_sent_at")
    private LocalDateTime warningSentAt;

    /** Thời điểm tạo dòng usage trong ngày. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm cập nhật dòng usage trong ngày gần nhất. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}