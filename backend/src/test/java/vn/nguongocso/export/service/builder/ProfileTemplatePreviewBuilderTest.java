package vn.nguongocso.export.service.builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.export.constant.MandatoryFields;
import vn.nguongocso.export.entity.ProfileTemplate;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị cho ProfileTemplatePreviewBuilder (NCL-12-CN-003).
 * Đảm bảo logic trích xuất snapshot xem trước hồ sơ truy xuất theo cấu hình trường và độc lập với transaction.
 */
@ExtendWith(MockitoExtension.class)
class ProfileTemplatePreviewBuilderTest {

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private FarmLogAttachmentRepository farmLogAttachmentRepository;

    @Mock
    private ProductionLotCertificationRepository productionLotCertificationRepository;

    @Mock
    private InspectionRequestRepository inspectionRequestRepository;

    @Mock
    private InspectionCriterionResultRepository inspectionCriterionResultRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @InjectMocks
    private ProfileTemplatePreviewBuilder previewBuilder;

    private UUID shipmentId;
    private Organization testOrg;
    private FarmArea testFarmArea;
    private ProductionLot testLot;
    private Shipment testShipment;

    @BeforeEach
    void setUp() {
        UUID orgId = UUID.randomUUID();
        shipmentId = UUID.randomUUID();

        testOrg = Organization.builder()
                .organizationId(orgId)
                .name("HTX Nông Nghiệp Xanh")
                .address("Số 123 Đường Nông Nghiệp, Tỉnh Lâm Đồng")
                .build();

        testFarmArea = FarmArea.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .name("Vùng trồng Đơn Dương")
                .area(new BigDecimal("12.5"))
                .areaUnit(AreaUnit.HA)
                .build();

        ProductCategory cat = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Rau củ quả sạch")
                .build();

        testLot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô Cà Chua VietGAP 01")
                .farmArea(testFarmArea)
                .productCategory(cat)
                .organization(testOrg)
                .build();

        testShipment = new Shipment();
        testShipment.setId(shipmentId);
        testShipment.setName("Lô hàng xuất siêu thị số 01");
        testShipment.setTotalQuantity(1500L);
        testShipment.setOrganization(testOrg);
        testShipment.setProductionLot(testLot);
        testShipment.setStatus(ShipmentStatus.ACTIVATED);
    }

    @Test
    @DisplayName("TC-01: buildPreviewSnapshot với 10 trường được cấu hình trả về đúng 10 trường")
    void shouldReturnExactlyTenFieldsWhenTenFieldsSelected() {
        UUID templateId = UUID.randomUUID();
        ProfileTemplate template = ProfileTemplate.builder()
                .id(templateId)
                .organization(testOrg)
                .name("Mẫu xuất khẩu BigC 10 trường")
                .partnerName("BigC")
                .isDefault(false)
                .fields(new ArrayList<>())
                .build();

        Set<String> selectedFieldKeys = Set.of(
                "organization.name",
                "organization.address",
                "farmArea.name",
                "farmArea.area",
                "productionLot.name",
                "productionLot.productCategory",
                "shipment.name",
                "shipment.totalQuantity",
                "farmLog.activityType",
                "chainEvent.eventType"
        );

        FarmLog farmLog = FarmLog.builder()
                .id(UUID.randomUUID())
                .activityType(FarmActivityType.FERTILIZING)
                .executedDate(LocalDate.now().minusDays(5))
                .build();
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(testLot.getId()))
                .thenReturn(List.of(farmLog));

        ChainEvent chainEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PACKAGING)
                .recordedAt(LocalDateTime.now().minusDays(1))
                .build();
        when(chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipmentId))
                .thenReturn(List.of(chainEvent));

        Map<String, Object> preview = previewBuilder.buildPreviewSnapshot(testShipment, template, selectedFieldKeys);

        assertThat(preview).isNotNull();
        assertThat(preview.get("shipmentId")).isEqualTo(shipmentId);

        @SuppressWarnings("unchecked")
        Map<String, Object> orgMap = (Map<String, Object>) preview.get("organization");
        assertThat(orgMap).containsOnlyKeys("name", "address");
        assertThat(orgMap.get("name")).isEqualTo("HTX Nông Nghiệp Xanh");
        assertThat(orgMap.get("address")).isEqualTo("Số 123 Đường Nông Nghiệp, Tỉnh Lâm Đồng");

        @SuppressWarnings("unchecked")
        Map<String, Object> farmAreaMap = (Map<String, Object>) preview.get("farmArea");
        assertThat(farmAreaMap).containsOnlyKeys("name", "area");
        assertThat(farmAreaMap.get("name")).isEqualTo("Vùng trồng Đơn Dương");

        @SuppressWarnings("unchecked")
        Map<String, Object> lotMap = (Map<String, Object>) preview.get("productionLot");
        assertThat(lotMap).containsOnlyKeys("name", "productCategory");

        @SuppressWarnings("unchecked")
        Map<String, Object> shipmentMap = (Map<String, Object>) preview.get("shipment");
        assertThat(shipmentMap).containsOnlyKeys("name", "totalQuantity");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> farmLogs = (List<Map<String, Object>>) preview.get("farmLogs");
        assertThat(farmLogs).isNotEmpty();
        assertThat(farmLogs.get(0)).containsOnlyKeys("activityType");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) preview.get("timelineEvents");
        assertThat(events).isNotEmpty();
        assertThat(events.get(0)).containsOnlyKeys("eventType");

        int totalFieldCount = orgMap.size() + farmAreaMap.size() + lotMap.size()
                + shipmentMap.size() + farmLogs.get(0).size() + events.get(0).size();
        assertThat(totalFieldCount).isEqualTo(10);
    }

    @Test
    @DisplayName("TC-01b: buildPreviewSnapshot khi template == null -> thông tin mẫu hiển thị Mặc định hệ thống")
    void shouldFallBackToSystemDefaultWhenTemplateIsNull() {
        Set<String> selectedFieldKeys = MandatoryFields.QTN11_MANDATORY_FIELD_KEYS;

        Map<String, Object> preview = previewBuilder.buildPreviewSnapshot(testShipment, null, selectedFieldKeys);

        assertThat(preview).isNotNull();
        assertThat(preview.get("shipmentId")).isEqualTo(shipmentId);

        @SuppressWarnings("unchecked")
        Map<String, Object> appliedTemplate = (Map<String, Object>) preview.get("appliedTemplate");
        assertThat(appliedTemplate).isNotNull();
        assertThat(appliedTemplate.get("templateName")).isEqualTo("Mặc định hệ thống");
        assertThat(appliedTemplate.get("isDefault")).isEqualTo(true);
    }

    @Test
    @DisplayName("buildPreviewSnapshot với chứng chỉ và kiểm định chất lượng: dữ liệu được trích xuất chính xác")
    void shouldMaterializeDataCorrectlyWhenCertificationsAndInspectionsConfigured() {
        Set<String> selectedFieldKeys = Set.of(
                "organization.name",
                "certification.name",
                "certification.certifier",
                "inspection.inspectionUnit",
                "inspection.passed"
        );

        vn.nguongocso.certification.entity.Certification cert = vn.nguongocso.certification.entity.Certification.builder()
                .id(UUID.randomUUID())
                .name("VietGAP Trồng trọt")
                .issuedBy("Tổ chức chứng nhận Vinacert")
                .build();
        ProductionLotCertification lotCert = ProductionLotCertification.builder()
                .id(UUID.randomUUID())
                .certification(cert)
                .build();
        when(productionLotCertificationRepository.findByProductionLotIdIn(List.of(testLot.getId())))
                .thenReturn(List.of(lotCert));

        UUID inspectionId = UUID.randomUUID();
        InspectionRequest inspection = InspectionRequest.builder()
                .id(inspectionId)
                .inspectionUnit("Trung tâm kiểm nghiệm Vinacert")
                .sampleSentDate(LocalDate.now().minusDays(10))
                .status(vn.nguongocso.certification.enums.InspectionRequestStatus.PASSED)
                .build();
        when(inspectionRequestRepository.findByProductionLot_IdOrderByCreatedAtDesc(testLot.getId()))
                .thenReturn(List.of(inspection));
        when(inspectionCriterionResultRepository.findByInspectionCriterion_InspectionRequest_Id(inspectionId))
                .thenReturn(List.of());

        Map<String, Object> preview = previewBuilder.buildPreviewSnapshot(testShipment, null, selectedFieldKeys);

        assertThat(preview).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> certs = (List<Map<String, Object>>) preview.get("certifications");
        assertThat(certs).hasSize(1);
        assertThat(certs.get(0)).containsEntry("name", "VietGAP Trồng trọt");
        assertThat(certs.get(0)).containsEntry("certifier", "Tổ chức chứng nhận Vinacert");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inspections = (List<Map<String, Object>>) preview.get("inspections");
        assertThat(inspections).hasSize(1);
        assertThat(inspections.get(0)).containsEntry("inspectionUnit", "Trung tâm kiểm nghiệm Vinacert");
        assertThat(inspections.get(0)).containsEntry("passed", "PASSED");
    }

    @Test
    @DisplayName("buildPreviewSnapshot với nhật ký canh tác và tệp đính kèm: định dạng danh sách thành công")
    void shouldMaterializeFarmLogAndAttachmentsCorrectlyWhenConfigured() {
        Set<String> selectedFieldKeys = Set.of(
                "farmLog.activityType",
                "farmLog.attachments"
        );

        UUID farmLogId = UUID.randomUUID();
        FarmLog farmLog = FarmLog.builder()
                .id(farmLogId)
                .activityType(FarmActivityType.WATERING)
                .executedDate(LocalDate.now().minusDays(2))
                .build();
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(testLot.getId()))
                .thenReturn(List.of(farmLog));

        FarmLogAttachment attachment = FarmLogAttachment.builder()
                .id(UUID.randomUUID())
                .fileName("chung-tu-thu-hoach.pdf")
                .build();
        when(farmLogAttachmentRepository.findByFarmLogId(farmLogId))
                .thenReturn(List.of(attachment));

        Map<String, Object> preview = previewBuilder.buildPreviewSnapshot(testShipment, null, selectedFieldKeys);

        assertThat(preview).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> farmLogs = (List<Map<String, Object>>) preview.get("farmLogs");
        assertThat(farmLogs).hasSize(1);
        assertThat(farmLogs.get(0)).containsEntry("activityType", "Tưới nước");

        @SuppressWarnings("unchecked")
        List<String> attachments = (List<String>) farmLogs.get(0).get("attachments");
        assertThat(attachments).hasSize(1);
        assertThat(attachments.get(0)).isEqualTo("chung-tu-thu-hoach.pdf");
    }
}
