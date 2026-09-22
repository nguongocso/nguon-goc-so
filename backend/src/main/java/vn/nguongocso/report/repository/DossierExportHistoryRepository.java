package vn.nguongocso.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.report.entity.DossierExportHistory;

import java.util.List;
import java.util.UUID;

/** Repository quản lý lịch sử xuất hồ sơ. */
@Repository
public interface DossierExportHistoryRepository extends JpaRepository<DossierExportHistory, UUID> {

    /** Xóa tất cả lịch sử xuất hồ sơ liên quan đến một lô hàng. */
    void deleteByShipmentId(UUID shipmentId);

    /** Lấy danh sách lịch sử xuất hồ sơ của một tổ chức theo thời gian mới nhất. */
    List<DossierExportHistory> findByOrganization_OrganizationIdOrderByExportedAtDesc(UUID organizationId);
}
