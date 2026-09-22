package vn.nguongocso.recall.service;

import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.recall.dto.request.ApproveRecallRequest;
import vn.nguongocso.recall.dto.request.CreateRecallRequest;
import vn.nguongocso.recall.dto.request.RejectRecallRequest;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;

/** Service quản lý yêu cầu thu hồi lô sản xuất. */
public interface RecallRequestService {
    /**
     * Tạo yêu cầu thu hồi.
     */
    RecallRequestResponse create(
        CreateRecallRequest request,
        CustomUserDetails currentUser
    );

    /**
     * Tạo yêu cầu thu hồi từ phản hồi của người tiêu dùng.
     */
    RecallRequestResponse createFromFeedback(
        ProductFeedback feedback,
        UUID shipmentId,
        String reason,
        String evidence,
        CustomUserDetails currentUser
    );

    /**
     * Lấy danh sách yêu cầu thu hồi có phân trang.
     */
    PageResponse<RecallRequestResponse> list(
        String status,
        int page,
        int size,
        CustomUserDetails currentUser
    );

    /**
     * Lấy chi tiết một yêu cầu thu hồi.
     */
    RecallRequestResponse getById(
        UUID id,
        CustomUserDetails currentUser
    );

    /**
     * Duyệt một yêu cầu thu hồi.
     */
    RecallRequestResponse approve(
        UUID id,
        ApproveRecallRequest request,
        CustomUserDetails currentUser
    );

    /**
     * Từ chối một yêu cầu thu hồi.
     */
    RecallRequestResponse reject(
        UUID id,
        RejectRecallRequest request,
        CustomUserDetails currentUser
    );
}
