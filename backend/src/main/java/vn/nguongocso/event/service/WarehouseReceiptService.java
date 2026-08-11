package vn.nguongocso.event.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.WarehouseReceiptRequest;
import vn.nguongocso.event.dto.response.WarehouseReceiptResponse;

/**
 * Service interface cho nghiệp vụ nhập kho và đối chiếu số lượng.
 *
 * @author Team
 */
public interface WarehouseReceiptService {

    /**
     * Ghi nhận sự kiện nhập kho và đối chiếu số lượng.
     *
     * @param request     yêu cầu ghi nhận nhập kho
     * @param currentUser người dùng hiện tại (VT-04 - Doanh nghiệp thu mua)
     * @return phản hồi kết quả nhập kho
     */
    WarehouseReceiptResponse recordWarehouseReceipt(WarehouseReceiptRequest request, CustomUserDetails currentUser);
}