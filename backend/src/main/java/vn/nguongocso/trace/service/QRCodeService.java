package vn.nguongocso.trace.service;

import java.util.UUID;

/** Sinh và lưu ảnh QR cho mã truy xuất. */
public interface QRCodeService {
    /** Tạo ảnh QR và trả về đường dẫn lưu trữ. */
    String generateQRCode(String codeValue, UUID organizationId, UUID productionLotId, UUID shipmentId);
}
