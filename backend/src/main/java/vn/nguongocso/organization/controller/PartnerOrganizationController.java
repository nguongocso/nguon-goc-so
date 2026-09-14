package vn.nguongocso.organization.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.response.PartnerOrganizationResponse;
import vn.nguongocso.trace.service.ShipmentService;

/** API tra cứu đối tác doanh nghiệp phục vụ phân bổ lô hàng. */
@RestController
@RequestMapping("/api/v1/partner-organizations")
@RequiredArgsConstructor
public class PartnerOrganizationController {
    private final ShipmentService shipmentService;
    @GetMapping
    public ApiResult<PageResponse<PartnerOrganizationResponse>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResult.success(shipmentService.getPartnerOrganizations(keyword, page, size));
    }
}
