package vn.nguongocso.trace.recall.enums;

/**
 * Kết quả xử lý từng lô trong vụ việc thu hồi (NCL-08-CN-012).
 *
 * <p>UNRECOVERABLE = "Không thu hồi được" — bắt buộc kèm lý do và biện pháp
 * xử lý rủi ro (kiểm tra trong RecallCaseServiceImpl).</p>
 */
public enum LotResolution {
    DESTROYED,
    RETURNED,
    REPROCESSED,
    UNRECOVERABLE
}
