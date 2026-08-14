package vn.nguongocso.trace.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.trace.entity.Recall;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Repository quản lý các đợt thu hồi.
 */
@Repository
public interface RecallRepository extends JpaRepository<Recall, UUID> {
    /**
     * Lấy bản ghi thu hồi mới nhất của một lô hàng.
     */
    Optional<Recall> findTopByShipmentOrderByRecalledAtDesc(Shipment shipment);

    /**
     * Lấy bản ghi thu hồi của một lô hàng theo shipmentId.
     */
    Optional<Recall> findByShipmentId(UUID shipmentId);
}