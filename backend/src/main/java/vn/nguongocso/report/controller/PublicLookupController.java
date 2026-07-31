package vn.nguongocso.report.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.report.dto.response.LookupResponse;
import vn.nguongocso.report.service.PublicLookupService;

@RestController
@RequestMapping("/api/v1/public/trace-codes")
@RequiredArgsConstructor
public class PublicLookupController {

    private final PublicLookupService publicLookupService;

    @GetMapping("/{codeValue}")
    public ResponseEntity<ApiResult<LookupResponse>> lookupCode(
            @PathVariable String codeValue,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String location,
            HttpServletRequest request) {

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        LookupResponse response = publicLookupService.lookupCode(codeValue, latitude, longitude, location, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
