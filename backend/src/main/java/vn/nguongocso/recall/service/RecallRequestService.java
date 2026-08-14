package vn.nguongocso.recall.service;

import java.util.UUID;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.recall.dto.request.*;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;

/**
 * Dịch vụ quản lý yêu cầu thu hồi lô sản xuất (NCL-08-CN-008).
 */
public interface RecallRequestService {

    /** Tạo yêu cầu thu hồi (VT-03). */
    RecallRequestResponse create(CreateRecallRequest request, CustomUserDetails currentUser);

    /** Lấy danh sách yêu cầu thu hồi the filtered trạng thái, phân trang (VT-02). */
    PageResponse<RecallRequestResponse> list(String status, int page, int size, CustomUserDetails currentUser);

    /** Lấy chi tiết một yêu cầu thu hồi (VT-02). */
    RecallRequestResponse getById(UUID id, CustomUserDetails currentUser);

    /** Duyệt một yêu cầu thu hồi (VT-02). */
    RecallRequestResponse approve(UUID id, ApproveRecallRequest request, CustomUserDetails currentUser);

    /** Từ chối một yêu cầu thu hồi (VT-02). */
    RecallRequestResponse reject(UUID id, RejectRecallRequest request, CustomUserDetails currentUser);
}