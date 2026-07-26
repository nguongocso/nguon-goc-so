package vn.nguongocso.trace.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.trace.entity.Shipment;

/**
 * Repository quản lý lô hàng.
 */
public interface ShipmentRepository extends JpaRepository<Shipment, UUID>{

    List<Shipment> findByOrganizationOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<Shipment> findByOrganizationOrganizationIdAndProductionLotIdOrderByCreatedAtDesc(UUID organizationId, UUID productionLotId);

}
