package vn.nguongocso.certification.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionResultRequest;
import vn.nguongocso.certification.dto.response.CanActivateSealCheckResponse;
import vn.nguongocso.certification.dto.response.InspectionCriterionResultResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service cho kết quả kiểm nghiệm.
 */
public interface InspectionCriterionResultService {

    /**
     * Tạo/cập nhật kết quả kiểm nghiệm cho một chỉ tiêu.
     *
     * @param criterionId ID của chỉ tiêu kiểm nghiệm.
     * @param request     DTO chứa thông tin kết quả.
     * @param currentUser Thông tin người dùng hiện tại.
     * @return DTO phản hồi kết quả kiểm nghiệm.
     */
    InspectionCriterionResultResponse recordOrUpdateResult(
            String criterionId,
            InspectionCriterionResultRequest request,
            CustomUserDetails currentUser);

    /**
     * Lấy danh sách kết quả kiểm nghiệm cho tất cả chỉ tiêu của một yêu cầu kiểm nghiệm.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @return Danh sách kết quả kiểm nghiệm.
     */
    List<InspectionCriterionResultResponse> getResultsByRequest(UUID inspectionRequestId);

    /**
     * Lấy kết quả kiểm nghiệm cho một chỉ tiêu.
     *
     * @param criterionId ID của chỉ tiêu kiểm nghiệm.
     * @return DTO phản hồi kết quả kiểm nghiệm.
     */
    InspectionCriterionResultResponse getResultByCriterion(String criterionId);

    /**
     * Xóa kết quả kiểm nghiệm.
     *
     * @param resultId ID của kết quả kiểm nghiệm.
     */
    void deleteResult(String resultId);

    /**
     * Kiểm tra xem lô sản xuất có thể kích hoạt tem hay không dựa trên kết quả kiểm nghiệm.
     *
     * @param productionLotId ID của lô sản xuất.
     * @param currentUser     Thông tin người dùng hiện tại.
     * @return Phản hồi chứa trạng thái kích hoạt và lý do nếu có.
     */
    CanActivateSealCheckResponse checkCanActivateSeal(
            UUID productionLotId,
            CustomUserDetails currentUser);
}
