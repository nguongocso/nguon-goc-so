package vn.nguongocso.alert.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.dto.request.CategoryThresholdOverrideRequest;
import vn.nguongocso.alert.dto.request.ImpactEstimationRequest;
import vn.nguongocso.alert.dto.request.UpdateGlobalThresholdRequest;
import vn.nguongocso.alert.dto.response.AllThresholdsResponse;
import vn.nguongocso.alert.dto.response.AnomalyThresholdResponse;
import vn.nguongocso.alert.dto.response.ImpactEstimationResponse;
import vn.nguongocso.alert.service.AnomalyThresholdService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;

/**
 * Controller quản lý cấu hình ngưỡng phát hiện quét bất thường dành cho Quản trị viên nền tảng (VT-01) (NCL-08-CN-014).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/anomaly-thresholds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VT-01')")
public class AnomalyThresholdController {

    private final AnomalyThresholdService anomalyThresholdService;

    /**
     * Lấy toàn bộ cấu hình ngưỡng (gồm cấu hình toàn cục và các cấu hình ghi đè danh mục).
     */
    @GetMapping
    public ResponseEntity<ApiResult<AllThresholdsResponse>> getAllThresholds() {
        AllThresholdsResponse response = anomalyThresholdService.getAllThresholds();
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy cấu hình ngưỡng mặc định toàn cục.
     */
    @GetMapping("/global")
    public ResponseEntity<ApiResult<AnomalyThresholdResponse>> getGlobalThreshold() {
        AnomalyThresholdResponse response = anomalyThresholdService.getGlobalThreshold();
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Cập nhật cấu hình ngưỡng mặc định toàn cục.
     */
    @PutMapping("/global")
    public ResponseEntity<ApiResult<AnomalyThresholdResponse>> updateGlobalThreshold(
            @Valid @RequestBody UpdateGlobalThresholdRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        AnomalyThresholdResponse response = anomalyThresholdService.updateGlobalThreshold(request, currentUser);
        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Lấy danh sách tất cả các cấu hình ghi đè theo danh mục nông sản.
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResult<List<AnomalyThresholdResponse>>> getCategoryOverrides() {
        List<AnomalyThresholdResponse> response = anomalyThresholdService.getCategoryOverrides();
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Tạo mới hoặc cập nhật cấu hình ghi đè theo danh mục nông sản.
     */
    @PostMapping("/categories")
    public ResponseEntity<ApiResult<AnomalyThresholdResponse>> saveCategoryOverride(
            @Valid @RequestBody CategoryThresholdOverrideRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        AnomalyThresholdResponse response = anomalyThresholdService.saveCategoryOverride(request, currentUser);
        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Xóa cấu hình ghi đè danh mục nông sản (để danh mục quay về dùng ngưỡng toàn cục).
     */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResult<Void>> deleteCategoryOverride(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        anomalyThresholdService.deleteCategoryOverride(id, currentUser);
        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), null));
    }

    /**
     * Ước lượng tác động của ngưỡng dự thảo trên dữ liệu quét 30 ngày gần nhất (dry-run).
     */
    @PostMapping("/estimate")
    public ResponseEntity<ApiResult<ImpactEstimationResponse>> estimateImpact(
            @Valid @RequestBody ImpactEstimationRequest request) {
        ImpactEstimationResponse response = anomalyThresholdService.estimateImpact(request);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
