package vn.nguongocso.alert.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.alert.dto.request.ResolveAlertRequest;
import vn.nguongocso.alert.dto.response.AlertListResponse;
import vn.nguongocso.alert.dto.response.ResolveAlertResponse;
import vn.nguongocso.alert.enums.AlertStatus;

/** Dịch vụ quản lý cảnh báo. */
public interface AlertService {
    /**
     * Lấy danh sách cảnh báo theo bộ lọc.
     */
    AlertListResponse getAlerts(
            vn.nguongocso.alert.enums.AlertType type,
            AlertStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            UUID organizationId,
            Pageable pageable);

    /** Xử lý cảnh báo. */
    ResolveAlertResponse resolveAlert(
            UUID alertId,
            ResolveAlertRequest request);

}