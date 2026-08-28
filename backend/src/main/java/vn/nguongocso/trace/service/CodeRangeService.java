package vn.nguongocso.trace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.dto.request.CreateCodeRangeRequest;
import vn.nguongocso.trace.dto.response.CodeRangeResponse;
import vn.nguongocso.trace.dto.response.CodeRangeStatusResponse;
import vn.nguongocso.trace.dto.response.RemainingCodesResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.repository.CodeRangeRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
/** Cấp và theo dõi dải mã truy xuất. */
public class CodeRangeService {
    private final CodeRangeRepository codeRangeRepository;
    private final OrganizationRepository organizationRepository;

    /** Cấp một dải mã mới cho tổ chức. */
    @Transactional
    public CodeRangeResponse createCodeRange(CreateCodeRangeRequest request, CustomUserDetails admin) {
        // Kiểm tra quyền admin
        if (!"VT-01".equals(admin.getRoleCode())) {
            throw new BusinessException("Chỉ quản trị viên nền tảng mới có quyền cấp dải mã");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new BusinessException("Tổ chức không tồn tại"));

        // Kiểm tra prefix trùng
        if (codeRangeRepository.findByPrefix(request.getPrefix()).isPresent()) {
            throw new BusinessException("Tiền tố mã đã tồn tại");
        }

        // (Tùy chọn) kiểm tra hạn mức so với sản lượng khai báo (nếu có)
        // Hiện tại chưa có sản lương, có thể bỏ qua hoặc thêm sau

        CodeRange codeRange = CodeRange.builder()
                .organization(organization)
                .prefix(request.getPrefix())
                .fromNumber(null)
                .toNumber(null)
                .totalLimit(request.getTotalLimit())
                .usedCount(0L)
                .createdBy(admin.getUserId())
                .build();
        codeRangeRepository.save(codeRange);

        log.info("Cấp dải mã thành công: prefix={}, org={}", codeRange.getPrefix(), organization.getCode());

        return toResponse(codeRange);
    }

    /** Chuyển entity dải mã sang response. */
    private CodeRangeResponse toResponse(CodeRange codeRange) {

        return CodeRangeResponse.builder()
                .id(codeRange.getId())
                .organizationId(codeRange.getOrganization().getOrganizationId())
                .organizationName(codeRange.getOrganization().getName())
                .prefix(codeRange.getPrefix())
                .totalLimit(codeRange.getTotalLimit())
                .usedCount(codeRange.getUsedCount())
                .createdAt(codeRange.getCreatedAt())
                .build();
    }

    /** Lấy trạng thái sử dụng của toàn bộ dải mã. */
    public List<CodeRangeStatusResponse> getCodeRangeStatus() {
        List<CodeRange> ranges = codeRangeRepository.findAll();
        return ranges.stream()
                .map(this::toStatusResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy số lượng mã truy xuất còn lại của một tổ chức.
     *
     * <p>
     * Chỉ đọc để hiển thị nên dùng query KHÔNG khoá row. Nếu dùng query có
     * {@code FOR UPDATE} (PESSIMISTIC_WRITE) thì MySQL sẽ báo lỗi 1792
     * "Cannot execute statement in a READ ONLY transaction" vì phương thức này
     * chạy trong transaction {@code readOnly = true}.
     * </p>
     *
     * @param organizationId ID của tổ chức
     * @return thông tin số mã còn lại
     */
    @Transactional(readOnly = true)
    public RemainingCodesResponse getRemainingCodesForOrganization(UUID organizationId) {
        return codeRangeRepository
                .findFirstReadOnlyByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId)
                .map(range -> RemainingCodesResponse.builder()
                        .remainingCount(Math.max(0, range.getTotalLimit() - range.getUsedCount()))
                        .totalLimit(range.getTotalLimit())
                        .usedCount(range.getUsedCount())
                        .hasCodeRange(true)
                        .build())
                .orElse(RemainingCodesResponse.builder()
                        .remainingCount(0)
                        .totalLimit(0)
                        .usedCount(0)
                        .hasCodeRange(false)
                        .build());
    }

    /** Chuyển entity dải mã sang response trạng thái. */
    private CodeRangeStatusResponse toStatusResponse(CodeRange range) {
        double percent = (double) range.getUsedCount() / range.getTotalLimit() * 100;
        String status;
        if (percent >= 100)
            status = "EXHAUSTED";
        else if (percent >= 80)
            status = "NEARLY_EXHAUSTED";
        else
            status = "OK";

        return CodeRangeStatusResponse.builder()
                .id(range.getId())
                .organizationId(range.getOrganization().getOrganizationId())
                .organizationName(range.getOrganization().getName())
                .prefix(range.getPrefix())
                .totalLimit(range.getTotalLimit())
                .usedCount(range.getUsedCount())
                .usagePercent(Math.round(percent * 10) / 10.0)
                .status(status)
                .build();
    }
}
