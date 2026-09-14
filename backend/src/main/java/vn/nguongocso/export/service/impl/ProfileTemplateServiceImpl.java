package vn.nguongocso.export.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.export.constant.MandatoryFields;
import vn.nguongocso.export.dto.request.CreateProfileTemplateRequest;
import vn.nguongocso.export.dto.request.FieldSelectionDto;
import vn.nguongocso.export.dto.request.UpdateProfileTemplateRequest;
import vn.nguongocso.export.dto.response.FieldGroupDefinition;
import vn.nguongocso.export.dto.response.ProfileTemplateFieldResponse;
import vn.nguongocso.export.dto.response.ProfileTemplateResponse;
import vn.nguongocso.export.entity.ProfileTemplate;
import vn.nguongocso.export.entity.ProfileTemplateField;
import vn.nguongocso.export.exception.MandatoryFieldsViolationException;
import vn.nguongocso.export.exception.TemplateNotOwnedException;
import vn.nguongocso.export.repository.ProfileTemplateFieldRepository;
import vn.nguongocso.export.repository.ProfileTemplateRepository;
import vn.nguongocso.export.service.ProfileTemplateService;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ cấu hình mẫu hồ sơ truy xuất theo đối tác.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileTemplateServiceImpl implements ProfileTemplateService {

    private final ProfileTemplateRepository profileTemplateRepository;
    private final ProfileTemplateFieldRepository profileTemplateFieldRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final FarmLogRepository farmLogRepository;
    private final ChainEventRepository chainEventRepository;
    private final InspectionRequestRepository inspectionRequestRepository;
    private final ProductionLotCertificationRepository productionLotCertificationRepository;

    /**
     * Tạo mới mẫu hồ sơ truy xuất (TC-01, TC-02).
     */
    @Override
    @Transactional
    public ProfileTemplateResponse createTemplate(UUID orgId, CreateProfileTemplateRequest request, CustomUserDetails currentUser) {
        validateOrganizationOwnership(orgId, currentUser);

        // Kiểm tra trùng tên mẫu trong cùng tổ chức
        if (profileTemplateRepository.existsByNameAndOrganization_OrganizationId(request.getName(), orgId)) {
            throw new BusinessException("Tên mẫu hồ sơ đã tồn tại trong tổ chức của bạn.");
        }

        // Kiểm tra các trường bắt buộc QTN-11 (TC-02)
        Set<String> selectedKeys = request.getSelectedFields().stream()
                .map(FieldSelectionDto::getFieldKey)
                .collect(Collectors.toSet());

        List<String> missingMandatory = MandatoryFields.findMissingMandatoryFields(selectedKeys);
        if (!missingMandatory.isEmpty()) {
            throw new MandatoryFieldsViolationException(missingMandatory);
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tổ chức."));

        User creator = currentUser.getUserId() != null
                ? userRepository.findById(currentUser.getUserId()).orElse(null)
                : null;

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());

        // Nếu mẫu mới được đặt làm mặc định, hủy cờ mặc định của các mẫu hiện có trong tổ chức
        if (isDefault) {
            profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(orgId)
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        profileTemplateRepository.save(existingDefault);
                    });
        }

        ProfileTemplate template = ProfileTemplate.builder()
                .organization(org)
                .name(request.getName().trim())
                .partnerName(request.getPartnerName() != null ? request.getPartnerName().trim() : null)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .isDefault(isDefault)
                .createdBy(creator)
                .build();

        ProfileTemplate savedTemplate = profileTemplateRepository.save(template);

        // Lưu danh sách trường chọn
        List<ProfileTemplateField> fields = buildTemplateFields(savedTemplate, request.getSelectedFields());
        profileTemplateFieldRepository.saveAll(fields);
        savedTemplate.setFields(fields);

        log.info("Đã tạo mẫu hồ sơ '{}' (ID: {}) cho tổ chức ID: {}", savedTemplate.getName(), savedTemplate.getId(), orgId);
        return mapToResponse(savedTemplate);
    }

    /**
     * Lấy danh sách các mẫu hồ sơ thuộc tổ chức của người dùng (TC-04).
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProfileTemplateResponse> listTemplates(UUID orgId, CustomUserDetails currentUser) {
        validateOrganizationOwnership(orgId, currentUser);

        List<ProfileTemplate> templates = profileTemplateRepository.findAllByOrganization_OrganizationIdOrderByNameAsc(orgId);
        return templates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết mẫu hồ sơ theo ID (TC-04).
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileTemplateResponse getTemplate(UUID orgId, UUID templateId, CustomUserDetails currentUser) {
        validateOrganizationOwnership(orgId, currentUser);

        ProfileTemplate template = profileTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ."));

        if (template.getOrganization() == null || !template.getOrganization().getOrganizationId().equals(orgId)) {
            throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của bạn.");
        }

        return mapToResponse(template);
    }

    /**
     * Cập nhật mẫu hồ sơ (TC-02).
     */
    @Override
    @Transactional
    public ProfileTemplateResponse updateTemplate(UUID orgId, UUID templateId, UpdateProfileTemplateRequest request, CustomUserDetails currentUser) {
        validateOrganizationOwnership(orgId, currentUser);

        ProfileTemplate template = profileTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ."));

        if (template.getOrganization() == null || !template.getOrganization().getOrganizationId().equals(orgId)) {
            throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của bạn.");
        }

        // Kiểm tra trùng tên với mẫu khác trong cùng tổ chức
        if (profileTemplateRepository.existsByNameAndOrganization_OrganizationIdAndIdNot(request.getName(), orgId, templateId)) {
            throw new BusinessException("Tên mẫu hồ sơ đã tồn tại trong tổ chức của bạn.");
        }

        // Kiểm tra các trường bắt buộc QTN-11 (TC-02)
        Set<String> selectedKeys = request.getSelectedFields().stream()
                .map(FieldSelectionDto::getFieldKey)
                .collect(Collectors.toSet());

        List<String> missingMandatory = MandatoryFields.findMissingMandatoryFields(selectedKeys);
        if (!missingMandatory.isEmpty()) {
            throw new MandatoryFieldsViolationException(missingMandatory);
        }

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());

        // Nếu cập nhật thành mẫu mặc định, hủy cờ mặc định của các mẫu khác
        if (isDefault && !Boolean.TRUE.equals(template.getIsDefault())) {
            List<ProfileTemplate> otherDefaults = profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrueAndIdNot(orgId, templateId);
            for (ProfileTemplate ot : otherDefaults) {
                ot.setIsDefault(false);
                profileTemplateRepository.save(ot);
            }
        }

        template.setName(request.getName().trim());
        template.setPartnerName(request.getPartnerName() != null ? request.getPartnerName().trim() : null);
        template.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        template.setIsDefault(isDefault);

        // Xóa trường cũ và lưu trường mới
        profileTemplateFieldRepository.deleteByTemplate_Id(templateId);

        List<ProfileTemplateField> fields = buildTemplateFields(template, request.getSelectedFields());
        profileTemplateFieldRepository.saveAll(fields);
        template.setFields(fields);

        ProfileTemplate updated = profileTemplateRepository.save(template);
        log.info("Đã cập nhật mẫu hồ sơ ID: {} cho tổ chức ID: {}", templateId, orgId);
        return mapToResponse(updated);
    }

    /**
     * Xóa mẫu hồ sơ.
     */
    @Override
    @Transactional
    public void deleteTemplate(UUID orgId, UUID templateId, CustomUserDetails currentUser) {
        validateOrganizationOwnership(orgId, currentUser);

        ProfileTemplate template = profileTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ cần xóa."));

        if (template.getOrganization() == null || !template.getOrganization().getOrganizationId().equals(orgId)) {
            throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của bạn.");
        }

        profileTemplateRepository.delete(template);
        log.info("Đã xóa mẫu hồ sơ ID: {} khỏi tổ chức ID: {}", templateId, orgId);
    }

    /**
     * Lấy mẫu hồ sơ mặc định của tổ chức (TC-03).
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileTemplate getDefaultTemplate(UUID orgId) {
        return profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mẫu hồ sơ mặc định của tổ chức."));
    }

    /**
     * Lấy DTO thông tin mẫu hồ sơ mặc định của tổ chức (TC-03).
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileTemplateResponse getDefaultTemplateResponse(UUID orgId, CustomUserDetails currentUser) {
        validateOrganizationOwnership(orgId, currentUser);
        ProfileTemplate template = getDefaultTemplate(orgId);
        return mapToResponse(template);
    }

    /**
     * Lấy danh mục tất cả các trường dữ liệu hệ thống hỗ trợ cấu hình.
     */
    @Override
    public List<FieldGroupDefinition> getAllAvailableFields() {
        return MandatoryFields.buildFullCatalog();
    }

    /**
     * Xem trước nội dung hồ sơ truy xuất của lô hàng theo mẫu cấu hình (TC-01, TC-03).
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> buildPreview(UUID shipmentId, UUID templateId, CustomUserDetails currentUser) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin lô hàng."));

        validateShipmentAccess(shipment, currentUser);

        UUID userOrgId = currentUser.getOrganizationId();

        // 1. Xác định template áp dụng
        ProfileTemplate template = null;
        if (templateId != null) {
            template = profileTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ."));
            if (!template.getOrganization().getOrganizationId().equals(userOrgId)) {
                throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của bạn.");
            }
        } else {
            // Không truyền templateId -> tìm mẫu mặc định của tổ chức (TC-03)
            template = getDefaultTemplate(userOrgId);
        }

        // 2. Thu thập tập trường được chọn
        Set<String> selectedFieldKeys;
        if (template != null && template.getFields() != null && !template.getFields().isEmpty()) {
            selectedFieldKeys = template.getFields().stream()
                    .map(ProfileTemplateField::getFieldKey)
                    .collect(Collectors.toSet());
        } else {
            // Nếu không có template hoặc template rỗng -> lấy tất cả các trường
            selectedFieldKeys = new HashSet<>(MandatoryFields.FIELD_DISPLAY_NAMES.keySet());
        }

        // 3. Xây dựng dữ liệu xem trước đã lọc theo đúng các trường được chọn (TC-01)
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("shipmentId", shipment.getId());

        Map<String, Object> appliedTemplateInfo = new LinkedHashMap<>();
        if (template != null) {
            appliedTemplateInfo.put("templateId", template.getId());
            appliedTemplateInfo.put("templateName", template.getName());
            appliedTemplateInfo.put("isDefault", template.getIsDefault());
            appliedTemplateInfo.put("totalFields", selectedFieldKeys.size());
        } else {
            appliedTemplateInfo.put("templateId", null);
            appliedTemplateInfo.put("templateName", "Mặc định hệ thống");
            appliedTemplateInfo.put("isDefault", true);
            appliedTemplateInfo.put("totalFields", selectedFieldKeys.size());
        }
        preview.put("appliedTemplate", appliedTemplateInfo);

        // Organization fields
        Organization org = shipment.getOrganization();
        Map<String, Object> orgData = new LinkedHashMap<>();
        addFieldIfSelected(orgData, "name", "organization.name", selectedFieldKeys, org != null ? org.getName() : null);
        addFieldIfSelected(orgData, "code", "organization.code", selectedFieldKeys, org != null ? org.getCode() : null);
        addFieldIfSelected(orgData, "address", "organization.address", selectedFieldKeys, org != null ? org.getAddress() : null);
        addFieldIfSelected(orgData, "phone", "organization.phone", selectedFieldKeys, org != null ? org.getPhone() : null);
        addFieldIfSelected(orgData, "email", "organization.email", selectedFieldKeys, org != null ? org.getEmail() : null);
        if (!orgData.isEmpty()) {
            preview.put("organization", orgData);
        }

        // FarmArea & ProductionLot fields
        ProductionLot lot = shipment.getProductionLot();
        if (lot != null) {
            FarmArea farmArea = lot.getFarmArea();
            Map<String, Object> farmAreaData = new LinkedHashMap<>();
            addFieldIfSelected(farmAreaData, "name", "farmArea.name", selectedFieldKeys, farmArea != null ? farmArea.getName() : null);
            addFieldIfSelected(farmAreaData, "area", "farmArea.area", selectedFieldKeys, farmArea != null ? farmArea.getArea() : null);
            addFieldIfSelected(farmAreaData, "areaUnit", "farmArea.areaUnit", selectedFieldKeys, farmArea != null && farmArea.getAreaUnit() != null ? farmArea.getAreaUnit().name() : null);
            if (!farmAreaData.isEmpty()) {
                preview.put("farmArea", farmAreaData);
            }

            Map<String, Object> lotData = new LinkedHashMap<>();
            addFieldIfSelected(lotData, "name", "productionLot.name", selectedFieldKeys, lot.getName());
            addFieldIfSelected(lotData, "productCategory", "productionLot.productCategory", selectedFieldKeys,
                    lot.getProductCategory() != null ? lot.getProductCategory().getName() : null);
            addFieldIfSelected(lotData, "plantingDate", "productionLot.plantingDate", selectedFieldKeys, lot.getPlantingDate());
            addFieldIfSelected(lotData, "harvestDate", "productionLot.harvestDate", selectedFieldKeys, lot.getHarvestDate());
            addFieldIfSelected(lotData, "expectedQuantity", "productionLot.expectedQuantity", selectedFieldKeys, lot.getExpectedQuantity());
            addFieldIfSelected(lotData, "actualQuantity", "productionLot.actualQuantity", selectedFieldKeys, lot.getActualQuantity());
            addFieldIfSelected(lotData, "status", "productionLot.status", selectedFieldKeys, lot.getStatus() != null ? lot.getStatus().name() : null);
            if (!lotData.isEmpty()) {
                preview.put("productionLot", lotData);
            }
        }

        // Shipment fields
        Map<String, Object> shipmentData = new LinkedHashMap<>();
        addFieldIfSelected(shipmentData, "name", "shipment.name", selectedFieldKeys, shipment.getName());
        addFieldIfSelected(shipmentData, "totalQuantity", "shipment.totalQuantity", selectedFieldKeys, shipment.getTotalQuantity());
        addFieldIfSelected(shipmentData, "packagingInfo", "shipment.packagingInfo", selectedFieldKeys, shipment.getPackagingInfo());
        addFieldIfSelected(shipmentData, "status", "shipment.status", selectedFieldKeys, shipment.getStatus() != null ? shipment.getStatus().name() : null);
        if (!shipmentData.isEmpty()) {
            preview.put("shipment", shipmentData);
        }

        // FarmLog fields
        if (lot != null && hasAnyPrefixSelected("farmLog.", selectedFieldKeys)) {
            List<FarmLog> logs = farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lot.getId());
            List<Map<String, Object>> logList = new ArrayList<>();
            for (FarmLog l : logs) {
                Map<String, Object> item = new LinkedHashMap<>();
                addFieldIfSelected(item, "activityType", "farmLog.activityType", selectedFieldKeys, l.getActivityType() != null ? l.getActivityType().name() : null);
                addFieldIfSelected(item, "executedDate", "farmLog.executedDate", selectedFieldKeys, l.getExecutedDate());
                addFieldIfSelected(item, "material", "farmLog.material", selectedFieldKeys, l.getMaterial());
                addFieldIfSelected(item, "quantity", "farmLog.quantity", selectedFieldKeys, l.getQuantity());
                addFieldIfSelected(item, "notes", "farmLog.notes", selectedFieldKeys, l.getNotes());
                if (!item.isEmpty()) {
                    logList.add(item);
                }
            }
            if (!logList.isEmpty()) {
                preview.put("farmLogs", logList);
            }
        }

        // Inspection fields
        if (lot != null && hasAnyPrefixSelected("inspection.", selectedFieldKeys)) {
            List<InspectionRequest> inspections = inspectionRequestRepository.findByProductionLot_IdOrderByCreatedAtDesc(lot.getId());
            List<Map<String, Object>> inspList = new ArrayList<>();
            for (InspectionRequest ir : inspections) {
                Map<String, Object> item = new LinkedHashMap<>();
                addFieldIfSelected(item, "sampleSentDate", "inspection.sampleSentDate", selectedFieldKeys, ir.getSampleSentDate());
                addFieldIfSelected(item, "inspectionUnit", "inspection.inspectionUnit", selectedFieldKeys, ir.getInspectionUnit());
                addFieldIfSelected(item, "status", "inspection.passed", selectedFieldKeys, ir.getStatus() != null ? ir.getStatus().name() : null);
                if (!item.isEmpty()) {
                    inspList.add(item);
                }
            }
            if (!inspList.isEmpty()) {
                preview.put("inspections", inspList);
            }
        }

        // ChainEvent / Timeline fields
        if (hasAnyPrefixSelected("chainEvent.", selectedFieldKeys)) {
            List<ChainEvent> events = chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipment.getId());
            List<Map<String, Object>> eventList = new ArrayList<>();
            for (ChainEvent ce : events) {
                Map<String, Object> item = new LinkedHashMap<>();
                addFieldIfSelected(item, "eventType", "chainEvent.eventType", selectedFieldKeys, ce.getEventType() != null ? ce.getEventType().name() : null);
                addFieldIfSelected(item, "recordedAt", "chainEvent.recordedAt", selectedFieldKeys, ce.getRecordedAt());
                addFieldIfSelected(item, "recordedBy", "chainEvent.recordedBy", selectedFieldKeys, ce.getRecordedBy() != null ? ce.getRecordedBy().getFullName() : null);
                if (!item.isEmpty()) {
                    eventList.add(item);
                }
            }
            if (!eventList.isEmpty()) {
                preview.put("timelineEvents", eventList);
            }
        }

        return preview;
    }

    private void addFieldIfSelected(Map<String, Object> target, String outputKey, String fieldKey, Set<String> selectedFieldKeys, Object value) {
        if (selectedFieldKeys.contains(fieldKey)) {
            target.put(outputKey, value);
        }
    }

    private boolean hasAnyPrefixSelected(String prefix, Set<String> selectedFieldKeys) {
        return selectedFieldKeys.stream().anyMatch(key -> key.startsWith(prefix));
    }

    private void validateOrganizationOwnership(UUID orgId, CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getOrganizationId() == null || !currentUser.getOrganizationId().equals(orgId)) {
            log.warn("Từ chối thao tác mẫu hồ sơ: orgId yêu cầu={}, orgId của user={}, username={}",
                    orgId, currentUser != null ? currentUser.getOrganizationId() : null, currentUser != null ? currentUser.getUsername() : null);
            throw new TemplateNotOwnedException("Từ chối thao tác: Bạn không có quyền truy cập dữ liệu của tổ chức khác.");
        }
    }

    private void validateShipmentAccess(Shipment shipment, CustomUserDetails currentUser) {
        UUID userOrgId = currentUser.getOrganizationId();
        if ("VT-02".equals(currentUser.getRoleCode())) {
            if (shipment.getOrganization() == null || !shipment.getOrganization().getOrganizationId().equals(userOrgId)) {
                throw new TemplateNotOwnedException("Từ chối thao tác: Lô hàng không thuộc tổ chức của bạn.");
            }
        }
    }

    private List<ProfileTemplateField> buildTemplateFields(ProfileTemplate template, List<FieldSelectionDto> dtoList) {
        List<ProfileTemplateField> fields = new ArrayList<>();
        int order = 1;
        for (FieldSelectionDto dto : dtoList) {
            fields.add(ProfileTemplateField.builder()
                    .template(template)
                    .fieldKey(dto.getFieldKey())
                    .fieldGroup(dto.getFieldGroup())
                    .isMandatory(MandatoryFields.isMandatory(dto.getFieldKey()))
                    .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order++)
                    .build());
        }
        return fields;
    }

    private ProfileTemplateResponse mapToResponse(ProfileTemplate template) {
        List<ProfileTemplateFieldResponse> fieldResponses = Collections.emptyList();
        if (template.getFields() != null) {
            fieldResponses = template.getFields().stream()
                    .map(f -> ProfileTemplateFieldResponse.builder()
                            .id(f.getId())
                            .fieldKey(f.getFieldKey())
                            .fieldGroup(f.getFieldGroup())
                            .displayName(MandatoryFields.FIELD_DISPLAY_NAMES.getOrDefault(f.getFieldKey(), f.getFieldKey()))
                            .mandatory(Boolean.TRUE.equals(f.getIsMandatory()))
                            .sortOrder(f.getSortOrder() != null ? f.getSortOrder() : 0)
                            .build())
                    .collect(Collectors.toList());
        }

        return ProfileTemplateResponse.builder()
                .id(template.getId())
                .organizationId(template.getOrganization() != null ? template.getOrganization().getOrganizationId() : null)
                .name(template.getName())
                .partnerName(template.getPartnerName())
                .description(template.getDescription())
                .isDefault(Boolean.TRUE.equals(template.getIsDefault()))
                .totalFields(fieldResponses.size())
                .fields(fieldResponses)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
