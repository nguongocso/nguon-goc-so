package vn.nguongocso.certification.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.nguongocso.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dịch vụ quản lý mã định danh tệp tạm thời cho cổng nhập kết quả kiểm nghiệm công khai (QTN-20).
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

                if (!binding.tokenHash().equals(tokenHash)
                                || !binding.requestId().equals(requestId)
                                || !binding.criterionId().equals(criterionId)) {
                        throw new BusinessException(
                                        HttpStatus.FORBIDDEN,
                                        "Tệp đính kèm không thuộc về chỉ tiêu hoặc yêu cầu kiểm nghiệm này.");
                }

                return binding.realFilePath();
        }
}
