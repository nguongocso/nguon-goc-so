package vn.nguongocso.certification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionCatalogRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionCatalogResponse;

/**
 * Service for managing the inspection criterion catalog.
 * Story: NCL-09-CN-009
 */
public interface InspectionCriterionCatalogService {

    Page<InspectionCriterionCatalogResponse> searchCriteria(
            String keyword, String status, Pageable pageable, CustomUserDetails currentUser);

    InspectionCriterionCatalogResponse getCriterion(Long id, CustomUserDetails currentUser);

    InspectionCriterionCatalogResponse createCriterion(
            InspectionCriterionCatalogRequest request, CustomUserDetails currentUser);

    InspectionCriterionCatalogResponse updateCriterion(
            Long id, InspectionCriterionCatalogRequest request, CustomUserDetails currentUser);

    void disableCriterion(Long id, CustomUserDetails currentUser);

    InspectionCriterionCatalogResponse enableCriterion(Long id, CustomUserDetails currentUser);

    void deleteCriterion(Long id, CustomUserDetails currentUser);
}
