package vn.nguongocso.alert.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.dto.request.ResolveAlertRequest;
import vn.nguongocso.alert.dto.response.AlertListResponse;
import vn.nguongocso.alert.dto.response.AlertResponse;
import vn.nguongocso.alert.dto.response.ResolveAlertResponse;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.alert.service.AlertService;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.dto.response.OrganizationProfileResponse;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.organization.service.OrganizationService;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/** Triển khai dịch vụ quản lý cảnh báo. */
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {
        private static final String ADMIN_ROLE = "VT-01";
        private static final String ORG_MANAGER_ROLE = "VT-02";

        private final AlertRepository alertRepository;
        private final UserRepository userRepository;
        private final TraceCodeRepository traceCodeRepository;
        private final OrganizationUserRepository organizationUserRepository;
        private final ObjectMapper objectMapper;
        private final CertificationRepository certificationRepository;
        private final OrganizationService organizationService;

        /** Lấy danh sách cảnh báo quét bất thường. */
        @Override
        public AlertListResponse getAlerts(
                        AlertType type,
                        AlertStatus status,
                        LocalDate fromDate,
                        LocalDate toDate,
                        UUID organizationId,
                        Pageable pageable) {

                User currentUser = getCurrentUser();

                List<OrganizationUser> organizationUsers = getOrganizationUsers(currentUser);

                if (isAdmin(organizationUsers)) {

                        // VT-01 được xem toàn bộ cảnh báo.
                        organizationId = null;

                } else if (isOrganizationManager(organizationUsers)) {

                        // VT-02 chỉ xem cảnh báo của tổ chức hiện tại.
                        OrganizationProfileResponse profile = organizationService.getCurrentOrganizationProfile();

                        organizationId = profile.getOrganizationId();

                } else {

                        throw new BusinessException(
                                        "Bạn không có quyền xem cảnh báo.");
                }

                Page<Alert> alertPage = findAlerts(
                                type,
                                status,
                                fromDate,
                                toDate,
                                organizationId,
                                pageable);

                AlertListResponse response = new AlertListResponse();

                response.setContent(
                                alertPage.getContent()
                                                .stream()
                                                .map(this::toAlertResponse)
                                                .toList());

                response.setTotalElements((int) alertPage.getTotalElements());
                response.setTotalPages(alertPage.getTotalPages());
                response.setPage(alertPage.getNumber());
                response.setSize(alertPage.getSize());

                return response;
        }

        /** Xử lý cảnh báo. */
        @Override
        @Transactional
        public ResolveAlertResponse resolveAlert(
                        UUID alertId,
                        ResolveAlertRequest request) {

                Alert alert = alertRepository.findById(alertId)
                                .orElseThrow(() -> new BusinessException("Cảnh báo không tồn tại."));

                if (alert.getStatus() != AlertStatus.PENDING) {
                        throw new BusinessException(
                                        "Cảnh báo không thể xử lý.");
                }

                User currentUser = getCurrentUser();

                List<OrganizationUser> organizationUsers = getOrganizationUsers(currentUser);

                if (!isAdmin(organizationUsers)) {

                        if (!isOrganizationManager(organizationUsers)) {
                                throw new BusinessException(
                                                "Bạn không có quyền xử lý cảnh báo.");
                        }

                        // Lấy organization hiện tại của user
                        OrganizationProfileResponse profile = organizationService.getCurrentOrganizationProfile();

                        UUID currentOrganizationId = profile.getOrganizationId();

                        // Kiểm tra alert thuộc organization hiện tại
                        if (!isAlertBelongToOrganization(
                                        alert,
                                        currentOrganizationId)) {

                                throw new BusinessException(
                                                "Bạn không có quyền xử lý cảnh báo này.");
                        }
                }

                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolvedAt(LocalDateTime.now());
                alert.setResolvedBy(currentUser.getUserId());

                Alert resolvedAlert = alertRepository.save(alert);

                return toResolveAlertResponse(resolvedAlert);
        }

        /** Lấy người dùng đang đăng nhập. */
        private User getCurrentUser() {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null
                                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {

                        throw new BusinessException(
                                        "Người dùng chưa đăng nhập.");
                }

                return userRepository.findById(
                                userDetails.getUser().getUserId())
                                .orElseThrow(() -> new BusinessException(
                                                "Người dùng không tồn tại."));
        }

        /** Lấy tất cả vai trò của người dùng trong các tổ chức. */
        private List<OrganizationUser> getOrganizationUsers(
                        User user) {

                return organizationUserRepository.findAllByUser_UserId(
                                user.getUserId());
        }

        /** Kiểm tra người dùng có vai trò quản trị. */
        private boolean isAdmin(
                        List<OrganizationUser> organizationUsers) {

                return organizationUsers.stream()
                                .anyMatch(organizationUser -> ADMIN_ROLE.equals(
                                                organizationUser.getRole().getCode()));
        }

        /** Kiểm tra người dùng có vai trò quản lý tổ chức. */
        private boolean isOrganizationManager(
                        List<OrganizationUser> organizationUsers) {

                return organizationUsers.stream()
                                .anyMatch(organizationUser -> ORG_MANAGER_ROLE.equals(
                                                organizationUser.getRole().getCode()));
        }

        /** Kiểm tra cảnh báo có thuộc tổ chức hay không. */
        private boolean isAlertBelongToOrganization(
                        Alert alert,
                        UUID organizationId) {

                if (alert.getRelatedEntityId() == null) {
                        return false;
                }

                if (alert.getType() == AlertType.SCAN_ANOMALY) {
                        TraceCode traceCode = traceCodeRepository
                                        .findById(alert.getRelatedEntityId())
                                        .orElseThrow(() -> new BusinessException(
                                                        "Mã truy xuất không tồn tại."));

                        UUID alertOrganizationId = traceCode.getShipment()
                                        .getOrganization()
                                        .getOrganizationId();

                        return alertOrganizationId.equals(organizationId);
                } else {
                        Certification cert = certificationRepository
                                        .findById(alert.getRelatedEntityId())
                                        .orElseThrow(() -> new BusinessException(
                                                        "Chứng nhận không tồn tại."));

                        UUID alertOrganizationId = cert.getOrganization()
                                        .getOrganizationId();

                        return alertOrganizationId.equals(organizationId);
                }
        }

        /** Tìm danh sách cảnh báo theo điều kiện. */
        private Page<Alert> findAlerts(
                        AlertType type,
                        AlertStatus status,
                        LocalDate fromDate,
                        LocalDate toDate,
                        UUID organizationId,
                        Pageable pageable) {

                LocalDateTime from = fromDate != null
                                ? fromDate.atStartOfDay()
                                : null;

                LocalDateTime to = toDate != null
                                ? toDate.atTime(23, 59, 59)
                                : null;

                return alertRepository.searchAlerts(
                                type,
                                status,
                                organizationId,
                                from,
                                to,
                                pageable);
        }

        /** Chuyển Alert sang dữ liệu phản hồi. */
        private AlertResponse toAlertResponse(Alert alert) {

                AlertResponse response = new AlertResponse();

                response.setId(alert.getId());

                response.setType(alert.getType());

                response.setRelatedEntityType(alert.getRelatedEntityType());
                response.setRelatedEntityId(alert.getRelatedEntityId());

                response.setSeverity(alert.getSeverity());
                try {
                        response.setDetails(
                                        objectMapper.readValue(
                                                        alert.getDetails(),
                                                        Object.class));
                } catch (JsonProcessingException e) {
                        throw new BusinessException(
                                        "Không thể đọc dữ liệu cảnh báo.");
                }

                response.setStatus(alert.getStatus());

                response.setCreatedAt(alert.getCreatedAt());
                response.setResolvedAt(alert.getResolvedAt());
                response.setResolvedBy(alert.getResolvedBy());

                return response;
        }

        /** Chuyển Alert sang dữ liệu phản hồi xử lý. */
        private ResolveAlertResponse toResolveAlertResponse(Alert alert) {

                ResolveAlertResponse response = new ResolveAlertResponse();

                response.setId(alert.getId());

                response.setStatus(alert.getStatus());

                response.setResolvedAt(alert.getResolvedAt());
                response.setResolvedBy(alert.getResolvedBy());

                return response;
        }
}