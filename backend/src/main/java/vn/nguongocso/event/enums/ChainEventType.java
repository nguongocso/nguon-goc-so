package vn.nguongocso.event.enums;

/**
 * Danh sách các loại sự kiện trong vòng đời chuỗi cung ứng.
 *
 * @author Triệu Văn Đại
 */

public enum ChainEventType {
    // Sự kiện thu hoạch
    HARVEST, // Thu hoạch

    // Sự kiện sơ chế và phân loại - Sản phẩm được gọt, rửa, sấy, phân loại phẩm cấp và tính tỷ lệ hao hụt.
    PREPROCESSING, // Sơ chế và phân loại

    // Sự kiện đóng gói - Sản phẩm được đóng gói và dán nhãn.
    PACKAGING, // Đóng gói

    // Sự kiện vận chuyển - Sản phẩm được di chuyển giữa các địa điểm.
    TRANSPORT, // Vận chuyển

    // Sự kiện thu mua - Sản phẩm được mua hoặc tiếp nhận.
    PROCUREMENT, // Thu mua

    // Sự kiện sửa lỗi - Điều chỉnh hoặc sửa dữ liệu sự kiện trước đó.
    CORRECTION, // Sửa lỗi

    // Sự kiện nhập kho và đối chiếu số lượng - Doanh nghiệp thu mua ghi nhận số lượng thực nhận.
    WAREHOUSE_RECEIPT, // Nhập kho

    // Sự kiện theo dõi điều kiện bảo quản khi vận chuyển.
    STORAGE_CONDITION // Theo dõi bảo quản
}