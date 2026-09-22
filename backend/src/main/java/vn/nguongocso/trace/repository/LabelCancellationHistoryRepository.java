package vn.nguongocso.trace.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.nguongocso.trace.entity.LabelCancellationHistory;

/** Repository quản lý lịch sử hủy tem. */
@Repository
public interface LabelCancellationHistoryRepository extends JpaRepository<LabelCancellationHistory, UUID> {

    /** Lấy lịch sử hủy tem theo lô hàng và tổ chức. */
    @Query("SELECT h FROM LabelCancellationHistory h WHERE h.shipment.id = :shipmentId AND h.organization.id = :organizationId ORDER BY h.cancelledAt DESC")
    List<LabelCancellationHistory> findByShipmentIdAndOrganizationIdOrderByCancelledAtDesc(
            @Param("shipmentId") UUID shipmentId,
            @Param("organizationId") UUID organizationId);
}
