package vn.nguongocso.trace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.organization.entity.Organization;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thực thể đại diện cho một dải mã truy xuất.
 */
@Entity
@Table(name = "code_ranges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeRange {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, unique = true)
    private String prefix;

    @Column(name = "from_number")
    private Long fromNumber;

    @Column(name = "to_number")
    private Long toNumber;

    @Column(name = "total_limit", nullable = false)
    private Long totalLimit;

    @Column(name = "used_count", nullable = false)
    private Long usedCount;

    @Column(name = "created_by")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null)
            id = UUID.randomUUID();
        if (usedCount == null)
            usedCount = 0L;
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
