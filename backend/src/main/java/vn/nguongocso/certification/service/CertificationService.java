package vn.nguongocso.certification.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.AttachCertificationRequest;
import vn.nguongocso.certification.dto.request.CreateCertificationRequest;
import vn.nguongocso.certification.dto.response.CertificationResponse;
import vn.nguongocso.certification.dto.response.ProductionLotCertificationResponse;
import vn.nguongocso.common.PageResponse;

import java.util.List;
import java.util.UUID;

/**
 * Giao diện CertificationService định nghĩa các phương thức liên quan đến quản
 * lý chứng nhận.
 */
public interface CertificationService {
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
         * Lấy danh sách chứng nhận hợp lệ của tổ chức hiện tại.
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
         *
         * <p>Ba trạng thái rời rạc (không giao nhau):
         * {@code valid} = hết hạn sau hơn 30 ngày,
         * {@code expiring} = còn hiệu lực trong vòng 30 ngày,
         * {@code expired} = đã quá hạn.</p>
         *
         * @param keyword      từ khoá tìm theo tên / số hiệu / cơ quan cấp
         *                     (null hoặc rỗng để bỏ qua).
         * @param status       valid | expiring | expired (null để lấy tất cả).
         * @param sortBy       trường sắp xếp (name | issueDate | expiryDate).
         * @param sortDir      asc | desc (mặc định desc).
         * @param page         chỉ số trang (bắt đầu từ 0).
         * @param size         số bản ghi mỗi trang (tối đa 100).
         * @param currentUser  người dùng hiện tại (scope theo tổ chức).
         * @return trang dữ liệu chứng nhận kèm tổng số bản ghi.
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
}