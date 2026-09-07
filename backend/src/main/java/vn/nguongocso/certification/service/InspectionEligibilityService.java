package vn.nguongocso.certification.service;

import vn.nguongocso.certification.dto.response.InspectionEligibilityResult;
import vn.nguongocso.farm.entity.ProductionLot;

/**
 * Service đánh giá điều kiện kiểm nghiệm của lô sản xuất theo QTN-30
 * (NCL-11-CN-005): lô chưa đạt kiểm nghiệm không được tạo lô hàng.
 *
 * <p>
 * Đây là nguồn sự thật DUY NHẤT cho gate chặn nghiệp vụ phía backend:
 * <ul>
 * <li>{@code POST /api/v1/shipments} — chặn tạo lô hàng từ lô chưa đạt
 * (TC-01);</li>
 * <li>{@code POST /api/v1/shipments/{id}/activate} — rào chắn thứ hai
 * trước khi kích hoạt tem (QTN-21);</li>
 * <li>{@code POST /api/v1/production-lots/{id}/cancel} — chặn hủy lô khi
 * kết luận kiểm nghiệm mới nhất là Không đạt (phải dispose hoặc kiểm
 * nghiệm lại).</li>
 * </ul>
 * </p>
 */
public interface InspectionEligibilityService {

    /**
     * Đánh giá lô có đủ điều kiện tạo lô hàng / kích hoạt tem hay không.
     *
     * <p>
     * Với lô thuộc loại nông sản KHÔNG bắt buộc kiểm nghiệm
     * ({@code product_categories.requires_inspection = false}) luôn đủ
     * điều kiện. Với lô bắt buộc kiểm nghiệm, điều kiện được tính trên
     * kết quả MỚI NHẤT THEO TỪNG CHỈ TIÊU trên toàn bộ yêu cầu kiểm
     * nghiệm của lô (cùng nguồn sự thật với pre-check
     * {@code can-activate-seal} — quyết định thiết kế D-8). Vòng kiểm
     * nghiệm lại PASSED tự động thay thế vòng FAILED trước đó.
     * </p>
     *
     * @param lot lô sản xuất cần đánh giá
     * @return kết quả đánh giá; khi bị chặn chứa {@code reasonCode} và
     *         {@code message} theo contract QTN-30
     */
    InspectionEligibilityResult evaluateForShipment(ProductionLot lot);

    /**
     * Kiểm tra kết luận kiểm nghiệm HOÀN THÀNH MỚI NHẤT của lô
     * (status PASSED/FAILED) có phải là Không đạt hay không.
     *
     * <p>
     * Dùng để chặn {@code POST /production-lots/{id}/cancel} khi lô chưa
     * đạt kiểm nghiệm (QTN-30: hệ thống phải yêu cầu xử lý theo 1 trong
     * 2 hướng — loại bỏ hoặc kiểm nghiệm lại). Lô chưa có yêu cầu kiểm
     * nghiệm hoàn thành nào hoặc kết luận mới nhất là PASSED trả về
     * {@code false} (hủy như hiện tại).
     * </p>
     *
     * @param lot lô sản xuất cần kiểm tra
     * @return true nếu kết luận hoàn thành mới nhất là FAILED
     */
    boolean hasLatestFailedConclusion(ProductionLot lot);
}
