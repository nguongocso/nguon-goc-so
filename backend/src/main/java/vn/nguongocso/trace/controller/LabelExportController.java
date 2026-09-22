package vn.nguongocso.trace.controller;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.trace.dto.request.ExportLabelsRequest;
import vn.nguongocso.trace.dto.response.LabelExportResponse;
import vn.nguongocso.trace.service.LabelExportService;

/**
 * Controller xuất tem QR cho lô hàng.
 */
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class LabelExportController {

    private final LabelExportService labelExportService;

    /**
     * Xuất file PDF chứa tem QR của lô hàng và ghi lịch sử xuất.
     */
    @PostMapping("/{shipmentId}/labels/export")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<byte[]> exportLabels(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody ExportLabelsRequest request) {

        LabelExportResponse result = labelExportService.exportLabels(shipmentId, request);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.getPdfBytes());
    }
}
