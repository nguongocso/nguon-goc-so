package vn.nguongocso.publicapi.service;

import vn.nguongocso.publicapi.dto.response.PublicLotCertificationsResponse;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;

/** Cung cấp dữ liệu truy xuất công khai. */
public interface PublicTraceService {
    /** Lấy thông tin truy xuất công khai. */
    PublicTraceResponse getPublicTrace(String codeValue, Double latitude, Double longitude, String ipAddress, String userAgent);

    /** Lấy chứng nhận công khai của lô hàng. */
    PublicLotCertificationsResponse getPublicCertifications(String codeValue);
}