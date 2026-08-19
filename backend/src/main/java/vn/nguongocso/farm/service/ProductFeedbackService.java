package vn.nguongocso.farm.service;

import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/** Ghi nhận và quản lý phản ánh sản phẩm. */
public interface ProductFeedbackService {
    /** Tạo phản ánh mới cho lô sản xuất (public). */
    ProductFeedbackResponse createFeedback(UUID productionLotId, CreateProductFeedbackRequest request);

    /** Lấy danh sách phản ánh (phân trang) cho nội bộ - VT-01, VT-02. */
    PageResponse<ProductFeedbackResponse> getFeedbacks(Pageable pageable);

    /** Lấy chi tiết một phản ánh. */
    ProductFeedbackResponse getFeedbackById(UUID feedbackId);
}
