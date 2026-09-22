package vn.nguongocso.trace.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.request.ExportTraceCodesRequest;
import vn.nguongocso.trace.dto.response.TraceCodeHistoryResponse;
import vn.nguongocso.trace.dto.response.TraceCodeSummaryResponse;

/** Service xem và tra cứu trạng thái từng mã tem trong lô hàng. */
public interface TraceCodeStatusService {
    /** Lấy danh sách mã tem theo lô hàng có bộ lọc và phân trang. */
    PageResponse<TraceCodeSummaryResponse> getTraceCodesByShipment(
        UUID shipmentId,
        String status,
        String search,
        Pageable pageable,
        CustomUserDetails currentUser
    );

    /** Lấy lịch sử chi tiết vòng đời của một mã tem. */
    TraceCodeHistoryResponse getTraceCodeHistory(
        String codeValue,
        CustomUserDetails currentUser
    );

    /** Xuất danh sách mã tem ra file CSV. */
    byte[] exportTraceCodes(
        UUID shipmentId,
        ExportTraceCodesRequest request,
        CustomUserDetails currentUser
    );
}
