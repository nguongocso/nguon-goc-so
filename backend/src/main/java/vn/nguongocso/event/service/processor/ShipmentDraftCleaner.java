package vn.nguongocso.event.service.processor;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.report.repository.DossierExportHistoryRepository;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Component xử lý nghiệp vụ hủy bản nháp lô hàng và hoàn lại dải mã truy xuất.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentDraftCleaner {

    private final ShipmentRepository shipmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final DossierExportHistoryRepository dossierExportHistoryRepository;
    private final CodeRangeRepository codeRangeRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Hủy bản nháp lô hàng, xóa các dữ liệu liên quan và hoàn trả số lượng mã vào dải mã tổ chức.
     *
     * @param draftId     định danh bản nháp lô hàng cần hủy
     * @param currentUser thông tin người dùng thực hiện thao tác
     */
    @Transactional
    public void deleteDraft(UUID draftId, CustomUserDetails currentUser) {
        Shipment shipment = shipmentRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy bản nháp hợp lệ."));

        if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException("Bạn không thuộc tổ chức quản lý của lô hàng này.");
        }

        if (shipment.getParentShipment() != null) {
            throw new BusinessException("Không thể hủy lô con đã được phân bổ từ lô cha.");
        }

        if (shipment.getStatus() != ShipmentStatus.DRAFT && shipment.getStatus() != ShipmentStatus.CODE_PRINTED) {
            throw new BusinessException("Không thể hủy bản nháp vì lô hàng đã được kích hoạt hoặc thu hồi.");
        }

        chainEventRepository.deleteByShipmentId(shipment.getId());
        traceCodeRepository.deleteByShipmentId(shipment.getId());
        dossierExportHistoryRepository.deleteByShipmentId(shipment.getId());

        CodeRange codeRange = codeRangeRepository
                .findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy dải mã của tổ chức."));
        codeRange.setUsedCount(Math.max(0, codeRange.getUsedCount() - shipment.getTotalQuantity()));
        codeRangeRepository.save(codeRange);

        shipmentRepository.delete(shipment);

        publishActivityLog(currentUser, "DELETE_SHIPMENT_DRAFT",
                "Hủy bản nháp lô hàng '" + shipment.getName() + "' và hoàn lại dải mã",
                "SHIPMENT", shipment.getId().toString());

        log.info("Hủy bản nháp lô hàng thành công: id={}, name={}", shipment.getId(), shipment.getName());
    }

    private void publishActivityLog(CustomUserDetails currentUser, String action, String description,
            String entityType, String entityId) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
