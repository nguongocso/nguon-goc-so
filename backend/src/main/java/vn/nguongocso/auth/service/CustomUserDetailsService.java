package vn.nguongocso.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/**
 * Dịch vụ tải thông tin người dùng và phân quyền theo tổ chức cho Spring
 * Security.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
        private final UserRepository userRepository;
        private final OrganizationUserRepository organizationUserRepository;
        private final RoleRepository roleRepository;

        /**
         * Lấy User theo username.
         */
        public User loadUser(String username) {
                return userRepository.findByUserName(username)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "Không tìm thấy người dùng"));
        }

        /**
         * Lấy UserDetails theo username và organization code.
         */
        public UserDetails loadUserByUsernameAndOrg(
                        String username,
                        String orgCode) {
                User user = userRepository.findByUserName(username)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "Không tìm thấy người dùng"));
                OrganizationUser orgUser;
                if (orgCode != null && !orgCode.isEmpty()) {

                        orgUser = organizationUserRepository
                                        .findByUserAndOrganization_Code(user, orgCode)
                                        .orElseThrow(() -> new BusinessException(
                                                        "Người dùng không thuộc tổ chức có mã: " + orgCode));
                } else {
                        orgUser = organizationUserRepository
                                        .findFirstByUser(user)
                                        .orElseThrow(() -> new BusinessException(
                                                        "Người dùng chưa được gán vào tổ chức nào"));
                }
                Role role = roleRepository
                                .findById(orgUser.getRole().getRoleId())
                                .orElseThrow(() -> new BusinessException(
                                                "Không tìm thấy vai trò của người dùng"));
                return new CustomUserDetails(
                                user,
                                orgUser,
                                role);
        }

        /**
         * Lấy UserDetails theo userId và organizationId.
         */
        @Override
        public UserDetails loadUserByUsername(String username)
                        throws UsernameNotFoundException {
                throw new UnsupportedOperationException(
                                "Vui lòng sử dụng phương thức loadUserByUsernameAndOrg()");
        }

        public CustomUserDetails loadUserByUserIdAndOrganizationId(
                        UUID userId,
                        UUID organizationId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "Không tìm thấy người dùng"));

                if (user.getStatus() != UserStatus.ACTIVE) {
                        throw new BusinessException(
                                        "Tài khoản đã bị khóa hoặc ngừng hoạt động");
                }
                OrganizationUser orgUser = organizationUserRepository
                                .findByUser_UserIdAndOrganization_OrganizationId(
                                                userId,
                                                organizationId)
                                .orElseThrow(() -> new BusinessException(
                                                "Người dùng không thuộc tổ chức này"));
                if (orgUser.getStatus() != OrganizationUserStatus.ACTIVE) {
                        throw new BusinessException(
                                        "Thành viên đã bị vô hiệu hóa trong tổ chức này");
                }
                Role role = roleRepository
                                .findById(orgUser.getRole().getRoleId())
                                .orElseThrow(() -> new BusinessException(
                                                "Không tìm thấy vai trò của người dùng"));
                return new CustomUserDetails(
                                user,
                                orgUser,
                                role);
        }
}