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
import vn.nguongocso.trace.dto.response.ShipmentResponse;
import vn.nguongocso.trace.dto.response.ProcurementShipmentResponse;
import vn.nguongocso.trace.dto.response.ShipmentSummaryResponse;
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
	 * Lấy danh sách lô hàng theo ID lô sản xuất.
	 *
	 * @param productionLotId ID của lô sản xuất
	 * @return danh sách lô hàng
	 */
	@GetMapping("/production-lots/{productionLotId}")
	@PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03')")
	public ApiResult<List<ShipmentResponse>> getShipmentsByProductionLot(@PathVariable UUID productionLotId) {

		return ApiResult.success(shipmentService.getShipmentsByProductionLot(productionLotId));
	}

	/**
	 * Lấy danh sách lô hàng theo ID lô sản xuất với phân trang.
	 *
	 * @param productionLotId ID của lô sản xuất
	 * @param page            số trang (mặc định 0)
	 * @param size            số bản ghi trên mỗi trang (mặc định 10)
	 * @return danh sách lô hàng phân trang
	 */
	@GetMapping("/production-lots/{productionLotId}/paged")
	@PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03')")
	public ApiResult<PageResponse<ShipmentResponse>> getShipmentsByProductionLotPaged(
			@PathVariable UUID productionLotId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		return ApiResult.success(
				shipmentService.getShipmentsByProductionLotPaged(productionLotId, page, size));
	}

	/**
	 * Tra cứu lô hàng bằng mã truy xuất (codeValue in trên tem QR).
	 * Dùng bởi VT-04 để xác nhận lô hàng trước khi ghi sự kiện thu mua.
	 */
	@GetMapping("/by-code")
	public ApiResult<ShipmentSummaryResponse> getShipmentByCode(@RequestParam String code) {

		return ApiResult.success(shipmentService.getShipmentByCode(code));
	}

	/**
	 * Lấy danh sách lô hàng đủ điều kiện thu mua (status = ACTIVATED).
	 * Dùng cho Doanh nghiệp thu mua (VT‑04).
	 */
	@GetMapping("/eligible")
	public ApiResult<List<ProcurementShipmentResponse>> getEligibleShipments() {

		return ApiResult.success(shipmentService.getEligibleShipments());
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
}