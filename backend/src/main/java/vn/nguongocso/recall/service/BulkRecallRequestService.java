package vn.nguongocso.recall.service;

import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.recall.dto.request.ApproveBulkRecallRequest;
import vn.nguongocso.recall.dto.request.CreateBulkRecallRequest;
import vn.nguongocso.recall.dto.request.RejectBulkRecallRequest;
import vn.nguongocso.recall.dto.response.BulkRecallRequestResponse;

/**
 * Dịch vụ quản lý đề nghị thu hồi hàng loạt theo phạm vi ảnh hưởng (NCL-08-CN-011).
 */
public interface BulkRecallRequestService {

    /**
     * Tạo đề nghị thu hồi hàng loạt (VT-02).
     *
     * @param request     thông tin đề nghị
     * @param currentUser người dùng hiện tại
     * @return response DTO
     */
    BulkRecallRequestResponse createBulkRecallRequest(
            CreateBulkRecallRequest request, CustomUserDetails currentUser);

    /**
     * Lấy chi tiết đề nghị thu hồi (VT-02).
     *
     * @param id          ID đề nghị
     * @param currentUser người dùng hiện tại
     * @return response DTO
     */
    BulkRecallRequestResponse getBulkRecallRequest(UUID id, CustomUserDetails currentUser);

    /**
     * Lấy danh sách đề nghị thu hồi với phân trang (VT-02).
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
     * Phê duyệt đề nghị thu hồi hàng loạt (VT-02, không phải người tạo).
     *
     * @param id          ID đề nghị
     * @param request     thông tin phê duyệt
     * @param currentUser người dùng hiện tại
     * @return response DTO
     */
    BulkRecallRequestResponse approveBulkRecallRequest(
            UUID id, ApproveBulkRecallRequest request, CustomUserDetails currentUser);

    /**
     * Từ chối đề nghị thu hồi hàng loạt (VT-02).
     *
     * @param id          ID đề nghị
     * @param request     thông tin từ chối
     * @param currentUser người dùng hiện tại
     * @return response DTO
     */
    BulkRecallRequestResponse rejectBulkRecallRequest(
            UUID id, RejectBulkRecallRequest request, CustomUserDetails currentUser);
}
