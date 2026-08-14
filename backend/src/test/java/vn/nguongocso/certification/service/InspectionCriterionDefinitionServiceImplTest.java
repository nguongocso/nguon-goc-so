package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionResponse;
import vn.nguongocso.certification.entity.InspectionCriterionDefinition;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.InspectionCriterionDefinitionRepository;
import vn.nguongocso.certification.repository.StandardRepository;
import vn.nguongocso.certification.service.impl.InspectionCriterionDefinitionServiceImpl;

@ExtendWith(MockitoExtension.class)
class InspectionCriterionDefinitionServiceImplTest {

    @Mock
    private StandardRepository standardRepository;

    @Mock
    private InspectionCriterionDefinitionRepository inspectionCriterionDefinitionRepository;

    @InjectMocks
    private InspectionCriterionDefinitionServiceImpl service;

    private UUID standardId;
    private CustomUserDetails currentUser;
    private Standard standard;

    @BeforeEach
    void setUp() {
        standardId = UUID.randomUUID();
        standard = new Standard();
        standard.setId(standardId);
        standard.setName("VietGAP");

        currentUser = Mockito.mock(CustomUserDetails.class);
        when(currentUser.getRoleCode()).thenReturn("VT-01");
    }

    @Test
    void createCriteria_shouldCreateWhenAdminAndCodeUnique() {
        InspectionCriterionRequest request = new InspectionCriterionRequest();
        request.setStandardId(standardId);
        request.setCriterionCode("HEAVY_METAL");
        request.setCriterionName("Kim loại nặng");
        request.setNote("Tiêu chí bắt buộc kiểm tra theo quy định");

        when(standardRepository.findById(standardId)).thenReturn(Optional.of(standard));
        when(inspectionCriterionDefinitionRepository.existsByStandard_IdAndCodeIgnoreCase(standardId, "HEAVY_METAL"))
                .thenReturn(false);
        when(inspectionCriterionDefinitionRepository.save(any(InspectionCriterionDefinition.class)))
                .thenAnswer(invocation -> {
                    InspectionCriterionDefinition entity = invocation.getArgument(0);
                    entity.setId(101);
                    entity.setNote(request.getNote());
                    return entity;
                });

        InspectionCriterionResponse response = service.createCriteria(standardId, request, currentUser);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("HEAVY_METAL");
        assertThat(response.getName()).isEqualTo("Kim loại nặng");
        assertThat(response.getStandardId()).isEqualTo(standardId);
        assertThat(response.getNote()).isEqualTo("Tiêu chí bắt buộc kiểm tra theo quy định");
    }
}
