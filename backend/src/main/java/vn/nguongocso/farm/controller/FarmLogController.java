package vn.nguongocso.farm.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.request.CorrectFarmLogRequest;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.service.FarmLogService;
import vn.nguongocso.permission.service.PermissionChecker;

import vn.nguongocso.farm.dto.response.HarvestEligibilityResponse;
import vn.nguongocso.farm.service.HarvestEligibilityService;

/**
 * Controller quản lý nhật ký canh tác.
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
     *
     * @param request thông tin nhật ký canh tác
     * @return thông tin nhật ký vừa tạo
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ApiResult<FarmLogResponse> create(
            @Valid @RequestBody CreateFarmLogRequest request) {

        permissionChecker.check("FARM_LOG", "CREATE");
        return ApiResult.success(farmLogService.create(request));
    }

    /**
     * NCL-03-CN-006: Đính chính một nhật ký canh tác.
     *
     * <p>Bản gốc được giữ nguyên và đánh dấu đã đính chính; hệ thống tạo bản
     * ghi mới liên kết tới bản gốc. Chỉ người ghi gốc (VT-03) hoặc Quản lý
     * hợp tác xã (VT-02) được phép thực hiện.</p>
     *
     * @param id      ID của nhật ký cần đính chính
     * @param request dữ liệu đính chính và lý do
     * @return thông tin bản ghi đính chính vừa tạo
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
     *
     * @param productionLotId mã lô sản xuất
     * @param page            số trang (mặc định 0)
     * @param size            số bản ghi trên mỗi trang (mặc định 10)
     * @return danh sách nhật ký canh tác
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
     * Kiểm tra và tính toán thời gian cách ly đủ điều kiện thu hoạch của lô sản xuất (NCL-681 / NCL-843).
     *
     * @param productionLotId mã lô sản xuất
     * @return kết quả đánh giá điều kiện thu hoạch
     */
    @GetMapping("/harvest-eligibility")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03')")
    public ApiResult<HarvestEligibilityResponse> getHarvestEligibility(
            @RequestParam UUID productionLotId) {
        return ApiResult.success(harvestEligibilityService.calculateHarvestEligibility(productionLotId));
    }

    /**
     * NCL-03-CN-006: Lấy chi tiết một nhật ký canh tác theo ID, phục vụ trang
     * đính chính nhật ký.
     *
     * @param id ID của nhật ký
     * @return thông tin nhật ký
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ApiResult<FarmLogResponse> getFarmLog(@PathVariable UUID id) {

        permissionChecker.check("FARM_LOG", "READ");
        return ApiResult.success(farmLogService.getFarmLog(id));
    }
}