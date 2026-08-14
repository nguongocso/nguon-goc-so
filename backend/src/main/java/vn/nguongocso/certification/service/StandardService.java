package vn.nguongocso.certification.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateStandardRequest;
import vn.nguongocso.certification.dto.request.UpdateStandardRequest;
import vn.nguongocso.certification.dto.response.StandardResponse;

/**
 * Service quản lý danh mục tiêu chuẩn chất lượng.
 */
public interface StandardService {
    /**
     * Thêm mới tiêu chuẩn chất lượng.
     */
    StandardResponse createStandard(CreateStandardRequest request, CustomUserDetails currentUser);

    /**
     * Cập nhật thông tin tiêu chuẩn.
     */
    StandardResponse updateStandard(UUID standardId, UpdateStandardRequest request, CustomUserDetails currentUser);

    /**
     * Lấy danh sách tiêu chuẩn chất lượng.
     */
    Page<StandardResponse> getStandards(Boolean isActive, Pageable pageable, CustomUserDetails currentUser);

}