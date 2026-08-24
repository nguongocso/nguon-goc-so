package vn.nguongocso.trace.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.service.ActivityLogService;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.trace.dto.request.RecallRequest;
import vn.nguongocso.trace.dto.response.RecallInfoResponse;
import vn.nguongocso.trace.dto.response.RecallResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Recall;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.RecallStatus;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.RecallRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.ShipmentRecallService;
import vn.nguongocso.alert.dto.request.ActivityLogRequest;

/**
 * Triển khai dịch vụ thu hồi lô hàng.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ShipmentRecallServiceImpl implements ShipmentRecallService {
    private static final String ORG_MANAGER_ROLE = "VT-02";

    private static final String MSG_SHIPMENT_NOT_FOUND = "Không tìm thấy lô hàng.";
    private static final String MSG_SHIPMENT_ALREADY_RECALLED = "Lô hàng đã được thu hồi trước đó.";
    private static final String MSG_USER_NOT_FOUND = "Người dùng không tồn tại.";
    private static final String MSG_NO_PERMISSION = "Bạn không có quyền thu hồi lô hàng.";
    private static final String MSG_NO_PERMISSION_OTHER_ORG = "Bạn không có quyền thu hồi lô hàng của tổ chức khác.";

    private final ShipmentRepository shipmentRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final CodeRangeRepository codeRangeRepository;
    private final RecallRepository recallRepository;
    private final UserRepository userRepository;
    private final NotificationService alertNotificationService;
    private final ActivityLogService activityLogService;

    /*
     * {@inheritDoc}
     */
    @Override
    public RecallResponse recallShipment(
            UUID shipmentId,
            RecallRequest request,
            String ipAddress) {

        CustomUserDetails currentUser = getCurrentUser();

        // 1. Kiểm tra quyền
        validateRole(currentUser);

        // 2. Tìm lô hàng
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(MSG_SHIPMENT_NOT_FOUND));

        // 3. Kiểm tra tổ chức
        validateOrganization(currentUser, shipment);

        // 4. Không cho phép thu hồi lại
        if (shipment.getStatus() == ShipmentStatus.RECALLED) {
            throw new BusinessException(MSG_SHIPMENT_ALREADY_RECALLED);
        }

        // 5. Lấy user thực hiện thao tác
        User actor = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(MSG_USER_NOT_FOUND));

        // 6. Tạo bản ghi thu hồi
        Recall recall = new Recall();

        recall.setShipment(shipment);
        recall.setReason(request.getReason());
        recall.setRecalledBy(actor);
        recall.setRecalledAt(LocalDateTime.now());
        recall.setStatus(RecallStatus.ACTIVE);

        recallRepository.save(recall);

        // 7. Cập nhật trạng thái Shipment
        shipment.setStatus(ShipmentStatus.RECALLED);
        shipmentRepository.save(shipment);

        // 8. Cập nhật trạng thái toàn bộ TraceCode
        List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(shipmentId);

        traceCodes.forEach(code -> code.setStatus(TraceCodeStatus.RECALLED));

        traceCodeRepository.saveAll(traceCodes);

        // 8b. Trả lại mã đã dùng cho dải mã của tổ chức
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

        // 9. Ghi lịch sử hoạt động
        ActivityLogRequest activityLogRequest = ActivityLogRequest.builder()
                .userId(actor.getUserId())
                .username(actor.getUserName())
                .fullName(actor.getFullName())
                .organizationId(
                        shipment.getOrganization().getOrganizationId())
                .action("RECALL_SHIPMENT")
                .description(
                        "Thu hồi lô hàng \"" +
                                shipment.getName() +
                                "\". Lý do: " +
                                request.getReason())
                .entityType("SHIPMENT")
                .entityId(shipment.getId())
                .ipAddress(ipAddress)
                .build();

        activityLogService.logActivity(activityLogRequest);

        // 10. Gửi thông báo
        alertNotificationService.sendShipmentRecallNotification(recall);

        // 11. Trả response
        RecallResponse response = new RecallResponse();

        response.setId(recall.getId());
        response.setShipmentId(shipment.getId());
        response.setReason(recall.getReason());
        response.setRecalledBy(actor.getUserId());
        response.setRecalledAt(recall.getRecalledAt());
        response.setStatus(recall.getStatus().name());
        response.setShipmentStatus(shipment.getStatus().name());
        response.setTraceCodesUpdated(traceCodes.size());

        return response;
    }

    /**
     * Lấy thông tin thu hồi của một lô hàng.
     */
    @Override
    public RecallInfoResponse getRecallInfo(UUID shipmentId) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(MSG_SHIPMENT_NOT_FOUND));

        RecallInfoResponse response = new RecallInfoResponse();
        response.setShipmentId(shipmentId);

        recallRepository.findByShipmentId(shipmentId)
                .ifPresentOrElse(recall -> {
                    response.setRecalled(true);
                    response.setReason(recall.getReason());
                    response.setRecalledAt(recall.getRecalledAt());
                }, () -> {
                    response.setRecalled(false);
                    response.setReason(null);
                    response.setRecalledAt(null);
                });

        return response;
    }

    /**
     * Lấy thông tin người dùng hiện tại từ SecurityContext.
     */
    private CustomUserDetails getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return (CustomUserDetails) authentication.getPrincipal();
    }

    /**
     * Kiểm tra người dùng có vai trò VT-02 (Quản lý HTX) hay không.
     */
    private void validateRole(CustomUserDetails currentUser) {

        if (!ORG_MANAGER_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(MSG_NO_PERMISSION);
        }
    }

    /**
     * Kiểm tra người dùng thuộc tổ chức sở hữu lô hàng.
     */
    private void validateOrganization(
            CustomUserDetails currentUser,
            Shipment shipment) {

        if (!shipment.getOrganization()
                .getOrganizationId()
                .equals(currentUser.getOrganizationId())) {

            throw new BusinessException(MSG_NO_PERMISSION_OTHER_ORG);
        }
    }
}