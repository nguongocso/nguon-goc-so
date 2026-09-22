package vn.nguongocso.trace.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.request.CancelHandoverRequest;
import vn.nguongocso.trace.dto.request.CreateHandoverRequest;
import vn.nguongocso.trace.dto.response.HandoverResponse;
import vn.nguongocso.trace.dto.response.HandoverSummaryResponse;

/** Service xử lý nghiệp vụ phiếu bàn giao lô hàng. */
public interface ShipmentHandoverService {
    /** Tạo phiếu bàn giao mới. */
    HandoverResponse create(
        CreateHandoverRequest request
    );

    /** Hủy phiếu bàn giao. */
    HandoverResponse cancel(
        UUID id,
        CancelHandoverRequest request
    );

    /** Xác nhận nhận bàn giao. */
    HandoverResponse accept(
        UUID id
    );

    /** Từ chối nhận bàn giao. */
    HandoverResponse reject(
        UUID id,
        CancelHandoverRequest request
    );

    /** Lấy chi tiết phiếu bàn giao theo ID. */
    HandoverResponse getById(
        UUID id
    );

    /** Lấy danh sách phiếu bàn giao đã gửi. */
    List<HandoverResponse> getSentHandovers();

    /** Lấy danh sách phiếu bàn giao đã nhận. */
    List<HandoverResponse> getReceivedHandovers();

    /** Lấy danh sách phiếu bàn giao nhận của tổ chức hiện tại có phân trang. */
    PageResponse<HandoverSummaryResponse> listForCurrentOrganization(
        String status,
        String search,
        int page,
        int size
    );

    /** Lấy danh sách phiếu bàn giao của tổ chức hiện tại theo vai trò. */
    PageResponse<HandoverSummaryResponse> listForCurrentOrganization(
        String status,
        String search,
        int page,
        int size,
        CustomUserDetails currentUser
    );

    /** Lấy số lượng còn lại có thể bàn giao của lô hàng. */
    Long getRemainingQuantity(
        UUID shipmentId
    );

    /** Kiểm tra lô hàng có phiếu bàn giao đang chờ không. */
    boolean hasPendingHandover(
        UUID shipmentId
    );

    /** Lưu file chứng từ giao hàng trước khi tạo phiếu bàn giao. */
    String uploadAttachment(
        MultipartFile file
    );
}
