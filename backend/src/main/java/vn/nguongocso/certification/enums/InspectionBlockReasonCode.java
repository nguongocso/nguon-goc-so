package vn.nguongocso.certification.enums;

/**
 * Mã nguyên nhân chặn tạo lô hàng / kích hoạt tem theo QTN-30
 * (NCL-11-CN-005): lô chưa đạt kiểm nghiệm không được tạo lô hàng.
 *
 * <p>
 * Đây KHÔNG phải trạng thái của lô sản xuất (ProductionLotStatus giữ
 * nguyên theo quyết định thiết kế D-3), mà là mã nguyên nhân máy đọc
 * được trả trong {@code errors.reasonCode} của response lỗi 409 CONFLICT
 * khi gate QTN-30 chặn nghiệp vụ.
 * </p>
 *
 * <p>
 * Thứ tự ưu tiên khi một lô vi phạm nhiều điều kiện đồng thời:
 * {@link #INSPECTION_FAILED} → {@link #INSPECTION_PENDING} →
 * {@link #INSPECTION_EXPIRED} → {@link #INSPECTION_MISSING}.
 * </p>
 */
public enum InspectionBlockReasonCode {

    /**
     * Lô chưa có kết quả kiểm nghiệm đạt cho mọi chỉ tiêu bắt buộc.
     */
    INSPECTION_MISSING,

    /**
     * Lô còn yêu cầu kiểm nghiệm đang chờ kết quả
     * (bao gồm cả đang chờ kiểm nghiệm lại).
     */
    INSPECTION_PENDING,

    /**
     * Kết quả kiểm nghiệm đạt mới nhất đã hết hiệu lực.
     */
    INSPECTION_EXPIRED,

    /**
     * Kết luận kiểm nghiệm mới nhất của lô là Không đạt.
     */
    INSPECTION_FAILED
}
