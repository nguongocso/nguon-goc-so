package vn.nguongocso.trace.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.request.CancelTraceCodesRequest;
import vn.nguongocso.trace.dto.response.CancelTraceCodesResponse;
import vn.nguongocso.trace.dto.response.LabelCancellationHistoryResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.LabelCancellationHistory;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.LabelCancellationHistoryRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.LabelCancellationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelCancellationServiceImpl implements LabelCancellationService {

    private final ShipmentRepository shipmentRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final CodeRangeRepository codeRangeRepository;
    private final LabelCancellationHistoryRepository labelCancellationHistoryRepository;
    private final PermissionChecker permissionChecker;
    private final ApplicationEventPublisher eventPublisher;

    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new BusinessException("Chưa đăng nhập");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    @Override
    @Transactional
    public CancelTraceCodesResponse cancelTraceCodes(UUID shipmentId, CancelTraceCodesRequest request) {
        permissionChecker.check("shipment", "UPDATE");

        CustomUserDetails currentUser = getCurrentUser();
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng với ID: " + shipmentId));

        if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException("Bạn không có quyền thao tác trên lô hàng của tổ chức khác.");
        }

        if (request.getReasonType() != null && "OTHER".equalsIgnoreCase(request.getReasonType())) {
            if (request.getReasonNote() == null || request.getReasonNote().trim().length() < 10) {
                throw new BusinessException("Lý do khác yêu cầu ghi chú chi tiết tối thiểu 10 ký tự.");
            }
        }

        CodeRange codeRange = shipment.getCodeRange();
        if (codeRange == null) {
            codeRange = codeRangeRepository
                    .findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(currentUser.getOrganizationId())
                    .orElse(null);
        }
        String expectedPrefix = codeRange != null ? codeRange.getPrefix() : null;

        List<TraceCode> targetCodes = new ArrayList<>();
        String cancellationType = request.getCancelType() != null ? request.getCancelType().toUpperCase() : "SINGLE";
        final String rangeFromCode;
        final String rangeToCode;

        if ("RANGE".equals(cancellationType)) {
            if (request.getFromCode() == null || request.getFromCode().isBlank()
                    || request.getToCode() == null || request.getToCode().isBlank()) {
                throw new BusinessException("Hủy theo khoảng mã yêu cầu từ mã (fromCode) và đến mã (toCode).");
            }
            String parsedFrom = request.getFromCode().trim();
            String parsedTo = request.getToCode().trim();
            rangeFromCode = parsedFrom;
            rangeToCode = parsedTo;

            if (expectedPrefix != null && !expectedPrefix.isBlank()) {
                if (!rangeFromCode.startsWith(expectedPrefix)) {
                    throw new BusinessException("Mã bắt đầu '" + rangeFromCode
                            + "' không đúng tiền tố dải mã '" + expectedPrefix + "' của lô hàng.");
                }
                if (!rangeToCode.startsWith(expectedPrefix)) {
                    throw new BusinessException("Mã kết thúc '" + rangeToCode
                            + "' không đúng tiền tố dải mã '" + expectedPrefix + "' của lô hàng.");
                }
            }

            // Verify both fromCode and toCode exist in the shipment
            TraceCode startCode = traceCodeRepository.findByShipmentIdAndCodeValue(shipmentId, rangeFromCode)
                    .orElseThrow(() -> new BusinessException("Mã bắt đầu '" + rangeFromCode + "' không tồn tại trong lô hàng này."));
            TraceCode endCode = traceCodeRepository.findByShipmentIdAndCodeValue(shipmentId, rangeToCode)
                    .orElseThrow(() -> new BusinessException("Mã kết thúc '" + rangeToCode + "' không tồn tại trong lô hàng này."));

            long fromSeq = parseSequence(rangeFromCode, expectedPrefix);
            long toSeq = parseSequence(rangeToCode, expectedPrefix);

            if (fromSeq != -1 && toSeq != -1) {
                if (fromSeq > toSeq) {
                    throw new BusinessException("Mã bắt đầu (" + rangeFromCode + ") phải nhỏ hơn hoặc bằng mã kết thúc (" + rangeToCode + ").");
                }
                final long fSeq = fromSeq;
                final long tSeq = toSeq;
                final String pfx = expectedPrefix;
                List<TraceCode> allShipmentCodes = traceCodeRepository.findByShipmentId(shipmentId);
                targetCodes = allShipmentCodes.stream()
                        .filter(tc -> {
                            long seq = parseSequence(tc.getCodeValue(), pfx);
                            return seq >= fSeq && seq <= tSeq;
                        })
                        .toList();
            } else {
                targetCodes = traceCodeRepository.findByShipmentIdAndCodeValueBetween(shipmentId, rangeFromCode, rangeToCode);
            }

            if (targetCodes.isEmpty()) {
                throw new BusinessException("Không tìm thấy mã tem nào trong khoảng mã đã nhập.");
            }
        } else {
            rangeFromCode = null;
            rangeToCode = null;
            if (request.getCodeValues() == null || request.getCodeValues().isEmpty()) {
                throw new BusinessException("Vui lòng cung cấp danh sách mã tem cần hủy.");
            }
            targetCodes = traceCodeRepository.findByShipmentIdAndCodeValueIn(shipmentId, request.getCodeValues());
            if (targetCodes.isEmpty()) {
                throw new BusinessException("Không tìm thấy mã tem nào phù hợp trong danh sách đã chọn.");
            }
        }

        // Validate status: ONLY INACTIVE trace codes can be cancelled.
        List<String> activeCodes = new ArrayList<>();
        List<String> invalidStatusCodes = new ArrayList<>();

        for (TraceCode tc : targetCodes) {
            if (tc.getStatus() == TraceCodeStatus.ACTIVE) {
                activeCodes.add(tc.getCodeValue());
            } else if (tc.getStatus() != TraceCodeStatus.INACTIVE) {
                invalidStatusCodes.add(tc.getCodeValue());
            }
        }

        // Rule QTN-28 (TC-02): Block cancellation if active codes exist
        if (!activeCodes.isEmpty()) {
            throw new BusinessException(
                    "Không thể hủy tem đã kích hoạt! Tem " + String.join(", ", activeCodes.stream().limit(5).toList())
                            + (activeCodes.size() > 5 ? "..." : "")
                            + " đã được kích hoạt. Vui lòng sử dụng chức năng Khóa mã (LOCKED) hoặc Thu hồi (RECALLED).");
        }

        if (!invalidStatusCodes.isEmpty()) {
            throw new BusinessException(
                    "Một số tem không ở trạng thái 'Chưa kích hoạt' (INACTIVE) để hủy: "
                            + String.join(", ", invalidStatusCodes.stream().limit(5).toList()));
        }

        User userEntity = new User();
        userEntity.setUserId(currentUser.getUserId());

        int count = targetCodes.size();
        LocalDateTime now = LocalDateTime.now();

        // Update trace codes to CANCELLED
        for (TraceCode tc : targetCodes) {
            tc.setStatus(TraceCodeStatus.CANCELLED);
            tc.setCancelledAt(now);
            tc.setCancelledBy(userEntity);
            tc.setCancelReasonType(request.getReasonType());
            tc.setCancelReason(request.getReasonNote());
        }
        traceCodeRepository.saveAll(targetCodes);

        // Quota refund logic (QTN-03 & QTN-28)

        long remainingQuota = 0;
        if (codeRange != null) {
            codeRangeRepository.refundQuota(codeRange.getId(), (long) count);
            // Refresh codeRange usedCount in entity
            codeRange.setUsedCount(Math.max(0, codeRange.getUsedCount() - count));
            remainingQuota = Math.max(0, codeRange.getTotalLimit() - codeRange.getUsedCount());
        }

        // Record history log (TC-04)
        Organization orgEntity = new Organization();
        orgEntity.setOrganizationId(currentUser.getOrganizationId());

        LabelCancellationHistory history = LabelCancellationHistory.builder()
                .shipment(shipment)
                .organization(orgEntity)
                .cancelledBy(userEntity)
                .cancelledAt(now)
                .quantity(count)
                .cancellationType(cancellationType)
                .rangeFromCode(rangeFromCode)
                .rangeToCode(rangeToCode)
                .reasonType(request.getReasonType())
                .reasonNote(request.getReasonNote())
                .build();
        labelCancellationHistoryRepository.save(history);

        // Publish activity log
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action("CANCEL_LABELS")
                .description("Đã hủy " + count + " tem in hỏng của lô hàng " + shipment.getName() + " và hoàn trả " + count + " hạn mức.")
                .entityType("Shipment")
                .entityId(shipmentId.toString())
                .ipAddress(IpUtils.getClientIp())
                .timestamp(now)
                .build());

        log.info("Successfully cancelled {} labels for shipment {}, refunded {} quota to org {}",
                count, shipmentId, count, currentUser.getOrganizationId());

        return CancelTraceCodesResponse.builder()
                .shipmentId(shipmentId)
                .totalCancelled(count)
                .refundedQuota(count)
                .remainingQuota(remainingQuota)
                .cancelledAt(now)
                .cancelledBy(currentUser.getFullName())
                .message("Hủy thành công " + count + " tem in hỏng và đã hoàn trả " + count + " hạn mức dải mã.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelCancellationHistoryResponse> getCancellationHistory(UUID shipmentId) {
        permissionChecker.check("shipment", "READ");
        CustomUserDetails currentUser = getCurrentUser();

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng với ID: " + shipmentId));

        if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException("Bạn không có quyền xem thông tin của tổ chức khác.");
        }

        List<LabelCancellationHistory> historyList = labelCancellationHistoryRepository
                .findByShipmentIdAndOrganizationIdOrderByCancelledAtDesc(shipmentId, currentUser.getOrganizationId());

        return historyList.stream()
                .map(h -> LabelCancellationHistoryResponse.builder()
                        .id(h.getId())
                        .shipmentId(h.getShipment().getId())
                        .shipmentName(h.getShipment().getName())
                        .cancelledByName(h.getCancelledBy() != null ? h.getCancelledBy().getFullName() : null)
                        .cancelledAt(h.getCancelledAt())
                        .quantity(h.getQuantity())
                        .cancellationType(h.getCancellationType())
                        .rangeFromCode(h.getRangeFromCode())
                        .rangeToCode(h.getRangeToCode())
                        .reasonType(h.getReasonType())
                        .reasonNote(h.getReasonNote())
                        .build())
                .toList();
    }

    private long parseSequence(String codeValue, String prefix) {
        if (codeValue == null) return -1;
        if (prefix != null && !prefix.isBlank() && codeValue.startsWith(prefix)) {
            String seqStr = codeValue.substring(prefix.length());
            try {
                return Long.parseLong(seqStr);
            } catch (NumberFormatException ignored) {
            }
        }
        int i = codeValue.length() - 1;
        while (i >= 0 && Character.isDigit(codeValue.charAt(i))) {
            i--;
        }
        if (i < codeValue.length() - 1) {
            try {
                return Long.parseLong(codeValue.substring(i + 1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }
}
