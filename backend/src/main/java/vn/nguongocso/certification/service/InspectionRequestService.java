package vn.nguongocso.certification.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateInspectionRequest;
import vn.nguongocso.certification.dto.response.InspectionRequestListResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestResponse;
import vn.nguongocso.certification.dto.response.ProductionLotTestCriteriaResponse;
import vn.nguongocso.certification.enums.InspectionRequestStatus;

/**
 * Service cho yêu cầu kiểm nghiệm
 */
public interface InspectionRequestService {

    /**
     * Tạo yêu cầu kiểm nghiệm mới
     */
    InspectionRequestResponse createInspectionRequest(
            UUID lotId,
            CreateInspectionRequest request,
            CustomUserDetails currentUser);

    ProductionLotTestCriteriaResponse getTestCriteria(
            UUID lotId,
            CustomUserDetails currentUser);

    Page<InspectionRequestListResponse> getInspectionRequests(
            UUID lotId,
            InspectionRequestStatus status,
            Pageable pageable,
            CustomUserDetails currentUser);
}