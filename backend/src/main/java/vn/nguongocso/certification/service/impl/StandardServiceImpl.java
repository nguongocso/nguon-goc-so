package vn.nguongocso.certification.service.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateStandardRequest;
import vn.nguongocso.certification.dto.request.UpdateStandardRequest;
import vn.nguongocso.certification.dto.response.StandardResponse;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.StandardRepository;
import vn.nguongocso.certification.service.StandardService;
import vn.nguongocso.exception.BusinessException;

/**
 * Triển khai các phương thức của StandardService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StandardServiceImpl implements StandardService {
    private static final String ADMIN_ROLE = "VT-01";

    private static final String MSG_NO_PERMISSION = "Bạn không có quyền quản lý danh mục tiêu chuẩn.";
    private static final String MSG_STANDARD_NOT_FOUND = "Tiêu chuẩn không tồn tại.";
    private static final String MSG_STANDARD_NAME_EXISTS = "Tên tiêu chuẩn đã tồn tại.";

    private final StandardRepository standardRepository;

    /**
     * Thêm mới tiêu chuẩn chất lượng.
     */
    @Override
    public StandardResponse createStandard(CreateStandardRequest request, CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        String name = request.getName().trim();

        standardRepository.findByNameIgnoreCase(name)
                .ifPresent(standard -> {
                    throw new BusinessException(MSG_STANDARD_NAME_EXISTS);
                });

        Standard standard = Standard.builder()
                .name(name)
                .description(request.getDescription())
                .issuingBody(request.getIssuingBody())
                .build();

        return buildResponse(
                standardRepository.save(standard));
    }

    /**
     * Cập nhật thông tin tiêu chuẩn.
     */
    @Override
    public StandardResponse updateStandard(UUID standardId,
            UpdateStandardRequest request, CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        Standard standard = standardRepository.findById(standardId)
                .orElseThrow(() -> new BusinessException(MSG_STANDARD_NOT_FOUND));

        String name = request.getName().trim();

        standardRepository.findByNameIgnoreCaseAndIdNot(name, standardId)
                .ifPresent(item -> {
                    throw new BusinessException(MSG_STANDARD_NAME_EXISTS);
                });

        standard.setName(name);
        standard.setDescription(request.getDescription());
        standard.setIssuingBody(request.getIssuingBody());
        standard.setIsActive(request.getIsActive());

        return buildResponse(
                standardRepository.save(standard));
    }

    /**
     * Lấy danh sách tiêu chuẩn chất lượng.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<StandardResponse> getStandards(Boolean isActive,
            Pageable pageable, CustomUserDetails currentUser) {

        Page<Standard> page;

        if (isActive == null) {
            page = standardRepository.findAll(pageable);
        } else {
            page = standardRepository.findByIsActive(isActive, pageable);
        }

        return page.map(this::buildResponse);
    }

    /**
     * Kiểm tra người dùng có quyền Quản trị viên nền tảng hay không.
     */
    private void validateAdminPermission(CustomUserDetails currentUser) {

        if (!ADMIN_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(MSG_NO_PERMISSION);
        }
    }

    private StandardResponse buildResponse(Standard standard) {

        return StandardResponse.builder()
                .id(standard.getId())
                .name(standard.getName())
                .description(standard.getDescription())
                .issuingBody(standard.getIssuingBody())
                .isActive(standard.getIsActive())
                .createdAt(standard.getCreatedAt())
                .updatedAt(standard.getUpdatedAt())
                .build();
    }
}