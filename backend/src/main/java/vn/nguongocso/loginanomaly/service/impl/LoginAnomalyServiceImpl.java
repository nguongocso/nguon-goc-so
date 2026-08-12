package vn.nguongocso.loginanomaly.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.alert.enums.NotificationType;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.loginanomaly.dto.response.LockLoginAnomalyResponse;
import vn.nguongocso.loginanomaly.dto.response.LoginAnomalyResponse;
import vn.nguongocso.loginanomaly.entity.LoginAnomaly;
import vn.nguongocso.loginanomaly.entity.LoginAttempt;
import vn.nguongocso.loginanomaly.enums.LoginAnomalySeverity;
import vn.nguongocso.loginanomaly.repository.LoginAnomalyRepository;
import vn.nguongocso.loginanomaly.repository.LoginAttemptRepository;
import vn.nguongocso.loginanomaly.service.LoginAnomalyService;
import vn.nguongocso.loginanomaly.util.IpLocationUtils;
import vn.nguongocso.notification.entity.Notification;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/**
 * Triển khai dịch vụ theo dõi đăng nhập bất thường.
 *
 * <p>
 * Ngưỡng phát hiện: 5 lần đăng nhập sai liên tiếp trong 2 phút (TC-01).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoginAnomalyServiceImpl implements LoginAnomalyService {

    /** Số lần thất bại tối đa trong cửa sổ trước khi đánh dấu bất thường. */
    static final int FAILURE_THRESHOLD = 5;

    /** Cửa sổ thời gian theo dõi các lần thất bại. */
    static final Duration FAILURE_WINDOW = Duration.ofMinutes(2);

    private static final String WRONG_PASSWORD_REASON = "Sai mật khẩu";

    private static final String ANOMALY_REASON =
            "Đăng nhập sai 5 lần liên tiếp trong 2 phút";

    private static final String PLATFORM_ADMIN_ROLE = "VT-01";

    private final LoginAttemptRepository loginAttemptRepository;

    private final LoginAnomalyRepository loginAnomalyRepository;

    private final UserRepository userRepository;

    private final OrganizationUserRepository organizationUserRepository;

    private final NotificationRepository notificationRepository;

    private final ApplicationEventPublisher eventPublisher;

    // =========================================================
    // 1. LẤY DANH SÁCH BẤT THƯỜNG
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoginAnomalyResponse> getAnomalies(
            LoginAnomalySeverity severity,
            UserStatus accountStatus,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {

        CustomUserDetails currentUser = getCurrentUser();

        /*
         * VT-01 (admin) xem toàn nền tảng.
         * VT-02 (quản lý HTX) chỉ xem dữ liệu thuộc tổ chức của mình (TC-03).
         */
        UUID scopeOrganizationId = PLATFORM_ADMIN_ROLE.equals(currentUser.getRoleCode())
                ? null
                : currentUser.getOrganizationId();

        String normalizedKeyword = StringUtils.hasText(keyword)
                ? keyword.trim()
                : null;

        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));

        Page<LoginAnomaly> result = loginAnomalyRepository.findWithFilters(
                scopeOrganizationId,
                severity,
                accountStatus,
                normalizedKeyword,
                from,
                to,
                pageable);

        Map<UUID, UserStatus> statusMap = loadUserStatusMap(result.getContent());

        List<LoginAnomalyResponse> items = result.getContent().stream()
                .map(anomaly -> LoginAnomalyResponse.from(anomaly, statusMap))
                .toList();

        return PageResponse.from(result, items);
    }

    // =========================================================
    // 2. KHÓA TẠM TÀI KHOẢN
    // =========================================================

    @Override
    public LockLoginAnomalyResponse lockAnomaly(UUID anomalyId) {

        LoginAnomaly anomaly = loginAnomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new BusinessException(
                        "Bản ghi đăng nhập bất thường không tồn tại"));

        User user = userRepository.findByUserName(anomaly.getUsername())
                .orElseThrow(() -> new BusinessException(
                        "Không tìm thấy tài khoản \"" + anomaly.getUsername() + "\""));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException("Tài khoản đã bị khóa");
        }

        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);

        CustomUserDetails actor = getCurrentUser();

        // Ghi vào lịch sử hoạt động (TC-04)
        publishActivityLog(anomaly, user, actor);

        // Thông báo cho chủ tài khoản (TC-02)
        notifyAccountOwner(user);

        log.info(
                "Đã khóa tạm tài khoản: username={}, lockedBy={}",
                user.getUserName(),
                actor.getUsername());

        return LockLoginAnomalyResponse.builder()
                .id(anomaly.getAnomalyId())
                .username(user.getUserName())
                .accountStatus(UserStatus.LOCKED)
                .lockedAt(LocalDateTime.now())
                .lockedBy(actor.getFullName())
                .build();
    }

    // =========================================================
    // 3. GHI NHẬN ĐĂNG NHẬP THẤT BẠI + PHÁT HIỆN BẤT THƯỜNG
    // =========================================================

    @Override
    public void recordFailedLogin(String username, String ipAddress) {

        User user = userRepository.findByUserName(username).orElse(null);

        LoginAttempt attempt = LoginAttempt.builder()
                .username(username)
                .userId(user != null ? user.getUserId() : null)
                .ipAddress(ipAddress)
                .reason(WRONG_PASSWORD_REASON)
                .failedAt(LocalDateTime.now())
                .build();
        loginAttemptRepository.save(attempt);

        long failedCount = loginAttemptRepository.countByUsernameAndFailedAtAfter(
                username,
                LocalDateTime.now().minus(FAILURE_WINDOW));

        /*
         * Chỉ đánh dấu một lần ở lần thất bại thứ 5 để tránh tạo
         * nhiều bản ghi trùng lặp cho cùng một đợt thất bại (TC-01).
         */
        if (failedCount == FAILURE_THRESHOLD) {
            createAnomaly(user, username, ipAddress);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /** Tạo bản ghi bất thường khi đạt ngưỡng. */
    private void createAnomaly(User user, String username, String ipAddress) {

        OrganizationUser membership = firstActiveMembership(user);

        LoginAnomaly anomaly = LoginAnomaly.builder()
                .userId(user != null ? user.getUserId() : null)
                .username(username)
                .fullName(user != null ? user.getFullName() : null)
                .organizationId(membership != null
                        ? membership.getOrganization().getOrganizationId()
                        : null)
                .organizationName(membership != null
                        ? membership.getOrganization().getName()
                        : null)
                .ipAddress(ipAddress)
                .location(IpLocationUtils.infer(ipAddress))
                .reason(ANOMALY_REASON)
                .severity(LoginAnomalySeverity.HIGH)
                .loginAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        loginAnomalyRepository.save(anomaly);

        // Thông báo cho quản trị viên nền tảng (TC-01)
        notifyPlatformAdmins(user, username, ipAddress);

        log.warn(
                "Phát hiện đăng nhập bất thường: username={}, ip={}",
                username,
                ipAddress);
    }

    /** Lấy membership ACTIVE đầu tiên của user (để lấy thông tin tổ chức). */
    private OrganizationUser firstActiveMembership(User user) {
        if (user == null) {
            return null;
        }
        List<OrganizationUser> memberships = organizationUserRepository
                .findByUser_UserIdAndStatus(user.getUserId(), OrganizationUserStatus.ACTIVE);
        return memberships.isEmpty() ? null : memberships.get(0);
    }

    /** Ghi lịch sử hoạt động khi khóa tạm tài khoản (TC-04). */
    private void publishActivityLog(LoginAnomaly anomaly, User user, CustomUserDetails actor) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(actor.getUserId())
                .username(actor.getUsername())
                .fullName(actor.getFullName())
                .organizationId(actor.getOrganizationId())
                .action("LOCK_ACCOUNT")
                .description("Khóa tạm tài khoản \"" + user.getUserName()
                        + "\" do đăng nhập bất thường")
                .entityType("User")
                .entityId(user.getUserId().toString())
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }

    /** Thông báo cho chủ tài khoản khi bị khóa tạm (TC-02). */
    private void notifyAccountOwner(User user) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.ALERT);
        notification.setTitle("Tài khoản đã bị khóa tạm");
        notification.setContent("Tài khoản \"" + user.getUserName()
                + "\" của bạn đã bị khóa tạm do hoạt động đăng nhập bất thường. "
                + "Vui lòng liên hệ quản trị viên để mở khóa.");
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    /** Thông báo cho quản trị viên nền tảng (VT-01) khi phát hiện bất thường (TC-01). */
    private void notifyPlatformAdmins(User user, String username, String ipAddress) {
        List<OrganizationUser> adminMemberships = organizationUserRepository
                .findAllByRole_Code(PLATFORM_ADMIN_ROLE);

        List<User> admins = adminMemberships.stream()
                .map(OrganizationUser::getUser)
                .distinct()
                .toList();

        if (admins.isEmpty()) {
            log.debug("Không có quản trị viên nền tảng nào để nhận thông báo bất thường");
            return;
        }

        for (User admin : admins) {
            Notification notification = new Notification();
            notification.setUser(admin);
            notification.setType(NotificationType.ALERT);
            notification.setTitle("Phát hiện đăng nhập bất thường");
            notification.setContent("Tài khoản \"" + username + "\" đã đăng nhập sai 5 lần "
                    + "liên tiếp trong 2 phút (IP: " + ipAddress + "). "
                    + "Tài khoản đã được đưa vào danh sách bất thường.");
            notification.setIsRead(false);
            notificationRepository.save(notification);
        }
    }

    /** Nạp bản đồ userId -> trạng thái tài khoản từ bảng users. */
    private Map<UUID, UserStatus> loadUserStatusMap(List<LoginAnomaly> anomalies) {
        Map<UUID, UserStatus> statusMap = new HashMap<>();

        List<UUID> userIds = anomalies.stream()
                .map(LoginAnomaly::getUserId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds)
                    .forEach(user -> statusMap.put(user.getUserId(), user.getStatus()));
        }

        return statusMap;
    }

    /** Lấy user hiện tại từ SecurityContext. */
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new BusinessException("Bạn chưa đăng nhập");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
