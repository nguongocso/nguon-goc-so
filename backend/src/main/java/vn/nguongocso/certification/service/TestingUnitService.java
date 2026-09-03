package vn.nguongocso.certification.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateTestingUnitRequest;
import vn.nguongocso.certification.dto.request.UpdateTestingUnitRequest;
import vn.nguongocso.certification.dto.response.TestingUnitResponse;

/**
 * Service quản lý danh mục đơn vị kiểm nghiệm dùng chung (NCL-11-CN-006 Phase 1).
 */
public interface TestingUnitService {

    /**
     * Tạo mới đơn vị kiểm nghiệm (chỉ VT-01).
     */
    TestingUnitResponse createTestingUnit(
            CreateTestingUnitRequest request,
            CustomUserDetails currentUser);

    /**
     * Cập nhật thông tin đơn vị kiểm nghiệm (chỉ VT-01).
     */
    TestingUnitResponse updateTestingUnit(
            UUID testingUnitId,
            UpdateTestingUnitRequest request,
            CustomUserDetails currentUser);

    /**
     * Lấy trang danh sách đơn vị kiểm nghiệm, có thể lọc theo trạng thái hiệu lực.
     */
    Page<TestingUnitResponse> getTestingUnits(
            Boolean isActive,
            Pageable pageable,
            CustomUserDetails currentUser);

    /**
     * Vô hiệu hoá đơn vị kiểm nghiệm (soft delete: isActive = false), chỉ VT-01.
     */
    void deactivateTestingUnit(
            UUID testingUnitId,
            CustomUserDetails currentUser);
}