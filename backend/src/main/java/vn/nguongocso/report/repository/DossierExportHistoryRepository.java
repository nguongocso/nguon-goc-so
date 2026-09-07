package vn.nguongocso.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.report.entity.DossierExportHistory;

import java.util.UUID;

/**
 * Repository cho thực thể DossierExportHistory.
 */
@Repository
public interface DossierExportHistoryRepository extends JpaRepository<DossierExportHistory, UUID> {
    /**
     * Xóa tất cả các bản ghi lịch sử xuất hồ sơ liên quan đến một lô hàng cụ thể.
     *
     * @param shipmentId ID của lô hàng
     */
    void deleteByShipmentId(UUID shipmentId);

    /**
     * Lấy danh sách lịch sử xuất hồ sơ của một tổ chức theo thời gian mới nhất.
     *
     * @param organizationId ID tổ chức
     * @return Danh sách lịch sử xuất hồ sơ
     */
    java.util.List<DossierExportHistory> findByOrganization_OrganizationIdOrderByExportedAtDesc(UUID organizationId);
}
