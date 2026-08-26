package vn.nguongocso.certification.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateTestingUnitRequest;
import vn.nguongocso.certification.dto.request.UpdateTestingUnitRequest;
import vn.nguongocso.certification.dto.response.TestingUnitResponse;
import vn.nguongocso.certification.entity.TestingUnit;
import vn.nguongocso.certification.repository.TestingUnitRepository;
import vn.nguongocso.certification.service.TestingUnitService;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;

/**
 * Triển khai các phương thức của TestingUnitService.
 * <p>
 * Quy tắc nghiệp vụ (theo mẫu StandardServiceImpl):
 * - Chỉ Quản trị viên nền tảng (VT-01) được thêm/sửa/vô hiệu hoá (QTN-17).
 * - Tên đơn vị kiểm nghiệm là duy nhất, không phân biệt hoa/thường.
 * - Xoá mềm: isActive = false, không xoá cứng để bảo toàn lịch sử yêu cầu.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TestingUnitServiceImpl implements TestingUnitService {

    private static final String ADMIN_ROLE = "VT-01";

    private static final String MSG_NO_PERMISSION =
            "Bạn không có quyền quản lý danh mục đơn vị kiểm nghiệm.";

    private static final String MSG_UNIT_NOT_FOUND =
            "Đơn vị kiểm nghiệm không tồn tại.";

    private static final String MSG_UNIT_NAME_EXISTS =
            "Tên đơn vị kiểm nghiệm đã tồn tại, vui lòng chọn tên khác.";

    private final TestingUnitRepository testingUnitRepository;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Thêm mới đơn vị kiểm nghiệm.
     */
    @Override
    public TestingUnitResponse createTestingUnit(
            CreateTestingUnitRequest request,
            CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        String name = request.getName().trim();

        testingUnitRepository.findByNameIgnoreCase(name)
                .ifPresent(unit -> {
                    throw new BusinessException(MSG_UNIT_NAME_EXISTS);
                });

        TestingUnit unit = TestingUnit.builder()
                .name(name)
                .accreditationCode(request.getAccreditationCode().trim())
                .contactInfo(request.getContactInfo())
                .accreditationExpiryDate(request.getAccreditationExpiryDate())
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .build();

        TestingUnit saved = testingUnitRepository.save(unit);

        publishActivityLog(
                currentUser,
                "CREATE_TESTING_UNIT",
                "Tạo đơn vị kiểm nghiệm '" + saved.getName()
                        + "' (mã công nhận: " + saved.getAccreditationCode() + ")",
                "TESTING_UNIT",
                saved.getId().toString());

        return buildResponse(saved);
    }

    /**
     * Cập nhật thông tin đơn vị kiểm nghiệm.
     */
    @Override
    public TestingUnitResponse updateTestingUnit(
            UUID testingUnitId,
            UpdateTestingUnitRequest request,
            CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        TestingUnit unit = testingUnitRepository.findById(testingUnitId)
                .orElseThrow(() -> new BusinessException(MSG_UNIT_NOT_FOUND));

        String name = request.getName().trim();

        testingUnitRepository.findByNameIgnoreCaseAndIdNot(name, testingUnitId)
                .ifPresent(existing -> {
                    throw new BusinessException(MSG_UNIT_NAME_EXISTS);
                });

        unit.setName(name);
        unit.setAccreditationCode(request.getAccreditationCode().trim());
        unit.setContactInfo(request.getContactInfo());
        unit.setAccreditationExpiryDate(request.getAccreditationExpiryDate());
        unit.setIsActive(request.getIsActive() == null ? unit.getIsActive() : request.getIsActive());

        TestingUnit saved = testingUnitRepository.save(unit);

        publishActivityLog(
                currentUser,
                "UPDATE_TESTING_UNIT",
                "Cập nhật đơn vị kiểm nghiệm '" + saved.getName() + "'",
                "TESTING_UNIT",
                saved.getId().toString());

        return buildResponse(saved);
    }

    /**
     * Lấy trang danh sách đơn vị kiểm nghiệm.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TestingUnitResponse> getTestingUnits(
            Boolean isActive,
            Pageable pageable,
            CustomUserDetails currentUser) {

        Page<TestingUnit> page;

        if (isActive == null) {
            page = testingUnitRepository.findAll(pageable);
        } else {
            page = testingUnitRepository.findByIsActive(isActive, pageable);
        }

        return page.map(this::buildResponse);
    }

    /**
     * Vô hiệu hoá đơn vị kiểm nghiệm (soft delete).
     */
    @Override
    public void deactivateTestingUnit(
            UUID testingUnitId,
            CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        TestingUnit unit = testingUnitRepository.findById(testingUnitId)
                .orElseThrow(() -> new BusinessException(MSG_UNIT_NOT_FOUND));

        unit.setIsActive(Boolean.FALSE);

        TestingUnit saved = testingUnitRepository.save(unit);

        publishActivityLog(
                currentUser,
                "DEACTIVATE_TESTING_UNIT",
                "Vô hiệu hoá đơn vị kiểm nghiệm '" + saved.getName() + "'",
                "TESTING_UNIT",
                saved.getId().toString());
    }

    /**
     * Kiểm tra người dùng có quyền Quản trị viên nền tảng hay không.
     */
    private void validateAdminPermission(CustomUserDetails currentUser) {

        if (!ADMIN_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(MSG_NO_PERMISSION);
        }
    }

    private TestingUnitResponse buildResponse(TestingUnit unit) {

        return TestingUnitResponse.builder()
                .id(unit.getId())
                .name(unit.getName())
                .accreditationCode(unit.getAccreditationCode())
                .contactInfo(unit.getContactInfo())
                .accreditationExpiryDate(unit.getAccreditationExpiryDate())
                .isActive(unit.getIsActive())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .build();
    }

    /**
     * Ghi nhật ký hoạt động theo convention của hệ thống (TASK-27).
     */
    private void publishActivityLog(
            CustomUserDetails currentUser,
            String action,
            String description,
            String entityType,
            String entityId) {

        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }
}