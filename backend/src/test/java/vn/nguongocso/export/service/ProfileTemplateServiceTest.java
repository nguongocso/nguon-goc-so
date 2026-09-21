package vn.nguongocso.export.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
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
import vn.nguongocso.export.service.builder.ProfileTemplatePreviewBuilder;
import vn.nguongocso.export.service.impl.ProfileTemplateServiceImpl;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị cho ProfileTemplateServiceImpl (NCL-07-CN-007).
 * Kiểm tra các tiêu chí chấp nhận: TC-02 (QTN-11), TC-03 (mẫu mặc định),
 * TC-04 (phân quyền tổ chức), CRUD mẫu hồ sơ và điều phối dữ liệu xem trước.
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
    private UserRepository userRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ProfileTemplatePreviewBuilder profileTemplatePreviewBuilder;

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
        lenient().when(userDetailsOrgA.getRoleCode()).thenReturn("VT-02");

        userDetailsOrgB = mock(CustomUserDetails.class);
        lenient().when(userDetailsOrgB.getUserId()).thenReturn(UUID.randomUUID());
        lenient().when(userDetailsOrgB.getOrganizationId()).thenReturn(orgBId);
        lenient().when(userDetailsOrgB.getRoleCode()).thenReturn("VT-02");
    }

    private List<FieldSelectionDto> createValidSelectionsWithExtraFields(int extraCount) {
        List<FieldSelectionDto> selections = new ArrayList<>();
        int order = 1;
        for (String fieldKey : MandatoryFields.QTN11_MANDATORY_FIELD_KEYS) {
            ProfileFieldGroup group = ProfileFieldGroup.ORGANIZATION;
            if (fieldKey.startsWith("farmArea.")) {
                group = ProfileFieldGroup.FARM_AREA;
            } else if (fieldKey.startsWith("productionLot.")) {
                group = ProfileFieldGroup.PRODUCTION_LOT;
            } else if (fieldKey.startsWith("shipment.")) {
                group = ProfileFieldGroup.SHIPMENT;
            } else if (fieldKey.startsWith("farmLog.")) {
                group = ProfileFieldGroup.FARM_LOG;
            } else if (fieldKey.startsWith("chainEvent.")) {
                group = ProfileFieldGroup.CHAIN_EVENT;
            }

            selections.add(FieldSelectionDto.builder()
                    .fieldKey(fieldKey)
                    .fieldGroup(group)
                    .sortOrder(order++)
                    .build());
        }
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

    @Test
    @DisplayName("buildPreview: Điều phối thành công tới ProfileTemplatePreviewBuilder khi dữ liệu hợp lệ")
    void buildPreview_delegatesToPreviewBuilder_whenValidShipment() {
        UUID shipmentId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(orgA);

        ProfileTemplate template = ProfileTemplate.builder()
                .id(templateId)
                .organization(orgA)
                .name("Mẫu Org A")
                .fields(new ArrayList<>())
                .build();

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(profileTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        Map<String, Object> expectedSnapshot = Map.of("shipmentId", shipmentId, "organization", Map.of("name", "HTX A"));
        when(profileTemplatePreviewBuilder.buildPreviewSnapshot(eq(shipment), eq(template), any()))
                .thenReturn(expectedSnapshot);

        Map<String, Object> result = profileTemplateService.buildPreview(shipmentId, templateId, userDetailsOrgA);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedSnapshot);
        verify(profileTemplatePreviewBuilder).buildPreviewSnapshot(eq(shipment), eq(template), any());
    }

    @Test
    @DisplayName("buildPreview: Lô hàng thuộc tổ chức khác -> Ném TemplateNotOwnedException (403)")
    void buildPreview_otherOrgShipment_throwsTemplateNotOwnedException() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(orgB);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> profileTemplateService.buildPreview(shipmentId, null, userDetailsOrgA))
                .isInstanceOf(TemplateNotOwnedException.class)
                .hasMessageContaining("không thuộc tổ chức");
    }

    @Test
    @DisplayName("TC-02 (Cao): Bỏ trường bắt buộc khi tạo mẫu -> Ném MandatoryFieldsViolationException (422)")
    void tc02_createTemplate_missingMandatoryField_throwsMandatoryFieldsViolationException() {
        List<FieldSelectionDto> missingMandatorySelections = new ArrayList<>();
        for (String fieldKey : MandatoryFields.QTN11_MANDATORY_FIELD_KEYS) {
            if ("productionLot.name".equals(fieldKey)) {
                continue;
            }
            missingMandatorySelections.add(FieldSelectionDto.builder()
                    .fieldKey(fieldKey)
                    .fieldGroup(ProfileFieldGroup.PRODUCTION_LOT)
                    .build());
        }

        CreateProfileTemplateRequest request = CreateProfileTemplateRequest.builder()
                .name("Mẫu vi phạm")
                .partnerName("Đối tác X")
                .isDefault(false)
                .selectedFields(missingMandatorySelections)
                .build();

        assertThatThrownBy(() -> profileTemplateService.createTemplate(orgAId, request, userDetailsOrgA))
                .isInstanceOf(MandatoryFieldsViolationException.class)
                .satisfies(ex -> {
                    MandatoryFieldsViolationException mfe = (MandatoryFieldsViolationException) ex;
                    assertThat(mfe.getMissingFields()).anyMatch(s -> s.contains("productionLot.name"));
                    assertThat(mfe.getMessage()).contains("theo quy định.");
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

        List<FieldSelectionDto> invalidFields = List.of(
                FieldSelectionDto.builder()
                        .fieldKey("organization.name")
                        .fieldGroup(ProfileFieldGroup.ORGANIZATION)
                        .build());

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

        verify(profileTemplateRepository, never()).findAllByOrganization_OrganizationIdOrderByNameAsc(orgBId);
    }

    @Test
    @DisplayName("TC-04 (Cao): Truy cập mẫu của tổ chức khác -> Ném TemplateNotOwnedException (403)")
    void tc04_getTemplate_otherOrg_throwsTemplateNotOwnedException() {
        UUID templateBId = UUID.randomUUID();
        ProfileTemplate templateOfOrgB = ProfileTemplate.builder()
                .id(templateBId)
                .organization(orgB)
                .name("Mẫu bí mật của Org B")
                .build();

        when(profileTemplateRepository.findById(templateBId)).thenReturn(Optional.of(templateOfOrgB));

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
        when(profileTemplateRepository.save(any(ProfileTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        FieldGroupDefinition orgGroup = catalog.stream()
                .filter(g -> g.getFieldGroup() == ProfileFieldGroup.ORGANIZATION)
                .findFirst()
                .orElse(null);
        assertThat(orgGroup).isNotNull();

        boolean hasMandatoryOrgName = orgGroup.getFields().stream()
                .anyMatch(f -> "organization.name".equals(f.getFieldKey()) && f.isMandatory());
        assertThat(hasMandatoryOrgName).isTrue();
    }
}
