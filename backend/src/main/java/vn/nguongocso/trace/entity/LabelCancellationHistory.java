package vn.nguongocso.trace.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.organization.entity.Organization;

/**
 * Thực thể đại diện cho một đợt hủy tem truy xuất.
 */
@Getter
@Setter
@Entity
@Table(name = "label_cancellation_history")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelCancellationHistory {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancelled_by", nullable = false)
    private User cancelledBy;

    @Column(name = "cancelled_at", nullable = false)
    private LocalDateTime cancelledAt;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "cancellation_type", nullable = false, length = 20)
    private String cancellationType; // 'RANGE' hoặc 'SINGLE'

    @Column(name = "range_from_code", length = 100)
    private String rangeFromCode;

    @Column(name = "range_to_code", length = 100)
    private String rangeToCode;

    @Column(name = "reason_type", nullable = false, length = 50)
    private String reasonType;

    @Column(name = "reason_note", columnDefinition = "TEXT")
    private String reasonNote;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (cancelledAt == null) {
            cancelledAt = LocalDateTime.now();
        }
    }
}
