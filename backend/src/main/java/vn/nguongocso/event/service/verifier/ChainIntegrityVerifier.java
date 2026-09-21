package vn.nguongocso.event.service.verifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.dto.response.ChainVerificationResponse;
import vn.nguongocso.event.dto.response.EventVerificationItem;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventHashService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Verifier chuyên trách kiểm chứng tính toàn vẹn chuỗi băm liên kết (QTN-19 / NCL-08-CN-006).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainIntegrityVerifier {

    private final ShipmentRepository shipmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final EventHashService eventHashService;
    private final OrganizationUserRepository organizationUserRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Kiểm chứng tính toàn vẹn dòng sự kiện của một lô hàng.
     */
    public ChainVerificationResponse verifyChainIntegrity(UUID shipmentId, CustomUserDetails currentUser) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy lô hàng."));

        String role = currentUser.getRoleCode();
        if ("VT-01".equals(role) || "VT-05".equals(role)) {
            // Platform admin & industry regulator may verify any shipment
        } else if ("VT-04".equals(role)) {
            validateStorageProcurementRelationship(shipment, currentUser);
        } else {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền kiểm chứng dòng sự kiện của lô này.");
        }

        List<ChainEvent> events = chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipmentId)
                .stream()
                .sorted(eventHashService.eventOrdering())
                .toList();

        if (events.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Lô hàng chưa có sự kiện nào để kiểm chứng.");
        }

        LocalDateTime verifiedAt = LocalDateTime.now();
        String previousHash = "";
        boolean verified = true;
        Integer failedIndex = null;
        UUID failedEventId = null;
        String failureReason = null;

        List<EventVerificationItem> verificationItems = new ArrayList<>();

        for (int i = 0; i < events.size(); i++) {
            ChainEvent event = events.get(i);
            int index = i + 1;

            String expectedHash = eventHashService.calculateHash(event, previousHash);
            String storedHash = event.getHash();
            String storedPrevious = event.getPreviousHash();

            boolean isPreviousValid = (i == 0)
                    ? (storedPrevious == null || storedPrevious.isEmpty())
                    : previousHash.equals(storedPrevious != null ? storedPrevious : "");

            boolean isHashValid = expectedHash.equals(storedHash != null ? storedHash : "");

            boolean isValid = isPreviousValid && isHashValid;

            EventVerificationItem.EventVerificationItemBuilder itemBuilder = EventVerificationItem.builder()
                    .index(index)
                    .eventId(event.getId())
                    .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                    .recordedAt(event.getRecordedAt())
                    .hash(storedHash)
                    .previousHash(event.getPreviousHash())
                    .isValid(isValid);

            if (!isValid && verified) {
                verified = false;
                failedIndex = index;
                failedEventId = event.getId();
                if (!isPreviousValid) {
                    failureReason = "Previous hash mismatch: expected " + previousHash + ", got "
                            + (storedPrevious != null ? storedPrevious : "") + ".";
                } else if (!isHashValid) {
                    failureReason = "Hash mismatch: expected " + expectedHash + ", got "
                            + (storedHash != null ? storedHash : "") + ".";
                }
                itemBuilder.expectedHash(expectedHash);
            } else if (!isValid) {
                itemBuilder.expectedHash(expectedHash);
            }

            verificationItems.add(itemBuilder.build());
            previousHash = expectedHash;
        }

        ChainVerificationResponse response = ChainVerificationResponse.builder()
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .totalEvents(events.size())
                .isIntegrityVerified(verified)
                .verificationStatus(verified ? "INTACT" : "BROKEN")
                .failedEventIndex(failedIndex)
                .failedEventId(failedEventId)
                .failureReason(failureReason)
                .verifiedAt(verifiedAt)
                .hashAlgorithm(EventHashService.HASH_ALGORITHM)
                .events(verificationItems)
                .build();

        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action("VERIFY_CHAIN")
                .description("Kiểm chứng dòng sự kiện lô hàng: " + shipment.getName())
                .entityType("SHIPMENT")
                .entityId(shipment.getId().toString())
                .ipAddress(IpUtils.getClientIp())
                .timestamp(verifiedAt)
                .build());

        return response;
    }

    private void validateStorageProcurementRelationship(Shipment shipment, CustomUserDetails currentUser) {
        UUID currentOrgId = currentUser.getOrganizationId();
        if (currentOrgId == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không thuộc tổ chức nào, không thể thực hiện thao tác này.");
        }

        boolean hasProcurementEvent = chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipment.getId())
                .stream()
                .filter(e -> e.getEventType() == ChainEventType.PROCUREMENT)
                .anyMatch(e -> {
                    if (e.getRecordedBy() == null) {
                        return false;
                    }
                    return organizationUserRepository
                            .findByOrganization_OrganizationIdAndUser_UserId(
                                    currentOrgId, e.getRecordedBy().getUserId())
                            .isPresent();
                });

        if (!hasProcurementEvent) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền kiểm chứng dòng sự kiện của lô này. Chỉ doanh nghiệp đã thu mua lô hàng mới được thực hiện.");
        }
    }
}
