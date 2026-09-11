package vn.nguongocso.trace.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.dto.request.ExportTraceCodesRequest;
import vn.nguongocso.trace.dto.response.HistoryEvent;
import vn.nguongocso.trace.dto.response.TraceCodeHistoryResponse;
import vn.nguongocso.trace.dto.response.TraceCodeSummaryResponse;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.TraceCodeStatusService;

/**
 * Triển khai dịch vụ xem và tra cứu trạng thái từng mã tem trong lô hàng (NCL-04-CN-008).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceCodeStatusServiceImpl implements TraceCodeStatusService {

    private static final String ROLE_COOPERATIVE_MANAGER = "VT-02";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ShipmentRepository shipmentRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final TraceCodeScanLogRepository traceCodeScanLogRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TraceCodeSummaryResponse> getTraceCodesByShipment(
            UUID shipmentId,
            String status,
            String search,
            Pageable pageable,
            CustomUserDetails currentUser) {

        // 1. Kiểm tra quyền hạn vai trò (Chỉ VT-02)
        validateUserRole(currentUser);

        // 2. Kiểm tra tồn tại lô hàng và cô lập dữ liệu theo tổ chức (QTN-01)
        Shipment shipment = getAndValidateShipmentAccess(shipmentId, currentUser);

        // 3. Chuẩn hóa bộ lọc
        TraceCodeStatus parsedStatus = parseStatus(status);
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        // 4. Truy vấn danh sách mã tem phân trang
        Page<TraceCode> page = traceCodeRepository.findByShipmentAndFilters(
                shipment.getId(),
                currentUser.getOrganizationId(),
                parsedStatus,
                trimmedSearch,
                pageable);

        if (page.isEmpty()) {
            return PageResponse.from(page, Collections.emptyList());
        }

        // 5. Tính số lượt quét tối ưu hóa (tránh N+1)
        Map<UUID, Long> scanCounts = getScanCountsMap(page.getContent());

        // 6. Chuyển đổi sang DTO
        List<TraceCodeSummaryResponse> items = page.getContent().stream()
                .map(tc -> TraceCodeSummaryResponse.builder()
                        .id(tc.getId())
                        .codeValue(tc.getCodeValue())
                        .status(tc.getStatus())
                        .activatedAt(tc.getActivatedAt())
                        .printedAt(tc.getPrintedAt())
                        .cancelledAt(tc.getCancelledAt())
                        .lockedAt(tc.getLockedAt())
                        .scanCount(scanCounts.getOrDefault(tc.getId(), 0L))
                        .createdAt(tc.getCreatedAt())
                        .build())
                .toList();

        return PageResponse.from(page, items);
    }

    @Override
    @Transactional(readOnly = true)
    public TraceCodeHistoryResponse getTraceCodeHistory(
            String codeValue,
            CustomUserDetails currentUser) {

        // 1. Kiểm tra quyền hạn vai trò (Chỉ VT-02)
        validateUserRole(currentUser);

        if (codeValue == null || codeValue.isBlank()) {
            throw new BusinessException("Mã tem không được để trống.");
        }

        // 2. Tìm mã tem
        TraceCode traceCode = traceCodeRepository.findByCodeValue(codeValue.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã tem truy xuất: " + codeValue));

        // 3. Kiểm tra cô lập dữ liệu theo tổ chức (QTN-01)
        Shipment shipment = traceCode.getShipment();
        if (shipment == null || !currentUser.getOrganizationId().equals(shipment.getOrganization().getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem mã tem của tổ chức khác.");
        }

        // 4. Tổng số lượt quét
        long scanCount = traceCodeScanLogRepository.countByTraceCode_Id(traceCode.getId());

        // 5. Thu thập dòng thời gian các sự kiện
        List<HistoryEvent> events = buildHistoryEvents(traceCode);

        return TraceCodeHistoryResponse.builder()
                .codeValue(traceCode.getCodeValue())
                .status(traceCode.getStatus())
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .scanCount(scanCount)
                .createdAt(traceCode.getCreatedAt())
                .events(events)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTraceCodes(
            UUID shipmentId,
            ExportTraceCodesRequest request,
            CustomUserDetails currentUser) {

        // 1. Kiểm tra quyền hạn vai trò (Chỉ VT-02)
        validateUserRole(currentUser);

        // 2. Kiểm tra tồn tại lô hàng và cô lập dữ liệu theo tổ chức (QTN-01)
        Shipment shipment = getAndValidateShipmentAccess(shipmentId, currentUser);

        // 3. Chuẩn hóa bộ lọc
        String status = (request != null) ? request.getStatus() : null;
        String search = (request != null) ? request.getSearch() : null;
        TraceCodeStatus parsedStatus = parseStatus(status);
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        // 4. Truy vấn toàn bộ mã tem thỏa mãn bộ lọc
        List<TraceCode> traceCodes = traceCodeRepository.findAllByShipmentAndFilters(
                shipment.getId(),
                currentUser.getOrganizationId(),
                parsedStatus,
                trimmedSearch);

        Map<UUID, Long> scanCounts = getScanCountsMap(traceCodes);

        // 5. Sinh nội dung file CSV kèm BOM UTF-8
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff'); // BOM cho Excel hiển thị đúng tiếng Việt
        sb.append("STT,Mã tem,Trạng thái,Ngày tạo,Ngày in,Ngày kích hoạt,Lượt quét\n");

        int stt = 1;
        for (TraceCode tc : traceCodes) {
            sb.append(stt++).append(",");
            sb.append(escapeCsv(tc.getCodeValue())).append(",");
            sb.append(escapeCsv(formatStatusLabel(tc.getStatus()))).append(",");
            sb.append(formatDateTime(tc.getCreatedAt())).append(",");
            sb.append(formatDateTime(tc.getPrintedAt())).append(",");
            sb.append(formatDateTime(tc.getActivatedAt())).append(",");
            sb.append(scanCounts.getOrDefault(tc.getId(), 0L)).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==================== CÁC PHƯƠNG THỨC HỖ TRỢ NỘI BỘ ====================

    private void validateUserRole(CustomUserDetails currentUser) {
        if (currentUser == null || !ROLE_COOPERATIVE_MANAGER.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý hợp tác xã (VT-02) mới có quyền truy cập chức năng này.");
        }
    }

    private Shipment getAndValidateShipmentAccess(UUID shipmentId, CustomUserDetails currentUser) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô hàng."));

        if (!currentUser.getOrganizationId().equals(shipment.getOrganization().getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem mã tem của tổ chức khác.");
        }

        return shipment;
    }

    private TraceCodeStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TraceCodeStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái mã tem không hợp lệ: " + status);
        }
    }

    private Map<UUID, Long> getScanCountsMap(List<TraceCode> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> ids = codes.stream().map(TraceCode::getId).toList();
        List<Object[]> results = traceCodeScanLogRepository.countScansByTraceCodeIds(ids);
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : results) {
            if (row.length >= 2 && row[0] instanceof UUID id && row[1] instanceof Number count) {
                map.put(id, count.longValue());
            }
        }
        return map;
    }

    private List<HistoryEvent> buildHistoryEvents(TraceCode tc) {
        List<HistoryEvent> list = new ArrayList<>();

        // 1. Sự kiện khởi tạo
        if (tc.getCreatedAt() != null) {
            String creatorName = (tc.getShipment() != null && tc.getShipment().getCreatedBy() != null)
                    ? tc.getShipment().getCreatedBy().getFullName()
                    : "Hệ thống";
            list.add(HistoryEvent.builder()
                    .type("CREATED")
                    .timestamp(tc.getCreatedAt())
                    .details("Mã tem được khởi tạo cùng lô hàng")
                    .actorName(creatorName)
                    .build());
        }

        // 2. Sự kiện in tem
        if (tc.getPrintedAt() != null) {
            String printInfo = (tc.getPrintBatchId() != null)
                    ? "Xuất file in tem QR (Đợt in: " + tc.getPrintBatchId() + ")"
                    : "Xuất file in tem QR cho lô hàng";
            list.add(HistoryEvent.builder()
                    .type("PRINTED")
                    .timestamp(tc.getPrintedAt())
                    .details(printInfo)
                    .actorName("Quản lý hợp tác xã")
                    .build());
        }

        // 3. Sự kiện kích hoạt
        if (tc.getActivatedAt() != null) {
            String activatorName = (tc.getActivatedBy() != null)
                    ? tc.getActivatedBy().getFullName()
                    : "Hệ thống";
            list.add(HistoryEvent.builder()
                    .type("ACTIVATED")
                    .timestamp(tc.getActivatedAt())
                    .details("Kích hoạt mã tem cho lô hàng xuất bán")
                    .actorName(activatorName)
                    .build());
        }

        // 4. Sự kiện khóa tem
        if (tc.getLockedAt() != null) {
            String lockerName = (tc.getLockedBy() != null)
                    ? tc.getLockedBy().getFullName()
                    : "Quản trị viên";
            String reason = (tc.getLockReason() != null && !tc.getLockReason().isBlank())
                    ? ": " + tc.getLockReason()
                    : "";
            list.add(HistoryEvent.builder()
                    .type("LOCKED")
                    .timestamp(tc.getLockedAt())
                    .details("Khóa mã tem" + reason)
                    .actorName(lockerName)
                    .build());
        }

        // 5. Sự kiện mở khóa tem
        if (tc.getUnlockedAt() != null) {
            String unlockerName = (tc.getUnlockedBy() != null)
                    ? tc.getUnlockedBy().getFullName()
                    : "Quản trị viên";
            String conclusion = (tc.getUnlockConclusion() != null && !tc.getUnlockConclusion().isBlank())
                    ? ": " + tc.getUnlockConclusion()
                    : "";
            list.add(HistoryEvent.builder()
                    .type("UNLOCKED")
                    .timestamp(tc.getUnlockedAt())
                    .details("Mở khóa mã tem" + conclusion)
                    .actorName(unlockerName)
                    .build());
        }

        // 6. Sự kiện hủy tem
        if (tc.getCancelledAt() != null) {
            String cancellerName = (tc.getCancelledBy() != null)
                    ? tc.getCancelledBy().getFullName()
                    : "Quản lý hợp tác xã";
            String reason = (tc.getCancelReason() != null && !tc.getCancelReason().isBlank())
                    ? ": " + tc.getCancelReason()
                    : (tc.getCancelReasonType() != null ? ": " + tc.getCancelReasonType() : "");
            list.add(HistoryEvent.builder()
                    .type("CANCELLED")
                    .timestamp(tc.getCancelledAt())
                    .details("Hủy mã tem" + reason)
                    .actorName(cancellerName)
                    .build());
        }

        // 7. Sự kiện thu hồi nếu mã/lô hàng ở trạng thái RECALLING hoặc RECALLED
        if (tc.getShipment() != null && tc.getShipment().getStatus() == ShipmentStatus.RECALLING) {
            LocalDateTime recallTime = (tc.getShipment().getUpdatedAt() != null)
                    ? tc.getShipment().getUpdatedAt()
                    : LocalDateTime.now();
            list.add(HistoryEvent.builder()
                    .type("RECALLING")
                    .timestamp(recallTime)
                    .details("Lô hàng đang trong quá trình thu hồi")
                    .actorName("Hệ thống")
                    .build());
        } else if (tc.getStatus() == TraceCodeStatus.RECALLED
                || (tc.getShipment() != null && tc.getShipment().getStatus() == ShipmentStatus.RECALLED)) {
            LocalDateTime recallTime = (tc.getShipment() != null && tc.getShipment().getUpdatedAt() != null)
                    ? tc.getShipment().getUpdatedAt()
                    : LocalDateTime.now();
            list.add(HistoryEvent.builder()
                    .type("RECALLED")
                    .timestamp(recallTime)
                    .details("Mã tem bị thu hồi cùng lô hàng")
                    .actorName("Hệ thống")
                    .build());
        }

        // 8. Tối đa 5 lượt quét người tiêu dùng gần nhất
        List<TraceCodeScanLog> recentScans = traceCodeScanLogRepository
                .findTop5ByTraceCode_IdOrderByScannedAtDesc(tc.getId());
        for (TraceCodeScanLog scan : recentScans) {
            StringBuilder sb = new StringBuilder("Quét tra cứu thông tin");
            if (scan.getLocation() != null && !scan.getLocation().isBlank()) {
                sb.append(" tại ").append(scan.getLocation());
            }
            if (scan.getIpAddress() != null && !scan.getIpAddress().isBlank()) {
                sb.append(" (IP: ").append(scan.getIpAddress()).append(")");
            }
            if (Boolean.TRUE.equals(scan.getIsAbnormal())) {
                sb.append(" [Cảnh báo bất thường]");
            }
            list.add(HistoryEvent.builder()
                    .type("SCANNED")
                    .timestamp(scan.getScannedAt())
                    .details(sb.toString())
                    .actorName("Người tiêu dùng")
                    .build());
        }

        // Sắp xếp các sự kiện theo thứ tự thời gian giảm dần (mới nhất lên đầu)
        list.sort(Comparator.comparing(HistoryEvent::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));

        return list;
    }

    private String formatStatusLabel(TraceCodeStatus status) {
        if (status == null) return "";
        return switch (status) {
            case INACTIVE -> "Chưa kích hoạt";
            case ACTIVE -> "Đã kích hoạt";
            case LOCKED -> "Đang bị khóa";
            case CANCELLED -> "Đã hủy";
            case RECALLED -> "Đã thu hồi";
            case SUSPECT -> "Nghi vấn";
        };
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "—";
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
