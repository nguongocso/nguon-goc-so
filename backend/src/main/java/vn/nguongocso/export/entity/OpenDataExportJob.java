package vn.nguongocso.export.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.export.enums.ExportJobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thực thể lưu trữ thông tin và trạng thái tác vụ xuất dữ liệu mở bất đồng bộ.
 */
@Entity
@Table(name = "open_data_export_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenDataExportJob {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "requested_by")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID requestedBy;

    @Column(name = "requested_by_username", length = 100)
    private String requestedByUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExportJobStatus status;

    @Column(length = 20)
    private String format;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ExportJobStatus.PENDING;
        }
    }
}
