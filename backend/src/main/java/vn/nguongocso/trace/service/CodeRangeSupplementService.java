package vn.nguongocso.trace.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.request.ApproveSupplementRequest;
import vn.nguongocso.trace.dto.request.CreateSupplementRequest;
import vn.nguongocso.trace.dto.request.RejectSupplementRequest;
import vn.nguongocso.trace.dto.response.CodeRangeSupplementResponse;
import vn.nguongocso.trace.dto.response.EvidenceEventResponse;

/** Service quản lý yêu cầu cấp bổ sung dải mã truy xuất. */
public interface CodeRangeSupplementService {
    /** Tạo yêu cầu cấp bổ sung cho tổ chức. */
    CodeRangeSupplementResponse create(
        CreateSupplementRequest request,
        CustomUserDetails currentUser
    );

    /** Lấy danh sách sự kiện bằng chứng sản lượng thực. */
    List<EvidenceEventResponse> listEvidenceEvents(
        CustomUserDetails currentUser
    );

    /** Lấy danh sách tất cả yêu cầu cấp bổ sung có phân trang. */
    PageResponse<CodeRangeSupplementResponse> list(
        String status,
        int page,
        int size,
        CustomUserDetails currentUser
    );

    /** Lấy danh sách yêu cầu cấp bổ sung của tổ chức có phân trang. */
    PageResponse<CodeRangeSupplementResponse> listMine(
        String status,
        int page,
        int size,
        CustomUserDetails currentUser
    );

    /** Lấy chi tiết một yêu cầu cấp bổ sung. */
    CodeRangeSupplementResponse getById(
        UUID id,
        CustomUserDetails currentUser
    );

    /** Phê duyệt yêu cầu cấp bổ sung dải mã. */
    CodeRangeSupplementResponse approve(
        UUID id,
        ApproveSupplementRequest request,
        CustomUserDetails currentUser
    );

    /** Từ chối yêu cầu cấp bổ sung dải mã. */
    CodeRangeSupplementResponse reject(
        UUID id,
        RejectSupplementRequest request,
        CustomUserDetails currentUser
    );
}
