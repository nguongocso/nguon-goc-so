package vn.nguongocso.trace.service;

import java.util.UUID;

import vn.nguongocso.trace.dto.request.ExportLabelsRequest;
import vn.nguongocso.trace.dto.response.LabelExportResponse;

/** Service xuất tem QR cho lô hàng. */
public interface LabelExportService {
    /** Xuất file PDF chứa tem QR của lô hàng và ghi lịch sử xuất. */
    LabelExportResponse exportLabels(
        UUID shipmentId,
        ExportLabelsRequest request
    );
}
