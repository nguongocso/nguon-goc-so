package vn.nguongocso.certification.enums;

/**
 * Mã nguyên nhân chặn tạo lô hàng / kích hoạt tem theo QTN-30
 * (NCL-11-CN-005): lô chưa đạt kiểm nghiệm không được tạo lô hàng.
 */
public enum InspectionBlockReasonCode {
    INSPECTION_MISSING, // Lô chưa có kết quả kiểm nghiệm đạt cho mọi chỉ tiêu bắt buộc.

    INSPECTION_PENDING, // Lô còn yêu cầu kiểm nghiệm đang chờ kết quả.

    INSPECTION_EXPIRED, // Kết quả kiểm nghiệm đạt mới nhất đã hết hiệu lực.

    INSPECTION_FAILED // Kết luận kiểm nghiệm mới nhất của lô là Không đạt.
}
