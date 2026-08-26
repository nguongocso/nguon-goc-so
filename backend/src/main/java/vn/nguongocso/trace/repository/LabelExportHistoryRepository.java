package vn.nguongocso.trace.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.trace.entity.LabelExportHistory;

/**
 * Repository quản lý lịch sử xuất tem QR (NCL-04-CN-005).
 */
public interface LabelExportHistoryRepository extends JpaRepository<LabelExportHistory, UUID> {

        /**
         * Lấy lịch sử xuất tem của một lô hàng (mới nhất trước).
         */
        List<LabelExportHistory> findByShipment_IdOrderByExportedAtDesc(UUID shipmentId);

        /**
         * Lấy lịch sử xuất tem của một tổ chức.
         */
        List<LabelExportHistory> findByOrganization_OrganizationIdOrderByExportedAtDesc(UUID organizationId);
}
