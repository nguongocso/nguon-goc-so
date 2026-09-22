package vn.nguongocso.certification.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.IssueInspectionResultEntryLinkRequest;
import vn.nguongocso.certification.dto.response.InspectionResultEntryLinkResponse;
import vn.nguongocso.certification.dto.response.PublicInspectionResultEntryCriterionResponse;
import vn.nguongocso.certification.dto.response.PublicInspectionResultEntryResponse;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.InspectionResultEntryLink;
import vn.nguongocso.certification.entity.TestingUnit;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.enums.InspectionResultEntryLinkStatus;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.InspectionResultEntryLinkRepository;
import vn.nguongocso.certification.repository.TestingUnitRepository;
import vn.nguongocso.certification.service.InspectionResultEntryLinkService;
import vn.nguongocso.certification.service.InspectionResultPortalRateLimitService;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.mail.service.EmailService;

/**
 * Triển khai dịch vụ quản lý liên kết nhập kết quả kiểm nghiệm (NCL-11-CN-007).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InspectionResultEntryLinkServiceImpl implements InspectionResultEntryLinkService {
        private static final String MSG_REQUEST_NOT_FOUND = "Yêu cầu kiểm nghiệm không tồn tại.";
        private static final String MSG_REQUEST_STATUS_INVALID = "Yêu cầu kiểm nghiệm phải ở trạng thái chờ kết quả.";
        private static final String MSG_MISSING_TESTING_UNIT = "Yêu cầu kiểm nghiệm chưa được gán đơn vị kiểm nghiệm.";
        private static final String MSG_LINK_NOT_FOUND = "Liên kết không hợp lệ hoặc không tồn tại.";
        private static final String MSG_NO_LINK_ISSUED = "Yêu cầu kiểm nghiệm chưa từng được cấp liên kết nhập kết quả.";
        private static final String MSG_LINK_EXPIRED = "Liên kết đã hết hạn. Vui lòng liên hệ hợp tác xã để được cấp lại.";
        private static final String MSG_LINK_INACTIVE = "Liên kết đã được sử dụng hoặc đã được thay thế.";
        private static final String MSG_REQUEST_NO_LONGER_PENDING = "Yêu cầu kiểm nghiệm không còn ở trạng thái chờ kết quả.";

        private static final int TOKEN_BYTE_LENGTH = 32;
        private static final String HASH_ALGORITHM = "SHA-256";

        private final InspectionResultEntryLinkRepository linkRepository;
        private final InspectionRequestRepository requestRepository;
        private final TestingUnitRepository testingUnitRepository;
        private final EmailService emailService;
        private final InspectionResultPortalRateLimitService rateLimitService;
        private final ApplicationEventPublisher eventPublisher;
        private final SecureRandom secureRandom = new SecureRandom();

        @Value("${app.frontend-url:http://localhost:5173}")
        private String frontendUrl;

        /**
         * Cấp mới hoặc cấp lại liên kết nhập kết quả kiểm nghiệm cho yêu cầu.
         */
        @Override
        public InspectionResultEntryLinkResponse issueLink(
                        UUID requestId,
                        IssueInspectionResultEntryLinkRequest request,
                        CustomUserDetails currentUser) {
                InspectionRequest inspectionRequest = requestRepository
                                .findByIdAndOrganizationIdForUpdate(requestId, currentUser.getOrganizationId())
                                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, MSG_REQUEST_NOT_FOUND));

                if (inspectionRequest.getStatus() != InspectionRequestStatus.PENDING_RESULT) {
                        throw new BusinessException(HttpStatus.CONFLICT, MSG_REQUEST_STATUS_INVALID);
                }

                if (inspectionRequest.getTestingUnitId() == null) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, MSG_MISSING_TESTING_UNIT);
                }

                TestingUnit testingUnit = testingUnitRepository
                                .findById(inspectionRequest.getTestingUnitId())
                                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                                                MSG_MISSING_TESTING_UNIT));

                int revokedCount = linkRepository.revokeActiveLinksByRequestId(
                                requestId,
                                InspectionResultEntryLinkStatus.ACTIVE,
                                InspectionResultEntryLinkStatus.REVOKED,
                                LocalDateTime.now(),
                                currentUser.getUser());
                boolean isReissue = revokedCount > 0;

                String rawToken = generateRawToken();
                String tokenHash = hashToken(rawToken);
                String tokenPrefix = rawToken.substring(0, Math.min(8, rawToken.length()));

                int expiryDays = (request.getExpiryDays() != null
                                && request.getExpiryDays() >= 1
                                && request.getExpiryDays() <= 30)
                                                ? request.getExpiryDays()
                                                : 7;
                LocalDateTime expiresAt = LocalDateTime.now().plusDays(expiryDays);

                InspectionResultEntryLink link = InspectionResultEntryLink.builder()
                                .inspectionRequest(inspectionRequest)
                                .organization(inspectionRequest.getProductionLot().getOrganization())
                                .testingUnit(testingUnit)
                                .recipientEmail(request.getRecipientEmail().trim())
                                .tokenPrefix(tokenPrefix)
                                .tokenHash(tokenHash)
                                .status(InspectionResultEntryLinkStatus.ACTIVE)
                                .expiresAt(expiresAt)
                                .createdBy(currentUser.getUser())
                                .createdAt(LocalDateTime.now())
                                .build();

                link = linkRepository.save(link);

                String entryUrl = frontendUrl + "/inspection-result-entry/" + rawToken;

                try {
                        String orgName = inspectionRequest.getProductionLot().getOrganization().getName();
                        String testingUnitName = testingUnit.getName();
                        String lotCode = inspectionRequest.getProductionLot().getName();

                        emailService.sendInspectionResultEntryEmail(
                                        request.getRecipientEmail().trim(),
                                        orgName,
                                        testingUnitName,
                                        lotCode,
                                        entryUrl,
                                        expiryDays);
                } catch (Exception e) {
                        log.error("Lỗi khi gửi email liên kết nhập kết quả: {}", e.getMessage());
                }

                publishActivityLog(
                                currentUser,
                                isReissue ? "REISSUE_INSPECTION_RESULT_LINK" : "ISSUE_INSPECTION_RESULT_LINK",
                                (isReissue ? "Cấp lại" : "Cấp") + " liên kết nhập kết quả kiểm nghiệm cho đơn vị '"
                                                + testingUnit.getName() + "' (email: "
                                                + maskEmail(request.getRecipientEmail().trim()) + ")",
                                "INSPECTION_RESULT_ENTRY_LINK",
                                link.getId().toString());

                return InspectionResultEntryLinkResponse.builder()
                                .id(link.getId())
                                .status(link.getStatus())
                                .recipientEmail(link.getRecipientEmail())
                                .tokenPrefix(link.getTokenPrefix())
                                .expiresAt(link.getExpiresAt())
                                .usedAt(link.getUsedAt())
                                .createdAt(link.getCreatedAt())
                                .entryUrl(entryUrl)
                                .build();
        }

        /**
         * Lấy thông tin liên kết mới nhất của yêu cầu kiểm nghiệm.
         */
        @Override
        @Transactional(readOnly = true)
        public InspectionResultEntryLinkResponse getLatestLink(
                        UUID requestId,
                        CustomUserDetails currentUser) {
                InspectionRequest inspectionRequest = requestRepository
                                .findByIdAndProductionLot_Organization_OrganizationId(requestId,
                                                currentUser.getOrganizationId())
                                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, MSG_REQUEST_NOT_FOUND));

                InspectionResultEntryLink link = linkRepository
                                .findFirstByInspectionRequest_IdOrderByCreatedAtDesc(requestId)
                                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, MSG_NO_LINK_ISSUED));

                InspectionResultEntryLinkStatus effectiveStatus = link.getStatus();
                if (effectiveStatus == InspectionResultEntryLinkStatus.ACTIVE
                                && LocalDateTime.now().isAfter(link.getExpiresAt())) {
                        effectiveStatus = InspectionResultEntryLinkStatus.EXPIRED;
                }

                return InspectionResultEntryLinkResponse.builder()
                                .id(link.getId())
                                .status(effectiveStatus)
                                .recipientEmail(link.getRecipientEmail())
                                .tokenPrefix(link.getTokenPrefix())
                                .expiresAt(link.getExpiresAt())
                                .usedAt(link.getUsedAt())
                                .createdAt(link.getCreatedAt())
                                .entryUrl(null)
                                .build();
        }

        /**
         * Lấy thông tin công khai của yêu cầu kiểm nghiệm qua token bí mật.
         */
        @Override
        @Transactional(readOnly = true)
        public PublicInspectionResultEntryResponse getPublicPortalData(
                        String rawToken,
                        String clientIp) {
                InspectionResultEntryLink link = validateAndGetActiveLink(rawToken, clientIp);
                InspectionRequest request = link.getInspectionRequest();

                if (request.getStatus() != InspectionRequestStatus.PENDING_RESULT) {
                        throw new BusinessException(HttpStatus.CONFLICT, MSG_REQUEST_NO_LONGER_PENDING);
                }

                List<PublicInspectionResultEntryCriterionResponse> criteria = request.getCriteria().stream()
                                .map(c -> PublicInspectionResultEntryCriterionResponse.builder()
                                                .criterionId(c.getId())
                                                .code(c.getCriterionCode())
                                                .name(c.getCriterionName())
                                                .standardName(c.getStandard() != null ? c.getStandard().getName()
                                                                : null)
                                                .build())
                                .collect(Collectors.toList());

                String testingUnitName = link.getTestingUnit() != null
                                ? link.getTestingUnit().getName()
                                : request.getInspectionUnit();

                String lotCode = request.getProductionLot() != null
                                ? request.getProductionLot().getName()
                                : "";

                return PublicInspectionResultEntryResponse.builder()
                                .testingUnitName(testingUnitName)
                                .testingUnit(testingUnitName)
                                .lotCode(lotCode)
                                .lotName(lotCode)
                                .sampleSentDate(request.getSampleSentDate())
                                .expiresAt(link.getExpiresAt())
                                .criteria(criteria)
                                .build();
        }

        /**
         * Xác thực token và lấy thực thể liên kết đang hoạt động còn hạn.
         */
        @Override
        public InspectionResultEntryLink validateAndGetActiveLink(
                        String rawToken,
                        String clientIp) {
                rateLimitService.checkIpRateLimit(clientIp);

                String tokenHash = hashToken(rawToken);
                InspectionResultEntryLink link = linkRepository.findByTokenHash(tokenHash)
                                .orElseThrow(() -> {
                                        rateLimitService.recordInvalidTokenAttempt(clientIp);
                                        return new BusinessException(HttpStatus.NOT_FOUND, MSG_LINK_NOT_FOUND);
                                });

                rateLimitService.checkTokenRateLimit(tokenHash);

                if (LocalDateTime.now().isAfter(link.getExpiresAt())
                                || link.getStatus() == InspectionResultEntryLinkStatus.EXPIRED) {
                        throw new BusinessException(HttpStatus.GONE, MSG_LINK_EXPIRED);
                }

                if (link.getStatus() == InspectionResultEntryLinkStatus.USED
                                || link.getStatus() == InspectionResultEntryLinkStatus.REVOKED) {
                        throw new BusinessException(HttpStatus.GONE, MSG_LINK_INACTIVE);
                }

                InspectionRequest request = link.getInspectionRequest();
                if (request == null || request.getProductionLot() == null
                                || request.getProductionLot().getOrganization() == null
                                || !link.getOrganization().getOrganizationId()
                                                .equals(request.getProductionLot().getOrganization()
                                                                .getOrganizationId())) {
                        throw new BusinessException(HttpStatus.NOT_FOUND, MSG_LINK_NOT_FOUND);
                }

                if (request.getTestingUnitId() != null && link.getTestingUnit() != null
                                && !link.getTestingUnit().getId().equals(request.getTestingUnitId())) {
                        throw new BusinessException(HttpStatus.NOT_FOUND, MSG_LINK_NOT_FOUND);
                }

                return link;
        }

        /**
         * Thu hồi tất cả các liên kết đang hoạt động của yêu cầu kiểm nghiệm.
         */
        @Override
        public void revokeActiveLinksForRequest(UUID requestId, User actor) {
                linkRepository.revokeActiveLinksByRequestId(
                                requestId,
                                InspectionResultEntryLinkStatus.ACTIVE,
                                InspectionResultEntryLinkStatus.REVOKED,
                                LocalDateTime.now(),
                                actor);
        }

        /**
         * Băm chuỗi token bằng thuật toán SHA-256.
         */
        @Override
        public String hashToken(String rawToken) {
                if (rawToken == null || rawToken.isBlank()) {
                        return "";
                }
                try {
                        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
                        byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
                        StringBuilder hexString = new StringBuilder();
                        for (byte b : hash) {
                                String hex = Integer.toHexString(0xff & b);
                                if (hex.length() == 1) {
                                        hexString.append('0');
                                }
                                hexString.append(hex);
                        }
                        return hexString.toString();
                } catch (NoSuchAlgorithmException e) {
                        throw new IllegalStateException("Thuật toán SHA-256 không khả dụng trên hệ thống", e);
                }
        }

        /**
         * Sinh chuỗi token ngẫu nhiên bảo mật cao.
         */
        private String generateRawToken() {
                byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
                secureRandom.nextBytes(randomBytes);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        }

        /**
         * Ẩn một phần địa chỉ email để bảo mật thông tin nhật ký.
         */
        private String maskEmail(String email) {
                if (email == null || !email.contains("@")) {
                        return "***";
                }
                int atIndex = email.indexOf('@');
                String name = email.substring(0, atIndex);
                String domain = email.substring(atIndex);
                if (name.length() <= 2) {
                        return name.charAt(0) + "***" + domain;
                }
                return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
        }

        /**
         * Ghi nhật ký hoạt động vào hệ thống.
         */
        private void publishActivityLog(
                        CustomUserDetails currentUser,
                        String action,
                        String description,
                        String entityType,
                        String entityId) {
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
