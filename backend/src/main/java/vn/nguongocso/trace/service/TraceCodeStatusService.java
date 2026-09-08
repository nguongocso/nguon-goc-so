package vn.nguongocso.trace.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.request.ExportTraceCodesRequest;
import vn.nguongocso.trace.dto.response.TraceCodeHistoryResponse;
import vn.nguongocso.trace.dto.response.TraceCodeSummaryResponse;

/**
 * Service xem và tra cứu trạng thái từng mã tem trong lô hàng (NCL-04-CN-008).
 */
public interface TraceCodeStatusService {

    /**
     * Lấy danh sách mã tem theo lô hàng với bộ lọc trạng thái, tìm kiếm và phân trang.
     *
     * @param shipmentId  ID lô hàng
     * @param status      Trạng thái mã tem (tuỳ chọn)
     * @param search      Chuỗi tìm kiếm mã tem (tuỳ chọn)
     * @param pageable    Thông tin phân trang
     * @param currentUser Người dùng hiện tại
     * @return Danh sách mã tem phân trang
     */
    PageResponse<TraceCodeSummaryResponse> getTraceCodesByShipment(
            UUID shipmentId,
            String status,
            String search,
            Pageable pageable,
            CustomUserDetails currentUser);

    /**
     * Lấy lịch sử chi tiết vòng đời của một mã tem.
     *
     * @param codeValue   Giá trị mã tem
     * @param currentUser Người dùng hiện tại
     * @return Thông tin lịch sử mã tem
     */
    TraceCodeHistoryResponse getTraceCodeHistory(
            String codeValue,
            CustomUserDetails currentUser);

    /**
     * Xuất danh sách mã tem ra file CSV.
     *
     * @param shipmentId  ID lô hàng
     * @param request     Bộ lọc xuất file
     * @param currentUser Người dùng hiện tại
     * @return Mảng byte của file CSV (kèm BOM UTF-8)
     */
    byte[] exportTraceCodes(
            UUID shipmentId,
            ExportTraceCodesRequest request,
            CustomUserDetails currentUser);
}
