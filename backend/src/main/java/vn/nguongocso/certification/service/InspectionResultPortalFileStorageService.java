package vn.nguongocso.certification.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import vn.nguongocso.exception.BusinessException;

/**
 * Dịch vụ quản lý mã định danh tệp tạm thời (Opaque File Handle) cho cổng nhập kết quả kiểm nghiệm công khai (BLOCKER 1 & QTN-20).
 *
 * <p>
 * Ngăn chặn client công khai biết đường dẫn tệp thực tế trên server và ngăn chặn tấn công giả mạo tệp
 * giữa các yêu cầu hoặc chỉ tiêu khác nhau (object-scope binding).
 * </p>
 */
@Service
public class InspectionResultPortalFileStorageService {

    /**
     * Bản ghi lưu trữ siêu dữ liệu liên kết giữa opaque handle và tệp tin thực tế.
     */
    public record PortalFileBinding(
            String fileHandle,
            String tokenHash,
            UUID requestId,
            UUID criterionId,
            String realFilePath,
            String originalFileName,
            LocalDateTime createdAt) {
    }

    private final Map<String, PortalFileBinding> handleMap = new ConcurrentHashMap<>();

    /**
     * Đăng ký tệp mới được tải lên và sinh mã handle ngẫu nhiên an toàn.
     *
     * @param tokenHash        Mã băm SHA-256 của token.
     * @param requestId        ID của yêu cầu kiểm nghiệm.
     * @param criterionId      ID của chỉ tiêu kiểm nghiệm.
     * @param realFilePath     Đường dẫn tệp thực tế trên máy chủ.
     * @param originalFileName Tên gốc của tệp tin.
     * @return Chuỗi opaque file handle (ví dụ: pfh_...).
     */
    public String registerUploadedFile(
            String tokenHash,
            UUID requestId,
            UUID criterionId,
            String realFilePath,
            String originalFileName) {

        String handle = "pfh_" + UUID.randomUUID().toString().replace("-", "");
        PortalFileBinding binding = new PortalFileBinding(
                handle,
                tokenHash,
                requestId,
                criterionId,
                realFilePath,
                originalFileName,
                LocalDateTime.now());

        handleMap.put(handle, binding);
        return handle;
    }

    /**
     * Xác thực quyền sở hữu và lấy đường dẫn tệp thực tế trước khi ghi nhận kết quả.
     *
     * @param fileHandle  Mã handle nhận được từ client.
     * @param tokenHash   Mã băm token của phiên nộp kết quả hiện tại.
     * @param requestId   ID của yêu cầu kiểm nghiệm hiện tại.
     * @param criterionId ID của chỉ tiêu kiểm nghiệm tương ứng.
     * @return Đường dẫn tệp tin thực tế trên hệ thống.
     */
    public String validateAndConsumeHandle(
            String fileHandle,
            String tokenHash,
            UUID requestId,
            UUID criterionId) {

        if (fileHandle == null || fileHandle.isBlank()) {
            return null;
        }

        PortalFileBinding binding = handleMap.get(fileHandle);
        if (binding == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Mã tệp đính kèm không hợp lệ hoặc đã hết hạn.");
        }

        // Kiểm tra đối tượng sở hữu: phải khớp chính xác token, request và criterion
        if (!binding.tokenHash().equals(tokenHash)
                || !binding.requestId().equals(requestId)
                || !binding.criterionId().equals(criterionId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tệp đính kèm không thuộc về chỉ tiêu hoặc yêu cầu kiểm nghiệm này.");
        }

        // Sau khi kiểm tra thành công, có thể xóa handle hoặc giữ lại trong transaction
        return binding.realFilePath();
    }
}
