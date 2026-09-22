package vn.nguongocso.integration.partner.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.partner.entity.PartnerLotAccessLog;
import vn.nguongocso.integration.partner.repository.PartnerLotAccessLogRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Service ghi nhận nhật ký đối tác bên thứ ba truy xuất dữ liệu của lô.
*/
@Service
@RequiredArgsConstructor
public class PartnerLotAccessService {
    private static final Logger log = LoggerFactory.getLogger(PartnerLotAccessService.class);

    private final PartnerLotAccessLogRepository partnerLotAccessLogRepository;

    private final ShipmentRepository shipmentRepository;

    private final ProductionLotRepository productionLotRepository;

    /**
     * Ghi nhận lượt truy xuất dữ liệu lô của đối tác.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLotAccess(PartnerApiKey partnerApiKey, UUID shipmentId, UUID productionLotId) {
        if (partnerApiKey == null) {
            return;
        }
        if (Boolean.TRUE.equals(partnerApiKey.getIsTest())) {
            return;
        }
        try {
            Shipment shipment = null;
            if (shipmentId != null) {
                shipment = shipmentRepository.findById(shipmentId).orElse(null);
            }
            ProductionLot productionLot = null;
            if (productionLotId != null) {
                productionLot = productionLotRepository.findById(productionLotId).orElse(null);
            } else if (shipment != null && shipment.getProductionLot() != null) {
                productionLot = shipment.getProductionLot();
            }
            if (shipment == null && productionLot == null) {
                return;
            }
            PartnerLotAccessLog accessLog = PartnerLotAccessLog.builder()
                    .partnerApiKey(partnerApiKey)
                    .shipment(shipment)
                    .productionLot(productionLot)
                    .accessedAt(LocalDateTime.now())
                    .build();

            partnerLotAccessLogRepository.save(accessLog);
            log.debug("Đã ghi nhận nhật ký truy xuất lô cho đối tác '{}' (shipmentId={}, lotId={})",
                    partnerApiKey.getPartnerName(), shipmentId, productionLotId);
        } catch (Exception e) {
            log.error("Không thể ghi nhận nhật ký truy xuất lô cho đối tác '{}': {}",
                    partnerApiKey.getPartnerName(), e.getMessage());
        }
    }
}
