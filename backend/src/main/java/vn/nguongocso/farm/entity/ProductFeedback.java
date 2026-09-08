package vn.nguongocso.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.trace.entity.TraceCode;

/**
 * Entity đại diện cho phản hồi sản phẩm.
 */
@Entity
@Table(name = "product_feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_lot_id", nullable = false)
    private ProductionLot productionLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trace_code_id")
    private TraceCode traceCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private ProductFeedbackStatus status = ProductFeedbackStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    @Builder.Default
    private ProductFeedbackSeverity severity = ProductFeedbackSeverity.INFORMATION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "processing_content", columnDefinition = "TEXT")
    private String processingContent;

    @Column(name = "public_response", columnDefinition = "TEXT")
    private String publicResponse;

    @Column(name = "lookup_code_hash", length = 64, unique = true)
    private String lookupCodeHash;

    @Column(name = "close_reason", columnDefinition = "TEXT")
    private String closeReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = ProductFeedbackStatus.NEW;
        }
        if (severity == null) {
            severity = ProductFeedbackSeverity.INFORMATION;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
