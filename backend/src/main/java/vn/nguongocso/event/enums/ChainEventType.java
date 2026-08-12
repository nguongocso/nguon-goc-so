package vn.nguongocso.event.enums;

/**
 * Danh sách các loại sự kiện trong vòng đời chuỗi cung ứng.
 * 
 * @author Triệu Văn Đại
 */

public enum ChainEventType {
    // Sự kiện thu hoạch
    HARVEST, // Thu hoạch

    // Sự kiện đóng gói - Sản phẩm được đóng gói và dán nhãn.
    PACKAGING, // Đóng gói

    // Sự kiện vận chuyển - Sản phẩm được di chuyển giữa các địa điểm.
    TRANSPORT, // Vận chuyển

    // Sự kiện thu mua - Sản phẩm được mua hoặc tiếp nhận.
    PROCUREMENT, // Thu mua

    // Sự kiện sửa lỗi - Điều chỉnh hoặc sửa dữ liệu sự kiện trước đó.
    CORRECTION // Sửa lỗi
}
