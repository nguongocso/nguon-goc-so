package vn.nguongocso.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.event.dto.response.JourneyResponse;
import vn.nguongocso.event.service.JourneyService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/shipments")
@RequiredArgsConstructor
/** Cung cấp hành trình công khai của lô hàng. */
public class PublicJourneyController {
    private final JourneyService journeyService;

    /** Lấy hành trình theo mã lô hàng. */
    @GetMapping("/{shipmentId}/journey")
    public ResponseEntity<ApiResult<JourneyResponse>> getJourney(@PathVariable UUID shipmentId) {
        JourneyResponse response = journeyService.getJourney(shipmentId);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
