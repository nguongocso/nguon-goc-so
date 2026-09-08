package vn.nguongocso.trace.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.request.ExportTraceCodesRequest;
import vn.nguongocso.trace.dto.response.TraceCodeHistoryResponse;
import vn.nguongocso.trace.dto.response.TraceCodeSummaryResponse;
import vn.nguongocso.trace.service.TraceCodeStatusService;

/**
 * Controller xem và tra cứu trạng thái từng mã tem trong lô hàng (NCL-04-CN-008).
 *
 * <p>Chỉ dành riêng cho Quản lý Hợp tác xã (VT-02).</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TraceCodeController {

    private final TraceCodeStatusService traceCodeStatusService;

    /**
     * Lấy danh sách mã tem của lô hàng (hỗ trợ lọc theo trạng thái, tìm kiếm mã và phân trang).
     *
     * @param shipmentId  ID lô hàng
     * @param status      Bộ lọc trạng thái (INACTIVE, ACTIVE, LOCKED, CANCELLED, RECALLED)
     * @param search      Bộ lọc tìm kiếm gần đúng theo mã tem
     * @param pageable    Thông tin phân trang
     * @param currentUser Người dùng đăng nhập
     * @return Danh sách mã tem phân trang
     */
    @GetMapping("/shipments/{shipmentId}/trace-codes")
    @PreAuthorize("hasRole('VT-02')")
    public ApiResult<PageResponse<TraceCodeSummaryResponse>> getShipmentTraceCodes(
            @PathVariable UUID shipmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "codeValue", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        PageResponse<TraceCodeSummaryResponse> response = traceCodeStatusService
                .getTraceCodesByShipment(shipmentId, status, search, pageable, currentUser);

        return ApiResult.success(response);
    }

    /**
     * Tra cứu dòng thời gian lịch sử chi tiết của một mã tem.
     *
     * @param codeValue   Giá trị mã tem
     * @param currentUser Người dùng đăng nhập
     * @return Thông tin lịch sử chi tiết mã tem
     */
    @GetMapping("/trace-codes/{codeValue}/history")
    @PreAuthorize("hasRole('VT-02')")
    public ApiResult<TraceCodeHistoryResponse> getTraceCodeHistory(
            @PathVariable String codeValue,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        TraceCodeHistoryResponse response = traceCodeStatusService
                .getTraceCodeHistory(codeValue, currentUser);

        return ApiResult.success(response);
    }

    /**
     * Xuất danh sách mã tem theo lô hàng ra file CSV phục vụ kiểm kê, đối soát.
     *
     * @param shipmentId  ID lô hàng
     * @param request     Bộ lọc xuất file
     * @param currentUser Người dùng đăng nhập
     * @return File CSV nhị phân kèm Content-Disposition attachment
     */
    @PostMapping("/shipments/{shipmentId}/trace-codes/export")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<byte[]> exportTraceCodes(
            @PathVariable UUID shipmentId,
            @RequestBody(required = false) ExportTraceCodesRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        byte[] csvBytes = traceCodeStatusService.exportTraceCodes(shipmentId, request, currentUser);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("Danh_sach_ma_tem_%s_%s.csv", shipmentId, timestamp);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvBytes);
    }
}
