package vn.nguongocso.recall.service;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.recall.dto.request.ApproveBulkRecallRequest;
import vn.nguongocso.recall.dto.request.CreateBulkRecallRequest;
import vn.nguongocso.recall.dto.request.RejectBulkRecallRequest;
import vn.nguongocso.recall.dto.response.BulkRecallRequestResponse;
import vn.nguongocso.recall.dto.response.RecallEvidenceResponse;
import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;

/** Service quản lý yêu cầu thu hồi hàng loạt. */
public interface BulkRecallRequestService {
    /** Tạo yêu cầu thu hồi hàng loạt. */
    BulkRecallRequestResponse createBulkRecallRequest(
        CreateBulkRecallRequest request,
        CustomUserDetails currentUser
    );

    /** Lấy chi tiết yêu cầu thu hồi. */
    BulkRecallRequestResponse getBulkRecallRequest(
        UUID id,
        CustomUserDetails currentUser
    );

    /** Lấy danh sách yêu cầu thu hồi có phân trang. */
    PageResponse<BulkRecallRequestResponse> listBulkRecallRequests(
        String status,
        int page,
        int size,
        CustomUserDetails currentUser
    );

    /** Phê duyệt yêu cầu thu hồi hàng loạt. */
    BulkRecallRequestResponse approveBulkRecallRequest(
        UUID id,
        ApproveBulkRecallRequest request,
        CustomUserDetails currentUser
    );

    /** Từ chối yêu cầu thu hồi hàng loạt. */
    BulkRecallRequestResponse rejectBulkRecallRequest(
        UUID id,
        RejectBulkRecallRequest request,
        CustomUserDetails currentUser
    );

    /** Kết thúc vụ việc thu hồi gắn liền với yêu cầu thu hồi đã duyệt. */
    BulkRecallRequestResponse closeBulkRecallRequest(
        UUID id,
        CloseRecallCaseRequest request,
        CustomUserDetails currentUser
    );

    /** Tải lên tệp biên bản đính kèm vụ việc thu hồi. */
    RecallEvidenceResponse uploadEvidenceFile(
        MultipartFile file,
        CustomUserDetails currentUser
    );

    /** Dữ liệu tệp biên bản gồm Resource, tên tệp gốc và MIME type. */
    record EvidenceFileContent(
        Resource resource,
        String fileName,
        String contentType
    ) {}

    /** Lấy tài nguyên tệp biên bản để tải về hoặc xem trực tiếp. */
    EvidenceFileContent getEvidenceFile(
        UUID fileId,
        CustomUserDetails currentUser
    );
}
