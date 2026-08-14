package vn.nguongocso.organization.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.dto.request.AddMemberRequest;
import vn.nguongocso.auth.dto.request.AssignRoleRequest;
import vn.nguongocso.auth.dto.response.OrganizationUserResponse;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.dto.request.CreateOrganizationRequest;
import vn.nguongocso.organization.dto.request.OrganizationUpdateRequest;
import vn.nguongocso.organization.dto.response.AvailableUserResponse;
import vn.nguongocso.organization.dto.response.CreateOrganizationMemberResponse;
import vn.nguongocso.organization.dto.response.OrganizationDetailResponse;
import vn.nguongocso.organization.dto.response.OrganizationProfileResponse;
import vn.nguongocso.organization.dto.response.OrganizationResponse;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.organization.service.OrganizationService;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ liên quan đến tổ chức.
 */
@Service
public class OrganizationServiceImpl
                implements OrganizationService {

        private static final Logger log = LoggerFactory.getLogger(
                        OrganizationServiceImpl.class);

        private final OrganizationRepository organizationRepository;

        private final UserRepository userRepository;

        private final RoleRepository roleRepository;

        private final OrganizationUserRepository organizationUserRepository;

        private final PasswordEncoder passwordEncoder;
        private final ApplicationEventPublisher eventPublisher;

        public OrganizationServiceImpl(
                        OrganizationRepository organizationRepository,
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        OrganizationUserRepository organizationUserRepository,
                        PasswordEncoder passwordEncoder,
                        ApplicationEventPublisher eventPublisher) {
                this.organizationRepository = organizationRepository;

                this.userRepository = userRepository;

                this.roleRepository = roleRepository;

                this.organizationUserRepository = organizationUserRepository;

                this.passwordEncoder = passwordEncoder;
                this.eventPublisher = eventPublisher;
        }

        /**
         * Tạo mới một tổ chức cùng tài khoản quản lý mặc định.
         *
         * @param request thông tin tổ chức và tài khoản quản lý
         * @return thông tin tổ chức sau khi tạo
         */
        @Override
        @Transactional
        public OrganizationResponse createOrganization(
                        CreateOrganizationRequest request) {
                log.info(
                                "Bắt đầu tạo organization với code={}",
                                request.getOrganizationCode());

                validateRequest(request);

                Organization organization = createOrganizationEntity(
                                request);

                User manager = createManager(
                                request);

                log.debug(
                                "Đã tạo Organization id={}",
                                organization.getOrganizationId());

                log.debug(
                                "Đã tạo User username={}",
                                manager.getUserName());

                assignManagerRole(
                                organization,
                                manager);

                log.info(
                                "Tạo organization thành công. organizationId={}, manager={}",
                                organization.getOrganizationId(),
                                manager.getUserName());

                return toResponse(
                                organization);
        }

        /**
         * Lấy toàn bộ danh sách tổ chức.
         *
         * @return danh sách tổ chức
         */
        @Override
        @Transactional(readOnly = true)
        public List<OrganizationResponse> getAllOrganizations() {

                log.info(
                                "Bắt đầu lấy danh sách organization");

                List<OrganizationResponse> organizations = organizationRepository
                                .findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();

                log.info(
                                "Lấy danh sách organization thành công, số lượng={}",
                                organizations.size());

                return organizations;
        }

        private Organization createOrganizationEntity(
                        CreateOrganizationRequest request) {
                log.debug(
                                "Đang lưu organization {}",
                                request.getOrganizationCode());

                Organization organization = new Organization();

                organization.setName(
                                request.getOrganizationName());

                organization.setCode(
                                request.getOrganizationCode());

                organization.setType(
                                request.getOrganizationType());

                organization.setStatus(
                                OrganizationStatus.ACTIVE);

                organization.setAddress(
                                request.getAddress());

                organization.setPhone(
                                request.getPhone());

                organization.setEmail(
                                request.getEmail());

                Organization saved = organizationRepository.save(
                                organization);

                log.debug(
                                "Đã lưu organization id={}",
                                saved.getOrganizationId());

                return saved;
        }

        private User createManager(
                        CreateOrganizationRequest request) {
                log.debug(
                                "Đang tạo tài khoản quản lý {}",
                                request.getUserName());

                User manager = new User();

                manager.setUserName(
                                request.getUserName());

                manager.setPasswordHash(
                                passwordEncoder.encode(
                                                request.getPassword()));

                manager.setFullName(
                                request.getFullName());

                manager.setPhone(
                                request.getManagerPhone());

                manager.setEmail(
                                request.getManagerEmail());

                manager.setStatus(
                                UserStatus.ACTIVE);

                User saved = userRepository.save(
                                manager);

                log.debug(
                                "Đã tạo user id={}",
                                saved.getUserId());

                return saved;
        }

        private void assignManagerRole(
                        Organization organization,
                        User manager) {
                Role role = getDefaultRole(
                                organization.getType());

                log.debug(
                                "Gán role {} cho user {} trong organization {}",
                                role.getCode(),
                                manager.getUserName(),
                                organization.getCode());

                OrganizationUser link = new OrganizationUser();

                link.setOrganization(
                                organization);

                link.setUser(
                                manager);

                link.setRole(
                                role);

                link.setStatus(
                                OrganizationUserStatus.ACTIVE);

                organizationUserRepository.save(
                                link);

                log.debug(
                                "Đã lưu OrganizationUser");
        }

        private void validateRequest(
                        CreateOrganizationRequest request) {
                if (request.getOrganizationType() == OrganizationType.SYSTEM) {
                        log.warn(
                                        "Yêu cầu tạo SYSTEM organization bị từ chối");

                        throw new BusinessException(
                                        "Không thể tạo tổ chức System thông qua API này");
                }

                if (organizationRepository.existsByCode(
                                request.getOrganizationCode())) {
                        log.warn(
                                        "Organization code đã tồn tại: {}",
                                        request.getOrganizationCode());

                        throw new BusinessException(
                                        "Organization code đã tồn tại");
                }

                if (userRepository.existsByUserName(
                                request.getUserName())) {
                        log.warn(
                                        "Username đã tồn tại: {}",
                                        request.getUserName());

                        throw new BusinessException(
                                        "Username đã tồn tại");
                }

                if (userRepository.existsByEmail(
                                request.getManagerEmail())) {
                        log.warn(
                                        "Email đã tồn tại: {}",
                                        request.getManagerEmail());

                        throw new BusinessException(
                                        "Email đã tồn tại");
                }
        }

        private String resolveManagerRoleCode(
                        OrganizationType type) {
                return switch (type) {
                        case SYSTEM ->
                                RoleCode.ADMIN;

                        case COOPERATIVE ->
                                RoleCode.ORG_MANAGER;

                        case ENTERPRISE ->
                                RoleCode.PROCUREMENT;

                        case GOVERNMENT ->
                                RoleCode.REGULATOR;
                };
        }

        private Role getDefaultRole(
                        OrganizationType type) {
                String code = resolveManagerRoleCode(
                                type);

                log.debug(
                                "Tìm role mặc định với code={}",
                                code);

                return roleRepository
                                .findByCode(code)
                                .orElseThrow(
                                                () -> {
                                                        log.error(
                                                                        "Không tìm thấy role mặc định với code={}",
                                                                        code);

                                                        return new RuntimeException(
                                                                        "Default role not found: "
                                                                                        + code);
                                                });
        }

        private OrganizationResponse toResponse(
                        Organization organization) {
                return new OrganizationResponse(
                                organization.getOrganizationId(),
                                organization.getName(),
                                organization.getCode(),
                                organization.getType(),
                                organization.getStatus(),
                                organization.getCreatedAt());
        }

        private void publishActivityLog(CustomUserDetails currentUser, String action, String description,
                        String entityType, String entityId) {
                try {
                        ActivityLogEvent.ActivityLogEventBuilder builder = ActivityLogEvent.builder()
                                        .action(action)
                                        .description(description)
                                        .entityType(entityType)
                                        .entityId(entityId)
                                        .ipAddress(IpUtils.getClientIp())
                                        .timestamp(LocalDateTime.now());

                        if (currentUser != null) {
                                builder.userId(currentUser.getUserId())
                                                .username(currentUser.getUsername())
                                                .fullName(currentUser.getFullName())
                                                .organizationId(currentUser.getOrganizationId());
                        } else {
                                // Fallback cho trường hợp không có user (ví dụ tạo tổ chức từ API public)
                                builder.userId(null)
                                                .username("SYSTEM")
                                                .fullName("Hệ thống")
                                                .organizationId(null);
                        }

                        eventPublisher.publishEvent(builder.build());
                        log.debug("Ghi log hoạt động thành công: action={}, entityType={}", action, entityType);
                } catch (Exception e) {
                        log.error("Không thể ghi log hoạt động: {}", e.getMessage(), e);
                }
        }

        private CustomUserDetails getCurrentUser() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                        return null;
                }
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                        return (CustomUserDetails) principal;
                }
                return null;
        }

        /**
         * Lấy organizationId của người dùng hiện tại.
         *
         * @return organizationId hiện tại
         */
        private UUID getCurrentOrganizationId() {
                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication == null
                                || !authentication
                                                .isAuthenticated()) {
                        throw new BusinessException(
                                        "Chưa đăng nhập");
                }

                Object principal = authentication.getPrincipal();

                if (!(principal instanceof CustomUserDetails userDetails)) {
                        throw new BusinessException(
                                        "Lỗi xác thực");
                }

                return userDetails.getOrganizationId();
        }

        /**
         * Lấy thông tin hồ sơ tổ chức hiện tại.
         *
         * @return thông tin hồ sơ tổ chức
         */
        @Override
        @Transactional(readOnly = true)
        public OrganizationProfileResponse getCurrentOrganizationProfile() {

                UUID organizationId = getCurrentOrganizationId();

                Organization organization = organizationRepository
                                .findById(
                                                organizationId)
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Tổ chức không tồn tại"));

                return toProfileResponse(
                                organization);
        }

        /**
         * Kiểm tra email/phone không trùng với tổ chức khác (khác orgId)
         */
        private void validateUniqueFieldsForUpdate(UUID orgId, String email, String phone) {
                if (email != null && !email.isBlank()) {
                        organizationRepository.findByEmail(email).ifPresent(existing -> {
                                if (!existing.getOrganizationId().equals(orgId)) {
                                        throw new BusinessException("Email đã được sử dụng bởi tổ chức khác");
                                }
                        });
                }
                if (phone != null && !phone.isBlank()) {
                        organizationRepository.findByPhone(phone).ifPresent(existing -> {
                                if (!existing.getOrganizationId().equals(orgId)) {
                                        throw new BusinessException("Số điện thoại đã được sử dụng bởi tổ chức khác");
                                }
                        });
                }
        }

        /**
         * Cập nhật hồ sơ tổ chức hiện tại.
         *
         * @param request dữ liệu cập nhật
         * @return hồ sơ sau khi cập nhật
         */
        @Override
        @Transactional
        public OrganizationProfileResponse updateCurrentOrganization(
                        OrganizationUpdateRequest request) {
                UUID organizationId = getCurrentOrganizationId();

                Organization organization = organizationRepository
                                .findById(
                                                organizationId)
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Tổ chức không tồn tại"));

                // Kiểm tra trùng email/phone với tổ chức khác
                validateUniqueFieldsForUpdate(organizationId, request.getEmail(), request.getPhone());

                organization.setName(
                                request.getName());

                organization.setAddress(
                                request.getAddress());

                organization.setPhone(
                                request.getPhone());

                organization.setEmail(
                                request.getEmail());

                organization.setUpdatedAt(
                                LocalDateTime.now());

                organizationRepository.save(
                                organization);

                log.info(
                                "Cập nhật hồ sơ tổ chức thành công: orgId={}",
                                organizationId);

                return toProfileResponse(
                                organization);
        }

        /**
         * Admin cập nhật một tổ chức theo ID.
         *
         * @param orgId   ID tổ chức
         * @param request dữ liệu cập nhật
         * @return hồ sơ sau khi cập nhật
         */
        @Override
        @Transactional
        public OrganizationProfileResponse updateOrganizationById(
                        UUID orgId,
                        OrganizationUpdateRequest request) {
                Organization organization = organizationRepository
                                .findById(
                                                orgId)
                                .orElseThrow(
                                                () -> new BusinessException(
                                                                "Tổ chức không tồn tại"));

                // Kiểm tra trùng email/phone với tổ chức khác
                validateUniqueFieldsForUpdate(orgId, request.getEmail(), request.getPhone());

                organization.setName(
                                request.getName());

                organization.setAddress(
                                request.getAddress());

                organization.setPhone(
                                request.getPhone());

                organization.setEmail(
                                request.getEmail());

                organization.setUpdatedAt(
                                LocalDateTime.now());

                organizationRepository.save(
                                organization);

                return toProfileResponse(
                                organization);
        }

        /**
         * Chuyển entity Organization sang OrganizationProfileResponse.
         *
         * @param organization entity tổ chức
         * @return response hồ sơ tổ chức
         */
        private OrganizationProfileResponse toProfileResponse(
                        Organization organization) {
                return OrganizationProfileResponse
                                .builder()
                                .organizationId(
                                                organization
                                                                .getOrganizationId())
                                .name(
                                                organization.getName())
                                .code(
                                                organization.getCode())
                                .type(
                                                organization.getType())
                                .status(
                                                organization.getStatus())
                                .address(
                                                organization.getAddress())
                                .phone(
                                                organization.getPhone())
                                .email(
                                                organization.getEmail())
                                .createdAt(
                                                organization.getCreatedAt())
                                .updatedAt(
                                                organization.getUpdatedAt())
                                .build();
        }

        /**
         * Lấy chi tiết tổ chức theo ID.
         *
         * @param organizationId ID tổ chức
         * @return chi tiết tổ chức
         */
        @Override
        @Transactional(readOnly = true)
        public OrganizationDetailResponse getOrganizationDetail(
                        UUID organizationId) {

                Organization organization = organizationRepository
                                .findById(organizationId)
                                .orElseThrow(() -> new BusinessException("Tổ chức không tồn tại"));

                List<OrganizationUserResponse> members = organizationUserRepository
                                .findByOrganization_OrganizationIdAndStatus(
                                                organizationId,
                                                OrganizationUserStatus.ACTIVE)
                                .stream()
                                .map(this::toOrganizationUserResponse)
                                .toList();

                return OrganizationDetailResponse.builder()
                                .profile(toProfileResponse(organization))
                                .members(members)
                                .build();
        }

        private OrganizationUserResponse toOrganizationUserResponse(
                        OrganizationUser organizationUser) {

                User user = organizationUser.getUser();
                Role role = organizationUser.getRole();

                return OrganizationUserResponse.builder()
                                .id(organizationUser.getId())
                                .organizationId(
                                                organizationUser.getOrganization().getOrganizationId())
                                .userId(user.getUserId())
                                .username(user.getUserName())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .roleId(role.getRoleId())
                                .roleCode(role.getCode())
                                .roleName(role.getName())
                                .status(organizationUser.getStatus())
                                .joinedAt(organizationUser.getJoinedAt())
                                .build();
        }

        /*
         * Tạo tài khoản cho 1 tổ chức cụ thể
         */
        @Override
        @Transactional
        public CreateOrganizationMemberResponse addMember(
                        UUID organizationId,
                        AddMemberRequest request) {

                log.info("Thêm thành viên vào tổ chức {}", organizationId);

                Organization organization = organizationRepository
                                .findById(organizationId)
                                .orElseThrow(() -> new BusinessException("Tổ chức không tồn tại"));

                if (userRepository.existsByUserName(request.getUsername())) {
                        throw new BusinessException("Tên đăng nhập đã tồn tại");
                }

                if (request.getEmail() != null
                                && !request.getEmail().isBlank()
                                && userRepository.existsByEmail(request.getEmail())) {
                        throw new BusinessException("Email đã tồn tại");
                }

                Role role = roleRepository.findById(request.getRoleId())
                                .orElseThrow(() -> new BusinessException("Vai trò không tồn tại"));

                User user = new User();
                user.setUserName(request.getUsername());
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                user.setFullName(request.getFullName());
                user.setPhone(request.getPhone());
                user.setEmail(request.getEmail());
                user.setStatus(UserStatus.ACTIVE);

                user = userRepository.save(user);

                OrganizationUser organizationUser = new OrganizationUser();
                organizationUser.setOrganization(organization);
                organizationUser.setUser(user);
                organizationUser.setRole(role);
                organizationUser.setStatus(OrganizationUserStatus.ACTIVE);

                organizationUser = organizationUserRepository.save(organizationUser);

                publishActivityLog(
                                getCurrentUser(),
                                "CREATE_MEMBER",
                                "Thêm thành viên " + user.getUserName(),
                                "ORGANIZATION_USER",
                                organizationUser.getId().toString());

                return CreateOrganizationMemberResponse.builder()
                                .id(organizationUser.getId())
                                .username(user.getUserName())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .roleCode(role.getCode())
                                .roleName(role.getName())
                                .status(user.getStatus())
                                .joinedAt(organizationUser.getJoinedAt())
                                .build();
        }

        /**
         * Lấy danh sách thành viên của tổ chức hiện tại.
         *
         * @return danh sách thành viên
         */
        @Override
        @Transactional(readOnly = true)
        public List<OrganizationUserResponse> getMembersOfCurrentOrganization() {
                UUID orgId = getCurrentOrganizationId(); // lấy orgId từ SecurityContext
                List<OrganizationUser> orgUsers = organizationUserRepository
                                .findByOrganization_OrganizationIdAndStatus(orgId, OrganizationUserStatus.ACTIVE);
                return orgUsers.stream()
                                .map(this::toOrganizationUserResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Lấy danh sách thành viên của một tổ chức theo ID.
         *
         * @param organizationId ID của tổ chức
         * @return danh sách thành viên
         */
        @Override
        @Transactional
        public OrganizationUserResponse assignRole(AssignRoleRequest request) {
                UUID orgId = getCurrentOrganizationId();
                String currentRoleCode = getCurrentUserRoleCode(); // cần viết thêm helper

                // 1. Tìm OrganizationUser
                OrganizationUser orgUser = organizationUserRepository
                                .findByOrganization_OrganizationIdAndUser_UserId(orgId, request.getUserId())
                                .orElseThrow(() -> new BusinessException("Thành viên không thuộc tổ chức này"));

                // 2. Lấy role mới
                Role newRole = roleRepository.findById(request.getRoleId())
                                .orElseThrow(() -> new ResourceNotFoundException("Vai trò không tồn tại"));

                // 3. Kiểm tra quyền: chỉ ADMIN mới gán được VT-01
                if (RoleCode.ADMIN.equals(newRole.getCode()) && !RoleCode.ADMIN.equals(currentRoleCode)) {
                        throw new BusinessException("Quản lý HTX không thể gán vai trò Quản trị viên nền tảng");
                }

                // 4. Nếu gán VT-02 (Quản lý HTX), hạ cấp quản lý cũ (nếu có)
                if (RoleCode.ORG_MANAGER.equals(newRole.getCode())) {
                        OrganizationUser currentManager = organizationUserRepository
                                        .findByOrganization_OrganizationIdAndRole_Code(orgId, RoleCode.ORG_MANAGER)
                                        .filter(m -> !m.getUser().getUserId().equals(request.getUserId()))
                                        .orElse(null);
                        if (currentManager != null) {
                                Role vt03Role = roleRepository.findByCode(RoleCode.EVENT_RECORDER)
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Không tìm thấy role VT-03"));
                                currentManager.setRole(vt03Role);
                                organizationUserRepository.save(currentManager);
                                log.info("Hạ quản lý cũ {} xuống VT-03", currentManager.getUser().getFullName());
                        }
                }

                // 5. Gán vai trò mới
                orgUser.setRole(newRole);
                orgUser = organizationUserRepository.save(orgUser);

                // 6. Ghi log
                CustomUserDetails currentUser = getCurrentUser();
                if (currentUser != null) {
                        publishActivityLog(
                                        currentUser,
                                        "ASSIGN_ROLE",
                                        "Gán vai trò " + newRole.getName() + " cho " + orgUser.getUser().getFullName(),
                                        "OrganizationUser",
                                        orgUser.getId().toString());
                }

                log.info("Gán role thành công: userId={}, orgId={}, newRole={}",
                                request.getUserId(), orgId, newRole.getCode());
                return toOrganizationUserResponse(orgUser);
        }

        private String getCurrentUserRoleCode() {
                CustomUserDetails userDetails = getCurrentUser();
                if (userDetails == null) {
                        throw new BusinessException("Chưa đăng nhập");
                }
                return userDetails.getRoleCode();
        }

        /**
         * Lấy danh sách người dùng có thể được gán vào tổ chức.
         *
         * @param organizationId ID của tổ chức
         * @return danh sách người dùng có thể được gán
         */
        @Override
        @Transactional(readOnly = true)
        public List<AvailableUserResponse> getAvailableUsersForOrganization(UUID organizationId) {
                Organization org = organizationRepository.findById(organizationId)
                                .orElseThrow(() -> new BusinessException("Tổ chức không tồn tại"));

                // Lấy danh sách user đã thuộc tổ chức này
                List<UUID> existingUserIds = organizationUserRepository
                                .findByOrganization_OrganizationId(organizationId)
                                .stream()
                                .map(ou -> ou.getUser().getUserId())
                                .toList();

                // Lấy các tổ chức cùng loại (không bao gồm tổ chức hiện tại)
                List<Organization> sameTypeOrgs = organizationRepository
                                .findByTypeAndOrganizationIdNot(org.getType(), organizationId);

                // Lấy tất cả user trong các tổ chức đó (đã active)
                List<User> availableUsers = organizationUserRepository
                                .findAllByOrganizationIn(sameTypeOrgs)
                                .stream()
                                .map(OrganizationUser::getUser)
                                .distinct()
                                .filter(u -> !existingUserIds.contains(u.getUserId()))
                                .toList();

                // Với mỗi user, lấy role hiện tại (trong tổ chức cùng loại đầu tiên)
                return availableUsers.stream()
                                .map(user -> {
                                        OrganizationUser orgUser = organizationUserRepository
                                                        .findFirstByUserAndOrganization_Type(user, org.getType())
                                                        .orElse(null);
                                        return AvailableUserResponse.builder()
                                                        .userId(user.getUserId())
                                                        .username(user.getUserName())
                                                        .fullName(user.getFullName())
                                                        .email(user.getEmail())
                                                        .phone(user.getPhone())
                                                        .currentRoleCode(orgUser != null ? orgUser.getRole().getCode()
                                                                        : null)
                                                        .currentRoleName(orgUser != null ? orgUser.getRole().getName()
                                                                        : null)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Thêm một người dùng hiện có vào tổ chức.
         *
         * @param organizationId ID của tổ chức
         * @param userId         ID của người dùng
         * @param roleId         ID của vai trò (nếu null, giữ vai trò hiện tại)
         * @return thông tin người dùng trong tổ chức sau khi thêm
         */
        @Override
        @Transactional
        public OrganizationUserResponse addExistingUserToOrganization(UUID organizationId, UUID userId,
                        Integer roleId) {
                Organization org = organizationRepository.findById(organizationId)
                                .orElseThrow(() -> new BusinessException("Tổ chức không tồn tại"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new BusinessException("User không tồn tại"));

                if (organizationUserRepository.existsByOrganizationAndUser(org, user)) {
                        throw new BusinessException("User đã thuộc tổ chức này");
                }

                // Xác định role
                Role role;
                if (roleId != null) {
                        role = roleRepository.findById(roleId)
                                        .orElseThrow(() -> new BusinessException("Vai trò không tồn tại"));
                } else {
                        // Lấy role hiện tại của user trong tổ chức cùng loại
                        OrganizationUser currentOrgUser = organizationUserRepository
                                        .findFirstByUserAndOrganization_Type(user, org.getType())
                                        .orElseThrow(() -> new BusinessException(
                                                        "Không tìm thấy role hiện tại của user"));
                        role = currentOrgUser.getRole();
                }

                // Tạo liên kết mới
                OrganizationUser newOrgUser = new OrganizationUser();
                newOrgUser.setOrganization(org);
                newOrgUser.setUser(user);
                newOrgUser.setRole(role);
                newOrgUser.setStatus(OrganizationUserStatus.ACTIVE);
                newOrgUser = organizationUserRepository.save(newOrgUser);

                // Ghi log
                CustomUserDetails currentUser = getCurrentUser();
                if (currentUser != null) {
                        publishActivityLog(
                                        currentUser,
                                        "ADD_EXISTING_USER",
                                        "Thêm user " + user.getUserName() + " vào tổ chức " + org.getName(),
                                        "OrganizationUser",
                                        newOrgUser.getId().toString());
                }

                return toOrganizationUserResponse(newOrgUser);
        }
}