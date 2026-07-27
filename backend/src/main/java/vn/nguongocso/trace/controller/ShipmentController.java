package vn.nguongocso.trace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.service.ChainEventService;
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
	private final ChainEventService chainEventService;

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
	 * Xem dòng sự kiện truy xuất của lô hàng.
	 *
	 * Chỉ Quản lý HTX (VT-02) thuộc tổ chức sở hữu lô hàng được phép xem.
	 *
	 * @param id          id lô hàng
	 * @param currentUser người dùng hiện tại
	 * @return danh sách dòng sự kiện theo thời gian
	 */
	@GetMapping("/{id}/chain-events")
	@PreAuthorize("hasRole('VT-02')")
	public ApiResult<List<ChainEventResponse>> getShipmentTimeLine(
			@PathVariable UUID id,
			@AuthenticationPrincipal CustomUserDetails currentUser) {

		List<ChainEventResponse> response = chainEventService.getShipmentTimeLine(id, currentUser);

		return ApiResult.success(response);
	}

}
