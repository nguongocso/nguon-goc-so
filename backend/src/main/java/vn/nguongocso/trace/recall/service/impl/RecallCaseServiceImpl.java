package vn.nguongocso.trace.recall.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.dto.request.ActivityLogRequest;
import vn.nguongocso.alert.service.ActivityLogService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;
import vn.nguongocso.trace.recall.dto.response.RecallCaseResponse;
import vn.nguongocso.trace.recall.dto.response.RecallLotResultResponse;
import vn.nguongocso.trace.recall.entity.RecallCase;
import vn.nguongocso.trace.recall.entity.RecallLotResult;
import vn.nguongocso.trace.recall.enums.LotResolution;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;
import vn.nguongocso.trace.recall.repository.RecallCaseRepository;
import vn.nguongocso.trace.recall.repository.RecallLotResultRepository;
import vn.nguongocso.trace.recall.service.RecallCaseService;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ quản lý vụ việc thu hồi (NCL-08-CN-012).
 *
 * <p>Vụ việc thu hồi gom toàn bộ lô hàng (Shipment) ở trạng thái {@code RECALLING}
 * của một lô sản xuất. Khi gọi danh sách, hệ thống tự tạo vụ việc (lazy
 * materialize) cho các lô sản xuất có lô hàng đang thu hồi nhưng chưa có vụ
 * việc. Vụ việc chỉ được đóng khi mọi lô hàng đã có kết quả xử lý và đã nhập
 * biện pháp khắc phục phòng ngừa (QTN-27), sau đó chuyển lô hàng sang {@code RECALLED}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecallCaseServiceImpl implements RecallCaseService {

    /** Vai trò Quản lý hợp tác xã — tác nhân chính của story. */
    private static final String ORG_MANAGER_ROLE = "VT-02";

    private static final String MSG_CASE_NOT_FOUND = "Không tìm thấy vụ việc thu hồi.";
    private static final String MSG_NO_PERMISSION = "Bạn không có quyền kết thúc vụ việc thu hồi.";
    private static final String MSG_CASE_ALREADY_CLOSED = "Vụ việc thu hồi đã được đóng trước đó.";
    private static final String MSG_MEASURES_REQUIRED =
            "Biện pháp khắc phục phòng ngừa là bắt buộc trước khi đóng vụ việc.";
    private static final String MSG_MISSING_RESULTS =
            "Còn %d lô chưa có kết quả xử lý: %s. Vui lòng nhập đủ kết quả xử lý cho tất cả các lô.";
    private static final String MSG_LOT_NOT_IN_CASE = "Lô hàng không thuộc vụ việc thu hồi này.";
    private static final String MSG_DUPLICATE_LOT = "Lô hàng %s bị trùng trong danh sách kết quả xử lý.";
    private static final String MSG_RESULT_REQUIRED = "Kết quả xử lý của lô hàng %s là bắt buộc.";
    private static final String MSG_QUANTITY_REQUIRED = "Số lượng thu hồi được của lô hàng %s là bắt buộc.";
    private static final String MSG_QUANTITY_RANGE =
            "Số lượng thu hồi được của lô hàng %s phải nằm trong khoảng từ 0 đến %s.";
    private static final String MSG_UNRECOVERABLE_REASON_REQUIRED =
            "Lô hàng %s: khi chọn kết quả \"Không thu hồi được\", bắt buộc nhập lý do và biện pháp xử lý rủi ro.";

    private static final DateTimeFormatter CASE_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RecallCaseRepository recallCaseRepository;
    private final RecallLotResultRepository recallLotResultRepository;
    private final ShipmentRepository shipmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final TraceCodeRepository traceCodeRepository;
    private final CodeRangeRepository codeRangeRepository;

    /**
     * Danh sách vụ việc thu hồi của tổ chức hiện tại.
     *
     * <p>Trước khi trả kết quả, tự tạo vụ việc (lazy materialize) cho các lô
     * sản xuất đã có lô hàng bị thu hồi nhưng chưa có vụ việc.</p>
     */
    @Override
    @Transactional
    public List<RecallCaseResponse> list(CustomUserDetails currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        materializeOpenCases(organizationId);

        List<RecallCase> cases =
                recallCaseRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        Map<UUID, List<Shipment>> recallingByLot =
                shipmentRepository.findByOrganization_OrganizationIdAndStatus(
                                organizationId, ShipmentStatus.RECALLING)
                        .stream()
                        .collect(Collectors.groupingBy(s -> s.getProductionLot().getId()));
        Map<UUID, List<Shipment>> recalledByLot =
                shipmentRepository.findByOrganization_OrganizationIdAndStatus(
                                organizationId, ShipmentStatus.RECALLED)
                        .stream()
                        .collect(Collectors.groupingBy(s -> s.getProductionLot().getId()));

        return cases.stream()
                .map(rc -> {
                    List<Shipment> lotShipments = rc.getStatus() == RecallCaseStatus.OPEN
                            ? recallingByLot.getOrDefault(rc.getProductionLot().getId(), List.of())
                            : recalledByLot.getOrDefault(rc.getProductionLot().getId(), List.of());
                    return toResponse(rc, lotShipments);
                })
                .toList();
    }

    /** Chi tiết một vụ việc thuộc tổ chức hiện tại, kèm kết quả xử lý từng lô. */
    @Override
    @Transactional(readOnly = true)
    public RecallCaseResponse getById(UUID id, CustomUserDetails currentUser) {
        RecallCase recallCase = recallCaseRepository
                .findByIdAndOrganizationId(id, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_CASE_NOT_FOUND));

        List<Shipment> shipments;
        if (recallCase.getStatus() == RecallCaseStatus.OPEN) {
            shipments = shipmentRepository.findByProductionLotIdAndStatus(
                    recallCase.getProductionLot().getId(), ShipmentStatus.RECALLING);
        } else {
            List<RecallLotResult> lotResults = recallLotResultRepository.findByRecallCaseId(recallCase.getId());
            shipments = lotResults.stream().map(RecallLotResult::getShipment).toList();
            if (shipments.isEmpty()) {
                shipments = shipmentRepository.findByProductionLotIdAndStatus(
                        recallCase.getProductionLot().getId(), ShipmentStatus.RECALLED);
            }
        }
        return toResponse(recallCase, shipments);
    }

    /**
     * Đóng vụ việc thu hồi (chỉ VT-02 cùng tổ chức sở hữu — QTN-01).
     *
     * <p>Trong cùng một transaction: kiểm tra điều kiện đóng theo QTN-27, lưu
     * kết quả xử lý từng lô, đóng vụ việc, gửi thông báo tới doanh nghiệp thu
     * mua liên quan và ghi lịch sử hoạt động (QTN-08).</p>
     */
    @Override
    @Transactional
    public RecallCaseResponse close(UUID id, CloseRecallCaseRequest request,
                                    CustomUserDetails currentUser) {
        // 1. Kiểm tra vai trò và cách ly tổ chức (QTN-01)
        if (!ORG_MANAGER_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(MSG_NO_PERMISSION);
        }
        RecallCase recallCase = recallCaseRepository
                .findByIdAndOrganizationId(id, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_CASE_NOT_FOUND));

        // 2. Vụ việc phải đang mở — không cho đóng lại vụ việc đã đóng
        if (recallCase.getStatus() != RecallCaseStatus.OPEN) {
            throw new BusinessException(MSG_CASE_ALREADY_CLOSED);
        }

        // 3. Biện pháp khắc phục phòng ngừa bắt buộc (QTN-27)
        String remediation = request.getRemediationMeasures() == null
                ? ""
                : request.getRemediationMeasures().trim();
        if (remediation.isEmpty()) {
            throw new BusinessException(MSG_MEASURES_REQUIRED);
        }

        // Kiểm tra số lượng tệp biên bản đính kèm (tối đa 5 tệp)
        if (request.getEvidenceFileIds() != null && request.getEvidenceFileIds().size() > 5) {
            throw new BusinessException("Chỉ được đính kèm tối đa 5 tệp biên bản thu hồi.");
        }

        // 4. Phạm vi vụ việc = mọi lô hàng RECALLING của lô sản xuất (QTN-24)
        List<Shipment> shipments = shipmentRepository.findByProductionLotIdAndStatus(
                recallCase.getProductionLot().getId(), ShipmentStatus.RECALLING);

        // 5. Mọi lô phải có kết quả xử lý — chặn đóng và liệt kê lô còn thiếu (TC-02)
        Map<UUID, CloseRecallCaseRequest.LotResultItem> itemByShipment = new HashMap<>();
        for (CloseRecallCaseRequest.LotResultItem item : safeItems(request)) {
            if (item.getShipmentId() == null) {
                throw new BusinessException(MSG_LOT_NOT_IN_CASE);
            }
            if (itemByShipment.containsKey(item.getShipmentId())) {
                throw new BusinessException(
                        String.format(MSG_DUPLICATE_LOT, item.getShipmentId()));
            }
            itemByShipment.put(item.getShipmentId(), item);
        }
        List<Shipment> missing = shipments.stream()
                .filter(s -> !itemByShipment.containsKey(s.getId()))
                .toList();
        if (!missing.isEmpty()) {
            String missingNames = missing.stream()
                    .map(Shipment::getName)
                    .collect(Collectors.joining(", "));
            throw new BusinessException(
                    String.format(MSG_MISSING_RESULTS, missing.size(), missingNames));
        }

        // 6. Kiểm tra và lưu kết quả xử lý từng lô
        Map<UUID, RecallLotResult> existingResults = recallLotResultRepository
                .findByRecallCaseId(recallCase.getId())
                .stream()
                .collect(Collectors.toMap(r -> r.getShipment().getId(), r -> r));

        List<RecallLotResult> results = new ArrayList<>();
        for (Shipment shipment : shipments) {
            CloseRecallCaseRequest.LotResultItem item = itemByShipment.get(shipment.getId());

            if (item.getResolution() == null) {
                throw new BusinessException(String.format(MSG_RESULT_REQUIRED, shipment.getName()));
            }
            BigDecimal quantity = item.getRecoveredQuantity();
            if (quantity == null) {
                throw new BusinessException(String.format(MSG_QUANTITY_REQUIRED, shipment.getName()));
            }
            BigDecimal maxQuantity = BigDecimal.valueOf(shipment.getTotalQuantity());
            if (quantity.signum() < 0 || quantity.compareTo(maxQuantity) > 0) {
                throw new BusinessException(String.format(
                        MSG_QUANTITY_RANGE, shipment.getName(), shipment.getTotalQuantity()));
            }
            if (item.getResolution() == LotResolution.UNRECOVERABLE
                    && (item.getNotes() == null || item.getNotes().isBlank())) {
                throw new BusinessException(
                        String.format(MSG_UNRECOVERABLE_REASON_REQUIRED, shipment.getName()));
            }

            RecallLotResult result = existingResults.getOrDefault(
                    shipment.getId(), RecallLotResult.builder().build());
            result.setRecallCase(recallCase);
            result.setShipment(shipment);
            result.setResolution(item.getResolution());
            result.setRecoveredQuantity(quantity);
            result.setNotes(item.getNotes());
            results.add(result);
        }
        recallLotResultRepository.saveAll(results);

        // 6b. Cập nhật trạng thái các lô hàng sang RECALLED và hoàn tất mã tem
        for (Shipment shipment : shipments) {
            shipment.setStatus(ShipmentStatus.RECALLED);
            shipmentRepository.save(shipment);

            // Cập nhật trạng thái toàn bộ TraceCode sang RECALLED
            List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(shipment.getId());
            traceCodes.forEach(code -> code.setStatus(TraceCodeStatus.RECALLED));
            traceCodeRepository.saveAll(traceCodes);

            // Hoàn trả số lượng mã đã dùng cho dải mã của tổ chức
            if (!traceCodes.isEmpty()) {
                CodeRange codeRange = shipment.getCodeRange() != null
                        ? shipment.getCodeRange()
                        : codeRangeRepository
                                .findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(
                                        shipment.getOrganization().getOrganizationId())
                                .orElse(null);
                if (codeRange != null) {
                    codeRange.setUsedCount(Math.max(0, codeRange.getUsedCount() - traceCodes.size()));
                    codeRangeRepository.save(codeRange);
                }
            }
        }

        // 7. Đóng vụ việc — một chiều, không cho sửa kết quả sau khi đóng
        recallCase.setStatus(RecallCaseStatus.CLOSED);
        recallCase.setRemediationMeasures(remediation);
        recallCase.setEvidenceFileIds(serializeEvidence(request.getEvidenceFileIds()));
        recallCase.setClosedBy(currentUser.getUser());
        recallCase.setClosedAt(LocalDateTime.now());
        recallCaseRepository.save(recallCase);

        // 8. Gửi thông báo kết thúc thu hồi tới doanh nghiệp thu mua liên quan (TC-04)
        notifyProcurementOrganizations(recallCase, shipments);

        // 9. Ghi lịch sử hoạt động (QTN-08)
        logCloseActivity(currentUser, recallCase, shipments.size());

        log.info("Đã đóng vụ việc thu hồi {}. caseId={}",
                recallCase.getCaseCode(), recallCase.getId());
        return toResponse(recallCase, shipments);
    }

    /** Lấy danh sách kết quả xử lý an toàn (null → rỗng). */
    private List<CloseRecallCaseRequest.LotResultItem> safeItems(
            CloseRecallCaseRequest request) {
        return request.getLotResults() == null
                ? List.of()
                : request.getLotResults();
    }

    /**
     * Tự tạo vụ việc thu hồi (lazy materialize) cho các lô sản xuất đã có lô
     * hàng bị thu hồi nhưng chưa có vụ việc. Idempotent theo lô sản xuất.
     */
    private void materializeOpenCases(UUID organizationId) {
        List<Shipment> recalling = shipmentRepository
                .findByOrganization_OrganizationIdAndStatus(
                        organizationId, ShipmentStatus.RECALLING);
        Map<UUID, Shipment> lotShipment = new HashMap<>();
        for (Shipment shipment : recalling) {
            lotShipment.putIfAbsent(shipment.getProductionLot().getId(), shipment);
        }
        for (Map.Entry<UUID, Shipment> entry : lotShipment.entrySet()) {
            if (recallCaseRepository.existsByProductionLotId(entry.getKey())) {
                continue;
            }
            RecallCase recallCase = RecallCase.builder()
                    .caseCode(generateCaseCode())
                    .productionLot(entry.getValue().getProductionLot())
                    .organizationId(organizationId)
                    .status(RecallCaseStatus.OPEN)
                    .build();
            recallCaseRepository.save(recallCase);
            log.info("Đã tạo vụ việc thu hồi mới {} cho lô sản xuất {}.",
                    recallCase.getCaseCode(), entry.getKey());
        }
    }

    /** Sinh mã vụ việc theo dạng RC-yyyyMMddHHmmss-XXXX. */
    private String generateCaseCode() {
        return "RC-" + CASE_CODE_TIME.format(LocalDateTime.now())
                + "-" + String.format("%04X", ThreadLocalRandom.current().nextInt(0x10000));
    }

    /**
     * Gửi thông báo kết thúc thu hồi tới doanh nghiệp thu mua liên quan
     * (các tổ chức ghi nhận sự kiện thu mua trên các lô hàng trong vụ việc).
     */
    private void notifyProcurementOrganizations(RecallCase recallCase,
                                                List<Shipment> shipments) {
        List<UUID> shipmentIds = shipments.stream().map(Shipment::getId).toList();
        List<UUID> procurementOrgIds =
                chainEventRepository.findDistinctProcurementOrganizationIdsByShipmentIds(shipmentIds);
        if (procurementOrgIds.isEmpty()) {
            log.info("Không có doanh nghiệp thu mua liên quan — bỏ qua gửi thông báo. caseId={}",
                    recallCase.getId());
            return;
        }

        Set<UUID> recipientIds = new LinkedHashSet<>();
        for (UUID orgId : procurementOrgIds) {
            organizationUserRepository
                    .findByOrganization_OrganizationIdAndStatus(orgId, OrganizationUserStatus.ACTIVE)
                    .forEach(ou -> {
                        if (ou.getUser() != null) {
                            recipientIds.add(ou.getUser().getUserId());
                        }
                    });
        }
        if (recipientIds.isEmpty()) {
            log.info("Không có người nhận hoạt động nào thuộc doanh nghiệp thu mua liên quan. caseId={}",
                    recallCase.getId());
            return;
        }

        int count = notificationService.sendRecallCaseClosedNotification(
                recallCase.getCaseCode(), new ArrayList<>(recipientIds));
        log.info("Đã gửi {} thông báo kết thúc vụ việc thu hồi {}. caseId={}",
                count, recallCase.getCaseCode(), recallCase.getId());
    }

    /** Ghi lịch sử hoạt động cho thao tác đóng vụ việc (QTN-08). */
    private void logCloseActivity(CustomUserDetails currentUser,
                                  RecallCase recallCase,
                                  int shipmentCount) {
        activityLogService.logActivity(ActivityLogRequest.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(recallCase.getOrganizationId())
                .action("CLOSE_RECALL_CASE")
                .description("Kết thúc vụ việc thu hồi \"" + recallCase.getCaseCode()
                        + "\" của lô sản xuất \"" + recallCase.getProductionLot().getName()
                        + "\". Đã xử lý " + shipmentCount + " lô hàng.")
                .entityType("RECALL_CASE")
                .entityId(recallCase.getId())
                .build());
    }

    /**
     * Dựng response của một vụ việc kèm toàn bộ lô hàng trong phạm vi.
     *
     * <p>Mọi lô hàng {@code RECALLED} của lô sản xuất đều xuất hiện trong
     * {@code lotResults}; lô chưa nhập kết quả có {@code resolution = null}
     * để màn hình hiển thị trạng thái "Chưa nhập" và cho phép nhập bổ sung.</p>
     */
    private RecallCaseResponse toResponse(RecallCase recallCase, List<Shipment> shipments) {
        Map<UUID, RecallLotResult> resultByShipment = recallLotResultRepository
                .findByRecallCaseId(recallCase.getId())
                .stream()
                .collect(Collectors.toMap(r -> r.getShipment().getId(), r -> r));

        List<RecallLotResultResponse> lotResults = shipments.stream()
                .map(shipment -> {
                    RecallLotResult result = resultByShipment.get(shipment.getId());
                    RecallLotResultResponse response = new RecallLotResultResponse();
                    response.setShipmentId(shipment.getId());
                    response.setShipmentName(shipment.getName());
                    response.setUnit(shipment.getProductionLot().getExpectedQuantityUnit());
                    if (result != null) {
                        response.setId(result.getId());
                        response.setResolution(result.getResolution());
                        response.setRecoveredQuantity(result.getRecoveredQuantity());
                        response.setNotes(result.getNotes());
                        response.setCreatedAt(result.getCreatedAt());
                    }
                    return response;
                })
                .toList();

        return RecallCaseResponse.builder()
                .id(recallCase.getId())
                .caseCode(recallCase.getCaseCode())
                .status(recallCase.getStatus())
                .productionLotId(recallCase.getProductionLot().getId())
                .productionLotName(recallCase.getProductionLot().getName())
                .organizationId(recallCase.getOrganizationId())
                .createdAt(recallCase.getCreatedAt())
                .updatedAt(recallCase.getUpdatedAt())
                .closedAt(recallCase.getClosedAt())
                .closedBy(recallCase.getClosedBy() != null
                        ? recallCase.getClosedBy().getUserId() : null)
                .remediationMeasures(recallCase.getRemediationMeasures())
                .evidenceFileIds(parseEvidence(recallCase.getEvidenceFileIds()))
                .lotResults(lotResults)
                .shipmentCount(shipments.size())
                .build();
    }

    /** Chuỗi hóa danh sách ID tệp biên bản (phân tách bởi dấu phẩy) hoặc null. */
    private String serializeEvidence(List<UUID> evidenceFileIds) {
        if (evidenceFileIds == null || evidenceFileIds.isEmpty()) {
            return null;
        }
        return evidenceFileIds.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    /** Phân giải chuỗi ID tệp biên bản đã lưu thành danh sách UUID hợp lệ. */
    private List<UUID> parseEvidence(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return UUID.fromString(s);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
