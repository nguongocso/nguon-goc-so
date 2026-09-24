package vn.nguongocso.event.enums;

/** Danh sách các loại sự kiện trong vòng đời chuỗi cung ứng. */
public enum ChainEventType {
    HARVEST, // Thu hoạch

    PREPROCESSING, // Sơ chế

    PACKAGING, // Đóng gói

    TRANSPORT, // Vận chuyển

    PROCUREMENT, // Thu mua

    CORRECTION, // Sửa lỗi

    WAREHOUSE_RECEIPT, // Nhập kho

    STORAGE_CONDITION, // Theo dõi bảo quản

    HANDOVER, // Bàn giao

    WAREHOUSE_ENTRY, // Nhập kho HTX

    WAREHOUSE_EXIT, // Xuất kho HTX

    SPLIT, // Tách lô

    FARM_LOG // Nhật ký canh tác
}
