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
 * Service quản lý kết quả kiểm nghiệm của chỉ tiêu.
 */
public interface InspectionCriterionResultService {
        /**
         * Tạo hoặc cập nhật kết quả kiểm nghiệm cho một chỉ tiêu.
         */
        InspectionCriterionResultResponse recordOrUpdateResult(
                        String criterionId,
                        InspectionCriterionResultRequest request,
                        CustomUserDetails currentUser);

        /**
         * Ghi nhận toàn bộ kết quả kiểm nghiệm của một yêu cầu trong một giao dịch.
         */
        List<InspectionCriterionResultResponse> recordResults(
                        UUID inspectionRequestId,
                        List<InspectionCriterionResultRequest> requests,
                        CustomUserDetails currentUser);

        /**
         * Lấy danh sách kết quả kiểm nghiệm cho tất cả chỉ tiêu của một yêu cầu kiểm nghiệm.
         */
        List<InspectionCriterionResultResponse> getResultsByRequest(
                        UUID inspectionRequestId,
                        CustomUserDetails currentUser);

        /**
         * Lấy kết quả kiểm nghiệm cho một chỉ tiêu.
         */
        InspectionCriterionResultResponse getResultByCriterion(
                        String criterionId,
                        CustomUserDetails currentUser);

        /**
         * Xóa kết quả kiểm nghiệm.
         */
        void deleteResult(
                        String resultId,
                        CustomUserDetails currentUser);

        /**
         * Tải lên phiếu kết quả kiểm nghiệm cho một chỉ tiêu.
         */
        String uploadResultFile(
                        String criterionId,
                        MultipartFile file,
                        CustomUserDetails currentUser);

        /**
         * Lấy tệp phiếu kết quả kiểm nghiệm để xem lại.
         */
        ResultFileResource getResultFile(
                        String resultId,
                        CustomUserDetails currentUser);

        /**
         * Kiểm tra xem lô sản xuất có thể kích hoạt tem hay không dựa trên kết quả kiểm nghiệm.
         */
        CanActivateSealCheckResponse checkCanActivateSeal(
                        UUID productionLotId,
                        CustomUserDetails currentUser);

        /**
         * Lấy lịch sử kiểm nghiệm của tất cả chỉ tiêu trên một lô sản xuất.
         */
        List<CriterionHistoryResponse> getInspectionHistory(
                        UUID productionLotId,
                        CustomUserDetails currentUser);

        /**
         * Tải lên phiếu kết quả kiểm nghiệm qua token bí mật cho cổng đơn vị kiểm nghiệm (NCL-11-CN-007).
         */
        String uploadPortalResultFile(
                        String token,
                        String criterionId,
                        MultipartFile file,
                        String clientIp);

        /**
         * Ghi nhận toàn bộ kết quả kiểm nghiệm qua cổng của đơn vị kiểm nghiệm (NCL-11-CN-007).
         */
        List<InspectionCriterionResultResponse> recordPortalResults(
                        String token,
                        List<InspectionCriterionResultRequest> requests,
                        String clientIp,
                        String userAgent);

        /**
         * Record tệp phiếu kết quả kiểm nghiệm kèm thông tin phục vụ phản hồi.
         */
        record ResultFileResource(Resource resource, MediaType contentType, String fileName) {}
}