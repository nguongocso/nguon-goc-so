package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.response.CertificationResponse;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.certification.service.impl.CertificationServiceImpl;
import vn.nguongocso.common.PageResponse;

/**
 * Kiểm tra logic tìm kiếm chứng nhận: whitelist sắp xếp, chặn size,
 * chuẩn hoá từ khoá/trạng thái và mapping PageResponse.
 */
@ExtendWith(MockitoExtension.class)
class CertificationSearchServiceTest {

    @Mock
    private CertificationRepository certificationRepository;

    @InjectMocks
    private CertificationServiceImpl certificationService;

    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(certificationService, "warningThresholdDays", 30);
    }

    private CustomUserDetails currentUser() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getOrganizationId()).thenReturn(orgId);
        return userDetails;
    }

    private Certification sampleCert(String name, String code, LocalDate expiryDate) {
        return Certification.builder()
                .id(UUID.randomUUID())
                .name(name)
                .code(code)
                .issuedBy("Cuc Trong trot")
                .issueDate(expiryDate.minusYears(1))
                .expiryDate(expiryDate)
                .build();
    }

    @Test
    void search_normalizesParamsAndAppliesWhitelistAndClampsSize() {
        when(certificationRepository.search(eq(orgId), isNull(), isNull(), any(LocalDate.class),
                any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        certificationService.searchCertifications("   ", "khong-hop-le", "truong-la", "sideways",
                -5, 500, currentUser());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(certificationRepository).search(eq(orgId), isNull(), isNull(),
                any(LocalDate.class), any(LocalDate.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "issueDate"));
    }

    @Test
    void search_trimsKeywordLowercasesStatusAndSortsAscendingByName() {
        when(certificationRepository.search(eq(orgId), eq("buoi"), eq("valid"), any(LocalDate.class),
                any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        certificationService.searchCertifications("  buoi  ", "VALID", "name", "asc",
                0, 10, currentUser());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(certificationRepository).search(eq(orgId), eq("buoi"), eq("valid"),
                any(LocalDate.class), any(LocalDate.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Test
    void search_mapsPageResponseWithTotalElements() {
        Certification cert = sampleCert("GlobalGAP Buoi", "GG-BUOI-01", LocalDate.now().plusDays(90));
        when(certificationRepository.search(eq(orgId), isNull(), eq("valid"), any(LocalDate.class),
                any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cert), PageRequest.of(0, 10), 12));

        PageResponse<CertificationResponse> response = certificationService.searchCertifications(
                null, "valid", null, null, 0, 10, currentUser());

        assertThat(response.getTotalElements()).isEqualTo(12);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getItems()).hasSize(1);

        CertificationResponse item = response.getItems().get(0);
        assertThat(item.getId()).isEqualTo(cert.getId());
        assertThat(item.getName()).isEqualTo("GlobalGAP Buoi");
        assertThat(item.getCode()).isEqualTo("GG-BUOI-01");
        assertThat(item.getIsValid()).isTrue();
    }
}
