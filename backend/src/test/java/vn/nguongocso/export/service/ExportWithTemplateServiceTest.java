package vn.nguongocso.export.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.export.constant.MandatoryFields;
import vn.nguongocso.export.entity.ExportLog;
import vn.nguongocso.export.entity.ProfileTemplate;
import vn.nguongocso.export.entity.ProfileTemplateField;
import vn.nguongocso.export.enums.ProfileFieldGroup;
import vn.nguongocso.export.exception.TemplateNotOwnedException;
import vn.nguongocso.export.repository.ExportLogRepository;
import vn.nguongocso.export.repository.ProfileTemplateRepository;
import vn.nguongocso.export.service.impl.ExportServiceImpl;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import vn.nguongocso.export.service.recorder.ExportLogRecorder;
import vn.nguongocso.export.service.renderer.ExportCsvRenderer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test cho tính năng exportWithTemplate trong ExportServiceImpl (NCL-07-CN-007). */
@ExtendWith(MockitoExtension.class)
public class ExportWithTemplateServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private ProductionLotCertificationRepository productionLotCertificationRepository;

    @Mock
    private ProfileTemplateRepository profileTemplateRepository;

    @Mock
    private ExportLogRepository exportLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileTemplateService profileTemplateService;

    private ExportCsvRenderer exportCsvRenderer;
    private ExportLogRecorder exportLogRecorder;
    private ExportServiceImpl exportService;

    private UUID orgId;
    private UUID userId;
    private UUID shipmentId;
    private Organization testOrg;
    private CustomUserDetails currentUser;
    private Shipment validShipment;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        shipmentId = UUID.randomUUID();

        exportCsvRenderer = new ExportCsvRenderer();
        exportLogRecorder = new ExportLogRecorder(exportLogRepository, userRepository);
        exportService = new ExportServiceImpl(
                shipmentRepository,
                profileTemplateRepository,
                profileTemplateService,
                null,
                exportCsvRenderer,
                exportLogRecorder
        );

        currentUser = mock(CustomUserDetails.class);
        lenient().when(currentUser.getUserId()).thenReturn(userId);
        lenient().when(currentUser.getOrganizationId()).thenReturn(orgId);
        lenient().when(currentUser.getOrganizationName()).thenReturn("HTX Xanh Lam Đồng");

        testOrg = Organization.builder()
                .organizationId(orgId)
                .name("HTX Xanh Lam Đồng")
                .address("100 Đà Lạt, Lâm Đồng")
                .build();

        FarmArea farmArea = FarmArea.builder()
                .id(UUID.randomUUID())
                .name("Vùng Đơn Dương")
                .area(new BigDecimal("10.0"))
                .areaUnit(AreaUnit.HA)
                .organization(testOrg)
                .build();

        ProductCategory category = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Rau củ sạch")
                .build();

        ProductionLot lot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô Cà Rốt 01")
                .organization(testOrg)
                .farmArea(farmArea)
                .productCategory(category)
                .build();

        validShipment = new Shipment();
        validShipment.setId(shipmentId);
        validShipment.setName("Chuyến hàng số 01");
        validShipment.setTotalQuantity(1000L);
        validShipment.setOrganization(testOrg);
        validShipment.setProductionLot(lot);
        validShipment.setStatus(ShipmentStatus.ACTIVATED);
    }

    private ProfileTemplate buildTestTemplate(UUID templateId, boolean isDefault) {
        ProfileTemplate template = ProfileTemplate.builder()
                .id(templateId)
                .organization(testOrg)
                .name("Mẫu xuất khẩu BigC")
                .partnerName("BigC")
                .isDefault(isDefault)
                .fields(new ArrayList<>())
                .build();

        int order = 1;
        for (String fieldKey : MandatoryFields.QTN11_MANDATORY_FIELD_KEYS) {
            ProfileFieldGroup group = ProfileFieldGroup.ORGANIZATION;
            if (fieldKey.startsWith("farmArea.")) group = ProfileFieldGroup.FARM_AREA;
            else if (fieldKey.startsWith("productionLot.")) group = ProfileFieldGroup.PRODUCTION_LOT;
            else if (fieldKey.startsWith("shipment.")) group = ProfileFieldGroup.SHIPMENT;
            else if (fieldKey.startsWith("farmLog.")) group = ProfileFieldGroup.FARM_LOG;
            else if (fieldKey.startsWith("chainEvent.")) group = ProfileFieldGroup.CHAIN_EVENT;

            template.getFields().add(ProfileTemplateField.builder()
                    .id(UUID.randomUUID())
                    .template(template)
                    .fieldKey(fieldKey)
                    .fieldGroup(group)
                    .isMandatory(true)
                    .sortOrder(order++)
                    .build());
        }
        // Thêm 2 trường phụ -> tổng cộng 10 trường (TC-01)
        template.getFields().add(ProfileTemplateField.builder()
                .id(UUID.randomUUID())
                .template(template)
                .fieldKey("organization.address")
                .fieldGroup(ProfileFieldGroup.ORGANIZATION)
                .isMandatory(false)
                .sortOrder(order++)
                .build());
        template.getFields().add(ProfileTemplateField.builder()
                .id(UUID.randomUUID())
                .template(template)
                .fieldKey("farmArea.area")
                .fieldGroup(ProfileFieldGroup.FARM_AREA)
                .isMandatory(false)
                .sortOrder(order++)
                .build());

        return template;
    }

    private void mockShipmentAndRelatedData() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(validShipment));

        // 3 sự kiện chuỗi bắt buộc để vượt qua QTN-11
        List<ChainEvent> events = List.of(
                ChainEvent.builder()
                        .shipment(validShipment)
                        .eventType(ChainEventType.HARVEST)
                        .recordedAt(LocalDateTime.now().minusDays(3))
                        .build(),
                ChainEvent.builder()
                        .shipment(validShipment)
                        .eventType(ChainEventType.PACKAGING)
                        .recordedAt(LocalDateTime.now().minusDays(2))
                        .build(),
                ChainEvent.builder()
                        .shipment(validShipment)
                        .eventType(ChainEventType.TRANSPORT)
                        .recordedAt(LocalDateTime.now().minusDays(1))
                        .build()
        );
        lenient().when(chainEventRepository.findByShipmentIdInOrderByRecordedAtAsc(any())).thenReturn(events);

        FarmLog farmLog = FarmLog.builder()
                .id(UUID.randomUUID())
                .activityType(FarmActivityType.WATERING)
                .executedDate(LocalDate.now().minusDays(4))
                .build();
        lenient().when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(any()))
                .thenReturn(List.of(farmLog));
    }

    @Test
    @DisplayName("TC-01: exportWithTemplate định dạng JSON trả về đúng 10 trường theo cấu hình mẫu")
    void tc01_exportWithTemplate_json_success() throws Exception {
        UUID templateId = UUID.randomUUID();
        ProfileTemplate template = buildTestTemplate(templateId, false);
        when(profileTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        mockShipmentAndRelatedData();

        Map<String, Object> mockPreview = new LinkedHashMap<>();
        mockPreview.put("organization", Map.of("name", "HTX Xanh Lam Đồng"));
        mockPreview.put("productionLot", Map.of("name", "Lô Cà Rốt 01", "productCategory", "Rau củ sạch"));
        mockPreview.put("shipment", Map.of("name", "Chuyến hàng số 01"));
        mockPreview.put("timelineEvents", List.of(Map.of("eventType", "HARVEST")));
        when(profileTemplateService.buildPreview(eq(shipmentId), eq(templateId), any())).thenReturn(mockPreview);

        Resource result = exportService.exportWithTemplate(shipmentId, templateId, "json", currentUser);

        assertThat(result).isNotNull();
        String jsonContent = new String(result.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(jsonContent).contains("HTX Xanh Lam Đồng");
        assertThat(jsonContent).contains("Lô Cà Rốt 01");
        assertThat(jsonContent).contains("Chuyến hàng số 01");
        assertThat(jsonContent).contains("Rau củ sạch");
        assertThat(jsonContent).contains("HARVEST");
    }

    @Test
    @DisplayName("TC-02: exportWithTemplate định dạng CSV trả về nội dung bảng đầy đủ theo cấu hình mẫu")
    void tc02_exportWithTemplate_csv_success() throws Exception {
        UUID templateId = UUID.randomUUID();
        ProfileTemplate template = buildTestTemplate(templateId, false);
        when(profileTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        mockShipmentAndRelatedData();

        Map<String, Object> mockPreview = new LinkedHashMap<>();
        mockPreview.put("organization", Map.of("name", "HTX Xanh Lam Đồng", "code", "HTX001"));
        mockPreview.put("productionLot", Map.of("name", "Lô Cà Rốt 01", "productCategory", "Rau củ sạch"));
        mockPreview.put("shipment", Map.of("name", "Chuyến hàng số 01"));
        mockPreview.put("farmLogs", List.of(Map.of("activityType", "HARVEST", "material", "Máy gặt")));
        when(profileTemplateService.buildPreview(eq(shipmentId), eq(templateId), any())).thenReturn(mockPreview);

        Resource result = exportService.exportWithTemplate(shipmentId, templateId, "csv", currentUser);

        assertThat(result).isNotNull();
        String csvContent = new String(result.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(csvContent).contains("HTX Xanh Lam Đồng");
        assertThat(csvContent).contains("Lô Cà Rốt 01");
        assertThat(csvContent).contains("Chuyến hàng số 01");
        assertThat(csvContent).contains("HARVEST");
        assertThat(csvContent).contains("HỒ SƠ TRUY XUẤT NGUỒN GỐC SẢN PHẨM");
    }

    @Test
    @DisplayName("TC-03: templateId == null -> dùng mẫu mặc định và ghi ExportLog")
    void tc03_exportWithTemplate_nullTemplate_usesDefaultAndLogs() {
        UUID defaultTemplateId = UUID.randomUUID();
        ProfileTemplate defaultTemplate = buildTestTemplate(defaultTemplateId, true);

        when(profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(orgId))
                .thenReturn(Optional.of(defaultTemplate));

        mockShipmentAndRelatedData();

        Map<String, Object> mockPreview = new LinkedHashMap<>();
        mockPreview.put("organization", Map.of("name", "HTX Xanh Lam Đồng"));
        when(profileTemplateService.buildPreview(eq(shipmentId), isNull(), any())).thenReturn(mockPreview);

        User user = User.builder().userId(userId).fullName("Nguyễn Văn A").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Resource result = exportService.exportWithTemplate(shipmentId, null, "json", currentUser);

        assertThat(result).isNotNull();

        // Kiểm tra ExportLog đã được ghi vào cơ sở dữ liệu (TC-03)
        ArgumentCaptor<ExportLog> logCaptor = ArgumentCaptor.forClass(ExportLog.class);
        verify(exportLogRepository, times(1)).save(logCaptor.capture());

        ExportLog savedLog = logCaptor.getValue();
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getShipment().getId()).isEqualTo(shipmentId);
        assertThat(savedLog.getTemplate().getId()).isEqualTo(defaultTemplateId);
        assertThat(savedLog.getExportedBy().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("TC-04: templateId thuộc tổ chức khác -> ném TemplateNotOwnedException (403)")
    void tc04_exportWithTemplate_otherOrgTemplate_throwsTemplateNotOwnedException() {
        UUID templateId = UUID.randomUUID();
        Organization otherOrg = Organization.builder().organizationId(UUID.randomUUID()).build();
        ProfileTemplate otherOrgTemplate = ProfileTemplate.builder()
                .id(templateId)
                .organization(otherOrg) // Khác orgId của user
                .name("Mẫu của Org khác")
                .build();

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(validShipment));
        when(profileTemplateRepository.findById(templateId)).thenReturn(Optional.of(otherOrgTemplate));

        assertThatThrownBy(() -> exportService.exportWithTemplate(shipmentId, templateId, "json", currentUser))
                .isInstanceOf(TemplateNotOwnedException.class)
                .hasMessageContaining("không thuộc tổ chức");

        verify(exportLogRepository, never()).save(any());
    }
}
