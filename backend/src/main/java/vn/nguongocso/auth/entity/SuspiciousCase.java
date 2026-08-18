package vn.nguongocso.auth.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import vn.nguongocso.auth.enums.AnomalyStatus;
import vn.nguongocso.organization.entity.Organization;

/**
 * Mỗi bản ghi đại diện cho một trường hợp nghi vấn của một tài khoản.
 *
 * <p>
 * Một SuspiciousCase được tạo khi user có đủ 5 anomaly trong vòng 24h.
 * Nó là "case" xử lý, không phải bản ghi lịch sử sự kiện.
 * Nếu có một nhóm anomaly mới sau khi case cũ đã giải quyết, sẽ tạo case mới,
 * không mở lại case cũ.
 * </p>
 */
@Entity
@Table(name = "suspicious_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspiciousCase {

    @Id
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyStatus status = AnomalyStatus.OPEN;

    @Column(nullable = false)
    private int anomalyCount;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = true)
    private OffsetDateTime resolvedAt;

    @Column(nullable = false)
    private OffsetDateTime firstDetectedAt;

    @Column(nullable = false)
    private OffsetDateTime lastDetectedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = AnomalyStatus.OPEN;
        }
        if (firstDetectedAt == null) {
            firstDetectedAt = createdAt;
        }
        if (lastDetectedAt == null) {
            lastDetectedAt = createdAt;
        }
    }
}
