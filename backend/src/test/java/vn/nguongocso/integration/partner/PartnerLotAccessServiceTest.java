package vn.nguongocso.integration.partner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.partner.entity.PartnerLotAccessLog;
import vn.nguongocso.integration.partner.repository.PartnerLotAccessLogRepository;
import vn.nguongocso.integration.partner.service.PartnerLotAccessService;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Unit test cho PartnerLotAccessService (NCL-12-CN-006).
 */
@ExtendWith(MockitoExtension.class)
class PartnerLotAccessServiceTest {

    @Mock
    private PartnerLotAccessLogRepository partnerLotAccessLogRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @InjectMocks
    private PartnerLotAccessService partnerLotAccessService;

    private PartnerApiKey partnerApiKey;
    private Shipment shipment;
    private ProductionLot productionLot;
    private UUID shipmentId;
    private UUID productionLotId;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        productionLotId = UUID.randomUUID();

        productionLot = ProductionLot.builder()
                .id(productionLotId)
                .name("LOT-LUA-TEST")
                .build();

        shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("SHIPMENT-001");
        shipment.setProductionLot(productionLot);

        partnerApiKey = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .partnerName("Đối tác Bách Hóa Xanh")
                .isTest(false)
                .build();
    }

    @Test
    @DisplayName("Ghi nhận lượt truy xuất lô hàng thành công cho đối tác dùng khóa thật")
    void testRecordLotAccess_Success() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        partnerLotAccessService.recordLotAccess(partnerApiKey, shipmentId, null);

        verify(partnerLotAccessLogRepository).save(any(PartnerLotAccessLog.class));
    }

    @Test
    @DisplayName("Bỏ qua không ghi nhận nhật ký nếu là khóa thử nghiệm (Sandbox key)")
    void testRecordLotAccess_IgnoreTestKey() {
        partnerApiKey.setIsTest(true);

        partnerLotAccessService.recordLotAccess(partnerApiKey, shipmentId, productionLotId);

        verify(partnerLotAccessLogRepository, never()).save(any(PartnerLotAccessLog.class));
    }

    @Test
    @DisplayName("Bỏ qua khi đối tác gọi không truyền khóa API hợp lệ (null)")
    void testRecordLotAccess_NullApiKey() {
        partnerLotAccessService.recordLotAccess(null, shipmentId, productionLotId);

        verify(partnerLotAccessLogRepository, never()).save(any(PartnerLotAccessLog.class));
    }
}
