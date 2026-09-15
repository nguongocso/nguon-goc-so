package vn.nguongocso.export.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.export.constant.MandatoryFields;
import vn.nguongocso.export.dto.request.CreateProfileTemplateRequest;
import vn.nguongocso.export.dto.request.FieldSelectionDto;
import vn.nguongocso.export.dto.request.UpdateProfileTemplateRequest;
import vn.nguongocso.export.dto.response.FieldGroupDefinition;
import vn.nguongocso.export.dto.response.ProfileTemplateResponse;
import vn.nguongocso.export.entity.ProfileTemplate;
import vn.nguongocso.export.entity.ProfileTemplateField;
import vn.nguongocso.export.enums.ProfileFieldGroup;
import vn.nguongocso.export.exception.MandatoryFieldsViolationException;
import vn.nguongocso.export.exception.TemplateNotOwnedException;
import vn.nguongocso.export.repository.ProfileTemplateFieldRepository;
import vn.nguongocso.export.repository.ProfileTemplateRepository;
import vn.nguongocso.export.service.impl.ProfileTemplateServiceImpl;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử đơn vị cho ProfileTemplateService (NCL-07-CN-007).
 * Kiểm tra đầy đủ 4 tiêu chí chấp nhận: TC-01, TC-02, TC-03, TC-04 và các hành vi nghiệp vụ liên quan.
 */
@ExtendWith(MockitoExtension.class)
public class ProfileTemplateServiceTest {

    @Mock
    private ProfileTemplateRepository profileTemplateRepository;

    @Mock
    private ProfileTemplateFieldRepository profileTemplateFieldRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private vn.nguongocso.auth.repository.UserRepository userRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private InspectionRequestRepository inspectionRequestRepository;

    @Mock
    private ProductionLotCertificationRepository lotCertRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @InjectMocks
    private ProfileTemplateServiceImpl profileTemplateService;

    private UUID orgAId;
    private UUID orgBId;
    private UUID userId;
    private Organization orgA;
    private Organization orgB;
    private CustomUserDetails userDetailsOrgA;
    private CustomUserDetails userDetailsOrgB;

    @BeforeEach
    void setUp() {
        orgAId = UUID.randomUUID();
        orgBId = UUID.randomUUID();
        userId = UUID.randomUUID();

        orgA = Organization.builder().organizationId(orgAId).name("HTX A").build();
        orgB = Organization.builder().organizationId(orgBId).name("HTX B").build();

        userDetailsOrgA = mock(CustomUserDetails.class);
        lenient().when(userDetailsOrgA.getUserId()).thenReturn(userId);
        lenient().when(userDetailsOrgA.getOrganizationId()).thenReturn(orgAId);

        userDetailsOrgB = mock(CustomUserDetails.class);
        lenient().when(userDetailsOrgB.getUserId()).thenReturn(UUID.randomUUID());
        lenient().when(userDetailsOrgB.getOrganizationId()).thenReturn(orgBId);
    }

    private List<FieldSelectionDto> createValidSelectionsWithExtraFields(int extraCount) {
        List<FieldSelectionDto> selections = new ArrayList<>();
        int order = 1;
        // 8 trường bắt buộc QTN-11
        for (String fieldKey : MandatoryFields.QTN11_MANDATORY_FIELD_KEYS) {
            ProfileFieldGroup group = ProfileFieldGroup.ORGANIZATION;
            if (fieldKey.startsWith("farmArea.")) group = ProfileFieldGroup.FARM_AREA;
            else if (fieldKey.startsWith("productionLot.")) group = ProfileFieldGroup.PRODUCTION_LOT;
            else if (fieldKey.startsWith("shipment.")) group = ProfileFieldGroup.SHIPMENT;
            else if (fieldKey.startsWith("farmLog.")) group = ProfileFieldGroup.FARM_LOG;
            else if (fieldKey.startsWith("chainEvent.")) group = ProfileFieldGroup.CHAIN_EVENT;

            selections.add(FieldSelectionDto.builder()
                    .fieldKey(fieldKey)
                    .fieldGroup(group)
                    .sortOrder(order++)
                    .build());
        }
        // Thêm 2 trường phụ tùy chọn
        if (extraCount >= 1) {
            selections.add(FieldSelectionDto.builder()
                    .fieldKey("organization.address")
                    .fieldGroup(ProfileFieldGroup.ORGANIZATION)
                    .sortOrder(order++)
                    .build());
        }
        if (extraCount >= 2) {
            selections.add(FieldSelectionDto.builder()
                    .fieldKey("farmArea.area")
                    .fieldGroup(ProfileFieldGroup.FARM_AREA)
                    .sortOrder(order++)
                    .build());
        }
        return selections;
    }

    // =========================================================================
    // TC-01: Mẫu 10 trường -> preview / xuất -> trả về đúng 10 trường đã cấu hình
    // =========================================================================
    @Test
    @DisplayName("TC-01 (Cao): Mẫu cấu hình 10 trường -> buildPreview trả về đúng 10 trường đã chọn")
    void tc01_buildPreview_withTenSelectedFields_returnsExactlyTenFields() {
        UUID shipmentId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        // Chuẩn bị template có đúng 10 trường (8 bắt buộc + 2 tùy chọn)
        ProfileTemplate template = ProfileTemplate.builder()
                .id(templateId)
                .organization(orgA)
                .name("Mẫu xuất khẩu BigC 10 trường")
                .partnerName("BigC")
                .isDefault(false)
                .fields(new ArrayList<>())
                .build();

        List<FieldSelectionDto> selections = createValidSelectionsWithExtraFields(2); // 8 + 2 = 10 trường
        for (FieldSelectionDto s : selections) {
            template.getFields().add(ProfileTemplateField.builder()
                    .id(UUID.randomUUID())
                    .template(template)
                    .fieldKey(s.getFieldKey())
                    .fieldGroup(s.getFieldGroup())
                    .isMandatory(MandatoryFields.isMandatory(s.getFieldKey()))
                    .sortOrder(s.getSortOrder())
                    .build());
        }

        when(profileTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // Mock dữ liệu lô hàng thực tế
        Organization org = Organization.builder()
                .organizationId(orgAId)
                .name("HTX Nông Nghiệp Xanh")
                .address("Số 123 Đường Nông Nghiệp, Tỉnh Lâm Đồng")
                .build();

        FarmArea farmArea = FarmArea.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .name("Vùng trồng Đơn Dương")
                .area(new BigDecimal("12.5"))
                .areaUnit(AreaUnit.HA)
                .build();

        ProductCategory cat = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Rau củ quả sạch")
                .build();

        ProductionLot lot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô Cà Chua VietGAP 01")
                .farmArea(farmArea)
                .productCategory(cat)
                .organization(org)
                .build();

        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô hàng xuất siêu thị số 01");
        shipment.setTotalQuantity(1500L);
        shipment.setOrganization(org);
        shipment.setProductionLot(lot);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        FarmLog farmLog = FarmLog.builder()
                .id(UUID.randomUUID())
                .activityType(FarmActivityType.FERTILIZING)
                .executedDate(LocalDate.now().minusDays(5))
                .build();
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lot.getId()))
                .thenReturn(List.of(farmLog));

        ChainEvent chainEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PACKAGING)
                .recordedAt(LocalDateTime.now().minusDays(1))
                .build();
        when(chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipmentId))
                .thenReturn(List.of(chainEvent));

        // Thực hiện build preview
        Map<String, Object> preview = profileTemplateService.buildPreview(shipmentId, templateId, userDetailsOrgA);

        assertThat(preview).isNotNull();

        // Kiểm tra đối tượng organization có đúng 2 trường: name và address
        @SuppressWarnings("unchecked")
        Map<String, Object> orgMap = (Map<String, Object>) preview.get("organization");
        assertThat(orgMap).isNotNull();
        assertThat(orgMap).containsOnlyKeys("name", "address");
        assertThat(orgMap.get("name")).isEqualTo("HTX Nông Nghiệp Xanh");
        assertThat(orgMap.get("address")).isEqualTo("Số 123 Đường Nông Nghiệp, Tỉnh Lâm Đồng");

        // Kiểm tra đối tượng farmArea có đúng 2 trường: name và area
        @SuppressWarnings("unchecked")
        Map<String, Object> farmAreaMap = (Map<String, Object>) preview.get("farmArea");
        assertThat(farmAreaMap).isNotNull();
        assertThat(farmAreaMap).containsOnlyKeys("name", "area");
        assertThat(farmAreaMap.get("name")).isEqualTo("Vùng trồng Đơn Dương");

        // Kiểm tra đối tượng productionLot có 2 trường: name và productCategory
        @SuppressWarnings("unchecked")
        Map<String, Object> lotMap = (Map<String, Object>) preview.get("productionLot");
        assertThat(lotMap).isNotNull();
        assertThat(lotMap).containsOnlyKeys("name", "productCategory");

        // Kiểm tra đối tượng shipment có 2 trường: name và totalQuantity
        @SuppressWarnings("unchecked")
        Map<String, Object> shipmentMap = (Map<String, Object>) preview.get("shipment");
        assertThat(shipmentMap).isNotNull();
        assertThat(shipmentMap).containsOnlyKeys("name", "totalQuantity");

        // Kiểm tra danh sách farmLogs chỉ chứa activityType (1 trường)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> farmLogs = (List<Map<String, Object>>) preview.get("farmLogs");
        assertThat(farmLogs).isNotEmpty();
        assertThat(farmLogs.get(0)).containsOnlyKeys("activityType");

        // Kiểm tra danh sách timelineEvents chỉ chứa eventType (1 trường)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) preview.get("timelineEvents");
        assertThat(events).isNotEmpty();
        assertThat(events.get(0)).containsOnlyKeys("eventType");

        // Tổng cộng các trường đã chọn và xuất hiện:
        // organization (2) + farmArea (2) + productionLot (2) + shipment (2) + farmLog (1) + chainEvent (1) = đúng 10 trường!
        int totalFieldCount = orgMap.size() + farmAreaMap.size() + lotMap.size()
                + shipmentMap.size() + farmLogs.get(0).size() + events.get(0).size();
        assertThat(totalFieldCount).isEqualTo(10);
    }

    @Test
    @DisplayName("TC-01b: buildPreview với templateId == null và tổ chức chưa có mẫu mặc định -> Dùng cấu hình mặc định hệ thống thành công")
    void tc01b_buildPreview_nullTemplate_noOrgDefault_fallsBackToSystemDefault() {
        UUID shipmentId = UUID.randomUUID();

        // Chuẩn bị Mock Shipment
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Chuyến hàng số 01");
        shipment.setTotalQuantity(1000L);
        shipment.setOrganization(orgA);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(orgAId))
                .thenReturn(Optional.empty()); // Chưa cấu hình mẫu mặc định

        Map<String, Object> preview = profileTemplateService.buildPreview(shipmentId, null, userDetailsOrgA);

        assertThat(preview).isNotNull();
        assertThat(preview.get("shipmentId")).isEqualTo(shipmentId);

        @SuppressWarnings("unchecked")
        Map<String, Object> appliedTemplate = (Map<String, Object>) preview.get("appliedTemplate");
        assertThat(appliedTemplate).isNotNull();
        assertThat(appliedTemplate.get("templateName")).isEqualTo("Mặc định hệ thống");
        assertThat(appliedTemplate.get("isDefault")).isEqualTo(true);
    }

    // =========================================================================
    // TC-02: Bỏ trường bắt buộc QTN-11 -> Ném lỗi 422 MandatoryFieldsViolationException
    // =========================================================================
    @Test
    @DisplayName("TC-02 (Cao): Bỏ trường bắt buộc QTN-11 khi tạo mẫu -> Ném MandatoryFieldsViolationException (422)")
    void tc02_createTemplate_missingMandatoryField_throwsMandatoryFieldsViolationException() {
        List<FieldSelectionDto> missingMandatorySelections = new ArrayList<>();
        for (String fieldKey : MandatoryFields.QTN11_MANDATORY_FIELD_KEYS) {
            if ("productionLot.name".equals(fieldKey)) {
                continue; // Cố tình bỏ qua trường bắt buộc
            }
            missingMandatorySelections.add(FieldSelectionDto.builder()
                    .fieldKey(fieldKey)
                    .fieldGroup(ProfileFieldGroup.PRODUCTION_LOT)
                    .build());
        }

        CreateProfileTemplateRequest request = CreateProfileTemplateRequest.builder()
                .name("Mẫu vi phạm QTN-11")
                .partnerName("Đối tác X")
                .isDefault(false)
                .selectedFields(missingMandatorySelections)
                .build();

        assertThatThrownBy(() -> profileTemplateService.createTemplate(orgAId, request, userDetailsOrgA))
                .isInstanceOf(MandatoryFieldsViolationException.class)
                .satisfies(ex -> {
                    MandatoryFieldsViolationException mfe = (MandatoryFieldsViolationException) ex;
                    assertThat(mfe.getMissingFields()).anyMatch(s -> s.contains("productionLot.name"));
                    assertThat(mfe.getMessage()).contains("QTN-11");
                });

        verify(profileTemplateRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-02 (Cao): Bỏ trường bắt buộc khi cập nhật mẫu -> Ném MandatoryFieldsViolationException (422)")
    void tc02_updateTemplate_missingMandatoryField_throwsMandatoryFieldsViolationException() {
        UUID templateId = UUID.randomUUID();
        ProfileTemplate existing = ProfileTemplate.builder()
                .id(templateId)
                .organization(orgA)
                .name("Mẫu cũ")
                .fields(new ArrayList<>())
                .build();

        when(profileTemplateRepository.findById(templateId)).thenReturn(Optional.of(existing));

        // Cố tình cập nhật chỉ có 1 trường, thiếu toàn bộ 7 trường bắt buộc khác
        List<FieldSelectionDto> invalidFields = List.of(
                FieldSelectionDto.builder()
                        .fieldKey("organization.name")
                        .fieldGroup(ProfileFieldGroup.ORGANIZATION)
                        .build()
        );

        UpdateProfileTemplateRequest request = UpdateProfileTemplateRequest.builder()
                .name("Mẫu cập nhật thiếu trường")
                .selectedFields(invalidFields)
                .build();

        assertThatThrownBy(() -> profileTemplateService.updateTemplate(orgAId, templateId, request, userDetailsOrgA))
                .isInstanceOf(MandatoryFieldsViolationException.class)
                .satisfies(ex -> {
                    MandatoryFieldsViolationException mfe = (MandatoryFieldsViolationException) ex;
                    assertThat(mfe.getMissingFields()).hasSize(7);
                });
    }

    // =========================================================================
    // TC-03: Không chọn mẫu -> Dùng mẫu mặc định của tổ chức
    // =========================================================================
    @Test
    @DisplayName("TC-03 (Trung bình): Không chọn mẫu (templateId == null) -> Lấy mẫu mặc định của tổ chức")
    void tc03_getDefaultTemplate_returnsDefaultTemplateOfOrganization() {
        UUID defaultTemplateId = UUID.randomUUID();
        ProfileTemplate defaultTemplate = ProfileTemplate.builder()
                .id(defaultTemplateId)
                .organization(orgA)
                .name("Mẫu hồ sơ chuẩn cơ bản")
                .partnerName("Mặc định")
                .isDefault(true)
                .fields(new ArrayList<>())
                .build();

        when(profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(orgAId))
                .thenReturn(Optional.of(defaultTemplate));

        ProfileTemplate result = profileTemplateService.getDefaultTemplate(orgAId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(defaultTemplateId);
        assertThat(result.getIsDefault()).isTrue();
        assertThat(result.getName()).isEqualTo("Mẫu hồ sơ chuẩn cơ bản");
    }

    @Test
    @DisplayName("TC-03 (Trung bình): Tổ chức chưa có mẫu mặc định -> Trả về ResourceNotFoundException")
    void tc03_getDefaultTemplate_notFound_throwsException() {
        when(profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(orgAId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileTemplateService.getDefaultTemplate(orgAId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy mẫu hồ sơ mặc định");
    }

    // =========================================================================
    // TC-04: Mẫu tổ chức khác -> Không trả về trong danh sách, chặn truy cập (403)
    // =========================================================================
    @Test
    @DisplayName("TC-04 (Cao): listTemplates chỉ trả về các mẫu thuộc tổ chức được yêu cầu, không lộ mẫu của tổ chức khác")
    void tc04_listTemplates_isolatedByOrganization() {
        ProfileTemplate t1 = ProfileTemplate.builder()
                .id(UUID.randomUUID())
                .organization(orgA)
                .name("Mẫu của Org A số 1")
                .build();
        ProfileTemplate t2 = ProfileTemplate.builder()
                .id(UUID.randomUUID())
                .organization(orgA)
                .name("Mẫu của Org A số 2")
                .build();

        when(profileTemplateRepository.findAllByOrganization_OrganizationIdOrderByNameAsc(orgAId))
                .thenReturn(List.of(t1, t2));

        List<ProfileTemplateResponse> result = profileTemplateService.listTemplates(orgAId, userDetailsOrgA);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProfileTemplateResponse::getName)
                .containsExactly("Mẫu của Org A số 1", "Mẫu của Org A số 2");

        // Đảm bảo không bao giờ query sang tổ chức khác
        verify(profileTemplateRepository, never()).findAllByOrganization_OrganizationIdOrderByNameAsc(orgBId);
    }

    @Test
    @DisplayName("TC-04 (Cao): Truy cập mẫu của tổ chức khác -> Ném TemplateNotOwnedException (403)")
    void tc04_getTemplate_otherOrg_throwsTemplateNotOwnedException() {
        UUID templateBId = UUID.randomUUID();
        ProfileTemplate templateOfOrgB = ProfileTemplate.builder()
                .id(templateBId)
                .organization(orgB) // Thuộc Org B
                .name("Mẫu bí mật của Org B")
                .build();

        when(profileTemplateRepository.findById(templateBId)).thenReturn(Optional.of(templateOfOrgB));

        // User của Org A cố tình truy vấn mẫu của Org B
        assertThatThrownBy(() -> profileTemplateService.getTemplate(orgAId, templateBId, userDetailsOrgA))
                .isInstanceOf(TemplateNotOwnedException.class)
                .hasMessageContaining("không thuộc tổ chức");
    }

    @Test
    @DisplayName("TC-04 (Cao): Cập nhật mẫu của tổ chức khác -> Ném TemplateNotOwnedException (403)")
    void tc04_updateTemplate_otherOrg_throwsTemplateNotOwnedException() {
        UUID templateBId = UUID.randomUUID();
        ProfileTemplate templateOfOrgB = ProfileTemplate.builder()
                .id(templateBId)
                .organization(orgB)
                .name("Mẫu Org B")
                .build();

        when(profileTemplateRepository.findById(templateBId)).thenReturn(Optional.of(templateOfOrgB));

        UpdateProfileTemplateRequest request = UpdateProfileTemplateRequest.builder()
                .name("Hacker cập nhật")
                .selectedFields(createValidSelectionsWithExtraFields(0))
                .build();

        assertThatThrownBy(() -> profileTemplateService.updateTemplate(orgAId, templateBId, request, userDetailsOrgA))
                .isInstanceOf(TemplateNotOwnedException.class);
    }

    @Test
    @DisplayName("TC-04 (Cao): Xóa mẫu của tổ chức khác -> Ném TemplateNotOwnedException (403)")
    void tc04_deleteTemplate_otherOrg_throwsTemplateNotOwnedException() {
        UUID templateBId = UUID.randomUUID();
        ProfileTemplate templateOfOrgB = ProfileTemplate.builder()
                .id(templateBId)
                .organization(orgB)
                .name("Mẫu Org B")
                .build();

        when(profileTemplateRepository.findById(templateBId)).thenReturn(Optional.of(templateOfOrgB));

        assertThatThrownBy(() -> profileTemplateService.deleteTemplate(orgAId, templateBId, userDetailsOrgA))
                .isInstanceOf(TemplateNotOwnedException.class);

        verify(profileTemplateRepository, never()).delete(any());
    }

    // =========================================================================
    // Các ca kiểm thử CRUD và tính năng bổ trợ
    // =========================================================================
    @Test
    @DisplayName("CRUD thành công: Tạo mẫu mới hợp lệ với 8 trường bắt buộc")
    void createTemplate_validRequest_savesAndReturnsResponse() {
        List<FieldSelectionDto> selections = createValidSelectionsWithExtraFields(1);
        CreateProfileTemplateRequest request = CreateProfileTemplateRequest.builder()
                .name("Mẫu VietGAP Tiêu Chuẩn")
                .partnerName("Co.op Mart")
                .isDefault(true)
                .selectedFields(selections)
                .build();

        when(organizationRepository.findById(orgAId)).thenReturn(Optional.of(orgA));
        when(profileTemplateRepository.save(any(ProfileTemplate.class))).thenAnswer(invocation -> {
            ProfileTemplate pt = invocation.getArgument(0);
            pt.setId(UUID.randomUUID());
            return pt;
        });

        ProfileTemplateResponse response = profileTemplateService.createTemplate(orgAId, request, userDetailsOrgA);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Mẫu VietGAP Tiêu Chuẩn");
        assertThat(response.getPartnerName()).isEqualTo("Co.op Mart");
        assertThat(response.isDefault()).isTrue();
        assertThat(response.getFields()).hasSize(9);

        // Kiểm tra đã lưu mẫu
        verify(profileTemplateRepository).save(any(ProfileTemplate.class));
    }

    @Test
    @DisplayName("CRUD thành công: Xóa mẫu hợp lệ")
    void deleteTemplate_validTemplate_deletesSuccessfully() {
        UUID templateId = UUID.randomUUID();
        ProfileTemplate template = ProfileTemplate.builder()
                .id(templateId)
                .organization(orgA)
                .name("Mẫu cần xóa")
                .build();

        when(profileTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        profileTemplateService.deleteTemplate(orgAId, templateId, userDetailsOrgA);

        verify(profileTemplateRepository).delete(template);
    }

    @Test
    @DisplayName("CRUD thành công: Cập nhật mẫu hồ sơ và danh mục trường in-place")
    void updateTemplate_validTemplate_updatesFieldsSuccessfully() {
        UUID templateId = UUID.randomUUID();
        ProfileTemplate template = ProfileTemplate.builder()
                .id(templateId)
                .organization(orgA)
                .name("Mẫu ban đầu")
                .partnerName("Đối tác cũ")
                .isDefault(false)
                .fields(new ArrayList<>())
                .build();

        // Giả lập template ban đầu có 8 trường bắt buộc
        List<FieldSelectionDto> initialSelections = createValidSelectionsWithExtraFields(0);
        for (FieldSelectionDto s : initialSelections) {
            template.getFields().add(ProfileTemplateField.builder()
                    .id(UUID.randomUUID())
                    .template(template)
                    .fieldKey(s.getFieldKey())
                    .fieldGroup(s.getFieldGroup())
                    .isMandatory(true)
                    .sortOrder(s.getSortOrder())
                    .build());
        }

        when(profileTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(profileTemplateRepository.save(any(ProfileTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Request cập nhật với 10 trường (8 trường cũ + 2 trường mới)
        List<FieldSelectionDto> updatedSelections = createValidSelectionsWithExtraFields(2);
        UpdateProfileTemplateRequest updateReq = UpdateProfileTemplateRequest.builder()
                .name("Mẫu đã cập nhật")
                .partnerName("Đối tác mới")
                .isDefault(true)
                .selectedFields(updatedSelections)
                .build();

        ProfileTemplateResponse resp = profileTemplateService.updateTemplate(orgAId, templateId, updateReq, userDetailsOrgA);

        assertThat(resp).isNotNull();
        assertThat(resp.getName()).isEqualTo("Mẫu đã cập nhật");
        assertThat(resp.getPartnerName()).isEqualTo("Đối tác mới");
        assertThat(resp.isDefault()).isTrue();
        assertThat(resp.getFields()).hasSize(10);
        assertThat(template.getFields()).hasSize(10);

        verify(profileTemplateRepository).save(template);
    }

    @Test
    @DisplayName("Danh mục trường: getAllAvailableFields trả về 8 nhóm trường")
    void getAllAvailableFields_returnsEightGroupsWithMandatoryMarked() {
        List<FieldGroupDefinition> catalog = profileTemplateService.getAllAvailableFields();

        assertThat(catalog).isNotNull();
        assertThat(catalog).hasSize(8);

        // Kiểm tra có nhóm ORGANIZATION
        FieldGroupDefinition orgGroup = catalog.stream()
                .filter(g -> g.getFieldGroup() == ProfileFieldGroup.ORGANIZATION)
                .findFirst()
                .orElse(null);
        assertThat(orgGroup).isNotNull();

        // Kiểm tra trường organization.name có isMandatory = true
        boolean hasMandatoryOrgName = orgGroup.getFields().stream()
                .anyMatch(f -> "organization.name".equals(f.getFieldKey()) && f.isMandatory());
        assertThat(hasMandatoryOrgName).isTrue();
    }
}
