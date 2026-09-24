package vn.nguongocso.trace.recall.enums;

/** Kết quả xử lý từng lô trong vụ việc thu hồi. */
public enum LotResolution {
    DESTROYED, // Tiêu hủy

    RETURNED, // Trả lại

    REPROCESSED, // Tái chế hoặc chế biến lại

    UNRECOVERABLE // Không thu hồi được
}
