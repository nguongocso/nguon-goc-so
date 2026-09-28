package vn.nguongocso.trace.recall.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;
import vn.nguongocso.trace.recall.dto.response.RecallCaseResponse;

/** Service quản lý vụ việc thu hồi. */
public interface RecallCaseService {
    /** Lấy danh sách vụ việc thu hồi của tổ chức hiện tại. */
    List<RecallCaseResponse> list(CustomUserDetails currentUser);

    /** Lấy chi tiết một vụ việc thu hồi thuộc tổ chức hiện tại. */
    RecallCaseResponse getById(UUID id, CustomUserDetails currentUser);

    /** Kết thúc và đóng vụ việc thu hồi. */
    RecallCaseResponse close(UUID id, CloseRecallCaseRequest request, CustomUserDetails currentUser);
}
