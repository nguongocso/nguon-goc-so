package vn.nguongocso.trace.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.trace.entity.LabelCancellationHistory;

public interface LabelCancellationHistoryRepository extends JpaRepository<LabelCancellationHistory, UUID> {

    @Query("SELECT h FROM LabelCancellationHistory h WHERE h.shipment.id = :shipmentId AND h.organization.id = :organizationId ORDER BY h.cancelledAt DESC")
    List<LabelCancellationHistory> findByShipmentIdAndOrganizationIdOrderByCancelledAtDesc(
            @Param("shipmentId") UUID shipmentId,
            @Param("organizationId") UUID organizationId);
}
