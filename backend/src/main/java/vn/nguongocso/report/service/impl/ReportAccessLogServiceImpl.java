package vn.nguongocso.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.report.entity.ReportAccessLog;
import vn.nguongocso.report.repository.ReportAccessLogRepository;
import vn.nguongocso.report.service.ReportAccessLogService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service ghi log truy cập báo cáo.
 *
 * @author Triệu Văn Đại
 */
@Service
@RequiredArgsConstructor
public class ReportAccessLogServiceImpl implements ReportAccessLogService {
        private final ReportAccessLogRepository reportAccessLogRepository;
        private final UserRepository userRepository;
        private final OrganizationRepository organizationRepository;

        /**
         * Ghi log truy cập báo cáo.
         *
         * @param userId      ID của người dùng truy cập
         * @param userOrgId   ID của tổ chức người dùng
         * @param targetOrgId ID của tổ chức đích
         * @param reportName  Tên báo cáo được truy cập
         * @param success     Trạng thái thành công hay thất bại của truy cập
         * @param ipAddress   Địa chỉ IP của người dùng
         */
        @Override
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logAccess(UUID userId, UUID userOrgId, UUID targetOrgId, String reportName, boolean success,
                        String ipAddress) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new BusinessException(
                                                "Không tìm thấy thông tin tài khoản để ghi nhật ký"));
                Organization userOrg = organizationRepository.findById(userOrgId)
                                .orElseThrow(() -> new BusinessException(
                                                "Không tìm thấy tổ chức người dùng để ghi nhật ký"));
                Organization targetOrg = organizationRepository.findById(targetOrgId)
                                .orElseThrow(() -> new BusinessException("Không tìm thấy tổ chức đích để ghi nhật ký"));
                ReportAccessLog logEntry = ReportAccessLog.builder()
                                .user(user)
                                .organization(userOrg)
                                .targetOrganization(targetOrg)
                                .reportName(reportName)
                                .accessedAt(LocalDateTime.now())
                                .success(success)
                                .ipAddress(ipAddress)
                                .build();
                reportAccessLogRepository.save(logEntry);
        }
}