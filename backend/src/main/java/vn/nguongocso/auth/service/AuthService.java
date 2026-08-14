package vn.nguongocso.auth.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.nguongocso.auth.dto.request.LoginRequest;
import vn.nguongocso.auth.dto.request.SelectOrganizationRequest;
import vn.nguongocso.auth.dto.response.LoginResponse;
import vn.nguongocso.auth.dto.response.OrganizationSelectionResponse;
import vn.nguongocso.auth.dto.response.SelectOrganizationResponse;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/**
 * Service xử lý xác thực người dùng.
 *
 * <p>
 * Authentication được chia thành 2 giai đoạn:
 * </p>
 *
 * <ol>
 * <li>
 * Login bằng username/password.
 * </li>
 * <li>
 * Chọn organization để xác lập organization context
 * và cấp Access JWT.
 * </li>
 * </ol>
 *
 * <p>
 * Trong giai đoạn login, hệ thống chưa xác định organization.
 * Sau khi username/password hợp lệ, hệ thống cấp một
 * Selection JWT có thời gian sống ngắn.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final CustomUserDetailsService userDetailsService;

    private final JwtTokenProvider tokenProvider;

    private final PasswordEncoder passwordEncoder;

    private final OrganizationUserRepository organizationUserRepository;

    /**
     * Xác thực người dùng bằng username và password.
     *
     * <p>
     * Phương thức này chỉ thực hiện authentication ở cấp User.
     * Organization và Role chưa được xác định tại bước này.
     * </p>
     *
     * <p>
     * Nếu xác thực thành công, hệ thống cấp một Selection JWT
     * có thời hạn 5 phút để người dùng tiếp tục chọn organization.
     * </p>
     *
     * @param request thông tin đăng nhập
     * @return Selection JWT và thông tin cơ bản của user
     * @throws BusinessException nếu thông tin đăng nhập không hợp lệ
     */
    public LoginResponse login(LoginRequest request) {

        try {

            log.info(
                    "Đăng nhập: user={}",
                    request.getUsername());

            /*
             * =====================================================
             * 1. Tìm User
             * =====================================================
             *
             * Ở bước này KHÔNG tìm OrganizationUser.
             */
            User user = userDetailsService.loadUser(
                    request.getUsername());

            /*
             * =====================================================
             * 2. Kiểm tra trạng thái User
             * =====================================================
             */
            if (user.getStatus() != UserStatus.ACTIVE) {

                throw new BusinessException(
                        "Tài khoản đã bị khóa hoặc không hoạt động");
            }

            /*
             * =====================================================
             * 3. Kiểm tra Password
             * =====================================================
             */
            boolean passwordMatches = passwordEncoder.matches(
                    request.getPassword(),
                    user.getPasswordHash());

            if (!passwordMatches) {

                throw new BusinessException(
                        "Sai mật khẩu");
            }

            /*
             * =====================================================
             * 4. Tạo Selection JWT
             * =====================================================
             *
             * Selection JWT:
             *
             * tokenType = ORG_SELECTION
             * TTL = 5 phút
             *
             * Token này chưa chứa:
             *
             * - organizationId
             * - organizationCode
             * - role
             */
            String selectionToken = tokenProvider.generateSelectionToken(user);

            log.info(
                    "Xác thực username/password thành công, "
                            + "chờ chọn tổ chức: user={}",
                    request.getUsername());

            /*
             * =====================================================
             * 5. Trả response
             * =====================================================
             */
            return buildSelectionLoginResponse(
                    user,
                    selectionToken);

        } catch (UsernameNotFoundException e) {

            log.warn(
                    "Không tìm thấy user: {}",
                    request.getUsername());

            throw new BusinessException(
                    "Tài khoản không tồn tại");

        } catch (BusinessException e) {

            throw e;

        } catch (Exception e) {

            log.error(
                    "Lỗi không xác định khi đăng nhập: user={}",
                    request.getUsername(),
                    e);

            throw new BusinessException(
                    "Lỗi hệ thống, vui lòng thử lại sau");
        }
    }

    /**
     * Lấy danh sách organization mà user hiện tại được gán vào.
     *
     * <p>
     * Phương thức này được sử dụng sau khi login thành công
     * nhưng trước khi organization được lựa chọn.
     * </p>
     *
     * <p>
     * User được xác định từ userId nằm trong Selection JWT.
     * Không sử dụng SecurityContext vì Selection JWT
     * không tạo Authentication.
     * </p>
     *
     * @param selectionToken Selection JWT
     * @return danh sách organization của user
     */
    public List<OrganizationSelectionResponse> getOrganizations(
            String selectionToken) {

        /*
         * =====================================================
         * 1. Kiểm tra Selection JWT
         * =====================================================
         */
        if (!tokenProvider.validateToken(selectionToken)) {

            throw new BusinessException(
                    "Selection token không hợp lệ hoặc đã hết hạn");
        }

        /*
         * =====================================================
         * 2. Kiểm tra đúng loại token
         * =====================================================
         *
         * Endpoint này CHỈ chấp nhận:
         *
         * ORG_SELECTION
         */
        String tokenType = tokenProvider.getTokenTypeFromToken(selectionToken);

        if (!JwtTokenProvider.TOKEN_TYPE_SELECTION.equals(tokenType)) {

            throw new BusinessException(
                    "Token không hợp lệ cho bước chọn tổ chức");
        }

        /*
         * =====================================================
         * 3. Lấy User ID từ Selection JWT
         * =====================================================
         */
        UUID userId = tokenProvider.getUserIdFromToken(selectionToken);

        /*
         * =====================================================
         * 4. Lấy các organization membership của User
         * =====================================================
         */
        List<OrganizationUser> organizationUsers = organizationUserRepository
                .findByUser_UserIdAndStatus(
                        userId,
                        OrganizationUserStatus.ACTIVE);

        /*
         * =====================================================
         * 5. Kiểm tra User có organization hay không
         * =====================================================
         */
        if (organizationUsers.isEmpty()) {

            throw new BusinessException(
                    "Người dùng chưa được gán vào tổ chức nào");
        }

        /*
         * =====================================================
         * 6. Chuyển sang response
         * =====================================================
         */
        return organizationUsers.stream()
                .map(organizationUser -> {

                    var organization = organizationUser.getOrganization();

                    var role = organizationUser.getRole();

                    return OrganizationSelectionResponse.builder()
                            .organizationId(
                                    organization
                                            .getOrganizationId()
                                            .toString())
                            .organizationCode(
                                    organization.getCode())
                            .organizationName(
                                    organization.getName())
                            .organizationType(
                                    organization.getType())
                            .roleCode(
                                    role.getCode())
                            .roleName(
                                    role.getName())
                            .build();
                })
                .toList();
    }

        public List<OrganizationSelectionResponse> getOrganizationsForUser(UUID userId) {
                return organizationUserRepository
                                .findByUser_UserIdAndStatus(userId, OrganizationUserStatus.ACTIVE)
                                .stream()
                                .map(organizationUser -> {
                                        var organization = organizationUser.getOrganization();
                                        var role = organizationUser.getRole();

                                        return OrganizationSelectionResponse.builder()
                                                        .organizationId(organization.getOrganizationId().toString())
                                                        .organizationCode(organization.getCode())
                                                        .organizationName(organization.getName())
                                                        .organizationType(organization.getType())
                                                        .roleCode(role.getCode())
                                                        .roleName(role.getName())
                                                        .build();
                                })
                                .toList();
        }

    /**
     * Tạo response sau khi xác thực username/password thành công.
     *
     * <p>
     * Response này chưa chứa thông tin organization hoặc role
     * vì user chưa lựa chọn organization.
     * </p>
     *
     * @param user           user đã xác thực
     * @param selectionToken Selection JWT
     * @return login response
     */
    private LoginResponse buildSelectionLoginResponse(
            User user,
            String selectionToken) {

        return LoginResponse.builder()

                /*
                 * Selection JWT
                 */
                .selectionToken(selectionToken)

                /*
                 * Token type
                 *
                 * Có thể giữ "Bearer" vì client vẫn gửi token
                 * trong Authorization header.
                 *
                 * Việc token là Selection hay Access được
                 * xác định bằng claim "tokenType".
                 */
                .tokenType("Bearer")

                /*
                 * Selection JWT sống 5 phút.
                 */
                .expiresIn(
                        tokenProvider
                                .getSelectionTokenExpirationInSeconds())

                /*
                 * Thông tin cơ bản của User.
                 *
                 * Không trả organization ở bước này.
                 */
                .user(
                        LoginResponse.UserInfo.builder()

                                .userId(
                                        user.getUserId()
                                                .toString())

                                .username(
                                        user.getUserName())

                                .fullName(
                                        user.getFullName())

                                .build())

                .build();
    }

    /**
     * Xác thực người dùng bằng Selection JWT và lựa chọn organization.
     *
     * <p>
     * Phương thức này được sử dụng sau khi login thành công
     * và user đã lựa chọn organization.
     * </p>
     *
     * <p>
     * Nếu xác thực thành công, hệ thống cấp một Access JWT
     * có thời hạn 1 giờ để người dùng tiếp tục truy cập các API nghiệp vụ.
     * </p>
     *
     * @param selectionToken Selection JWT
     * @param request        thông tin lựa chọn organization
     * @return Access JWT và thông tin user trong organization đã chọn
     * @throws BusinessException nếu token không hợp lệ hoặc user không thuộc tổ
     *                           chức
     */
    public SelectOrganizationResponse selectOrganization(
            String selectionToken,
            SelectOrganizationRequest request) {

        try {

            /*
             * =====================================================
             * 1. Validate Selection JWT
             * =====================================================
             */
            if (!tokenProvider.validateToken(selectionToken)) {

                throw new BusinessException(
                        "Selection token không hợp lệ hoặc đã hết hạn");
            }

            /*
             * =====================================================
             * 2. Kiểm tra token type
             * =====================================================
             */
            String tokenType = tokenProvider.getTokenTypeFromToken(
                    selectionToken);

            if (!JwtTokenProvider.TOKEN_TYPE_SELECTION
                    .equals(tokenType)) {

                throw new BusinessException(
                        "Token không phải là organization selection token");
            }

            /*
             * =====================================================
             * 3. Lấy User ID từ token
             * =====================================================
             */
            UUID userId = tokenProvider.getUserIdFromToken(
                    selectionToken);

            /*
             * =====================================================
             * 4. Tìm membership
             * =====================================================
             */
            OrganizationUser orgUser = organizationUserRepository
                    .findByUser_UserIdAndOrganization_OrganizationId(
                            userId,
                            request.getOrganizationId())
                    .orElseThrow(() -> new BusinessException(
                            "Người dùng không thuộc tổ chức này"));

            /*
             * =====================================================
             * 5. Kiểm tra membership
             * =====================================================
             */
            if (orgUser.getStatus() == null) {

                throw new BusinessException(
                        "Trạng thái thành viên tổ chức không hợp lệ");
            }

            /*
             * =====================================================
             * 6. Tạo UserDetails với organization context
             * =====================================================
             */
            CustomUserDetails userDetails = userDetailsService
                    .loadUserByUserIdAndOrganizationId(
                            userId,
                            request.getOrganizationId());

            /*
             * =====================================================
             * 7. Tạo ACCESS JWT
             * =====================================================
             */
            String accessToken = tokenProvider.generateAccessToken(
                    userDetails);

            /*
             * =====================================================
             * 8. Response
             * =====================================================
             */
            return SelectOrganizationResponse.builder()

                    .accessToken(accessToken)

                    .tokenType("Bearer")

                    .expiresIn(
                            tokenProvider.getExpirationInSeconds())

                    .user(
                            SelectOrganizationResponse.UserInfo.builder()

                                    .userId(
                                            userDetails
                                                    .getUserId()
                                                    .toString())

                                    .username(
                                            userDetails.getUsername())

                                    .fullName(
                                            userDetails.getFullName())

                                    .organizationId(
                                            userDetails
                                                    .getOrganizationId()
                                                    .toString())

                                    .organizationCode(
                                            userDetails
                                                    .getOrganizationCode())

                                    .organizationName(
                                            userDetails
                                                    .getOrganizationName())

                                    .organizationType(
                                            userDetails
                                                    .getOrganizationType()
                                                    .name())

                                    .roleCode(
                                            userDetails.getRoleCode())

                                    .roleName(
                                            userDetails.getRoleName())

                                    .build())

                    .build();

        } catch (BusinessException e) {

            throw e;

        } catch (Exception e) {

            log.error(
                    "Lỗi khi chọn organization",
                    e);

            throw new BusinessException(
                    "Lỗi hệ thống, vui lòng thử lại sau");
        }
    }

    public SelectOrganizationResponse switchOrganization(
            UUID userId,
            SelectOrganizationRequest request) {

        OrganizationUser orgUser = organizationUserRepository
                .findByUser_UserIdAndOrganization_OrganizationId(
                        userId,
                        request.getOrganizationId())
                .orElseThrow(() -> new BusinessException(
                        "Người dùng không thuộc tổ chức này"));

        if (orgUser.getStatus() != OrganizationUserStatus.ACTIVE) {
            throw new BusinessException("Tổ chức không còn hoạt động với tài khoản này");
        }

        CustomUserDetails userDetails = userDetailsService
                .loadUserByUserIdAndOrganizationId(userId, request.getOrganizationId());

        return SelectOrganizationResponse.builder()
                .accessToken(tokenProvider.generateAccessToken(userDetails))
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationInSeconds())
                .user(SelectOrganizationResponse.UserInfo.builder()
                        .userId(userDetails.getUserId().toString())
                        .username(userDetails.getUsername())
                        .fullName(userDetails.getFullName())
                        .organizationId(userDetails.getOrganizationId().toString())
                        .organizationCode(userDetails.getOrganizationCode())
                        .organizationName(userDetails.getOrganizationName())
                        .organizationType(userDetails.getOrganizationType().name())
                        .roleCode(userDetails.getRoleCode())
                        .roleName(userDetails.getRoleName())
                        .build())
                .build();
    }
}