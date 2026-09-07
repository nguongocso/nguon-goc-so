package vn.nguongocso.trace.service;

import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.request.ApproveSupplementRequest;
import vn.nguongocso.trace.dto.request.CreateSupplementRequest;
import vn.nguongocso.trace.dto.request.RejectSupplementRequest;
import vn.nguongocso.trace.dto.response.CodeRangeSupplementResponse;

/**
 * Dịch vụ quản lý yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007).
 */
public interface CodeRangeSupplementService {

    /** VT-02 tạo yêu cầu cấp bổ sung cho tổ chức của mình. */
    CodeRangeSupplementResponse create(CreateSupplementRequest request, CustomUserDetails currentUser);

    /** VT-01 xem tất cả yêu cầu, lọc theo trạng thái, phân trang. */
    PageResponse<CodeRangeSupplementResponse> list(String status, int page, int size, CustomUserDetails currentUser);

    /** VT-02 xem yêu cầu của tổ chức mình, lọc theo trạng thái, phân trang. */
    PageResponse<CodeRangeSupplementResponse> listMine(String status, int page, int size, CustomUserDetails currentUser);

    /** Xem chi tiết một yêu cầu (VT-01 xem tất cả, VT-02 chỉ xem của tổ chức mình). */
    CodeRangeSupplementResponse getById(UUID id, CustomUserDetails currentUser);

    /** VT-01 duyệt toàn bộ hoặc một phần (tăng hạn mức dải mã hiện có). */
    CodeRangeSupplementResponse approve(UUID id, ApproveSupplementRequest request, CustomUserDetails currentUser);

    /** VT-01 từ chối kèm lý do bắt buộc. */
    CodeRangeSupplementResponse reject(UUID id, RejectSupplementRequest request, CustomUserDetails currentUser);
}
