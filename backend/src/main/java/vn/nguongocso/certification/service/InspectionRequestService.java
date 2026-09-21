package vn.nguongocso.certification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateInspectionRequest;
import vn.nguongocso.certification.dto.response.InspectionRequestDetailResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestListResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestResponse;
import vn.nguongocso.certification.dto.response.ProductionLotTestCriteriaResponse;
import vn.nguongocso.certification.enums.InspectionRequestStatus;

import java.util.UUID;

/**
 * Service quản lý yêu cầu kiểm nghiệm.
 */
public interface InspectionRequestService {
        /**
         * Tạo yêu cầu kiểm nghiệm mới cho lô sản xuất.
         */
        InspectionRequestResponse createInspectionRequest(
                        UUID lotId,
                        CreateInspectionRequest request,
                        CustomUserDetails currentUser);

        /**
         * Lấy danh sách chỉ tiêu kiểm nghiệm áp dụng cho lô sản xuất.
         */
        ProductionLotTestCriteriaResponse getTestCriteria(
                        UUID lotId,
                        CustomUserDetails currentUser);

        /**
         * Lấy danh sách yêu cầu kiểm nghiệm của lô sản xuất có phân trang và lọc theo trạng thái.
         */
        Page<InspectionRequestListResponse> getInspectionRequests(
                        UUID lotId,
                        InspectionRequestStatus status,
                        Pageable pageable,
                        CustomUserDetails currentUser);

        /**
         * Lấy chi tiết yêu cầu kiểm nghiệm kèm danh sách chỉ tiêu và kết quả đã ghi.
         */
        InspectionRequestDetailResponse getDetail(
                        UUID requestId,
                        CustomUserDetails currentUser);
}