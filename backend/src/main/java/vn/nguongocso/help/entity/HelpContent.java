package vn.nguongocso.help.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thực thể lưu nội dung hướng dẫn sử dụng trong ứng dụng (NCL-01-CN-006).
 */
@Entity
@Table(name = "help_content")
@Getter
@Setter
@NoArgsConstructor
public class HelpContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Mã định danh màn hình (ví dụ: {@code farm-log-create}). */
    @Column(name = "screen_key", nullable = false, length = 100)
    private String screenKey;

    /** Mã vai trò (ví dụ: {@code VT-03}). Ký tự {@code GENERAL} là hướng dẫn dùng chung. */
    @Column(name = "role_code", nullable = false, length = 20)
    private String roleCode;

    /** Tiêu đề hướng dẫn. */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** JSON array chứa các bước hướng dẫn. */
    @Column(name = "steps", nullable = false, columnDefinition = "TEXT")
    private String steps;

    /** Dữ liệu ví dụ tuỳ chọn. */
    @Column(name = "example_data", columnDefinition = "TEXT")
    private String exampleData;

    /** Thứ tự hiển thị. */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (sortOrder == null) {
            sortOrder = 0;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}