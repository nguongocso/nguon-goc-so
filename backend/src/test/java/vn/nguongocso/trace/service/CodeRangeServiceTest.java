package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.dto.request.CreateCodeRangeRequest;
import vn.nguongocso.trace.dto.response.CodeRangeResponse;
import vn.nguongocso.trace.dto.response.CodeRangeStatusResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.repository.CodeRangeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class CodeRangeServiceTest  {

    @Mock
    private CodeRangeRepository codeRangeRepository;

    @Mock
    OrganizationRepository organizationRepository;

    @InjectMocks
    private CodeRangeService codeRangeService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @Test
    void createCodeRange_shouldSuccess_whenValid() {

        // Given
        Organization org = new Organization();
        org.setOrganizationId(orgId);
        org.setName("HTX Xanh");

        CreateCodeRangeRequest request = new CreateCodeRangeRequest();
        request.setOrganizationId(orgId);
        request.setPrefix("893001");
        request.setTotalLimit(1000L);

        CustomUserDetails admin = mock(CustomUserDetails.class);
        when(admin.getRoleCode()).thenReturn("VT-01");
        when(admin.getUserId()).thenReturn(adminId);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(codeRangeRepository.findByPrefix("893001")).thenReturn(Optional.empty());
        when(codeRangeRepository.save(any(CodeRange.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CodeRangeResponse response = codeRangeService.createCodeRange(request, admin);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPrefix()).isEqualTo("893001");
        assertThat(response.getTotalLimit()).isEqualTo(1000L);
        assertThat(response.getUsedCount()).isEqualTo(0L);
        verify(codeRangeRepository).save(any(CodeRange.class));
    }

    @Test
    void createCodeRange_shouldThrow_whenPrefixExists() {

        // Given
        CreateCodeRangeRequest request = new CreateCodeRangeRequest();
        request.setOrganizationId(orgId);
        request.setPrefix("893001");
        request.setTotalLimit(1000L);

        CustomUserDetails admin = mock(CustomUserDetails.class);
        when(admin.getRoleCode()).thenReturn("VT-01");

        Organization org = new Organization();
        org.setOrganizationId(orgId);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(codeRangeRepository.findByPrefix("893001")).thenReturn(Optional.of(new CodeRange()));

        // When & Then
        assertThatThrownBy(() -> codeRangeService.createCodeRange(request, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tiền tố mã đã tồn tại");
    }

    @Test
    void createCodeRange_shouldThrow_whenNotAdmin() {

        // Given
        CreateCodeRangeRequest request = new CreateCodeRangeRequest();
        request.setOrganizationId(orgId);
        request.setPrefix("893001");
        request.setTotalLimit(1000L);

        CustomUserDetails admin = mock(CustomUserDetails.class);
        when(admin.getRoleCode()).thenReturn("VT-02");

        // When & Then
        assertThatThrownBy(() -> codeRangeService.createCodeRange(request, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ quản trị viên nền tảng mới có quyền cấp dải mã");
    }

    @Test
    void getCodeRangeStatus_shouldReturnCorrectStatus() {

        // Given
        Organization org = new Organization();
        org.setOrganizationId(UUID.randomUUID());
        org.setName("HTX Xanh");

        CodeRange range = CodeRange.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .prefix("893001")
                .totalLimit(100L)
                .usedCount(85L)
                .build();

        when(codeRangeRepository.findAll()).thenReturn(List.of(range));

        // When
        List<CodeRangeStatusResponse> responses = codeRangeService.getCodeRangeStatus();

        assertThat(responses).hasSize(1);
        CodeRangeStatusResponse response = responses.get(0);
        assertThat(response.getStatus()).isEqualTo("NEARLY_EXHAUSTED");
        assertThat(response.getUsagePercent()).isEqualTo(85.0);
        assertThat(response.getTotalLimit()).isEqualTo(100L);
        assertThat(response.getUsedCount()).isEqualTo(85L);
        assertThat(response.getOrganizationName()).isEqualTo("HTX Xanh");
    }

    @Test
    void getCodeRangeStatus_shouldReturnExhausted_whenUsedCountEqualsLimit() {

        // Given
        Organization org = new Organization();
        org.setOrganizationId(UUID.randomUUID());
        org.setName("HTX Xanh");

        CodeRange range = CodeRange.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .prefix("893001")
                .totalLimit(100L)
                .usedCount(100L)
                .build();

        when(codeRangeRepository.findAll()).thenReturn(List.of(range));

        // When
        List<CodeRangeStatusResponse> responses = codeRangeService.getCodeRangeStatus();

        // Then
        assertThat(responses.get(0).getStatus()).isEqualTo("EXHAUSTED");
        assertThat(responses.get(0).getUsagePercent()).isEqualTo(100.0);
    }

    @Test
    void getCodeRangeStatus_shouldReturnOk_whenUsedCountBelow80Percent() {

        // Given
        Organization org = new Organization();
        org.setOrganizationId(UUID.randomUUID());
        org.setName("HTX Xanh");

        CodeRange range = CodeRange.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .prefix("893001")
                .totalLimit(100L)
                .usedCount(70L)
                .build();

        when(codeRangeRepository.findAll()).thenReturn(List.of(range));

        // When
        List<CodeRangeStatusResponse> responses = codeRangeService.getCodeRangeStatus();

        // Then
        assertThat(responses.get(0).getStatus()).isEqualTo("OK");
        assertThat(responses.get(0).getUsagePercent()).isEqualTo(70.0);
    }
}
