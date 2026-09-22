package vn.nguongocso.farm.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.request.AssignProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CloseProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRecallRequest;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.UpdateProductFeedbackProcessingRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.dto.response.PublicProductFeedbackCreatedResponse;
import vn.nguongocso.farm.dto.response.PublicProductFeedbackLookupResponse;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;

/**
 * Nghiệp vụ phản ánh sản phẩm.
*/
public interface ProductFeedbackService {
    /** Tạo phản ánh cho lô sản xuất. */
    PublicProductFeedbackCreatedResponse createFeedback(UUID productionLotId, CreateProductFeedbackRequest request);

    /** Tra cứu phản ánh công khai theo mã. */
    PublicProductFeedbackLookupResponse lookupPublicFeedback(String lookupCode);

    /** Lấy danh sách phản ánh. */
    PageResponse<ProductFeedbackResponse> getFeedbacks(
            String keyword,
            ProductFeedbackStatus status,
            ProductFeedbackSeverity severity,
            UUID productionLotId,
            UUID assignedToUserId,
            Pageable pageable);

    /** Lấy chi tiết phản ánh. */
    ProductFeedbackResponse getFeedbackById(UUID feedbackId);

    /** Gán phản ánh cho nhân viên xử lý. */
    ProductFeedbackResponse assign(UUID feedbackId, AssignProductFeedbackRequest request);

    /** Cập nhật tiến độ xử lý phản ánh. */
    ProductFeedbackResponse updateProcessing(UUID feedbackId, UpdateProductFeedbackProcessingRequest request);

    /** Đóng phản ánh. */
    ProductFeedbackResponse close(UUID feedbackId, CloseProductFeedbackRequest request);

    /** Tạo yêu cầu thu hồi từ phản ánh. */
    RecallRequestResponse createRecallRequest(UUID feedbackId, CreateProductFeedbackRecallRequest request);
}
