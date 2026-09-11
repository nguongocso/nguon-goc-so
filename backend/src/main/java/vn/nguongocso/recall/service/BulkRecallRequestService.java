package vn.nguongocso.recall.service;

import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.recall.dto.request.ApproveBulkRecallRequest;
import vn.nguongocso.recall.dto.request.CreateBulkRecallRequest;
import vn.nguongocso.recall.dto.request.RejectBulkRecallRequest;
import vn.nguongocso.recall.dto.response.BulkRecallRequestResponse;
import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;

/**
 * Dịch vụ quản lý yêu cầu thu hồi hàng loạt theo phạm vi ảnh hưởng (NCL-08-CN-011, NCL-08-CN-012).
 */
public interface BulkRecallRequestService {

    /**
     * Tạo yêu cầu thu hồi hàng loạt (VT-02).
     *
     * @param request     thông tin yêu cầu
     * @param currentUser người dùng hiện tại
     * @return response DTO
     */
    BulkRecallRequestResponse createBulkRecallRequest(
            CreateBulkRecallRequest request, CustomUserDetails currentUser);

    /**
     * Lấy chi tiết yêu cầu thu hồi (VT-02).
     *
     * @param id          ID yêu cầu
     * @param currentUser người dùng hiện tại
     * @return response DTO
     */
    BulkRecallRequestResponse getBulkRecallRequest(UUID id, CustomUserDetails currentUser);

    /**
     * Lấy danh sách yêu cầu thu hồi với phân trang (VT-02).
     *
     * @param status      trạng thái lọc (có thể null)
     * @param page        trang hiện tại
     * @param size        số lượng mỗi trang
     * @param currentUser người dùng hiện tại
     * @return danh sách phân trang
     */
    PageResponse<BulkRecallRequestResponse> listBulkRecallRequests(
            String status, int page, int size, CustomUserDetails currentUser);

    /**
     * Phê duyệt yêu cầu thu hồi hàng loạt (VT-02, không phải người tạo).
     *
     * @param id          ID yêu cầu
     * @param request     thông tin phê duyệt
     * @param currentUser người dùng hiện tại
     * @return response DTO
     */
    BulkRecallRequestResponse approveBulkRecallRequest(
            UUID id, ApproveBulkRecallRequest request, CustomUserDetails currentUser);

    /**
     * Từ chối yêu cầu thu hồi hàng loạt (VT-02).
     *
     * @param id          ID yêu cầu
     * @param request     thông tin từ chối
     * @param currentUser người dùng hiện tại
     * @return response DTO
     */
    BulkRecallRequestResponse rejectBulkRecallRequest(
            UUID id, RejectBulkRecallRequest request, CustomUserDetails currentUser);

    /**
     * Kết thúc vụ việc thu hồi gắn liền với yêu cầu thu hồi đã duyệt (NCL-08-CN-012).
     *
     * @param id          ID yêu cầu thu hồi hàng loạt
     * @param request     thông tin kết thúc vụ việc và kết quả xử lý từng lô
     * @param currentUser người dùng hiện tại
     * @return response DTO với trạng thái COMPLETED
     */
    BulkRecallRequestResponse closeBulkRecallRequest(
            UUID id, CloseRecallCaseRequest request, CustomUserDetails currentUser);

    /**
     * Tải lên tệp biên bản đính kèm vụ việc thu hồi (hỗ trợ PDF, Word .docx, .doc).
     *
     * @param file        tệp biên bản tải lên
     * @param currentUser người dùng hiện tại
     * @return thông tin tệp biên bản đã lưu
     */
    vn.nguongocso.recall.dto.response.RecallEvidenceResponse uploadEvidenceFile(
            org.springframework.web.multipart.MultipartFile file, CustomUserDetails currentUser);

    /**
     * Dữ liệu tệp biên bản gồm Resource, tên tệp gốc và MIME type.
     */
    record EvidenceFileContent(
            org.springframework.core.io.Resource resource,
            String fileName,
            String contentType
    ) {}

    /**
     * Lấy tài nguyên tệp biên bản để tải về hoặc xem trực tiếp trên tab mới.
     *
     * @param fileId      ID tệp biên bản
     * @param currentUser người dùng hiện tại
     * @return tài nguyên tệp kèm thông tin tên tệp và content type
     */
    EvidenceFileContent getEvidenceFile(UUID fileId, CustomUserDetails currentUser);
}
