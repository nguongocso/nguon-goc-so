package vn.nguongocso.export.service;

import org.springframework.core.io.Resource;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;
import vn.nguongocso.export.dto.response.OpenDataExportJobResponse;

import java.util.UUID;

/**
 * Giao diện dịch vụ xử lý xuất dữ liệu mở bất đồng bộ (Async Job Pattern).
 */
public interface OpenDataAsyncExportService {

    /**
     * Khởi tạo tác vụ xuất dữ liệu nền và trả về thông tin job.
     */
    OpenDataExportJobResponse submitJob(ExportOpenDataRequest request, CustomUserDetails currentUser);

    /**
     * Lấy thông tin trạng thái tác vụ theo mã định danh job.
     */
    OpenDataExportJobResponse getJobStatus(UUID jobId);

    /**
     * Lấy tài nguyên tệp đã xuất để tải về.
     */
    Resource getJobDownload(UUID jobId);

    /**
     * Dọn dẹp các tệp tạm và job cũ quá thời hạn TTL (mặc định 24h).
     */
    void cleanupExpiredJobs();
}
