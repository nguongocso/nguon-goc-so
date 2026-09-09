package vn.nguongocso.certification.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.AttachCertificationRequest;
import vn.nguongocso.certification.dto.request.CreateCertificationRequest;
import vn.nguongocso.certification.dto.request.RejectCertificateRequest;
import vn.nguongocso.certification.dto.request.VerifyCertificateRequest;
import vn.nguongocso.certification.dto.response.CertificationResponse;
import vn.nguongocso.certification.dto.response.CertificationVerificationResponse;
import vn.nguongocso.certification.dto.response.ProductionLotCertificationResponse;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;
import vn.nguongocso.common.PageResponse;

import java.util.List;
import java.util.UUID;

/**
 * Giao diện CertificationService định nghĩa các phương thức liên quan đến quản
 * lý và xác thực chứng nhận.
 */
public interface CertificationService {

        /** Record chứa tài liệu chứng nhận phục vụ xem trực tiếp hoặc tải về an toàn. */
        record DocumentResource(Resource resource, MediaType contentType, String fileName) {}

        /**
         * Lấy danh sách chứng nhận của một lô sản xuất.
         */
        List<ProductionLotCertificationResponse> getCertificationsOfLot(
                        UUID lotId,
                        CustomUserDetails currentUser);

        /**
         * Gắn chứng nhận cho lô sản xuất.
         */
        ProductionLotCertificationResponse attachCertification(
                        UUID lotId,
                        AttachCertificationRequest request,
                        CustomUserDetails currentUser);

        /**
         * Gỡ bỏ chứng nhận khỏi lô sản xuất.
         */
        void detachCertification(
                        UUID lotId,
                        UUID certificationId,
                        CustomUserDetails currentUser);

        /**
         * Lấy danh sách chứng nhận hợp lệ của tổ chức hiện tại để gắn cho lô (loại bỏ EXPIRED và REJECTED).
         */
        List<CertificationResponse> getValidCertifications(
                        CustomUserDetails currentUser);

        /**
         * Tạo mới chứng nhận cho tổ chức hiện tại.
         */
        CertificationResponse createCertification(
                        CreateCertificationRequest request,
                        CustomUserDetails currentUser);

        /**
         * Tìm kiếm chứng nhận của tổ chức hiện tại theo từ khoá và trạng thái
         * hiệu lực, có phân trang và sắp xếp.
         */
        PageResponse<CertificationResponse> searchCertifications(
                        String keyword,
                        String status,
                        String sortBy,
                        String sortDir,
                        int page,
                        int size,
                        CustomUserDetails currentUser);

        /**
         * Kiểm tra và tạo cảnh báo cho các chứng nhận
         * đã hết hạn hoặc sắp hết hạn.
         */
        void checkCertificationExpiry();

        /**
         * Lấy danh sách chứng nhận trên toàn nền tảng để Quản trị viên (VT-01) kiểm tra, đối chiếu.
         *
         * @param status         Trạng thái xác thực (mặc định PENDING nếu null).
         * @param keyword        Từ khóa tìm kiếm (số hiệu, tên, cơ quan cấp, tiêu chuẩn, tên tổ chức).
         * @param organizationId ID tổ chức (tùy chọn).
         * @param sortBy         Trường sắp xếp (createdAt, reviewedAt, expiryDate).
         * @param sortDir        Chiều sắp xếp (asc, desc).
         * @param page           Chỉ số trang (từ 0).
         * @param size           Số phần tử mỗi trang (1-100).
         * @param currentUser    Người dùng hiện tại (bắt buộc role VT-01).
         * @return Trang dữ liệu chứng nhận chờ duyệt.
         */
        PageResponse<CertificationVerificationResponse> getAdminCertifications(
                        CertificationVerificationStatus status,
                        String keyword,
                        UUID organizationId,
                        String sortBy,
                        String sortDir,
                        int page,
                        int size,
                        CustomUserDetails currentUser);

        /**
         * Lấy thông tin chi tiết đầy đủ dữ liệu cần đối chiếu của một chứng nhận (VT-01).
         *
         * @param certificationId ID chứng nhận.
         * @param currentUser     Người dùng hiện tại (bắt buộc role VT-01).
         * @return Chi tiết chứng nhận phục vụ đối chiếu.
         */
        CertificationVerificationResponse getAdminCertificationDetail(
                        UUID certificationId,
                        CustomUserDetails currentUser);

        /**
         * Lấy tệp tài liệu chứng nhận an toàn (VT-01).
         *
         * @param certificationId ID chứng nhận.
         * @param currentUser     Người dùng hiện tại (bắt buộc role VT-01).
         * @return Tài nguyên tệp đính kèm.
         */
        DocumentResource getCertificateDocumentResource(
                        UUID certificationId,
                        CustomUserDetails currentUser);

        /**
         * Xác thực chứng nhận của tổ chức (VT-01).
         *
         * @param certificationId ID chứng nhận cần xác thực.
         * @param request         Ghi chú xác thực (tùy chọn).
         * @param currentUser     Người dùng hiện tại (bắt buộc role VT-01).
         * @return Kết quả xác thực chứng nhận.
         */
        CertificationVerificationResponse verifyCertificate(
                        UUID certificationId,
                        VerifyCertificateRequest request,
                        CustomUserDetails currentUser);

        /**
         * Từ chối xác thực chứng nhận của tổ chức kèm lý do (VT-01).
         *
         * @param certificationId ID chứng nhận cần từ chối.
         * @param request         Lý do từ chối (bắt buộc).
         * @param currentUser     Người dùng hiện tại (bắt buộc role VT-01).
         * @return Kết quả từ chối chứng nhận kèm số lượng thông báo đã gửi.
         */
        CertificationVerificationResponse rejectCertificate(
                        UUID certificationId,
                        RejectCertificateRequest request,
                        CustomUserDetails currentUser);
}