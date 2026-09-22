package vn.nguongocso.alert.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Một dòng dữ liệu bất biến thuộc snapshot của export job. */
@Entity
@Table(name = "activity_log_export_items")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogExportItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID jobId;

    @Column(nullable = false)
    private Long sequenceNo;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    private String actorName;

    private String actorUsername;

    private String actorRole;

    private String actionType;

    private String objectType;

    private String objectIdentifier;

    @Column(columnDefinition = "TEXT")
    private String beforeValue;

    @Column(columnDefinition = "TEXT")
    private String afterValue;
}
