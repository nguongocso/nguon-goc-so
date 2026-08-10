package vn.nguongocso.organization.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.DuplicateResourceException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.dto.request.AcceptInvitationRequest;
import vn.nguongocso.organization.dto.request.CreateInvitationRequest;
import vn.nguongocso.organization.dto.response.AcceptInvitationResponse;
import vn.nguongocso.organization.dto.response.InvitationPublicResponse;
import vn.nguongocso.organization.dto.response.InvitationResponse;
import vn.nguongocso.organization.entity.Invitation;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.InvitationStatus;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.InvitationRepository;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.organization.service.InvitationService;

import org.springframework.beans.factory.annotation.Value;

import vn.nguongocso.mail.service.EmailService;

@Slf4j
@Service
/** Quản lý thư mời tham gia tổ chức. */
public class InvitationServiceImpl implements InvitationService {

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public InvitationServiceImpl(
            InvitationRepository invitationRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            OrganizationUserRepository organizationUserRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            EmailService emailService) {
        this.invitationRepository = invitationRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.organizationUserRepository = organizationUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.emailService = emailService;
    }

    /** Tạo thư mời mới. */
    @Override
    @Transactional
    public InvitationResponse createInvitation(CreateInvitationRequest request, CustomUserDetails currentUser) {
        UUID orgId = currentUser.getOrganizationId();
        if (orgId == null) {
            throw new BusinessException("Người dùng không thuộc tổ chức nào");
        }

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Tổ chức không tồn tại"));

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò không tồn tại trong hệ thống"));

        // VT-02 chỉ được gửi lời mời với vai trò VT-03
        if (RoleCode.ORG_MANAGER.equals(currentUser.getRoleCode())
                && !RoleCode.EVENT_RECORDER.equals(role.getCode())) {
        throw new BusinessException(
                "Quản lý hợp tác xã chỉ được mời thành viên với vai trò Người ghi sự kiện");
        }
        
        // Chỉ cho phép mời user chưa là thành viên ACTIVE của tổ chức hiện tại
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            organizationUserRepository.findByOrganization_OrganizationIdAndUser_UserId(orgId, user.getUserId())
                    .ifPresent(orgUser -> {
                        if (orgUser.getStatus() == OrganizationUserStatus.ACTIVE) {
                            throw new DuplicateResourceException(
                                    "Người dùng có email này đã là thành viên của tổ chức");
                        }
                    });
        });

        // Vô hiệu hóa các thư mời PENDING cũ cùng email – cùng tổ chức
        List<Invitation> oldInvitations = invitationRepository
                .findByEmailAndOrganizationOrganizationIdAndStatus(
                        request.getEmail(), orgId, InvitationStatus.PENDING);
        if (!oldInvitations.isEmpty()) {
            for (Invitation oldInv : oldInvitations) {
                oldInv.setStatus(InvitationStatus.EXPIRED);
            }
            invitationRepository.saveAll(oldInvitations);
            log.info("Đã vô hiệu hóa {} thư mời cũ cho email={}", oldInvitations.size(), request.getEmail());
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email(request.getEmail())
                .role(role)
                .token(token)
                .status(InvitationStatus.PENDING)
                .expiryDate(LocalDateTime.now().plusDays(request.getExpiryDays()))
                .createdBy(currentUser.getUser())
                .createdAt(LocalDateTime.now())
                .build();

        invitationRepository.save(invitation);

        String joinUrl = frontendUrl + "/join?token=" + token;
        emailService.sendInvitationEmail(
                request.getEmail(),
                organization.getName(),
                role.getName(),
                joinUrl,
                request.getExpiryDays());

        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(orgId)
                .action("CREATE")
                .description("Người dùng " + currentUser.getUsername() + " đã gửi thư mời tham gia tổ chức cho email "
                        + request.getEmail() + " với vai trò " + role.getName())
                .entityType("MEMBER_INVITATION")
                .entityId(invitation.getId().toString())
                .timestamp(LocalDateTime.now())
                .build());

        return InvitationResponse.builder()
                .id(invitation.getId())
                .email(invitation.getEmail())
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getName())
                .roleId(role.getRoleId())
                .roleName(role.getName())
                .status(invitation.getStatus().name())
                .token(invitation.getToken())
                .joinUrl(joinUrl)
                .expiryDate(invitation.getExpiryDate())
                .createdBy(currentUser.getUserId())
                .createdAt(invitation.getCreatedAt())
                .build();
    }

    /** Lấy thông tin thư mời công khai. */
    @Override
    @Transactional
    public InvitationPublicResponse getInvitationDetails(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Thư mời không tồn tại hoặc mã token không hợp lệ"));

        if (invitation.getStatus() == InvitationStatus.PENDING
                && invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            log.info("Lazy update: Thư mời token={} đã chuyển sang EXPIRED", token);
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException("Thư mời đã quá hạn hoặc đã được sử dụng");
        }

        boolean isExistingUser = userRepository.existsByEmail(invitation.getEmail());

        return InvitationPublicResponse.builder()
                .email(invitation.getEmail())
                .organizationName(invitation.getOrganization().getName())
                .roleName(invitation.getRole().getName())
                .status(invitation.getStatus().name())
                .expiryDate(invitation.getExpiryDate())
                .isExistingUser(isExistingUser)
                .build();
    }

    /** Chấp nhận thư mời. */
    @Override
    @Transactional
    public AcceptInvitationResponse acceptInvitation(String token, AcceptInvitationRequest request) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Thư mời không tồn tại hoặc mã token không hợp lệ"));

        // Lazy update hết hạn
        if (invitation.getStatus() == InvitationStatus.PENDING
                && invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            log.info("Lazy update: Thư mời token={} đã chuyển sang EXPIRED khi cố gắng chấp nhận", token);
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException("Thư mời đã quá hạn hoặc đã được sử dụng");
        }

        // Tìm user hiện có theo email
        User existingUser = userRepository.findByEmail(invitation.getEmail()).orElse(null);

        // Nếu user đã tồn tại, yêu cầu mật khẩu đúng để xác thực
        if (existingUser != null) {
            if (request.getPassword() == null || request.getPassword().isBlank()
                    || !passwordEncoder.matches(request.getPassword(), existingUser.getPasswordHash())) {
                throw new BusinessException("Mật khẩu tài khoản không chính xác");
            }

            OrganizationUser orgUser = organizationUserRepository
                    .findByOrganization_OrganizationIdAndUser_UserId(
                            invitation.getOrganization().getOrganizationId(), existingUser.getUserId())
                    .orElseGet(() -> {
                        OrganizationUser ou = new OrganizationUser();
                        ou.setOrganization(invitation.getOrganization());
                        ou.setUser(existingUser);
                        return ou;
                    });

            if (orgUser.getStatus() == OrganizationUserStatus.ACTIVE) {
                throw new DuplicateResourceException("Bạn đã là thành viên của tổ chức này");
            }

            // Nếu vai trò được mời là VT-03 → xóa user khỏi tổ chức cũ (nếu có)
            if (RoleCode.EVENT_RECORDER.equals(invitation.getRole().getCode())) {
                List<OrganizationUser> otherOrgLinks = organizationUserRepository
                        .findByUser_UserIdAndStatus(existingUser.getUserId(), OrganizationUserStatus.ACTIVE);
                for (OrganizationUser link : otherOrgLinks) {
                    if (!link.getOrganization().getOrganizationId()
                            .equals(invitation.getOrganization().getOrganizationId())) {
                        link.setStatus(OrganizationUserStatus.INACTIVE);
                        organizationUserRepository.save(link);
                        log.info("Đã xóa user {} khỏi tổ chức {}", existingUser.getUserName(),
                                link.getOrganization().getName());
                    }
                }
            }

            // Cập nhật/tạo liên kết
            orgUser.setRole(invitation.getRole());
            orgUser.setStatus(OrganizationUserStatus.ACTIVE);
            orgUser.setJoinedAt(LocalDateTime.now());
            organizationUserRepository.save(orgUser);

            // Cập nhật thư mời
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setUsedAt(LocalDateTime.now());
            invitationRepository.save(invitation);

            log.info("User hiện có {} đã tham gia tổ chức {}", existingUser.getUserName(),
                    invitation.getOrganization().getName());
            return AcceptInvitationResponse.builder()
                    .userId(existingUser.getUserId())
                    .userName(existingUser.getUserName())
                    .fullName(existingUser.getFullName())
                    .organizationId(invitation.getOrganization().getOrganizationId())
                    .organizationName(invitation.getOrganization().getName())
                    .roleCode(invitation.getRole().getCode())
                    .build();
        }

        // User chưa tồn tại – tạo mới
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new DuplicateResourceException("Tên đăng nhập đã tồn tại trong hệ thống");
        }

        User newUser = User.builder()
                .userId(UUID.randomUUID())
                .userName(request.getUserName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(invitation.getEmail())
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setOrganization(invitation.getOrganization());
        orgUser.setUser(savedUser);
        orgUser.setRole(invitation.getRole());
        orgUser.setStatus(OrganizationUserStatus.ACTIVE);

        organizationUserRepository.save(orgUser);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setUsedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(savedUser.getUserId())
                .username(savedUser.getUserName())
                .fullName(savedUser.getFullName())
                .organizationId(invitation.getOrganization().getOrganizationId())
                .action("ACCEPT")
                .description("Người dùng " + savedUser.getUserName() + " chấp nhận thư mời tham gia tổ chức bằng email "
                        + invitation.getEmail())
                .entityType("MEMBER_INVITATION")
                .entityId(invitation.getId().toString())
                .timestamp(LocalDateTime.now())
                .build());

        log.info("Thành viên mới tham gia tổ chức thành công: username={}, organization={}",
                savedUser.getUserName(), invitation.getOrganization().getName());

        return AcceptInvitationResponse.builder()
                .userId(savedUser.getUserId())
                .userName(savedUser.getUserName())
                .fullName(savedUser.getFullName())
                .organizationId(invitation.getOrganization().getOrganizationId())
                .organizationName(invitation.getOrganization().getName())
                .roleCode(invitation.getRole().getCode())
                .build();
    }
}