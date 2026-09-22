package vn.nguongocso.report.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.trace.entity.TraceCode;

/**
 * Log tra cứu mã truy xuất.
 */
@Table(name = "trace_code_scan_logs")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceCodeScanLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trace_code_id", nullable = false)
    private TraceCode traceCode;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "location")
    private String location;

    @Column(name = "is_abnormal", nullable = false)
    private Boolean isAbnormal;

    @Column(name = "abnormal_reason")
    private String abnormalReason;
}
