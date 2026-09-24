package vn.nguongocso.event.service;

import java.util.Comparator;

import vn.nguongocso.event.entity.ChainEvent;

/** Tính toán và kiểm chứng chuỗi băm liên kết theo QTN-19 (NCL-08-CN-006). */
public interface EventHashService {
    /** Tên thuật toán băm theo API contract. */
    String HASH_ALGORITHM = "SHA-256";

    /** Tính hash cho một sự kiện mới dựa trên previousHash. */
    String calculateHash(ChainEvent event, String previousHash);

    /** Chuẩn hóa dữ liệu JSON eventData bằng cách sắp xếp các key theo thứ tự bảng chữ cái. */
    String canonicalizeEventData(String eventDataJson);

    /** Trả về bộ so sánh sắp xếp sự kiện theo thứ tự bất biến (createdAt ASC, id ASC). */
    Comparator<ChainEvent> eventOrdering();
}