package vn.nguongocso.farm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.farm.dto.request.CreateFarmAreaRequest;
import vn.nguongocso.farm.dto.response.FarmAreaResponse;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.service.FarmAreaService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.List;

import java.util.UUID;
import vn.nguongocso.farm.dto.request.UpdateFarmAreaRequest;

@RestController
@RequestMapping("/api/v1/farm-areas")
@RequiredArgsConstructor
/** Quản lý vùng trồng. */
public class FarmAreaController {

    private final FarmAreaService farmAreaService;
    private final PermissionChecker permissionChecker;

    /** Lấy danh sách vùng trồng. */
    @GetMapping
    public ResponseEntity<ApiResult<List<FarmAreaResponse>>> getFarmAreas(
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(ApiResult.success(farmAreaService.getFarmAreas(activeOnly)));
    }

    /** Lấy chi tiết vùng trồng theo ID. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<FarmAreaResponse>> getFarmAreaById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResult.success(farmAreaService.getFarmAreaById(id)));
    }

    /** Lấy các đơn vị diện tích hỗ trợ. */
    @GetMapping("/units")
    public ResponseEntity<ApiResult<List<AreaUnit>>> getAreaUnits() {
        return ResponseEntity.ok(ApiResult.success(farmAreaService.getAreaUnits()));
    }

    /** Tạo mới vùng trồng. */
    @PostMapping
    public ResponseEntity<ApiResult<FarmAreaResponse>> createFarmArea(
            @Valid @RequestBody CreateFarmAreaRequest request) {
        permissionChecker.check("FARM_AREA", "CREATE");
        return ResponseEntity.ok(ApiResult.success(farmAreaService.create(request)));
    }

    /** Cập nhật thông tin vùng trồng (US NCL-02-CN-005). */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<FarmAreaResponse>> updateFarmArea(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFarmAreaRequest request) {
        permissionChecker.check("FARM_AREA", "UPDATE");
        return ResponseEntity.ok(ApiResult.success(farmAreaService.update(id, request)));
    }

    /** Đổi trạng thái kích hoạt / ngừng sử dụng vùng trồng (US NCL-02-CN-005). */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResult<FarmAreaResponse>> toggleFarmAreaStatus(
            @PathVariable UUID id,
            @RequestParam boolean isActive) {
        permissionChecker.check("FARM_AREA", "UPDATE");
        return ResponseEntity.ok(ApiResult.success(farmAreaService.toggleStatus(id, isActive)));
    }

    /** Xóa vùng trồng (US NCL-02-CN-005). */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> deleteFarmArea(@PathVariable UUID id) {
        permissionChecker.check("FARM_AREA", "DELETE");
        farmAreaService.delete(id);
        return ResponseEntity.ok(ApiResult.success(null));
    }
}