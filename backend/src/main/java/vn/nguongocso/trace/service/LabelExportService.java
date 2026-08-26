package vn.nguongocso.trace.service;

import java.util.UUID;

import vn.nguongocso.trace.dto.request.ExportLabelsRequest;
import vn.nguongocso.trace.dto.response.LabelExportResponse;

/**
 * Service xuất tem QR cho lô hàng (NCL-04-CN-005).
 *
 * <p>
 * Quy tắc: QTN-23 (số lượng xuất nằm trong số mã đã sinh + bắt buộc ghi log),
 * QTN-01 (cô lập dữ liệu theo tổ chức).
 * </p>
 */
public interface LabelExportService {

        /**
         * Xuất file PDF chứa tem QR của lô hàng và ghi lịch sử xuất.
         *
         * @param shipmentId ID lô hàng
         * @param request    tham số xuất (khoảng mã, khổ tem, trường hiển thị)
         * @return kết quả chứa file PDF và metadata
         * @throws vn.nguongocso.exception.ResourceNotFoundException nếu không tìm thấy lô hàng
         * @throws vn.nguongocso.exception.BusinessException        nếu vi phạm quyền, dữ liệu hoặc ràng buộc nghiệp vụ
         */
        LabelExportResponse exportLabels(UUID shipmentId, ExportLabelsRequest request);
}
