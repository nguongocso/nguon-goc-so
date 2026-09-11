package vn.nguongocso.trace.recall.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;
import vn.nguongocso.trace.recall.dto.response.RecallCaseResponse;

import java.util.List;
import java.util.UUID;

/**
 * Dịch vụ quản lý vụ việc thu hồi (NCL-08-CN-012).
 */
public interface RecallCaseService {

    /** Danh sách vụ việc của tổ chức hiện tại (kèm lazy materialize). */
    List<RecallCaseResponse> list(CustomUserDetails currentUser);

    /** Chi tiết một vụ việc thuộc tổ chức hiện tại. */
    RecallCaseResponse getById(UUID id, CustomUserDetails currentUser);

    /** Đóng vụ việc thu hồi (chỉ VT-02 cùng tổ chức sở hữu). */
    RecallCaseResponse close(UUID id, CloseRecallCaseRequest request, CustomUserDetails currentUser);
}
