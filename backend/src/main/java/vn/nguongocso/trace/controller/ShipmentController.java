package vn.nguongocso.trace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.trace.dto.request.CreateShipmentRequest;
import vn.nguongocso.trace.dto.response.ShipmentResponse;
import vn.nguongocso.trace.service.ShipmentService;

import java.util.List;
import java.util.UUID;

/**
 * API quản lý lô hàng.
 */
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

	private final ShipmentService shipmentService;

    /**
     * Tạo lô hàng và sinh mã truy xuất.
     *
     * @param request thông tin tạo lô hàng
     * @return thông tin lô hàng
     */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResult<ShipmentResponse> createShipment(@Valid @RequestBody CreateShipmentRequest request) {

		return ApiResult.success(shipmentService.createShipment(request));
	}
	@PostMapping("/{id}/activate")
	public ApiResult<ShipmentResponse> activateStamps(@PathVariable UUID id) {
		return ApiResult.success(shipmentService.activateShipmentStamps(id));
	}

    /**
     * Lấy danh sách lô hàng của tổ chức hiện tại.
     * Chỉ trả về các lô hàng thuộc tổ chức của người dùng đang đăng nhập.
     *
     * @param userDetails thông tin người dùng hiện tại
     * @return danh sách lô hàng
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResult<List<ShipmentResponse>> getShipments(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ApiResult.success(shipmentService.getShipmentsByOrganization(userDetails.getOrganizationId()));
    }

    /**
     * Lấy thông tin chi tiết lô hàng theo id.
     * Chỉ trả về lô hàng thuộc tổ chức của người dùng đang đăng nhập.
     *
     * @param id mã lô hàng
     * @param userDetails thông tin người dùng hiện tại
     * @return thông tin lô hàng kèm danh sách mã truy xuất
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<ShipmentResponse> getShipmentById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ApiResult.success(shipmentService.getShipmentById(id, userDetails.getOrganizationId()));
    }

}

