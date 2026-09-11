package vn.nguongocso.trace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.request.CreateShipmentRequest;
import vn.nguongocso.trace.dto.request.SplitShipmentRequest;
import vn.nguongocso.trace.dto.response.SplitPreviewResponse;
import vn.nguongocso.trace.dto.response.SplitShipmentResponse;
import vn.nguongocso.trace.dto.response.ShipmentResponse;
import vn.nguongocso.trace.dto.response.ProcurementShipmentResponse;
import vn.nguongocso.trace.dto.response.ShipmentSummaryResponse;
import vn.nguongocso.trace.service.ShipmentHandoverService;
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
	private final PermissionChecker permissionChecker;
	private final ShipmentHandoverService handoverService;

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

	/**
	 * Kích hoạt lô hàng và các mã truy xuất.
	 *
	 * @param id ID của lô hàng
	 * @return thông tin lô hàng đã kích hoạt
	 */
	@PostMapping("/{id}/activate")
	public ApiResult<ShipmentResponse> activateStamps(@PathVariable UUID id) {

		return ApiResult.success(shipmentService.activateShipmentStamps(id));
	}

	/**
	 * Tra cứu lô hàng bằng mã truy xuất (codeValue in trên tem QR).
	 * Dùng bởi VT-04 để xác nhận lô hàng trước khi ghi sự kiện thu mua.
	 *
	 * @param code mã truy xuất
	 * @return thông tin tóm tắt của lô hàng
	 */
	@GetMapping("/by-code")
	public ApiResult<ShipmentSummaryResponse> getShipmentByCode(@RequestParam String code) {

		return ApiResult.success(shipmentService.getShipmentByCode(code));
	}

	/**
	 * Lấy danh sách lô hàng liên quan tới Doanh nghiệp thu mua (lô đã thu mua,
	 * được bàn giao hoặc đã nhập kho). Chỉ VT‑04 được sử dụng.
	 */
	@GetMapping("/eligible")
	@PreAuthorize("hasRole('VT-04')")
	public ApiResult<List<ProcurementShipmentResponse>> getEligibleShipments() {

		return ApiResult.success(shipmentService.getEligibleShipments());
	}

	@GetMapping("/{shipmentId}/split-preview")
	public ApiResult<SplitPreviewResponse> getSplitPreview(@PathVariable UUID shipmentId) {
		return ApiResult.success(shipmentService.getSplitPreview(shipmentId));
	}

	@PostMapping("/{shipmentId}/split")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResult<SplitShipmentResponse> splitShipment(@PathVariable UUID shipmentId,
			@Valid @RequestBody SplitShipmentRequest request) {
		return ApiResult.success(HttpStatus.CREATED.value(), shipmentService.splitShipment(shipmentId, request));
	}

	/**
	 * Lấy chi tiết lô hàng theo ID.
	 *
	 * @param id ID của lô hàng
	 * @return chi tiết lô hàng
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05')")
	public ApiResult<ShipmentResponse> getShipmentById(@PathVariable UUID id) {

		return ApiResult.success(shipmentService.getShipmentById(id));
	}

	@GetMapping("/{id}/remaining-handover-quantity")
	public ApiResult<Long> getRemainingHandoverQuantity(@PathVariable UUID id) {
		return ApiResult.success(handoverService.getRemainingQuantity(id));
	}

	/**
	 * Kiểm tra lô hàng có phiếu bàn giao đang chờ xác nhận hay không.
	 * Frontend dùng để hiển thị nhãn "Đang bàn giao" trong thời gian chờ
	 * (NCL-05-CN-008). Trạng thái derived từ phiếu PENDING_CONFIRMATION,
	 * không phải cột mới trên lô hàng.
	 *
	 * @param id ID của lô hàng
	 * @return true khi tồn tại phiếu đang chờ xác nhận
	 */
	@GetMapping("/{id}/has-pending-handover")
	public ApiResult<Boolean> hasPendingHandover(@PathVariable UUID id) {
		return ApiResult.success(handoverService.hasPendingHandover(id));
	}
}
