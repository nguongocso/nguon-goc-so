package vn.nguongocso.farm.enums;

/** Trạng thái vòng đời của lô sản xuất. */
public enum ProductionLotStatus {
    DRAFT, // Bản nháp

    PENDING, // Đang chờ duyệt

    APPROVED, // Đã duyệt

    REJECTED, // Bị từ chối

    HARVESTED, // Đã thu hoạch

    PREPROCESSED, // Đã sơ chế

    PACKAGED, // Đã đóng gói

    CLOSED, // Đã đóng

    RECALLED, // Đã thu hồi

    CANCELLED, // Đã hủy

    DISPOSED // Đã loại bỏ (NCL-11-CN-005: lô không đạt kiểm nghiệm bị xử lý loại bỏ — trạng thái cuối)
}
