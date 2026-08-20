package vn.nguongocso.report.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity lịch sử xuất hồ sơ.
 *
 * @author Triệu Văn Đại
 */
@Table(name = "dossier_export_history")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierExportHistory {
    // ID lịch sử xuất
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    // Lô hàng được xuất
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    // Người xuất
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exporter_id", nullable = false)
    private User exporter;

    // Tổ chức xuất
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // Thời gian xuất
    @Column(name = "exported_at", nullable = false)
    private LocalDateTime exportedAt;

    // Tên file
    @Column(name = "file_name", nullable = false)
    private String fileName;

    // Kích thước file (bytes)
    @Column(name = "file_size")
    private Long fileSize;

    // Trạng thái xuất (SUCCESS/FAILED)
    @Column(name = "status", nullable = false)
    private String status;

    // Địa chỉ IP
    @Column(name = "ip_address")
    private String ipAddress;
}