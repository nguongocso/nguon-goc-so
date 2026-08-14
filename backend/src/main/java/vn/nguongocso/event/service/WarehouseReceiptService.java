package vn.nguongocso.event.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
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

    /**
     * Lấy danh sách sự kiện nhập kho của doanh nghiệp thu mua hiện tại.
     *
     * @param currentUser người dùng hiện tại (VT-04)
     * @param pageable    thông tin phân trang
     * @return danh sách phân trang
     */
    PageResponse<WarehouseReceiptResponse> getWarehouseReceipts(CustomUserDetails currentUser, Pageable pageable);

    /**
     * Lấy chi tiết một sự kiện nhập kho.
     *
     * @param eventId     ID của ChainEvent
     * @param currentUser người dùng hiện tại (VT-04)
     * @return chi tiết sự kiện nhập kho
     */
    WarehouseReceiptResponse getWarehouseReceiptDetail(UUID eventId, CustomUserDetails currentUser);
}