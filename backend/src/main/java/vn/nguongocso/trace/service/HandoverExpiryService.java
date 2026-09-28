package vn.nguongocso.trace.service;

/** Service xử lý phiếu bàn giao hết hạn. */
public interface HandoverExpiryService {
    /** Tìm các phiếu bàn giao quá thời hạn và chuyển sang hết hạn. */
    int expireOverdueHandovers();
}
