package vn.nguongocso.event.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.nguongocso.event.entity.ChainEvent;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChainEventRepository extends JpaRepository<ChainEvent, UUID> {

    @Query("SELECT ce FROM ChainEvent ce " +
            "WHERE ce.shipment.id = :shipmentId " +
            "AND ce.location IS NOT NULL " +
            "AND ce.isCorrection = false " +
            "ORDER BY ce.recordedAt ASC")
    List<ChainEvent> findJourneyPointsByShipmentId(@Param("shipmentId") UUID shipmentId);
}
