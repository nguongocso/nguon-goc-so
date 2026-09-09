package vn.nguongocso.trace.service;

/**
 * Dịch vụ xử lý phiếu bàn giao hết hạn (AC NCL-05-CN-009 TC-03).
 *
 * <p>Tách thành bean riêng chạy trong giao dịch REQUIRES_NEW để việc chuyển trạng
 * thái EXPIRED và thông báo tới cả hai tổ chức được duy trì ngay cả khi action gọi
 * bên ngoài (accept/reject/cancel/getById) sau đó ném ngoại lệ và bị roll back.
 */
public interface HandoverExpiryService {

    /**
     * Tìm các phiếu PENDING_CONFIRMATION quá thời hạn và chuyển sang EXPIRED,
     * đồng thời gửi thông báo tới cả tổ chức giao lẫn tổ chức nhận.
     *
     * <p>Idempotent: chỉ xử lý các phiếu còn ở trạng thái PENDING_CONFIRMATION,
     * nên sau lần chạy đầu tiên phiếu đã là EXPIRED và không bị xử lý lại.
     *
     * @return số lượng phiếu đã chuyển sang EXPIRED trong lần chạy này
     */
    int expireOverdueHandovers();
}