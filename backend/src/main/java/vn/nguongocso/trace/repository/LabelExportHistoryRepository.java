package vn.nguongocso.trace.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.trace.entity.LabelExportHistory;

/** Repository quản lý lịch sử xuất tem QR. */
@Repository
public interface LabelExportHistoryRepository extends JpaRepository<LabelExportHistory, UUID> {

    /** Lấy lịch sử xuất tem của một lô hàng. */
    List<LabelExportHistory> findByShipment_IdOrderByExportedAtDesc(UUID shipmentId);

    /** Lấy lịch sử xuất tem của một tổ chức. */
    List<LabelExportHistory> findByOrganization_OrganizationIdOrderByExportedAtDesc(UUID organizationId);
}
