package vn.nguongocso.certification.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionResponse;

public interface InspectionCriterionDefinitionService {

    List<InspectionCriterionResponse> getCriteriaByStandard(UUID standardId, CustomUserDetails currentUser);

    InspectionCriterionResponse createCriteria(UUID standardId, InspectionCriterionRequest request, CustomUserDetails currentUser);

    InspectionCriterionResponse updateCriteria(UUID standardId, Integer criteriaId, InspectionCriterionRequest request, CustomUserDetails currentUser);

    void deleteCriteria(UUID standardId, Integer criteriaId, CustomUserDetails currentUser);
}
