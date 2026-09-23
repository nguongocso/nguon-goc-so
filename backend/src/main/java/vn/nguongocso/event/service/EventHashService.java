package vn.nguongocso.event.service;

import java.util.Comparator;

import vn.nguongocso.event.entity.ChainEvent;

/**
 * Service giao diện tập trung tính toán và kiểm chứng chuỗi băm liên kết
 * cho tính toàn vẹn dòng sự kiện (NCL-08-CN-006 / QTN-19).
 *
 * <p>Quy tắc băm theo chuẩn hệ thống:</p>
 * <pre>
 * hash = SHA-256(
 *     eventType +
 *     shipmentId +
 *     recordedAt +
 *     recordedBy +
 *     eventData (JSON canonical, sort keys) +
 *     previousHash
 * )
 * </pre>
 *
 * <p>previousHash của sự kiện đầu tiên = chuỗi rỗng "".
 * Kết quả hash là chuỗi hex 64 ký tự.</p>
 */
public interface EventHashService {
    /** Tên thuật toán băm theo API contract. */
    String HASH_ALGORITHM = "SHA-256";

    /**
     * Tính hash cho một sự kiện mới dựa trên previousHash.
     *
     * @param event        sự kiện chuỗi cung ứng
     * @param previousHash mã băm của sự kiện liền trước (rỗng nếu là sự kiện đầu tiên)
     * @return mã băm SHA-256 dạng chuỗi hex 64 ký tự
     */
    String calculateHash(ChainEvent event, String previousHash);

    /**
     * Chuẩn hóa dữ liệu JSON eventData bằng cách sắp xếp các key theo thứ tự bảng chữ cái.
     *
     * @param eventDataJson chuỗi JSON gốc
     * @return chuỗi JSON chuẩn hóa với các key đã được sắp xếp
     */
    String canonicalizeEventData(String eventDataJson);

    /**
     * Trả về bộ so sánh sắp xếp sự kiện theo thứ tự bất biến (createdAt ASC, id ASC).
     *
     * @return bộ so sánh ChainEvent
     */
    Comparator<ChainEvent> eventOrdering();
}