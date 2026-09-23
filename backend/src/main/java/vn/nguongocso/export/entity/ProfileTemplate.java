package vn.nguongocso.export.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.organization.entity.Organization;

/**
 * Thực thể đại diện cho Mẫu hồ sơ truy xuất nguồn gốc (Profile Template) được cấu hình theo yêu cầu đối tác.
 * User Story: NCL-07-CN-007.
 */
@Entity
@Table(name = "profile_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileTemplate {
    /** Khóa chính định danh mẫu hồ sơ */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Tổ chức / Hợp tác xã sở hữu mẫu hồ sơ (QTN-01) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Tên mẫu hồ sơ (ví dụ: Mẫu xuất khẩu Nhật Bản, Mẫu giao Co.opmart) */
    @Column(name = "name", nullable = false)
    private String name;

    /** Tên đối tác / khách hàng áp dụng biểu mẫu */
    @Column(name = "partner_name")
    private String partnerName;

    /** Diễn giải chi tiết mục đích sử dụng mẫu */
    @Column(name = "description", length = 500)
    private String description;

    /** Cờ đánh dấu mẫu mặc định khi xuất hồ sơ trong tổ chức */
    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    /** Người tạo mẫu */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** Thời điểm tạo */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm cập nhật lần cuối */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Danh sách các trường dữ liệu thuộc mẫu hồ sơ */
    @Builder.Default
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProfileTemplateField> fields = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.isDefault == null) {
            this.isDefault = false;
        }
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
