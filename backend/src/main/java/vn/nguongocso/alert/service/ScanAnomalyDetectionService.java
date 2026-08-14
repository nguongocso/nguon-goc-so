package vn.nguongocso.alert.service;

import java.util.UUID;

/** Dịch vụ phát hiện quét bất thường. */
public interface ScanAnomalyDetectionService {
    /** Kiểm tra sau khi ghi nhận lượt quét. */
    void onScanRecorded(UUID traceCodeId);
}
