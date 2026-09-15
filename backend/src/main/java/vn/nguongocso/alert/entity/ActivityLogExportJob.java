package vn.nguongocso.alert.entity;

import java.time.LocalDate;
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
import vn.nguongocso.alert.enums.ActivityLogExportStatus;

/** Yêu cầu xuất nhật ký hoạt động được xử lý trong nền. */
@Entity
@Table(name = "activity_log_export_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogExportJob {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID organizationId;

    @Column(name = "requested_by", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID requestedBy;

    @Column(name = "requested_by_username", nullable = false, length = 100)
    private String requestedByUsername;

    @Column(name = "requested_by_role", nullable = false, length = 50)
    private String requestedByRole;

    private LocalDate startDate;
    private LocalDate endDate;
    private String actionFilter;
    private String actorFilter;
    private String objectTypeFilter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityLogExportStatus status;

    @Column(nullable = false)
    private Long recordCount;

    private String fileName;
    private String filePath;
    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
