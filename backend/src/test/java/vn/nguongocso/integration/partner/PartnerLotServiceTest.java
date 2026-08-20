package vn.nguongocso.integration.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.partner.dto.response.PartnerLotDossierResponse;
import vn.nguongocso.integration.partner.service.PartnerLotService;
import vn.nguongocso.organization.entity.Organization;

@ExtendWith(MockitoExtension.class)
class PartnerLotServiceTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private FarmLogRepository farmLogRepository;

    @InjectMocks
    private PartnerLotService partnerLotService;

    private UUID orgId1;
    private UUID orgId2;
    private UUID lotId;
    private Organization organization1;
    private Organization organization2;
    private PartnerApiKey partnerApiKey;
    private ProductionLot lotOrg1;

    @BeforeEach
    void setUp() {
        orgId1 = UUID.randomUUID();
        orgId2 = UUID.randomUUID();
        lotId = UUID.randomUUID();

        organization1 = new Organization();
        organization1.setOrganizationId(orgId1);
        organization1.setName("Hợp Tác Xã Nông Nghiệp 1");

        organization2 = new Organization();
        organization2.setOrganizationId(orgId2);
        organization2.setName("Hợp Tác Xã Nông Nghiệp 2");

        partnerApiKey = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .organization(organization1)
                .partnerName("Doanh Nghiệp Thu Mua ABC")
                .rateLimitPerHour(100)
                .build();

        lotOrg1 = ProductionLot.builder()
                .id(lotId)
                .organization(organization1)
                .name("Lô Xoài Cát Vụ Thu Đông 2026")
                .expectedQuantity(5000.0)
                .expectedQuantityUnit("kg")
                .plantingDate(LocalDate.of(2026, 3, 15))
                .status(ProductionLotStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("NCL-12-CN-002-TC-01: Lấy hồ sơ lô thành công khi khóa hợp lệ và lô thuộc tổ chức")
    void testGetLotDossier_Success_TC01() {
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lotOrg1));
        when(productionLotRepository.findDossierByIdAndOrganizationId(lotId, orgId1)).thenReturn(Optional.of(lotOrg1));

        PartnerLotDossierResponse dossier = partnerLotService.getLotDossierForPartner(lotId, partnerApiKey);

        assertNotNull(dossier);
        assertNotNull(dossier.getLotInfo());
        assertEquals("Lô Xoài Cát Vụ Thu Đông 2026", dossier.getLotInfo().getLotName());
        assertEquals(orgId1.toString(), dossier.getOrganizationInfo().getOrganizationId());
    }

    @Test
    @DisplayName("NCL-12-CN-002-TC-04: Khóa thuộc Tổ chức 1 gọi lấy lô thuộc Tổ chức 2 -> Báo lỗi nằm ngoài phạm vi (Cách ly dữ liệu)")
    void testCrossTenantAccess_ThrowsException_TC04() {
        // Lô thuộc Tổ chức 2
        ProductionLot lotOrg2 = ProductionLot.builder()
                .id(lotId)
                .organization(organization2)
                .name("Lô Lúa ST25 Tổ Chức 2")
                .build();

        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lotOrg2));

        // Dùng khóa của Tổ chức 1 gọi lấy lô của Tổ chức 2
        BusinessException exception = assertThrows(BusinessException.class,
                () -> partnerLotService.getLotDossierForPartner(lotId, partnerApiKey));

        assertTrue(exception.getMessage().contains("nằm ngoài phạm vi truy xuất"));
    }

    @Test
    @DisplayName("Lô không tồn tại -> Ném lỗi không tìm thấy lô")
    void testLotNotFound_ThrowsException() {
        UUID nonExistentLotId = UUID.randomUUID();
        when(productionLotRepository.findById(nonExistentLotId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> partnerLotService.getLotDossierForPartner(nonExistentLotId, partnerApiKey));

        assertTrue(exception.getMessage().contains("Không tìm thấy thông tin lô"));
    }
}
