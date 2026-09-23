package vn.nguongocso.farm.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.response.AttachmentResponse;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;

/**
 * Quản lý tệp đính kèm của nhật ký canh tác.
*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final FarmLogRepository farmLogRepository;
    private final FarmLogAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Value("${app.upload.base-dir}")
    private String baseDir;

    @Value("${app.upload.farm-log.relative-path:farm-logs}")
    private String farmLogRelativePath;

    @Value("${app.upload.farm-log.max-size:5242880}")
    private long maxFileSize;

    /** Xác định thư mục tải lên của một nhật ký. */
    private String getUploadDir(UUID logId) {
        return Paths.get(baseDir, farmLogRelativePath, logId.toString()).toString();
    }

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "application/pdf");

    /** Tải lên tệp đính kèm cho nhật ký canh tác. */
    @Transactional
    public AttachmentResponse uploadAttachment(UUID logId, MultipartFile file, String description,
            CustomUserDetails userDetails) {

        FarmLog farmLog = farmLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy nhật ký canh tác"));

        UUID orgId = userDetails.getOrganizationId();
        UUID lotOrgId = farmLog.getProductionLotId().getOrganization().getOrganizationId();
        if (!lotOrgId.equals(orgId)) {
            throw new BusinessException("Nhật ký không thuộc tổ chức của bạn");
        }

        if (file.isEmpty())
            throw new BusinessException("File không được để trống");
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("File vượt quá dung lượng cho phép (" + maxFileSize / 1024 / 1024 + "MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("Loại file không hỗ trợ. Chỉ chấp nhận JPG, PNG, PDF");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8)
                + extension;
        String uploadDir = getUploadDir(logId);
        String filePath = uploadDir + newFileName;

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Files.copy(file.getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Lỗi khi lưu file: {}", e.getMessage());
            throw new BusinessException("Lỗi hệ thống khi lưu file");
        }

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

        FarmLogAttachment attachment = FarmLogAttachment.builder()
                .farmLog(farmLog)
                .fileName(originalFilename != null ? originalFilename : "unknown")
                .fileSize(file.getSize())
                .fileType(contentType)
                .filePath(filePath)
                .description(description)
                .uploadedBy(user)
                .uploadedAt(LocalDateTime.now(clock))
                .build();
        attachmentRepository.save(attachment);

        publishActivityLog(userDetails, "CREATE",
                "Tải lên chứng từ cho nhật ký canh tác ID: " + logId,
                "FarmLogAttachment",
                attachment.getId().toString());

        log.info("Upload attachment thành công: id={}, logId={}", attachment.getId(), logId);

        return toResponse(attachment);
    }

    /** Lấy danh sách tệp đính kèm của nhật ký. */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(UUID logId, CustomUserDetails userDetails) {

        FarmLog farmLog = farmLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy nhật ký canh tác"));

        UUID orgId = userDetails.getOrganizationId();
        UUID lotOrgId = farmLog.getProductionLotId().getOrganization().getOrganizationId();
        if (!lotOrgId.equals(orgId)) {
            throw new BusinessException("Nhật ký không thuộc tổ chức của bạn");
        }

        return attachmentRepository.findByFarmLogId(logId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Xóa tệp đính kèm. */
    @Transactional
    public void deleteAttachment(UUID attachmentId, CustomUserDetails userDetails) {
        FarmLogAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy file đính kèm"));

        UUID orgId = userDetails.getOrganizationId();
        UUID lotOrgId = attachment.getFarmLog().getProductionLotId().getOrganization().getOrganizationId();
        if (!lotOrgId.equals(orgId)) {
            throw new BusinessException("Bạn không có quyền xóa file này");
        }

        try {
            Path filePath = Paths.get(attachment.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            } else {
                log.warn("File không tồn tại trên đĩa: {}", attachment.getFilePath());
            }
        } catch (IOException e) {
            log.error("Không thể xóa file: {}, lỗi: {}", attachment.getFilePath(), e.getMessage());
            throw new BusinessException("Không thể xóa file, vui lòng thử lại");
        }

        attachmentRepository.delete(attachment);
        log.info("Xóa attachment thành công: id={}", attachmentId);
    }

    /** Lấy tệp đính kèm để hiển thị (view). */
    @Transactional(readOnly = true)
    public Map.Entry<Resource, MediaType> getAttachmentForView(
            UUID attachmentId, CustomUserDetails userDetails) {
        FarmLogAttachment attachment = validateAttachmentAccess(attachmentId, userDetails);
        Resource resource = resolveFileResource(attachment);
        MediaType contentType = resolveContentType(attachment.getFileType());
        return new AbstractMap.SimpleEntry<>(resource, contentType);
    }

    /** DTO nội bộ cho download: chứa Resource, MediaType và tên file. */
    public record AttachmentResource(Resource resource, MediaType contentType, String fileName) {}

    /** Lấy tệp đính kèm để tải xuống (download). */
    @Transactional(readOnly = true)
    public AttachmentResource getAttachmentForDownload(
            UUID attachmentId, CustomUserDetails userDetails) {
        FarmLogAttachment attachment = validateAttachmentAccess(attachmentId, userDetails);
        Resource resource = resolveFileResource(attachment);
        MediaType contentType = resolveContentType(attachment.getFileType());
        String encodedFileName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return new AttachmentResource(resource, contentType, encodedFileName);
    }

    /** Kiểm tra quyền truy cập và trả về attachment. */
    private FarmLogAttachment validateAttachmentAccess(UUID attachmentId, CustomUserDetails userDetails) {
        FarmLogAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy file đính kèm"));

        UUID orgId = userDetails.getOrganizationId();
        UUID lotOrgId = attachment.getFarmLog().getProductionLotId().getOrganization().getOrganizationId();
        if (!lotOrgId.equals(orgId)) {
            throw new BusinessException("Bạn không có quyền truy cập file này");
        }

        return attachment;
    }

    /** Giải quyết file vật lý từ đường dẫn, ngăn path traversal. */
    private Resource resolveFileResource(FarmLogAttachment attachment) {
        Path filePath = Paths.get(attachment.getFilePath()).toAbsolutePath().normalize();

        Path baseDirPath = Paths.get(baseDir).toAbsolutePath().normalize();
        if (!filePath.startsWith(baseDirPath)) {
            log.warn("Path traversal attempt: {}", attachment.getFilePath());
            throw new BusinessException("Đường dẫn file không hợp lệ");
        }

        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new BusinessException("File không tồn tại hoặc không thể đọc");
        }

        return new FileSystemResource(filePath);
    }

    /** Xác định MediaType từ chuỗi MIME type. */
    private MediaType resolveContentType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(fileType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /** Gửi sự kiện nhật ký hoạt động. */
    private void publishActivityLog(CustomUserDetails currentUser, String action, String description, String entityType,
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
                .timestamp(LocalDateTime.now(clock))
                .build());
    }

    /** Chuyển entity đính kèm sang response. */
    private AttachmentResponse toResponse(FarmLogAttachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .farmLogId(attachment.getFarmLog().getId())
                .fileName(attachment.getFileName())
                .fileSize(attachment.getFileSize())
                .fileType(attachment.getFileType())
                .fileUrl("/api/v1/farm-logs/attachments/" + attachment.getId() + "/view")
                .description(attachment.getDescription())
                .uploadedBy(attachment.getUploadedBy().getFullName())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}