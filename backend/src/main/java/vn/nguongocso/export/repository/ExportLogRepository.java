package vn.nguongocso.export.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.export.entity.ExportLog;

import java.util.List;
import java.util.UUID;

/**
 * Repository quản lý nhật ký xuất hồ sơ (ExportLog).
 */
@Repository
public interface ExportLogRepository extends JpaRepository<ExportLog, UUID> {

    /**
     * Lấy danh sách lịch sử xuất của một lô hàng theo thời gian giảm dần.
     */
    List<ExportLog> findByShipment_IdOrderByExportedAtDesc(UUID shipmentId);

    /**
     * Tìm nhật ký xuất theo mẫu hồ sơ.
     */
    List<ExportLog> findByTemplate_Id(UUID templateId);
}
