package vn.nguongocso.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.dto.request.ChangePasswordRequest;
import vn.nguongocso.auth.dto.request.UpdateUserProfileRequest;
import vn.nguongocso.auth.dto.response.AvatarUploadResponse;
import vn.nguongocso.auth.dto.response.UserProfileResponse;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.UserService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.permission.service.PermissionChecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cài đặt nghiệp vụ quản lý hồ sơ người dùng và đổi mật khẩu chủ động (NCL-01-CN-010).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionChecker permissionChecker;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.upload.base-dir:./uploads}")
    private String baseDir;

    @Value("${app.upload.avatar.relative-path:avatars}")
    private String avatarRelativePath;

    @Value("${app.upload.avatar.max-size:5242880}")
    private long maxAvatarSize;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new BusinessException("Người dùng chưa được xác thực");
        }

        UUID userId = currentUser.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));

        List<String> permissions = permissionChecker.getPermissionsForCurrentUser();

        return mapToProfileResponse(user, currentUser, permissions);
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(CustomUserDetails currentUser, UpdateUserProfileRequest request) {
        if (currentUser == null) {
            throw new BusinessException("Người dùng chưa được xác thực");
        }

        UUID userId = currentUser.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));

        boolean hasChanges = false;

        if (request.getFullName() != null) {
            String trimmedName = request.getFullName().trim();
            if (trimmedName.isEmpty()) {
                throw new BusinessException("Họ và tên không được để trống");
            }
            user.setFullName(trimmedName);
            hasChanges = true;
        }

        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.isEmpty()) {
                if (userRepository.existsByEmailAndUserIdNot(newEmail, userId)) {
                    throw new BusinessException("Địa chỉ email đã được sử dụng bởi tài khoản khác");
                }
                user.setEmail(newEmail);
            } else {
                user.setEmail(null);
            }
            hasChanges = true;
        }

        if (request.getPhone() != null) {
            String newPhone = request.getPhone().trim();
            if (!newPhone.isEmpty()) {
                user.setPhone(newPhone);
            } else {
                user.setPhone(null);
            }
            hasChanges = true;
        }

        if (request.getAvatarUrl() != null) {
            String newAvatarUrl = request.getAvatarUrl().trim();
            if (!newAvatarUrl.isEmpty()) {
                user.setAvatarUrl(newAvatarUrl);
            } else {
                user.setAvatarUrl(null);
            }
            hasChanges = true;
        }

        if (!hasChanges) {
            throw new BusinessException("Không có dữ liệu thay đổi để cập nhật");
        }

        User savedUser = userRepository.save(user);
        log.info("Cập nhật thông tin hồ sơ cá nhân thành công cho userId={}", userId);

        publishActivityLog(
                currentUser,
                savedUser,
                "UPDATE_PROFILE",
                "Người dùng " + savedUser.getUserName() + " đã cập nhật thông tin hồ sơ cá nhân"
        );

        List<String> permissions = permissionChecker.getPermissionsForCurrentUser();
        return mapToProfileResponse(savedUser, currentUser, permissions);
    }

    @Override
    @Transactional
    public void changePassword(CustomUserDetails currentUser, ChangePasswordRequest request) {
        if (currentUser == null) {
            throw new BusinessException("Người dùng chưa được xác thực");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp");
        }

        UUID userId = currentUser.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Người dùng userId={} đã đổi mật khẩu thành công", userId);

        publishActivityLog(
                currentUser,
                user,
                "CHANGE_PASSWORD",
                "Người dùng " + user.getUserName() + " đã đổi mật khẩu thành công"
        );
    }

    @Override
    @Transactional
    public AvatarUploadResponse uploadAvatar(CustomUserDetails currentUser, MultipartFile file) {
        if (currentUser == null) {
            throw new BusinessException("Người dùng chưa được xác thực");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Tệp tin tải lên không được để trống");
        }

        if (file.getSize() > maxAvatarSize) {
            throw new BusinessException("Dung lượng tệp vượt quá giới hạn cho phép (" + (maxAvatarSize / 1024 / 1024) + "MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Định dạng tệp không hợp lệ. Chỉ chấp nhận các định dạng ảnh JPG, PNG, GIF, WEBP");
        }

        UUID userId = currentUser.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            extension = ".png";
        }

        String filename = userId + "_" + System.currentTimeMillis() + extension;
        Path targetDir = Paths.get(baseDir, avatarRelativePath);

        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            Path targetFile = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Lỗi khi lưu tệp ảnh đại diện: ", e);
            throw new BusinessException("Không thể lưu tệp ảnh đại diện. Vui lòng thử lại sau.");
        }

        String avatarUrl = "/uploads/" + avatarRelativePath + "/" + filename;
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        log.info("Tải lên ảnh đại diện thành công cho userId={}: {}", userId, avatarUrl);

        publishActivityLog(
                currentUser,
                user,
                "UPLOAD_AVATAR",
                "Người dùng " + user.getUserName() + " đã tải lên ảnh đại diện mới"
        );

        return AvatarUploadResponse.builder()
                .avatarUrl(avatarUrl)
                .build();
    }

    private UserProfileResponse mapToProfileResponse(User user, CustomUserDetails currentUser, List<String> permissions) {
        return UserProfileResponse.builder()
                .id(user.getUserId())
                .userId(user.getUserId())
                .username(user.getUserName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roleCode(currentUser.getRoleCode())
                .roleName(currentUser.getRoleName())
                .organizationId(currentUser.getOrganizationId())
                .organizationCode(currentUser.getOrganizationCode())
                .organizationName(currentUser.getOrganizationName())
                .organizationType(currentUser.getOrganizationType())
                .permissions(permissions)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private void publishActivityLog(CustomUserDetails currentUser, User user, String action, String description) {
        try {
            ActivityLogEvent event = ActivityLogEvent.builder()
                    .userId(user.getUserId())
                    .username(user.getUserName())
                    .fullName(user.getFullName())
                    .organizationId(currentUser.getOrganizationId())
                    .action(action)
                    .description(description)
                    .entityType("USER")
                    .entityId(user.getUserId().toString())
                    .timestamp(LocalDateTime.now())
                    .build();
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("Không thể ghi nhật ký hoạt động {}: {}", action, e.getMessage());
        }
    }
}

