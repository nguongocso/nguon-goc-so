package vn.nguongocso.trace.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.request.CancelHandoverRequest;
import vn.nguongocso.trace.dto.request.CreateHandoverRequest;
import vn.nguongocso.trace.dto.response.HandoverResponse;
import vn.nguongocso.trace.dto.response.HandoverSummaryResponse;

/**
 * Service xử lý nghiệp vụ phiếu bàn giao lô hàng.
 */
public interface ShipmentHandoverService {

    /**
     * Tạo phiếu bàn giao mới.
     */
    HandoverResponse create(CreateHandoverRequest request);

    /**
     * Hủy phiếu bàn giao (chỉ bên giao, chỉ khi PENDING_CONFIRMATION).
     */
    HandoverResponse cancel(UUID id, CancelHandoverRequest request);

    /**
     * Xác nhận nhận bàn giao (chỉ tổ chức nhận, chỉ khi PENDING_CONFIRMATION).
     */
    HandoverResponse accept(UUID id);

    /**
     * Từ chối nhận bàn giao (chỉ tổ chức nhận, chỉ khi PENDING_CONFIRMATION).
     */
    HandoverResponse reject(UUID id, CancelHandoverRequest request);

    /**
     * Lấy chi tiết phiếu bàn giao theo ID.
     * Trả 403 nếu người gọi không thuộc sender/receiver org.
     */
    HandoverResponse getById(UUID id);

    /**
     * Lấy danh sách phiếu bàn giao đã gửi (từ tổ chức hiện tại).
     */
    List<HandoverResponse> getSentHandovers();

    /**
     * Lấy danh sách phiếu bàn giao đã nhận (đến tổ chức hiện tại).
     */
    List<HandoverResponse> getReceivedHandovers();

    /**
     * Lấy danh sách phiếu bàn giao nhận của tổ chức hiện tại có phân trang và tìm kiếm.
     */
    PageResponse<HandoverSummaryResponse> listForCurrentOrganization(String status, String search, int page, int size);

    /**
     * Lấy danh sách phiếu bàn giao của tổ chức hiện tại theo vai trò (VT-02: đã gửi, VT-04: đã nhận).
     */
    PageResponse<HandoverSummaryResponse> listForCurrentOrganization(String status, String search, int page, int size, vn.nguongocso.auth.service.CustomUserDetails currentUser);

    /**
     * Lấy số lượng còn lại có thể bàn giao của lô hàng.
     */
    Long getRemainingQuantity(UUID shipmentId);

    /**
     * Kiểm tra lô hàng có phiếu bàn giao đang chờ không.
     */
    boolean hasPendingHandover(UUID shipmentId);

    /**
     * Lưu file chứng từ giao hàng trước khi tạo phiếu bàn giao.
     *
     * @param file File đính kèm (JPG/PNG/PDF, tối đa 5MB)
     * @return Đường dẫn file đã lưu, gửi kèm trong attachmentPath khi tạo phiếu
     */
    String uploadAttachment(MultipartFile file);
}
