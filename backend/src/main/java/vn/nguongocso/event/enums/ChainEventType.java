package vn.nguongocso.event.enums;

/**
 * Danh sách các loại sự kiện trong vòng đời chuỗi cung ứng.
 *
 * @author Triệu Văn Đại
 */
public enum ChainEventType {
    HARVEST, // Thu hoạch

    /** Sơ chế, phân loại phẩm cấp và tính tỷ lệ hao hụt sau khi gọt, rửa hoặc sấy. */
    PREPROCESSING,

    PACKAGING, // Đóng gói

    TRANSPORT, // Vận chuyển

    PROCUREMENT, // Thu mua

    CORRECTION, // Sửa lỗi

    /** Nhập kho và đối chiếu số lượng thực nhận của doanh nghiệp thu mua. */
    WAREHOUSE_RECEIPT,

    STORAGE_CONDITION, // Theo dõi bảo quản

    HANDOVER, // Bàn giao

    WAREHOUSE_ENTRY, // Nhập kho HTX

    WAREHOUSE_EXIT, // Xuất kho HTX

    SPLIT, // Tách lô

    /**
     * Nhật ký canh tác ngoại tuyến, được đồng bộ qua {@code POST /chain-events/sync}
     * và chuyển tiếp đến {@code FarmLogService}.
     */
    FARM_LOG
}
