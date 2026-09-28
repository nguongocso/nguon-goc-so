package vn.nguongocso.integration.partner.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.nguongocso.integration.partner.entity.PartnerLotAccessLog;

/**
 * Repository truy vấn nhật ký truy xuất lô của đối tác.
*/
@Repository
public interface PartnerLotAccessLogRepository extends JpaRepository<PartnerLotAccessLog, UUID> {
    /**
     * Tìm danh sách khóa API đối tác đã từng truy xuất lô hàng hoặc lô sản xuất.
     */
    @Query("""
            SELECT DISTINCT log.partnerApiKey.id
            FROM PartnerLotAccessLog log
            WHERE (log.shipment.id = :shipmentId OR log.productionLot.id = :productionLotId)
              AND log.accessedAt >= :since
            """)
    List<UUID> findDistinctPartnerApiKeyIdsByShipmentOrProductionLot(
            @Param("shipmentId") UUID shipmentId,
            @Param("productionLotId") UUID productionLotId,
            @Param("since") LocalDateTime since);

    /**
     * Kiểm tra đối tác đã từng truy xuất lô hàng hoặc lô sản xuất hay chưa.
     */
    @Query("""
            SELECT COUNT(log) > 0
            FROM PartnerLotAccessLog log
            WHERE log.partnerApiKey.id = :partnerApiKeyId
              AND (log.shipment.id = :shipmentId OR log.productionLot.id = :productionLotId)
              AND log.accessedAt >= :since
            """)
    boolean hasPartnerAccessedLot(
            @Param("partnerApiKeyId") UUID partnerApiKeyId,
            @Param("shipmentId") UUID shipmentId,
            @Param("productionLotId") UUID productionLotId,
            @Param("since") LocalDateTime since);
}
