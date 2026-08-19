package vn.nguongocso.report.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.organization.entity.Organization;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity ghi log truy cập báo cáo.
 *
 * @author Triệu Văn Đại
 */
@Table(name = "report_access_log")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportAccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_organization_id", nullable = false)
    private Organization targetOrganization;

    @Column(name = "report_name", nullable = false)
    private String reportName;

    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;
}
