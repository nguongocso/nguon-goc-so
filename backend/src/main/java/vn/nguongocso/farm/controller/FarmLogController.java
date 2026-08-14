package vn.nguongocso.farm.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.service.FarmLogService;
import vn.nguongocso.permission.service.PermissionChecker;

/**
 * Controller quản lý nhật ký canh tác.
 */
@RestController
@RequestMapping("/api/v1/farm-logs")
@RequiredArgsConstructor
public class FarmLogController {

    private final FarmLogService farmLogService;
    private final PermissionChecker permissionChecker;

    /**
     * Ghi nhật ký canh tác.
     *
     * @param request thông tin nhật ký canh tác
     * @return thông tin nhật ký vừa tạo
     */
    @PostMapping
    public ApiResult<FarmLogResponse> create(
            @Valid @RequestBody CreateFarmLogRequest request) {

        permissionChecker.check("FARM_LOG", "CREATE");
        return ApiResult.success(farmLogService.create(request));
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
}