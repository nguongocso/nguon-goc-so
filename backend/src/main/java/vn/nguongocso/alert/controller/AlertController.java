package vn.nguongocso.alert.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.dto.request.ResolveAlertRequest;
import vn.nguongocso.alert.dto.response.AlertListResponse;
import vn.nguongocso.alert.dto.response.ResolveAlertResponse;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.service.AlertService;
import vn.nguongocso.common.ApiResult;

/**
 * Controller xử lý các hoạt động liên quan đến cảnh báo.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {
        private final AlertService alertService;

        /**
         * Danh sách cảnh báo quét bất thường.
         */
        @GetMapping
        public ResponseEntity<ApiResult<AlertListResponse>> getAlerts(
                        @RequestParam(required = false) vn.nguongocso.alert.enums.AlertType type,

                        @RequestParam(required = false) AlertStatus status,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

                        @RequestParam(required = false) UUID organizationId,

                        @RequestParam(defaultValue = "0") Integer page,

                        @RequestParam(defaultValue = "10") Integer size) {

                Pageable pageable = PageRequest.of(page, size);

                AlertListResponse response = alertService.getAlerts(
                                type,
                                status,
                                fromDate,
                                toDate,
                                organizationId,
                                pageable);

                return ResponseEntity.ok(
                                ApiResult.success(response));
        }

        /**
         * Xử lý cảnh báo.
         */
        @PatchMapping("/{alertId}/resolve")
        public ResponseEntity<ApiResult<ResolveAlertResponse>> resolveAlert(
                        @PathVariable UUID alertId,
                        @Valid @RequestBody ResolveAlertRequest request) {

                ResolveAlertResponse response = alertService.resolveAlert(
                                alertId,
                                request);

                return ResponseEntity.ok(
                                ApiResult.success(response));
        }
}