package vn.nguongocso.certification.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Thực thể đơn vị kiểm nghiệm được công nhận trong danh mục dùng chung.
 * <p>
 * Danh mục này do Quản trị viên nền tảng (VT-01) quản lý
 * (story NCL-11-CN-006 - Phase 1). Khi tạo yêu cầu kiểm nghiệm,
 * người dùng chọn đơn vị từ danh mục thay vì nhập tự do.
 */
@Entity
@Table(name = "testing_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestingUnit {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Tên đơn vị kiểm nghiệm. Duy nhất trong danh mục, không phân biệt hoa/thường.
     */
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /**
     * Mã công nhận/trình độ kỹ thuật của đơn vị kiểm nghiệm.
     */
    @Column(name = "accreditation_code", nullable = false, length = 100)
    private String accreditationCode;

    /**
     * Thông tin liên hệ (địa chỉ, điện thoại, email...).
     */
    @Column(name = "contact_info", length = 500)
    private String contactInfo;

    /**
     * Ngày hết hạn công nhận của đơn vị kiểm nghiệm.
     */
    @Column(name = "accreditation_expiry_date")
    private LocalDate accreditationExpiryDate;

    /**
     * Trạng thái hiệu lực của đơn vị kiểm nghiệm trong danh mục.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (isActive == null) {
            isActive = true;
        }

        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}