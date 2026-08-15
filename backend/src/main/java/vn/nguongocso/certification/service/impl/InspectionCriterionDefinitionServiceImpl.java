package vn.nguongocso.certification.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionResponse;
import vn.nguongocso.certification.entity.InspectionCriterionDefinition;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.InspectionCriterionDefinitionRepository;
import vn.nguongocso.certification.repository.StandardRepository;
import vn.nguongocso.certification.service.InspectionCriterionDefinitionService;
import vn.nguongocso.exception.BusinessException;

@Service
@RequiredArgsConstructor
@Transactional
public class InspectionCriterionDefinitionServiceImpl implements InspectionCriterionDefinitionService {

    private static final String ADMIN_ROLE = "VT-01";
    private static final String MSG_NO_PERMISSION = "Bạn không có quyền quản lý tiêu chí kiểm nghiệm.";
    private static final String MSG_STANDARD_NOT_FOUND = "Tiêu chuẩn không tồn tại.";
    private static final String MSG_CRITERION_NOT_FOUND = "Tiêu chí kiểm nghiệm không tồn tại.";
    private static final String MSG_CRITERION_CODE_EXISTS = "Mã tiêu chí đã tồn tại trong tiêu chuẩn này.";

    private final StandardRepository standardRepository;
    private final InspectionCriterionDefinitionRepository inspectionCriterionDefinitionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InspectionCriterionResponse> getCriteriaByStandard(UUID standardId, CustomUserDetails currentUser) {
        validateAdminOrMember(currentUser);

        Standard standard = standardRepository.findById(standardId)
                .orElseThrow(() -> new BusinessException(MSG_STANDARD_NOT_FOUND));

        return inspectionCriterionDefinitionRepository.findByStandard_IdOrderByIdAsc(standard.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public InspectionCriterionResponse createCriteria(UUID standardId, InspectionCriterionRequest request, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        Standard standard = standardRepository.findById(standardId)
                .orElseThrow(() -> new BusinessException(MSG_STANDARD_NOT_FOUND));

        String code = request.getCriterionCode() == null ? "" : request.getCriterionCode().trim();
        String name = request.getCriterionName() == null ? "" : request.getCriterionName().trim();
        String note = request.getNote() == null ? null : request.getNote().trim();

        if (code.isBlank()) {
            throw new BusinessException("Mã tiêu chí không được để trống.");
        }
        if (name.isBlank()) {
            throw new BusinessException("Tên tiêu chí không được để trống.");
        }

        if (inspectionCriterionDefinitionRepository.existsByStandard_IdAndCodeIgnoreCase(standardId, code)) {
            throw new BusinessException(MSG_CRITERION_CODE_EXISTS);
        }

        InspectionCriterionDefinition entity = InspectionCriterionDefinition.builder()
                .code(code)
                .name(name)
                .note(note)
                .standard(standard)
                .build();

        return toResponse(inspectionCriterionDefinitionRepository.save(entity));
    }

    @Override
    public InspectionCriterionResponse updateCriteria(UUID standardId, Integer criteriaId, InspectionCriterionRequest request, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        Standard standard = standardRepository.findById(standardId)
                .orElseThrow(() -> new BusinessException(MSG_STANDARD_NOT_FOUND));

        InspectionCriterionDefinition entity = inspectionCriterionDefinitionRepository.findById(criteriaId)
                .orElseThrow(() -> new BusinessException(MSG_CRITERION_NOT_FOUND));

        if (!standard.getId().equals(entity.getStandard().getId())) {
            throw new BusinessException("Tiêu chí không thuộc tiêu chuẩn đã chọn.");
        }

        String code = request.getCriterionCode() == null ? "" : request.getCriterionCode().trim();
        String name = request.getCriterionName() == null ? "" : request.getCriterionName().trim();
        String note = request.getNote() == null ? null : request.getNote().trim();

        if (code.isBlank()) {
            throw new BusinessException("Mã tiêu chí không được để trống.");
        }
        if (name.isBlank()) {
            throw new BusinessException("Tên tiêu chí không được để trống.");
        }

        if (!entity.getCode().equalsIgnoreCase(code)
                && inspectionCriterionDefinitionRepository.existsByStandard_IdAndCodeIgnoreCase(standardId, code)) {
            throw new BusinessException(MSG_CRITERION_CODE_EXISTS);
        }

        entity.setCode(code);
        entity.setName(name);
        entity.setNote(note);
        entity.setStandard(standard);

        return toResponse(inspectionCriterionDefinitionRepository.save(entity));
    }

    @Override
    public void deleteCriteria(UUID standardId, Integer criteriaId, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        Standard standard = standardRepository.findById(standardId)
                .orElseThrow(() -> new BusinessException(MSG_STANDARD_NOT_FOUND));

        InspectionCriterionDefinition entity = inspectionCriterionDefinitionRepository.findById(criteriaId)
                .orElseThrow(() -> new BusinessException(MSG_CRITERION_NOT_FOUND));

        if (!standard.getId().equals(entity.getStandard().getId())) {
            throw new BusinessException("Tiêu chí không thuộc tiêu chuẩn đã chọn.");
        }

        inspectionCriterionDefinitionRepository.delete(entity);
    }

    private void validateAdminPermission(CustomUserDetails currentUser) {
        if (currentUser == null || !ADMIN_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(MSG_NO_PERMISSION);
        }
    }

    private void validateAdminOrMember(CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new BusinessException("Bạn chưa đăng nhập.");
        }
        if (!"VT-01".equals(currentUser.getRoleCode()) && !"VT-02".equals(currentUser.getRoleCode())) {
            throw new BusinessException("Bạn không có quyền xem tiêu chí kiểm nghiệm.");
        }
    }

    private InspectionCriterionResponse toResponse(InspectionCriterionDefinition entity) {
        return InspectionCriterionResponse.builder()
                .criteriaId(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .standardId(entity.getStandard() != null ? entity.getStandard().getId() : null)
                .standardName(entity.getStandard() != null ? entity.getStandard().getName() : null)
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
