package vn.nguongocso.export.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
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
import vn.nguongocso.export.service.builder.ProfileTemplatePreviewBuilder;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

/** Triển khai dịch vụ cấu hình mẫu hồ sơ truy xuất theo đối tác. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileTemplateServiceImpl implements ProfileTemplateService {

    private final ProfileTemplateRepository profileTemplateRepository;
    private final ProfileTemplateFieldRepository profileTemplateFieldRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final ProfileTemplatePreviewBuilder profileTemplatePreviewBuilder;

    @Override
    @Transactional
    public ProfileTemplateResponse createTemplate(
            UUID orgId,
            CreateProfileTemplateRequest request,
            CustomUserDetails currentUser) {
        validateOrganizationOwnership(orgId, currentUser);
        validateTemplateNameAndFields(orgId, request.getName(), null, request.getSelectedFields());

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tổ chức."));

        User creator = currentUser.getUserId() != null
                ? userRepository.findById(currentUser.getUserId()).orElse(null)
                : null;

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (isDefault) {
            resetExistingDefaultTemplate(orgId);
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
        List<ProfileTemplateField> fields = buildTemplateFields(savedTemplate, request.getSelectedFields());
        profileTemplateFieldRepository.saveAll(fields);

        if (savedTemplate.getFields() == null) {
            savedTemplate.setFields(new ArrayList<>());
        } else {
            savedTemplate.getFields().clear();
        }
        savedTemplate.getFields().addAll(fields);

        log.info(
                "Đã tạo mẫu hồ sơ '{}' (ID: {}) cho tổ chức ID: {}",
                savedTemplate.getName(),
                savedTemplate.getId(),
                orgId);
        return mapToResponse(savedTemplate);
    }

    @Override
    public List<ProfileTemplateResponse> listTemplates(UUID orgId, CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getOrganizationId() == null) {
            throw new TemplateNotOwnedException("Từ chối thao tác: Phiên đăng nhập không hợp lệ.");
        }
        if (!"VT-04".equals(currentUser.getRoleCode())) {
            validateOrganizationOwnership(orgId, currentUser);
        }

        List<ProfileTemplate> templates = profileTemplateRepository
                .findAllByOrganization_OrganizationIdOrderByNameAsc(orgId);
        return templates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProfileTemplateResponse> listTemplatesForMultipleOrganizations(
            List<UUID> organizationIds,
            CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getOrganizationId() == null) {
            throw new TemplateNotOwnedException("Từ chối thao tác: Phiên đăng nhập không hợp lệ.");
        }
        if (!"VT-04".equals(currentUser.getRoleCode())) {
            throw new TemplateNotOwnedException(
                    "Chỉ doanh nghiệp thu mua (VT-04) mới có thể xem mẫu của nhiều tổ chức.");
        }
        if (organizationIds == null || organizationIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProfileTemplate> allTemplates = new ArrayList<>();
        for (UUID orgId : organizationIds) {
            allTemplates.addAll(profileTemplateRepository.findAllByOrganization_OrganizationIdOrderByNameAsc(orgId));
        }

        return allTemplates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProfileTemplateResponse getTemplate(UUID orgId, UUID templateId, CustomUserDetails currentUser) {
        if (!"VT-04".equals(currentUser.getRoleCode())) {
            validateOrganizationOwnership(orgId, currentUser);
        }

        ProfileTemplate template = profileTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ."));

        if (template.getOrganization() == null || !template.getOrganization().getOrganizationId().equals(orgId)) {
            String message = "VT-04".equals(currentUser.getRoleCode())
                    ? "Mẫu hồ sơ không thuộc tổ chức được yêu cầu."
                    : "Mẫu hồ sơ không thuộc tổ chức của bạn.";
            throw new TemplateNotOwnedException(message);
        }

        return mapToResponse(template);
    }

    @Override
    @Transactional
    public ProfileTemplateResponse updateTemplate(
            UUID orgId,
            UUID templateId,
            UpdateProfileTemplateRequest request,
            CustomUserDetails currentUser) {
        validateOrganizationOwnership(orgId, currentUser);

        ProfileTemplate template = profileTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ."));

        if (template.getOrganization() == null || !template.getOrganization().getOrganizationId().equals(orgId)) {
            throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của bạn.");
        }

        validateTemplateNameAndFields(orgId, request.getName(), templateId, request.getSelectedFields());

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (isDefault && !Boolean.TRUE.equals(template.getIsDefault())) {
            resetOtherDefaultTemplates(orgId, templateId);
        }

        template.setName(request.getName().trim());
        template.setPartnerName(request.getPartnerName() != null ? request.getPartnerName().trim() : null);
        template.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        template.setIsDefault(isDefault);

        updateTemplateFields(template, request.getSelectedFields());

        ProfileTemplate updated = profileTemplateRepository.save(template);
        log.info("Đã cập nhật mẫu hồ sơ ID: {} cho tổ chức ID: {}", templateId, orgId);
        return mapToResponse(updated);
    }

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

    @Override
    public ProfileTemplate getDefaultTemplate(UUID orgId) {
        return profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mẫu hồ sơ mặc định của tổ chức."));
    }

    @Override
    public ProfileTemplateResponse getDefaultTemplateResponse(UUID orgId, CustomUserDetails currentUser) {
        if (!"VT-04".equals(currentUser.getRoleCode())) {
            validateOrganizationOwnership(orgId, currentUser);
        }
        ProfileTemplate template = getDefaultTemplate(orgId);
        return mapToResponse(template);
    }

    @Override
    public List<FieldGroupDefinition> getAllAvailableFields() {
        return MandatoryFields.buildFullCatalog();
    }

    @Override
    public Map<String, Object> buildPreview(UUID shipmentId, UUID templateId, CustomUserDetails currentUser) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin lô hàng."));

        validateShipmentAccess(shipment, currentUser);

        UUID userOrgId = currentUser.getOrganizationId();
        UUID effectiveOrgId = ("VT-04".equals(currentUser.getRoleCode()) && shipment.getOrganization() != null)
                ? shipment.getOrganization().getOrganizationId()
                : userOrgId;

        ProfileTemplate template =
                resolveTemplateForPreview(templateId, effectiveOrgId, userOrgId, currentUser.getRoleCode());
        Set<String> selectedFieldKeys = resolveSelectedFieldKeys(template);

        return profileTemplatePreviewBuilder.buildPreviewSnapshot(shipment, template, selectedFieldKeys);
    }

    private ProfileTemplate resolveTemplateForPreview(
            UUID templateId,
            UUID effectiveOrgId,
            UUID userOrgId,
            String roleCode) {
        if (templateId != null) {
            ProfileTemplate template = profileTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ."));
            if ("VT-04".equals(roleCode)) {
                if (!template.getOrganization().getOrganizationId().equals(effectiveOrgId)) {
                    throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của lô hàng này.");
                }
            } else {
                if (!template.getOrganization().getOrganizationId().equals(userOrgId)) {
                    throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của bạn.");
                }
            }
            return template;
        }
        return profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(effectiveOrgId).orElse(null);
    }

    private Set<String> resolveSelectedFieldKeys(ProfileTemplate template) {
        if (template != null && template.getFields() != null && !template.getFields().isEmpty()) {
            return template.getFields().stream()
                    .map(ProfileTemplateField::getFieldKey)
                    .collect(Collectors.toSet());
        }
        return new HashSet<>(MandatoryFields.FIELD_DISPLAY_NAMES.keySet());
    }

    private void validateTemplateNameAndFields(
            UUID orgId,
            String name,
            UUID templateId,
            List<FieldSelectionDto> selectedFields) {
        boolean nameExists = templateId == null
                ? profileTemplateRepository.existsByNameAndOrganization_OrganizationId(name, orgId)
                : profileTemplateRepository.existsByNameAndOrganization_OrganizationIdAndIdNot(name, orgId, templateId);
        if (nameExists) {
            throw new BusinessException("Tên mẫu hồ sơ đã tồn tại trong tổ chức của bạn.");
        }

        Set<String> selectedKeys = selectedFields.stream()
                .map(FieldSelectionDto::getFieldKey)
                .collect(Collectors.toSet());

        List<String> missingMandatory = MandatoryFields.findMissingMandatoryFields(selectedKeys);
        if (!missingMandatory.isEmpty()) {
            throw new MandatoryFieldsViolationException(missingMandatory);
        }
    }

    private void resetExistingDefaultTemplate(UUID orgId) {
        profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(orgId)
                .ifPresent(existingDefault -> {
                    existingDefault.setIsDefault(false);
                    profileTemplateRepository.save(existingDefault);
                });
    }

    private void resetOtherDefaultTemplates(UUID orgId, UUID templateId) {
        List<ProfileTemplate> otherDefaults =
                profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrueAndIdNot(orgId, templateId);
        for (ProfileTemplate ot : otherDefaults) {
            ot.setIsDefault(false);
            profileTemplateRepository.save(ot);
        }
    }

    private void updateTemplateFields(ProfileTemplate template, List<FieldSelectionDto> selectedFields) {
        if (template.getFields() == null) {
            template.setFields(new ArrayList<>());
        }

        Map<String, ProfileTemplateField> existingFieldsMap = template.getFields().stream()
                .filter(f -> f.getFieldKey() != null)
                .collect(Collectors.toMap(ProfileTemplateField::getFieldKey, f -> f, (f1, f2) -> f1));

        Set<String> newSelectedKeys = new HashSet<>();
        int order = 1;
        for (FieldSelectionDto dto : selectedFields) {
            newSelectedKeys.add(dto.getFieldKey());
            ProfileTemplateField existing = existingFieldsMap.get(dto.getFieldKey());
            int sortOrder = dto.getSortOrder() != null ? dto.getSortOrder() : order++;
            boolean mandatory = MandatoryFields.isMandatory(dto.getFieldKey());
            if (existing != null) {
                existing.setFieldGroup(dto.getFieldGroup());
                existing.setIsMandatory(mandatory);
                existing.setSortOrder(sortOrder);
            } else {
                template.getFields().add(ProfileTemplateField.builder()
                        .template(template)
                        .fieldKey(dto.getFieldKey())
                        .fieldGroup(dto.getFieldGroup())
                        .isMandatory(mandatory)
                        .sortOrder(sortOrder)
                        .build());
            }
        }

        template.getFields().removeIf(f -> !newSelectedKeys.contains(f.getFieldKey()));
    }

    private void validateOrganizationOwnership(UUID orgId, CustomUserDetails currentUser) {
        if (currentUser == null
                || currentUser.getOrganizationId() == null
                || !currentUser.getOrganizationId().equals(orgId)) {
            log.warn(
                    "Từ chối thao tác mẫu hồ sơ: orgId yêu cầu={}, orgId của user={}, username={}",
                    orgId,
                    currentUser != null ? currentUser.getOrganizationId() : null,
                    currentUser != null ? currentUser.getUsername() : null);
            throw new TemplateNotOwnedException(
                    "Từ chối thao tác: Bạn không có quyền truy cập dữ liệu của tổ chức khác.");
        }
    }

    private void validateShipmentAccess(Shipment shipment, CustomUserDetails currentUser) {
        UUID userOrgId = currentUser.getOrganizationId();
        if ("VT-02".equals(currentUser.getRoleCode())) {
            if (shipment.getOrganization() == null
                    || !shipment.getOrganization().getOrganizationId().equals(userOrgId)) {
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
                            .displayName(MandatoryFields.FIELD_DISPLAY_NAMES.getOrDefault(
                                    f.getFieldKey(),
                                    f.getFieldKey()))
                            .mandatory(Boolean.TRUE.equals(f.getIsMandatory()))
                            .sortOrder(f.getSortOrder() != null ? f.getSortOrder() : 0)
                            .build())
                    .collect(Collectors.toList());
        }

        return ProfileTemplateResponse.builder()
                .id(template.getId())
                .organizationId(template.getOrganization() != null
                        ? template.getOrganization().getOrganizationId()
                        : null)
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
