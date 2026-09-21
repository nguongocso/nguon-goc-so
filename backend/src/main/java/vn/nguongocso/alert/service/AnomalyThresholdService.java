package vn.nguongocso.alert.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.alert.dto.request.CategoryThresholdOverrideRequest;
import vn.nguongocso.alert.dto.request.ImpactEstimationRequest;
import vn.nguongocso.alert.dto.request.UpdateGlobalThresholdRequest;
import vn.nguongocso.alert.dto.response.AllThresholdsResponse;
import vn.nguongocso.alert.dto.response.AnomalyThresholdResponse;
import vn.nguongocso.alert.dto.response.ImpactEstimationResponse;
import vn.nguongocso.auth.service.CustomUserDetails;

/**
 * Service quản lý và tra cứu cấu hình ngưỡng quét bất thường (NCL-08-CN-014).
 */
public interface AnomalyThresholdService {
    /**
     * Lấy toàn bộ cấu hình ngưỡng (gồm cấu hình toàn cục và các cấu hình ghi đè
     * danh mục).
     */
    AllThresholdsResponse getAllThresholds();

    /**
     * Lấy cấu hình ngưỡng mặc định toàn cục.
     */
    AnomalyThresholdResponse getGlobalThreshold();

    /**
     * Cập nhật cấu hình ngưỡng mặc định toàn cục.
     */
    AnomalyThresholdResponse updateGlobalThreshold(UpdateGlobalThresholdRequest request, CustomUserDetails currentUser);

    /**
     * Lấy danh sách tất cả các cấu hình ghi đè theo danh mục nông sản đang hoạt
     * động.
     */
    List<AnomalyThresholdResponse> getCategoryOverrides();

    /**
     * Tạo mới hoặc cập nhật cấu hình ghi đè theo danh mục nông sản.
     */
    AnomalyThresholdResponse saveCategoryOverride(CategoryThresholdOverrideRequest request,
            CustomUserDetails currentUser);

    /**
     * Xóa (vô hiệu hóa / soft delete) cấu hình ghi đè theo danh mục nông sản.
     */
    void deleteCategoryOverride(UUID idOrCategoryId, CustomUserDetails currentUser);

    /**
     * Ước lượng tác động của ngưỡng dự thảo trên dữ liệu quét 30 ngày gần nhất
     * (dry-run).
     */
    ImpactEstimationResponse estimateImpact(ImpactEstimationRequest request);

    /**
     * Lấy cấu hình ngưỡng hiệu lực cho một danh mục nông sản cụ thể.
     */
    AnomalyThresholdResponse getEffectiveThreshold(UUID productCategoryId);
}
