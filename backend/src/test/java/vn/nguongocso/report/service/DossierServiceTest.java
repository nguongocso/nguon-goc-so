package vn.nguongocso.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.report.exception.DossierValidationException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.report.dto.response.DossierCheckResponse;
import vn.nguongocso.report.entity.DossierExportHistory;
import vn.nguongocso.report.repository.DossierExportHistoryRepository;
import vn.nguongocso.report.service.impl.DossierServiceImpl;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
/**
 * Test service DossierService.
 *
 * @author Triệu Văn Đại
 */
@ExtendWith(MockitoExtension.class)
public class DossierServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private FarmLogAttachmentRepository farmLogAttachmentRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private DossierExportHistoryRepository exportHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DossierServiceImpl dossierService;

    private UUID shipmentId;
    private Shipment shipment;
    private ProductionLot productionLot;
    private Organization org;
    private CustomUserDetails userDetails;
    private User testUser;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        org = Organization.builder()
                .organizationId(UUID.randomUUID())
                .name("HTX Long Cốc")
                .build();

        productionLot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô chè giống mới")
                .organization(org)
                .productCategory(ProductCategory.builder()
                        .id(UUID.randomUUID())
                        .name("Chè")
                        .group("TEA")
                        .description("Chè xanh")
                        .isActive(true)
                        .build())
                .status(ProductionLotStatus.CLOSED)
                .plantingDate(LocalDate.now().minusDays(30))
                .harvestDate(LocalDate.now())
                .expectedQuantity(1000.0)
                .actualQuantity(950.0)
                .build();

        shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô hàng xuất khẩu siêu thị");
        shipment.setProductionLot(productionLot);
        shipment.setOrganization(org);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        testUser = User.builder()
                .userId(UUID.randomUUID())
                .fullName("Nguyễn Văn A")
                .userName("mana_clc")
                .build();

        userDetails = mock(CustomUserDetails.class);
    }

    @Test
    void checkEligibility_shouldThrowException_whenShipmentNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dossierService.checkEligibility(shipmentId, userDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy thông tin lô hàng.");
    }

    @Test
    void checkEligibility_shouldThrowAccessDenied_whenUserLacksPermission() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(userDetails.getRoleCode()).thenReturn("VT-03"); // Người ghi sự kiện không có quyền xem/xuất

        assertThatThrownBy(() -> dossierService.checkEligibility(shipmentId, userDetails))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkEligibility_shouldThrowAccessDenied_whenHtxUserChecksOtherHtxShipment() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(userDetails.getRoleCode()).thenReturn("VT-02"); // Quản lý HTX
        when(userDetails.getOrganizationId()).thenReturn(UUID.randomUUID()); // Org khác

        assertThatThrownBy(() -> dossierService.checkEligibility(shipmentId, userDetails))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền truy cập lô hàng này");
    }

    @Test
    void checkEligibility_shouldThrowValidationException_whenLotNotCompletedAndNoAttachments() {
        productionLot.setStatus(ProductionLotStatus.DRAFT);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(userDetails.getRoleCode()).thenReturn("VT-01"); // Admin (bỏ qua check Org)

        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(productionLot.getId()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> dossierService.checkEligibility(shipmentId, userDetails))
                .isInstanceOf(DossierValidationException.class)
                .satisfies(ex -> {
                    DossierValidationException valEx = (DossierValidationException) ex;
                    assertThat(valEx.getErrors()).hasSize(5); // 1 lỗi trạng thái + 4 lỗi thiếu chứng từ
                    assertThat(valEx.getErrors()).contains(
                            "Lô sản xuất tương ứng chưa hoàn tất (Trạng thái yêu cầu: CLOSED hoặc PACKAGED)",
                            "Thiếu chứng từ gieo giống/xuống giống (PLANTING)",
                            "Thiếu chứng từ bón phân (FERTILIZING)",
                            "Thiếu chứng từ phun thuốc/phòng trừ sâu bệnh (PESTICIDE)",
                            "Thiếu chứng từ thu hoạch (HARVESTING)"
                    );
                });
    }

    @Test
    void checkEligibility_shouldReturnSuccess_whenEligible() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(userDetails.getRoleCode()).thenReturn("VT-01");

        List<FarmLog> logs = new ArrayList<>();
        logs.add(createFarmLog(FarmActivityType.PLANTING));
        logs.add(createFarmLog(FarmActivityType.FERTILIZING));
        logs.add(createFarmLog(FarmActivityType.PESTICIDE));
        logs.add(createFarmLog(FarmActivityType.HARVESTING));

        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(productionLot.getId())).thenReturn(logs);

        // Cung cấp chứng từ đính kèm cho mỗi log
        FarmLogAttachment mockAttachment = FarmLogAttachment.builder().fileName("doc.pdf").build();
        when(farmLogAttachmentRepository.findByFarmLogId(any(UUID.class)))
                .thenReturn(Collections.singletonList(mockAttachment));

        DossierCheckResponse response = dossierService.checkEligibility(shipmentId, userDetails);

        assertThat(response.isEligible()).isTrue();
        assertThat(response.getMissingDocuments()).isEmpty();
    }

    @Test
    void exportDossierPdf_shouldReturnBytesAndLogHistory_whenEligible() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        when(userDetails.getRoleCode()).thenReturn("VT-02");
        when(userDetails.getOrganizationId()).thenReturn(org.getOrganizationId());
        when(userDetails.getUserId()).thenReturn(testUser.getUserId());

        List<FarmLog> logs = List.of(
                createFarmLog(FarmActivityType.PLANTING),
                createFarmLog(FarmActivityType.FERTILIZING),
                createFarmLog(FarmActivityType.PESTICIDE),
                createFarmLog(FarmActivityType.HARVESTING)
        );
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(productionLot.getId())).thenReturn(logs);
        FarmLogAttachment mockAttachment = FarmLogAttachment.builder().fileName("doc.pdf").build();
        when(farmLogAttachmentRepository.findByFarmLogId(any(UUID.class)))
                .thenReturn(Collections.singletonList(mockAttachment));

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipmentId)).thenReturn(Collections.emptyList());

        byte[] pdfBytes = dossierService.exportDossierPdf(shipmentId, userDetails, "127.0.0.1");

        assertThat(pdfBytes).isNotEmpty();
        verify(exportHistoryRepository, times(1)).save(any(DossierExportHistory.class));
    }

    private FarmLog createFarmLog(FarmActivityType type) {
        return FarmLog.builder()
                .id(UUID.randomUUID())
                .activityType(type)
                .executedDate(LocalDate.now())
                .build();
    }
}
