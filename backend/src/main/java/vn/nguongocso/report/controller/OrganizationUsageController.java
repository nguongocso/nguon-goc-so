package vn.nguongocso.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse;
import vn.nguongocso.report.service.OrganizationUsageService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Controller bảng điều khiển mức độ sử dụng nền tảng theo tổ chức (NCL-07-CN-008).
 *
 * <p>Chỉ Quản trị viên nền tảng (VT-01) được truy cập. Mọi vai trò khác
 * (kể cả VT-02) đều bị từ chối với HTTP 403.</p>
 */
@RestController
@RequestMapping("/api/v1/reports/organization-usage")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VT-01')")
public class OrganizationUsageController {

    private final OrganizationUsageService organizationUsageService;

    /**
     * Lấy mức độ sử dụng nền tảng của từng tổ chức trong kỳ.
     *
     * @param startDate      ngày bắt đầu kỳ hiện tại (yyyy-MM-dd, mặc định 30 ngày gần nhất)
     * @param endDate        ngày kết thúc kỳ hiện tại (yyyy-MM-dd, mặc định hôm nay)
     * @param organizationId lọc một tổ chức cụ thể (mặc định tất cả tổ chức)
     * @return dữ liệu 6 chỉ số kèm so sánh kỳ trước theo từng tổ chức
     */
    @GetMapping
    public ResponseEntity<ApiResult<OrganizationUsageDashboardResponse>> getOrganizationUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID organizationId) {

        OrganizationUsageDashboardResponse response =
                organizationUsageService.getDashboard(startDate, endDate, organizationId);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Xuất báo cáo mức độ sử dụng theo kỳ ra file CSV.
     *
     * @param startDate      ngày bắt đầu kỳ hiện tại (yyyy-MM-dd, mặc định 30 ngày gần nhất)
     * @param endDate        ngày kết thúc kỳ hiện tại (yyyy-MM-dd, mặc định hôm nay)
     * @param organizationId lọc một tổ chức cụ thể (mặc định tất cả tổ chức)
     * @return file CSV đính kèm
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrganizationUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID organizationId) {

        byte[] csvBytes = organizationUsageService.exportCsv(startDate, endDate, organizationId);

        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = "Bao_cao_muc_do_su_dung_" + timestamp + ".csv";

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvBytes);
    }
}
