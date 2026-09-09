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
     * Lấy toàn bộ cấu hình ngưỡng (gồm cấu hình toàn cục và các cấu hình ghi đè danh mục).
     *
     * @return danh sách cấu hình
     */
    AllThresholdsResponse getAllThresholds();

    /**
     * Lấy cấu hình ngưỡng mặc định toàn cục.
     *
     * @return cấu hình toàn cục
     */
    AnomalyThresholdResponse getGlobalThreshold();

    /**
     * Cập nhật cấu hình ngưỡng mặc định toàn cục.
     *
     * @param request     dữ liệu cập nhật
     * @param currentUser thông tin người dùng thực hiện (VT-01)
     * @return cấu hình sau khi cập nhật
     */
    AnomalyThresholdResponse updateGlobalThreshold(UpdateGlobalThresholdRequest request, CustomUserDetails currentUser);

    /**
     * Lấy danh sách tất cả các cấu hình ghi đè theo danh mục nông sản đang hoạt động.
     *
     * @return danh sách cấu hình ghi đè
     */
    List<AnomalyThresholdResponse> getCategoryOverrides();

    /**
     * Tạo mới hoặc cập nhật cấu hình ghi đè theo danh mục nông sản.
     *
     * @param request     dữ liệu cấu hình ghi đè
     * @param currentUser thông tin người dùng thực hiện (VT-01)
     * @return cấu hình sau khi lưu
     */
    AnomalyThresholdResponse saveCategoryOverride(CategoryThresholdOverrideRequest request, CustomUserDetails currentUser);

    /**
     * Xóa (vô hiệu hóa / soft delete) cấu hình ghi đè theo danh mục nông sản.
     *
     * @param idOrCategoryId ID cấu hình hoặc ID danh mục nông sản
     * @param currentUser    thông tin người dùng thực hiện (VT-01)
     */
    void deleteCategoryOverride(UUID idOrCategoryId, CustomUserDetails currentUser);

    /**
     * Ước lượng tác động của ngưỡng dự thảo trên dữ liệu quét 30 ngày gần nhất (dry-run).
     *
     * @param request thông số ngưỡng dự thảo cần kiểm tra
     * @return kết quả ước lượng tác động
     */
    ImpactEstimationResponse estimateImpact(ImpactEstimationRequest request);

    /**
     * Lấy cấu hình ngưỡng hiệu lực cho một danh mục nông sản cụ thể.
     * <p>
     * Ưu tiên cấu hình ghi đè của danh mục (nếu có và đang active),
     * sau đó rơi về cấu hình toàn cục, hoặc giá trị mặc định hệ thống.
     * </p>
     *
     * @param productCategoryId ID danh mục nông sản (có thể null)
     * @return cấu hình ngưỡng hiệu lực
     */
    AnomalyThresholdResponse getEffectiveThreshold(UUID productCategoryId);
}
