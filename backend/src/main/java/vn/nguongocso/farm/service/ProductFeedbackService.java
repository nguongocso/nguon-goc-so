package vn.nguongocso.farm.service;

import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.request.AssignProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CloseProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRecallRequest;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.UpdateProductFeedbackProcessingRequest;
import vn.nguongocso.farm.dto.response.PublicProductFeedbackCreatedResponse;
import vn.nguongocso.farm.dto.response.PublicProductFeedbackLookupResponse;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/** Ghi nhận và quản lý phản ánh sản phẩm. */
public interface ProductFeedbackService {
    /** Tạo phản ánh mới cho lô sản xuất (public). */
    PublicProductFeedbackCreatedResponse createFeedback(UUID productionLotId, CreateProductFeedbackRequest request);

    /** Tra cứu trạng thái và phản hồi công khai bằng mã được cấp khi gửi phản ánh. */
    PublicProductFeedbackLookupResponse lookupPublicFeedback(String lookupCode);

    /** Lấy danh sách phản ánh (phân trang) cho nội bộ - VT-01, VT-02. */
    PageResponse<ProductFeedbackResponse> getFeedbacks(
            String keyword,
            ProductFeedbackStatus status,
            ProductFeedbackSeverity severity,
            UUID productionLotId,
            UUID assignedToUserId,
            Pageable pageable);

    /** Lấy chi tiết một phản ánh. */
    ProductFeedbackResponse getFeedbackById(UUID feedbackId);

    ProductFeedbackResponse assign(UUID feedbackId, AssignProductFeedbackRequest request);

    ProductFeedbackResponse updateProcessing(UUID feedbackId, UpdateProductFeedbackProcessingRequest request);

    ProductFeedbackResponse close(UUID feedbackId, CloseProductFeedbackRequest request);

    RecallRequestResponse createRecallRequest(UUID feedbackId, CreateProductFeedbackRecallRequest request);
}
