package vn.nguongocso.farm.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.request.CorrectFarmLogRequest;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.dto.response.HarvestEligibilityResponse;
import vn.nguongocso.farm.service.FarmLogService;
import vn.nguongocso.farm.service.HarvestEligibilityService;
import vn.nguongocso.permission.service.PermissionChecker;

/**
 * Quản lý nhật ký canh tác.
*/
@RestController
@RequestMapping("/api/v1/farm-logs")
@RequiredArgsConstructor
public class FarmLogController {
    private final FarmLogService farmLogService;

    private final HarvestEligibilityService harvestEligibilityService;

    private final PermissionChecker permissionChecker;

    /**
     * Ghi nhật ký canh tác.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ApiResult<FarmLogResponse> create(
            @Valid @RequestBody CreateFarmLogRequest request) {
        permissionChecker.check("FARM_LOG", "CREATE");
        return ApiResult.success(farmLogService.create(request));
    }
    /**
     * Đính chính một nhật ký canh tác.
     */
    @PostMapping("/{id}/correct")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ApiResult<FarmLogResponse> correct(
            @PathVariable UUID id,
            @Valid @RequestBody CorrectFarmLogRequest request) {
        permissionChecker.check("FARM_LOG", "UPDATE");
        return ApiResult.success(farmLogService.correctFarmLog(id, request));
    }
    /**
     * Lấy danh sách nhật ký canh tác của lô sản xuất theo phân trang.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ApiResult<PageResponse<FarmLogResponse>> getFarmLogsByProductionLot(
            @RequestParam UUID productionLotId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        permissionChecker.check("FARM_LOG", "READ");
        return ApiResult.success(
                farmLogService.getFarmLogsByProductionLot(
                        productionLotId,
                        page,
                        size));
    }
    /**
     * Kiểm tra điều kiện thu hoạch của lô sản xuất.
     */
    @GetMapping("/harvest-eligibility")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03')")
    public ApiResult<HarvestEligibilityResponse> getHarvestEligibility(
            @RequestParam UUID productionLotId) {
        return ApiResult.success(harvestEligibilityService.calculateHarvestEligibility(productionLotId));
    }
    /**
     * Lấy chi tiết nhật ký canh tác theo ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ApiResult<FarmLogResponse> getFarmLog(@PathVariable UUID id) {
        permissionChecker.check("FARM_LOG", "READ");
        return ApiResult.success(farmLogService.getFarmLog(id));
    }
}