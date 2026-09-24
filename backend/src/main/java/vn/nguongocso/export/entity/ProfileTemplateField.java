package vn.nguongocso.export.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
import vn.nguongocso.export.enums.ProfileFieldGroup;

/** Thực thể lưu trữ trường thông tin được chọn trong một mẫu hồ sơ truy xuất. */
@Entity
@Table(name = "profile_template_fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileTemplateField {
    /** Khóa chính của trường cấu hình */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Mẫu hồ sơ chứa trường này */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProfileTemplate template;

    /** Khóa định danh trường (ví dụ: organization.name, farmArea.name) */
    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    /** Nhóm trường dữ liệu */
    @Enumerated(EnumType.STRING)
    @Column(name = "field_group", nullable = false, length = 100)
    private ProfileFieldGroup fieldGroup;

    /** Cờ đánh dấu trường bắt buộc theo quy tắc QTN-11 */
    @Builder.Default
    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = false;

    /** Thứ tự sắp xếp hiển thị */
    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.isMandatory == null) {
            this.isMandatory = false;
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }
}
