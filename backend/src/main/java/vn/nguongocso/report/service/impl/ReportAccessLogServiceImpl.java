package vn.nguongocso.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.report.entity.ReportAccessLog;
import vn.nguongocso.report.repository.ReportAccessLogRepository;
import vn.nguongocso.report.service.ReportAccessLogService;

import java.time.LocalDateTime;
import java.util.UUID;

/** Triển khai dịch vụ ghi log truy cập báo cáo. */
@Service
@RequiredArgsConstructor
public class ReportAccessLogServiceImpl implements ReportAccessLogService {
    private final ReportAccessLogRepository reportAccessLogRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    /** Ghi nhận nhật ký truy cập báo cáo. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAccess(
        UUID userId,
        UUID userOrgId,
        UUID targetOrgId,
        String reportName,
        boolean success,
        String ipAddress) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin tài khoản để ghi nhật ký"));
        Organization userOrg = organizationRepository.findById(userOrgId)
            .orElseThrow(() -> new BusinessException("Không tìm thấy tổ chức người dùng để ghi nhật ký"));
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