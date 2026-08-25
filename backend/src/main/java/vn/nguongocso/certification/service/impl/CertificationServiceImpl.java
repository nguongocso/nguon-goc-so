package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.AttachCertificationRequest;
import vn.nguongocso.certification.dto.request.CreateCertificationRequest;
import vn.nguongocso.certification.dto.response.CertificationResponse;
import vn.nguongocso.certification.dto.response.ProductionLotCertificationResponse;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.certification.repository.StandardRepository;
import vn.nguongocso.certification.service.CertificationService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lớp CertificationServiceImpl triển khai các phương thức của
 * CertificationService.
 * Nó chịu trách nhiệm quản lý chứng nhận, bao gồm việc gắn chứng nhận cho lô
 * sản xuất,
 * tạo mới chứng nhận, kiểm tra hạn hiệu lực và tạo cảnh báo liên quan đến chứng
 * nhận.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificationServiceImpl implements CertificationService {

    /** Các trường cho phép sắp xếp danh sách chứng nhận. */
    private static final java.util.Set<String> ALLOWED_SORT_FIELDS = java.util.Set.of("name", "issueDate", "expiryDate");

    /** Trường sắp xếp mặc định (khớp hành vi UI: mới cấp lên đầu). */
    private static final String DEFAULT_SORT_FIELD = "issueDate";

    /** Số bản ghi tối đa mỗi trang. */
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductionLotRepository productionLotRepository;
    private final CertificationRepository certificationRepository;
    private final ProductionLotCertificationRepository plCertificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final StandardRepository standardRepository;
    private final OrganizationRepository organizationRepository;

    @Value("${app.certification.expiry-warning-threshold-days:30}")
    private int warningThresholdDays;

    /**
     * Lấy danh sách chứng nhận của một lô sản xuất.
     * Chỉ những người dùng thuộc cùng tổ chức với lô sản xuất mới có quyền truy
     * cập.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductionLotCertificationResponse> getCertificationsOfLot(UUID lotId, CustomUserDetails currentUser) {
        ProductionLot lot = findLotAndValidateOrganization(lotId, currentUser);

        List<ProductionLotCertification> list = plCertificationRepository.findByProductionLotId(lotId);
        return list.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gắn chứng nhận cho lô sản xuất.
     * Chỉ những người dùng thuộc cùng tổ chức với lô sản xuất mới có quyền thao
     * tác.
     */
    @Override
    @Transactional
    public ProductionLotCertificationResponse attachCertification(UUID lotId, AttachCertificationRequest request,
            CustomUserDetails currentUser) {
        // 1. Kiểm tra lô và quyền
        ProductionLot lot = findLotAndValidateOrganization(lotId, currentUser);

        // 2. Kiểm tra chứng nhận tồn tại và thuộc tổ chức
        Certification cert = certificationRepository.findByIdAndOrganizationId(
                request.getCertificationId(),
                currentUser.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy chứng nhận hoặc chứng nhận không thuộc tổ chức của bạn."));

        // 3. Kiểm tra hiệu lực (QTN-13)
        if (cert.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Chứng nhận đã hết hạn, không thể gắn cho lô sản xuất.");
        }

        // 4. Kiểm tra trùng lặp
        if (plCertificationRepository.existsByProductionLotIdAndCertificationId(lotId, cert.getId())) {
            throw new BusinessException("Chứng nhận này đã được gắn cho lô sản xuất.");
        }

        // 5. Lưu liên kết
        User actor = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        ProductionLotCertification plc = ProductionLotCertification.builder()
                .productionLot(lot)
                .certification(cert)
                .attachedBy(actor)
                .note(request.getNote())
                .build();
        plc = plCertificationRepository.save(plc);

        // 6. Ghi log (TC-04)
        publishActivityLog(currentUser, "ATTACH_CERTIFICATION",
                "Gắn chứng nhận '" + cert.getName() + "' vào lô sản xuất " + lot.getName(),
                "ProductionLot", lot.getId().toString());

        return toResponse(plc);
    }

    /**
     * Gỡ chứng nhận khỏi lô sản xuất.
     * Chỉ những người dùng thuộc cùng tổ chức với lô sản xuất mới có quyền thao
     * tác.
     */
    @Override
    @Transactional
    public void detachCertification(UUID lotId, UUID certificationId, CustomUserDetails currentUser) {
        // 1. Kiểm tra lô và quyền
        ProductionLot lot = findLotAndValidateOrganization(lotId, currentUser);

        // 2. Kiểm tra liên kết tồn tại
        ProductionLotCertification plc = plCertificationRepository
                .findByProductionLotIdAndCertificationId(lotId, certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy liên kết giữa lô và chứng nhận."));

        // 3. Xóa liên kết
        plCertificationRepository.delete(plc);

        // 4. Ghi log
        Certification cert = plc.getCertification();
        publishActivityLog(currentUser, "DETACH_CERTIFICATION",
                "Gỡ chứng nhận '" + cert.getName() + "' khỏi lô sản xuất " + lot.getName(),
                "ProductionLot", lotId.toString());
    }

    /**
     * Lấy danh sách chứng nhận hợp lệ của người dùng hiện tại.
     */
    @Override
    public List<CertificationResponse> getValidCertifications(CustomUserDetails currentUser) {
        List<Certification> certs = certificationRepository.findByOrganizationIdAndExpiryDateAfter(
                currentUser.getOrganizationId(), LocalDate.now());
        return certs.stream()
                .map(this::toCertificationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo mới chứng nhận cho tổ chức hiện tại.
     */
    @Override
    @Transactional
    public CertificationResponse createCertification(CreateCertificationRequest request,
            CustomUserDetails currentUser) {
        // 1. Kiểm tra quyền (đã có @PreAuthorize, nhưng vẫn kiểm tra lại)
        if (!"VT-02".equals(currentUser.getRoleCode())) {
            throw new BusinessException("Bạn không có quyền tạo chứng nhận.");
        }

        // 2. Kiểm tra tiêu chuẩn tồn tại
        Standard standard = standardRepository.findById(request.getStandardId())
                .orElseThrow(() -> new ResourceNotFoundException("Tiêu chuẩn không tồn tại."));

        // 3. Kiểm tra số hiệu chứng nhận đã tồn tại
        if (certificationRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("Số hiệu chứng nhận đã tồn tại.");
        }

        // 4. Kiểm tra tính hợp lệ của ngày tháng
        if (request.getExpiryDate().isBefore(request.getIssueDate())) {
            throw new BusinessException("Ngày hết hạn phải sau ngày cấp.");
        }
        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Chứng nhận đã hết hạn, không thể tạo mới.");
        }

        Organization organization = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức của người dùng."));

        // 6. Tạo Certification mới
        Certification certification = Certification.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .standard(standard)
                .name(standard.getName())
                .code(request.getCode())
                .issuedBy(request.getIssuedBy())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .build();

        certification = certificationRepository.save(certification);

        // 7. Ghi log
        publishActivityLog(currentUser, "CREATE_CERTIFICATION",
                "Tạo chứng nhận '" + certification.getCode() + "' cho tiêu chuẩn " + standard.getName(),
                "Certification", certification.getId().toString());

        // 8. Trả về response
        return toCertificationResponse(certification);
    }

    /**
     * Tìm kiếm chứng nhận của tổ chức hiện tại theo từ khoá và trạng thái
     * hiệu lực, có phân trang và sắp xếp.
     *
     * <p>Từ khoá khớp tên, số hiệu hoặc cơ quan cấp (không phân biệt hoa/thường).
     * Trạng thái "expiring" tính theo ngưỡng cảnh báo {@code warningThresholdDays}.
     * Kết quả luôn scope trong tổ chức của người dùng hiện tại.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CertificationResponse> searchCertifications(String keyword, String status,
            String sortBy, String sortDir, int page, int size, CustomUserDetails currentUser) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10;
        }
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }

        String normalizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String normalizedStatus = isValidStatusFilter(status) ? status.trim().toLowerCase() : null;
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(warningThresholdDays);

        Page<Certification> result = certificationRepository.search(
                currentUser.getOrganizationId(),
                normalizedKeyword,
                normalizedStatus,
                today,
                threshold,
                pageable);

        List<CertificationResponse> items = result.getContent().stream()
                .map(this::toCertificationResponse)
                .collect(Collectors.toList());

        return PageResponse.from(result, items);
    }

    private boolean isValidStatusFilter(String status) {
        return status != null
                && (status.equalsIgnoreCase("valid")
                        || status.equalsIgnoreCase("expiring")
                        || status.equalsIgnoreCase("expired"));
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : DEFAULT_SORT_FIELD;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private CertificationResponse toCertificationResponse(Certification cert) {
        return CertificationResponse.builder()
                .id(cert.getId())
                .name(cert.getName())
                .code(cert.getCode())
                .issuedBy(cert.getIssuedBy())
                .issueDate(cert.getIssueDate())
                .expiryDate(cert.getExpiryDate())
                .isValid(!cert.getExpiryDate().isBefore(LocalDate.now()))
                .build();
    }

    // --- Helper methods ---

    private ProductionLot findLotAndValidateOrganization(UUID lotId, CustomUserDetails currentUser) {
        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô sản xuất."));
        if (!lot.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException("Bạn không có quyền thao tác trên lô sản xuất này.");
        }
        return lot;
    }

    private ProductionLotCertificationResponse toResponse(ProductionLotCertification plc) {
        Certification cert = plc.getCertification();
        return ProductionLotCertificationResponse.builder()
                .id(plc.getId())
                .certificationId(cert.getId())
                .certificationName(cert.getName())
                .certificationCode(cert.getCode())
                .issuedBy(cert.getIssuedBy())
                .issueDate(cert.getIssueDate())
                .expiryDate(cert.getExpiryDate())
                .isValid(!cert.getExpiryDate().isBefore(LocalDate.now()))
                .attachedAt(plc.getAttachedAt())
                .attachedBy(plc.getAttachedBy().getFullName())
                .note(plc.getNote())
                .build();
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
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }

    /**
     * Quét và kiểm tra hạn hiệu lực của các chứng nhận.
     * Nếu chứng nhận đã hết hạn, tạo cảnh báo CERT_EXPIRED.
     * Nếu chứng nhận sắp hết hạn (dưới ngưỡng warningThresholdDays), tạo cảnh báo
     * CERT_EXPIRING.
     * Cảnh báo CERT_EXPIRING sẽ được tự động RESOLVED nếu chứng nhận hết hạn.
     */
    @Override
    @Transactional
    public void checkCertificationExpiry() {
        log.info("⏰ Bắt đầu quét kiểm tra hạn hiệu lực của các chứng nhận. Ngưỡng cảnh báo: {} ngày",
                warningThresholdDays);

        List<Certification> certifications = certificationRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Certification cert : certifications) {
            LocalDate expiryDate = cert.getExpiryDate();

            if (expiryDate.isBefore(today)) {
                // Đã hết hiệu lực
                processExpiredCertification(cert, today);
            } else {
                long daysRemaining = expiryDate.toEpochDay() - today.toEpochDay();
                if (daysRemaining <= warningThresholdDays) {
                    // Sắp hết hiệu lực
                    processExpiringCertification(cert, daysRemaining);
                }
            }
        }
    }

    private void processExpiredCertification(Certification cert, LocalDate today) {
        // Tự động giải quyết/đóng cảnh báo sắp hết hạn (nếu có)
        autoResolveExpiringAlert(cert.getId());

        // Tạo cảnh báo đã hết hạn nếu chưa tồn tại alert PENDING cùng loại
        boolean exists = alertRepository.existsByRelatedEntityIdAndTypeAndStatus(
                cert.getId(),
                AlertType.CERT_EXPIRED,
                AlertStatus.PENDING);

        if (!exists) {
            long daysOverdue = today.toEpochDay() - cert.getExpiryDate().toEpochDay();

            java.util.Map<String, Object> details = java.util.Map.of(
                    "certificationName", cert.getName(),
                    "certificationCode", cert.getCode(),
                    "issuedBy", cert.getIssuedBy() != null ? cert.getIssuedBy() : "",
                    "issueDate", cert.getIssueDate() != null ? cert.getIssueDate().toString() : "",
                    "expiryDate", cert.getExpiryDate().toString(),
                    "daysOverdue", daysOverdue,
                    "thresholdConfigured", warningThresholdDays);

            Alert alert = new Alert();
            alert.setId(UUID.randomUUID());
            alert.setType(AlertType.CERT_EXPIRED);
            alert.setRelatedEntityType("Certification");
            alert.setRelatedEntityId(cert.getId());
            alert.setSeverity(AlertSeverity.HIGH);
            alert.setStatus(AlertStatus.PENDING);
            alert.setCreatedAt(java.time.LocalDateTime.now());
            try {
                alert.setDetails(objectMapper.writeValueAsString(details));
            } catch (Exception e) {
                log.error("Lỗi parse details cho cảnh báo chứng nhận hết hiệu lực: {}", cert.getId(), e);
                return;
            }

            alertRepository.save(alert);
            notificationService.sendCertificationExpiryNotification(alert);
            log.warn("🚨 Đã tạo cảnh báo CERT_EXPIRED cho chứng nhận '{}'", cert.getName());
        }
    }

    private void processExpiringCertification(Certification cert, long daysRemaining) {
        // Tạo cảnh báo sắp hết hạn nếu chưa tồn tại alert PENDING cùng loại
        boolean exists = alertRepository.existsByRelatedEntityIdAndTypeAndStatus(
                cert.getId(),
                AlertType.CERT_EXPIRING,
                AlertStatus.PENDING);

        if (!exists) {
            java.util.Map<String, Object> details = java.util.Map.of(
                    "certificationName", cert.getName(),
                    "certificationCode", cert.getCode(),
                    "issuedBy", cert.getIssuedBy() != null ? cert.getIssuedBy() : "",
                    "issueDate", cert.getIssueDate() != null ? cert.getIssueDate().toString() : "",
                    "expiryDate", cert.getExpiryDate().toString(),
                    "daysRemaining", daysRemaining,
                    "thresholdConfigured", warningThresholdDays);

            Alert alert = new Alert();
            alert.setId(UUID.randomUUID());
            alert.setType(AlertType.CERT_EXPIRING);
            alert.setRelatedEntityType("Certification");
            alert.setRelatedEntityId(cert.getId());
            alert.setSeverity(AlertSeverity.MEDIUM);
            alert.setStatus(AlertStatus.PENDING);
            alert.setCreatedAt(java.time.LocalDateTime.now());
            try {
                alert.setDetails(objectMapper.writeValueAsString(details));
            } catch (Exception e) {
                log.error("Lỗi parse details cho cảnh báo chứng nhận sắp hết hiệu lực: {}", cert.getId(), e);
                return;
            }

            alertRepository.save(alert);
            notificationService.sendCertificationExpiryNotification(alert);
            log.info("⚠️ Đã tạo cảnh báo CERT_EXPIRING cho chứng nhận '{}' (còn {} ngày)", cert.getName(),
                    daysRemaining);
        }
    }

    private void autoResolveExpiringAlert(UUID certificationId) {
        java.util.List<Alert> pendingExpiringAlerts = alertRepository.findByRelatedEntityIdAndTypeAndStatus(
                certificationId,
                AlertType.CERT_EXPIRING,
                AlertStatus.PENDING);

        for (Alert alert : pendingExpiringAlerts) {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedAt(java.time.LocalDateTime.now());
            alertRepository.save(alert);
            log.info("⚙️ Tự động RESOLVED cảnh báo sắp hết hiệu lực của chứng nhận ID: {}", certificationId);
        }
    }
}