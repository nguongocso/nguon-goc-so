package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.AttachCertificationRequest;
import vn.nguongocso.certification.dto.request.CreateCertificationRequest;
import vn.nguongocso.certification.dto.request.RejectCertificateRequest;
import vn.nguongocso.certification.dto.request.VerifyCertificateRequest;
import vn.nguongocso.certification.dto.response.CertificateDocumentResponse;
import vn.nguongocso.certification.dto.response.CertificateReviewerResponse;
import vn.nguongocso.certification.dto.response.CertificationResponse;
import vn.nguongocso.certification.dto.response.CertificationVerificationResponse;
import vn.nguongocso.certification.dto.response.ProductionLotCertificationResponse;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.event.CertificationRejectedEvent;
import vn.nguongocso.certification.enums.CertificationValidityStatus;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;
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

import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lớp CertificationServiceImpl triển khai các phương thức của CertificationService.
 * Nó chịu trách nhiệm quản lý chứng nhận, gắn chứng nhận cho lô sản xuất,
 * tạo mới chứng nhận, kiểm tra hạn hiệu lực và xác thực/từ chối chứng nhận dành cho VT-01.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificationServiceImpl implements CertificationService {

    /** Các trường cho phép sắp xếp danh sách chứng nhận tổ chức. */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "issueDate", "expiryDate");

    /** Các trường cho phép sắp xếp danh sách quản trị viên VT-01. */
    private static final Set<String> ALLOWED_ADMIN_SORT_FIELDS = Set.of("createdAt", "reviewedAt", "expiryDate");

    /** Trường sắp xếp mặc định cho danh sách tổ chức. */
    private static final String DEFAULT_SORT_FIELD = "issueDate";

    /** Trường sắp xếp mặc định cho quản trị viên (mới nhất lên đầu). */
    private static final String DEFAULT_ADMIN_SORT_FIELD = "createdAt";

    /** Số bản ghi tối đa mỗi trang. */
    private static final int MAX_PAGE_SIZE = 100;

    /** Các loại tài liệu chứng nhận được phép lưu và hiển thị. */
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE);

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
    private final ActivityLogRepository activityLogRepository;

    @Value("${app.certification.expiry-warning-threshold-days:30}")
    private int warningThresholdDays;

    @Value("${app.upload.base-dir:./uploads}")
    private String uploadBaseDir;

    @Value("${app.upload.certification.relative-path:certifications}")
    private String certificationRelativePath;

    @Value("${app.upload.certification.max-size:5242880}")
    private long certificationMaxFileSize;

    /**
     * Lấy danh sách chứng nhận của một lô sản xuất.
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
     * Quy tắc QTN-13 (hạn dùng) và QTN-34 (xác thực):
     * - PENDING + còn hạn -> ALLOW
     * - VERIFIED + còn hạn -> ALLOW
     * - REJECTED -> BLOCK (409 Conflict)
     * - EXPIRED -> BLOCK (409 Conflict / BusinessException)
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

        // 3. Kiểm tra trạng thái xác thực (QTN-34)
        if (cert.getVerificationStatus() == CertificationVerificationStatus.REJECTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Chứng nhận đã bị từ chối xác thực, không thể gắn cho lô sản xuất.");
        }

        // 4. Kiểm tra hiệu lực (QTN-13)
        if (cert.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Chứng nhận đã hết hạn, không thể gắn cho lô sản xuất.");
        }

        // 5. Kiểm tra trùng lặp
        if (plCertificationRepository.existsByProductionLotIdAndCertificationId(lotId, cert.getId())) {
            throw new BusinessException("Chứng nhận này đã được gắn cho lô sản xuất.");
        }

        // 6. Lưu liên kết
        User actor = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        ProductionLotCertification plc = ProductionLotCertification.builder()
                .productionLot(lot)
                .certification(cert)
                .attachedBy(actor)
                .note(request.getNote())
                .build();
        plc = plCertificationRepository.save(plc);

        // 7. Ghi log
        publishActivityLog(currentUser, "ATTACH_CERTIFICATION",
                "Gắn chứng nhận '" + cert.getName() + "' vào lô sản xuất " + lot.getName(),
                "ProductionLot", lot.getId().toString());

        return toResponse(plc);
    }

    /**
     * Gỡ chứng nhận khỏi lô sản xuất.
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
     * Lấy danh sách chứng nhận hợp lệ của tổ chức hiện tại để gắn cho lô (loại bỏ EXPIRED và REJECTED).
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
     * Tạo mới chứng nhận cho tổ chức hiện tại (khởi tạo verificationStatus = PENDING).
     */
    @Override
    @Transactional
    public CertificationResponse createCertification(CreateCertificationRequest request,
            MultipartFile file,
            CustomUserDetails currentUser) {
        // 1. Kiểm tra quyền
        if (!"VT-02".equals(currentUser.getRoleCode())) {
            throw new BusinessException("Bạn không có quyền tạo chứng nhận.");
        }

        validateCertificationDocument(file);

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

        // 6. Tạo Certification mới với trạng thái mặc định PENDING
        UUID certificationId = UUID.randomUUID();
        StoredDocument storedDocument = storeCertificationDocument(certificationId, file);
        Certification certification = Certification.builder()
                .id(certificationId)
                .organization(organization)
                .standard(standard)
                .name(standard.getName())
                .code(request.getCode())
                .issuedBy(request.getIssuedBy())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .verificationStatus(CertificationVerificationStatus.PENDING)
                .documentFileName(storedDocument.originalFileName())
                .documentContentType(storedDocument.contentType())
                .documentFileSize(storedDocument.fileSize())
                .documentStoragePath(storedDocument.storagePath().toString())
                .build();

        certification = certificationRepository.save(certification);

        // 7. Ghi log
        saveActivityLog(currentUser, currentUser.getOrganizationId(), "CREATE_CERTIFICATION",
                "Tạo chứng nhận '" + certification.getCode() + "' cho tiêu chuẩn " + standard.getName(),
                "CERTIFICATION", certification.getId().toString());

        // 8. Trả về response
        return toCertificationResponse(certification);
    }

    /**
     * Tìm kiếm chứng nhận của tổ chức hiện tại theo từ khoá và trạng thái hiệu lực.
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

    // =========================================================================
    // QUẢN TRỊ XÁC THỰC CHỨNG NHẬN (VT-01)
    // =========================================================================

    /**
     * Lấy danh sách chứng nhận trên toàn nền tảng để Quản trị viên (VT-01) kiểm tra, đối chiếu.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CertificationVerificationResponse> getAdminCertifications(
            CertificationVerificationStatus status,
            String keyword,
            UUID organizationId,
            String sortBy,
            String sortDir,
            int page,
            int size,
            CustomUserDetails currentUser) {
        validatePlatformAdmin(currentUser);

        validateAdminPaginationAndSort(sortBy, sortDir, page, size);

        // Mặc định lọc PENDING nếu client không truyền trạng thái
        CertificationVerificationStatus targetStatus = (status != null) ? status : CertificationVerificationStatus.PENDING;

        String sortField = (sortBy == null || sortBy.isBlank()) ? DEFAULT_ADMIN_SORT_FIELD : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        String normalizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<Certification> pageResult = certificationRepository.searchForAdmin(
                targetStatus,
                organizationId,
                normalizedKeyword,
                pageable);

        List<CertificationVerificationResponse> items = pageResult.getContent().stream()
                .map(this::toVerificationResponse)
                .collect(Collectors.toList());

        return PageResponse.from(pageResult, items);
    }

    /**
     * Lấy chi tiết thông tin đối chiếu của một chứng nhận (VT-01).
     */
    @Override
    @Transactional(readOnly = true)
    public CertificationVerificationResponse getAdminCertificationDetail(
            UUID certificationId,
            CustomUserDetails currentUser) {
        validatePlatformAdmin(currentUser);

        Certification cert = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng nhận."));

        return toVerificationResponse(cert);
    }

    /**
     * Lấy tài nguyên tệp đính kèm an toàn phục vụ xem đối chiếu (VT-01).
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentResource getCertificateDocumentResource(
            UUID certificationId,
            CustomUserDetails currentUser) {
        validatePlatformAdmin(currentUser);

        Certification cert = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng nhận."));

        if (cert.getDocumentStoragePath() == null || cert.getDocumentStoragePath().isBlank()) {
            throw new ResourceNotFoundException("Chứng nhận chưa có tệp đính kèm.");
        }

        Path filePath = resolveStoredDocumentPath(cert);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            log.warn("🚨 Tệp chứng nhận vật lý bị mất trên máy chủ: certId={}, path={}", certificationId, cert.getDocumentStoragePath());
            throw new BusinessException(HttpStatus.GONE, "Tệp chứng nhận vật lý không còn trên máy chủ.");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(HttpStatus.GONE, "Tệp chứng nhận không thể đọc được.");
            }

            MediaType mediaType = MediaType.parseMediaType(cert.getDocumentContentType());

            String fileName = (cert.getDocumentFileName() != null && !cert.getDocumentFileName().isBlank())
                    ? cert.getDocumentFileName()
                    : "certificate-document";

            return new DocumentResource(resource, mediaType, fileName);
        } catch (MalformedURLException e) {
            log.error("Lỗi đường dẫn URI tệp chứng nhận: {}", filePath, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi đọc tệp tài liệu chứng nhận.");
        }
    }

    /**
     * Xác thực chứng nhận của tổ chức (VT-01).
     */
    @Override
    @Transactional
    public CertificationVerificationResponse verifyCertificate(
            UUID certificationId,
            VerifyCertificateRequest request,
            CustomUserDetails currentUser) {
        validatePlatformAdmin(currentUser);

        Certification cert = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng nhận."));

        // 1. Kiểm tra trạng thái hiện tại phải là PENDING
        if (cert.getVerificationStatus() != CertificationVerificationStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "Chứng nhận không ở trạng thái chờ xác thực.");
        }

        // 2. Kiểm tra dữ liệu bắt buộc để đối chiếu
        if (cert.getCode() == null || cert.getCode().isBlank()
                || cert.getIssuedBy() == null || cert.getIssuedBy().isBlank()
                || cert.getStandard() == null
                || cert.getIssueDate() == null
                || cert.getExpiryDate() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Chứng nhận thiếu dữ liệu bắt buộc để đối chiếu.");
        }

        if (cert.getExpiryDate().isBefore(cert.getIssueDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ngày hết hạn không được trước ngày cấp.");
        }

        // 3. Kiểm tra tệp chứng nhận đọc được
        if (cert.getDocumentStoragePath() == null || cert.getDocumentStoragePath().isBlank()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Chứng nhận thiếu tệp đính kèm để đối chiếu.");
        }
        Path filePath = resolveStoredDocumentPath(cert);
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Tệp chứng nhận vật lý không tồn tại hoặc không thể đọc được.");
        }

        // 4. Xử lý ghi chú xác thực
        String trimmedNote = (request != null && request.getReviewNote() != null)
                ? request.getReviewNote().trim()
                : null;
        if (trimmedNote != null && trimmedNote.length() > 1000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ghi chú xác thực không được vượt quá 1000 ký tự.");
        }

        User reviewer = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin quản trị viên."));

        LocalDateTime now = LocalDateTime.now();

        // 5. Cập nhật atomic có điều kiện (chống race condition)
        int updatedCount = certificationRepository.updateVerificationStatus(
                certificationId,
                CertificationVerificationStatus.PENDING,
                CertificationVerificationStatus.VERIFIED,
                reviewer,
                now,
                trimmedNote,
                null);

        if (updatedCount == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Chứng nhận đã được xử lý trước đó hoặc không còn ở trạng thái chờ.");
        }

        // Đồng bộ entity trong phiên làm việc
        cert.setVerificationStatus(CertificationVerificationStatus.VERIFIED);
        cert.setReviewedBy(reviewer);
        cert.setReviewedAt(now);
        cert.setReviewNote(trimmedNote);
        cert.setRejectionReason(null);
        cert.setUpdatedAt(now);

        // 6. Ghi lịch sử hoạt động gắn với tổ chức sở hữu chứng nhận
        saveActivityLog(
                currentUser,
                cert.getOrganization().getOrganizationId(),
                "VERIFY_CERTIFICATION",
                "Xác thực chứng nhận '" + cert.getCode() + "' của tổ chức " + cert.getOrganization().getName(),
                "CERTIFICATION",
                cert.getId().toString());

        return toVerificationResponse(cert);
    }

    /**
     * Từ chối xác thực chứng nhận của tổ chức (VT-01).
     */
    @Override
    @Transactional
    public CertificationVerificationResponse rejectCertificate(
            UUID certificationId,
            RejectCertificateRequest request,
            CustomUserDetails currentUser) {
        validatePlatformAdmin(currentUser);

        Certification cert = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng nhận."));

        // 1. Kiểm tra trạng thái hiện tại phải là PENDING
        if (cert.getVerificationStatus() != CertificationVerificationStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "Chứng nhận không ở trạng thái chờ xác thực.");
        }

        // 2. Kiểm tra lý do từ chối
        String trimmedReason = (request != null && request.getRejectionReason() != null)
                ? request.getRejectionReason().trim()
                : "";
        if (trimmedReason.length() < 10 || trimmedReason.length() > 1000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lý do từ chối phải từ 10 đến 1000 ký tự.");
        }

        User reviewer = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin quản trị viên."));

        LocalDateTime now = LocalDateTime.now();

        // 3. Cập nhật atomic có điều kiện (chống race condition)
        int updatedCount = certificationRepository.updateVerificationStatus(
                certificationId,
                CertificationVerificationStatus.PENDING,
                CertificationVerificationStatus.REJECTED,
                reviewer,
                now,
                null,
                trimmedReason);

        if (updatedCount == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Chứng nhận đã được xử lý trước đó hoặc không còn ở trạng thái chờ.");
        }

        // Đồng bộ entity trong phiên làm việc
        cert.setVerificationStatus(CertificationVerificationStatus.REJECTED);
        cert.setReviewedBy(reviewer);
        cert.setReviewedAt(now);
        cert.setReviewNote(null);
        cert.setRejectionReason(trimmedReason);
        cert.setUpdatedAt(now);

        // 4. Ghi lịch sử trong cùng transaction với quyết định từ chối.
        saveActivityLog(
                currentUser,
                cert.getOrganization().getOrganizationId(),
                "REJECT_CERTIFICATION",
                "Từ chối xác thực chứng nhận '" + cert.getCode() + "' của tổ chức " + cert.getOrganization().getName() + ". Lý do: " + trimmedReason,
                "CERTIFICATION",
                cert.getId().toString());

        // 5. Listener chỉ gửi thông báo sau khi transaction hiện tại commit thành công.
        eventPublisher.publishEvent(new CertificationRejectedEvent(cert.getId(), trimmedReason));

        CertificationVerificationResponse response = toVerificationResponse(cert);
        response.setNotifiedCount(0);
        return response;
    }

    // --- Helper methods ---

    private record StoredDocument(Path storagePath, String originalFileName, String contentType, long fileSize) {
    }

    /** Kiểm tra tệp chứng nhận trước khi ghi xuống vùng lưu trữ riêng. */
    private void validateCertificationDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tệp chứng nhận không được để trống.");
        }
        if (file.getSize() > certificationMaxFileSize) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Tệp chứng nhận vượt quá dung lượng cho phép (5 MiB).");
        }
        if (file.getContentType() == null || !ALLOWED_DOCUMENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Loại tệp không được hỗ trợ. Chỉ chấp nhận PDF, JPEG hoặc PNG.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName != null && sanitizeOriginalFileName(originalName).length() > 255) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tên tệp chứng nhận không được vượt quá 255 ký tự.");
        }
    }

    /** Lưu tệp bằng tên sinh nội bộ và đăng ký dọn tệp nếu transaction rollback. */
    private StoredDocument storeCertificationDocument(UUID certificationId, MultipartFile file) {
        Path root = getCertificationStorageRoot();
        Path directory = root.resolve(certificationId.toString()).normalize();
        if (!directory.startsWith(root)) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Cấu hình vùng lưu tài liệu không hợp lệ.");
        }

        String extension = switch (file.getContentType()) {
            case MediaType.APPLICATION_PDF_VALUE -> ".pdf";
            case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Loại tệp không được hỗ trợ.");
        };
        Path target = directory.resolve("document" + extension).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(directory);
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Không thể lưu tài liệu chứng nhận {}: {}", certificationId, ex.getMessage(), ex);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu tệp chứng nhận.");
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        try {
                            Files.deleteIfExists(target);
                        } catch (IOException ex) {
                            log.error("Không thể dọn tệp chứng nhận sau rollback: {}", target, ex);
                        }
                    }
                }
            });
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "certificate" + extension;
        } else {
            originalName = sanitizeOriginalFileName(originalName);
            if (originalName.isBlank()) {
                originalName = "certificate" + extension;
            }
        }
        return new StoredDocument(target.toAbsolutePath(), originalName, file.getContentType(), file.getSize());
    }

    private String sanitizeOriginalFileName(String originalName) {
        String normalized = originalName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        return fileName.replaceAll("[\\p{Cntrl}]", "").trim();
    }

    private Path getCertificationStorageRoot() {
        Path base = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        Path root = base.resolve(certificationRelativePath).normalize();
        if (!root.startsWith(base)) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Cấu hình vùng lưu tài liệu không hợp lệ.");
        }
        return root;
    }

    /** Chuẩn hóa và giới hạn đường dẫn đọc trong đúng vùng tài liệu chứng nhận. */
    private Path resolveStoredDocumentPath(Certification certification) {
        if (certification.getDocumentContentType() == null
                || !ALLOWED_DOCUMENT_TYPES.contains(certification.getDocumentContentType())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Metadata loại tệp chứng nhận không hợp lệ.");
        }
        Path root = getCertificationStorageRoot();
        Path filePath = Paths.get(certification.getDocumentStoragePath()).toAbsolutePath().normalize();
        if (!filePath.startsWith(root)) {
            log.warn("Từ chối đọc đường dẫn tài liệu ngoài vùng cho phép: certId={}", certification.getId());
            throw new BusinessException(HttpStatus.CONFLICT, "Đường dẫn tệp chứng nhận không hợp lệ.");
        }
        return filePath;
    }

    private void validateAdminPaginationAndSort(String sortBy, String sortDir, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Phân trang không hợp lệ.");
        }
        if (sortBy != null && !sortBy.isBlank() && !ALLOWED_ADMIN_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Trường sắp xếp không hợp lệ.");
        }
        if (sortDir != null && !sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chiều sắp xếp không hợp lệ.");
        }
    }

    /** Lưu activity log trực tiếp để cùng commit hoặc rollback với nghiệp vụ chứng nhận. */
    private void saveActivityLog(CustomUserDetails currentUser, UUID organizationId, String action,
            String description, String entityType, String entityId) {
        activityLogRepository.save(ActivityLog.builder()
                .organizationId(organizationId)
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Kiểm tra phòng vệ vai trò VT-01 (Quản trị viên nền tảng).
     */
    private void validatePlatformAdmin(CustomUserDetails currentUser) {
        if (currentUser == null || !"VT-01".equals(currentUser.getRoleCode())) {
            throw new AccessDeniedException("Chỉ Quản trị viên nền tảng (VT-01) mới có quyền thực hiện thao tác này.");
        }
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

    /**
     * Chuyển đổi Certification entity sang response đối chiếu dành cho VT-01.
     */
    private CertificationVerificationResponse toVerificationResponse(Certification cert) {
        LocalDate today = LocalDate.now();
        CertificationValidityStatus validityStatus = cert.getExpiryDate().isBefore(today)
                ? CertificationValidityStatus.EXPIRED
                : CertificationValidityStatus.VALID;

        CertificateDocumentResponse documentResponse = null;
        if (cert.getDocumentFileName() != null || cert.getDocumentStoragePath() != null) {
            documentResponse = CertificateDocumentResponse.builder()
                    .fileName(cert.getDocumentFileName())
                    .contentType(cert.getDocumentContentType())
                    .fileSize(cert.getDocumentFileSize())
                    .viewUrl("/api/v1/admin/certifications/" + cert.getId() + "/document")
                    .build();
        }

        CertificateReviewerResponse reviewerResponse = null;
        if (cert.getReviewedBy() != null) {
            reviewerResponse = CertificateReviewerResponse.builder()
                    .userId(cert.getReviewedBy().getUserId())
                    .fullName(cert.getReviewedBy().getFullName())
                    .build();
        }

        return CertificationVerificationResponse.builder()
                .id(cert.getId())
                .organizationId(cert.getOrganization() != null ? cert.getOrganization().getOrganizationId() : null)
                .organizationName(cert.getOrganization() != null ? cert.getOrganization().getName() : null)
                .standardId(cert.getStandard() != null ? cert.getStandard().getId() : null)
                .standardName(cert.getStandard() != null ? cert.getStandard().getName() : null)
                .code(cert.getCode())
                .issuedBy(cert.getIssuedBy())
                .issueDate(cert.getIssueDate())
                .expiryDate(cert.getExpiryDate())
                .verificationStatus(cert.getVerificationStatus())
                .validityStatus(validityStatus)
                .document(documentResponse)
                .reviewedBy(reviewerResponse)
                .reviewedAt(cert.getReviewedAt())
                .reviewNote(cert.getReviewNote())
                .rejectionReason(cert.getRejectionReason())
                .createdAt(cert.getCreatedAt())
                .updatedAt(cert.getUpdatedAt())
                .build();
    }

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
        publishActivityLog(currentUser, currentUser.getOrganizationId(), action, description, entityType, entityId);
    }

    private void publishActivityLog(CustomUserDetails currentUser, UUID organizationId, String action, String description,
            String entityType, String entityId) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(organizationId)
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Quét và kiểm tra hạn hiệu lực của các chứng nhận.
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
        autoResolveExpiringAlert(cert.getId());

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
            alert.setCreatedAt(LocalDateTime.now());
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
            alert.setCreatedAt(LocalDateTime.now());
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
        List<Alert> pendingExpiringAlerts = alertRepository.findByRelatedEntityIdAndTypeAndStatus(
                certificationId,
                AlertType.CERT_EXPIRING,
                AlertStatus.PENDING);

        for (Alert alert : pendingExpiringAlerts) {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
            log.info("⚙️ Tự động RESOLVED cảnh báo sắp hết hiệu lực của chứng nhận ID: {}", certificationId);
        }
    }
}
