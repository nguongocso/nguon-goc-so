package vn.nguongocso.trace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.trace.dto.response.ImpactScopeTraceResponse;
import vn.nguongocso.trace.service.ImpactScopeExportService;
import vn.nguongocso.trace.service.ImpactScopeTraceService;

@RestController
@RequestMapping("/api/v1/trace")
@RequiredArgsConstructor
public class ImpactScopeTraceController {

    private final ImpactScopeTraceService impactScopeTraceService;
    private final ImpactScopeExportService impactScopeExportService;

    /**
     * Truy vết phạm vi ảnh hưởng hai chiều (Upstream & Downstream) của một lô sản xuất, lô hàng hoặc mã tem.
     *
     * GET /api/v1/trace/impact-scope?code={code}
     */
    @GetMapping("/impact-scope")
    public ResponseEntity<ApiResult<ImpactScopeTraceResponse>> getImpactScopeTrace(
            @RequestParam("code") String code,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ImpactScopeTraceResponse response = impactScopeTraceService.getImpactScopeTrace(code, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Xuất tệp báo cáo phạm vi ảnh hưởng (Excel/PDF).
     *
     * GET /api/v1/trace/impact-scope/export?code={code}&format={format}
     */
    @GetMapping("/impact-scope/export")
    public ResponseEntity<byte[]> exportImpactScopeReport(
            @RequestParam("code") String code,
            @RequestParam(value = "format", defaultValue = "EXCEL") String format,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        byte[] reportBytes = impactScopeExportService.exportImpactScopeReport(code, format, currentUser);

        String filename = "Truy_vet_pham_vi_anh_huong_" + code + ".xlsx";
        MediaType contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        if ("PDF".equalsIgnoreCase(format)) {
            filename = "Truy_vet_pham_vi_anh_huong_" + code + ".pdf";
            contentType = MediaType.APPLICATION_PDF;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(contentType)
                .body(reportBytes);
    }
}
