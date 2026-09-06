package vn.nguongocso.certification.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionResultRequest;
import vn.nguongocso.certification.dto.response.CanActivateSealCheckResponse;
import vn.nguongocso.certification.dto.response.CriterionHistoryResponse;
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
     * Ghi nhận toàn bộ kết quả kiểm nghiệm của một yêu cầu trong một giao dịch.
     *
     * <p>
     * Toàn bộ payload được validate trước khi lưu; nếu có bất kỳ chỉ tiêu nào
     * không hợp lệ thì không có kết quả nào được ghi (all-or-nothing).
     * Trạng thái của yêu cầu chỉ được tính một lần sau khi toàn bộ kết quả
     * hợp lệ đã được lưu.
     * </p>
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @param requests            Danh sách kết quả cho tất cả chỉ tiêu của yêu cầu.
     * @param currentUser         Thông tin người dùng hiện tại.
     * @return Danh sách kết quả kiểm nghiệm đã lưu.
     */
    List<InspectionCriterionResultResponse> recordResults(
            UUID inspectionRequestId,
            List<InspectionCriterionResultRequest> requests,
            CustomUserDetails currentUser);

    /**
     * Lấy danh sách kết quả kiểm nghiệm cho tất cả chỉ tiêu của một yêu cầu kiểm nghiệm.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @param currentUser         Thông tin người dùng hiện tại.
     * @return Danh sách kết quả kiểm nghiệm.
     */
    List<InspectionCriterionResultResponse> getResultsByRequest(
            UUID inspectionRequestId,
            CustomUserDetails currentUser);

    /**
     * Lấy kết quả kiểm nghiệm cho một chỉ tiêu.
     *
     * @param criterionId ID của chỉ tiêu kiểm nghiệm.
     * @param currentUser Thông tin người dùng hiện tại.
     * @return DTO phản hồi kết quả kiểm nghiệm.
     */
    InspectionCriterionResultResponse getResultByCriterion(
            String criterionId,
            CustomUserDetails currentUser);

    /**
     * Xóa kết quả kiểm nghiệm.
     *
     * @param resultId    ID của kết quả kiểm nghiệm.
     * @param currentUser Thông tin người dùng hiện tại.
     */
    void deleteResult(String resultId, CustomUserDetails currentUser);

    /**
     * Tải lên phiếu kết quả kiểm nghiệm cho một chỉ tiêu.
     *
     * @param criterionId ID của chỉ tiêu kiểm nghiệm.
     * @param file        Tệp phiếu kết quả (JPG/PNG/PDF).
     * @param currentUser Thông tin người dùng hiện tại.
     * @return Đường dẫn tệp đã lưu (filePath).
     */
    String uploadResultFile(
            String criterionId,
            MultipartFile file,
            CustomUserDetails currentUser);

    /**
     * Lấy tệp phiếu kết quả kiểm nghiệm để xem lại.
     *
     * @param resultId    ID của kết quả kiểm nghiệm.
     * @param currentUser Thông tin người dùng hiện tại.
     * @return Resource tệp kèm MediaType và tên file.
     */
    ResultFileResource getResultFile(
            String resultId,
            CustomUserDetails currentUser);

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

    /**
     * Lấy lịch sử kiểm nghiệm của tất cả chỉ tiêu trên một lô sản xuất.
     *
     * <p>
     * Trả về danh sách theo chỉ tiêu, mỗi phần tử chứa dòng thời gian kết
     * quả từ cũ đến mới. Entry cuối cùng trong {@code history} là kết quả
     * hiện tại có hiệu lực của chỉ tiêu đó.
     * </p>
     *
     * @param productionLotId ID lô sản xuất.
     * @param currentUser     Người dùng đã xác thực (kiểm tra phạm vi tổ chức).
     * @return Danh sách lịch sử kiểm nghiệm theo chỉ tiêu.
     */
    List<CriterionHistoryResponse> getInspectionHistory(
            UUID productionLotId,
            CustomUserDetails currentUser);

    /**
     * Tệp phiếu kết quả kiểm nghiệm kèm thông tin phục vụ response.
     */
    record ResultFileResource(
            Resource resource,
            MediaType contentType,
            String fileName) {
    }
}