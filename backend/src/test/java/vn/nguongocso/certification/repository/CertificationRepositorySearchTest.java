package vn.nguongocso.certification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationType;

/**
 * Kiểm tra truy vấn tìm kiếm + phân trang của CertificationRepository (H2).
 */
@DataJpaTest
@ActiveProfiles("test")
class CertificationRepositorySearchTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CertificationRepository certificationRepository;

    private Organization orgA;
    private UUID orgBId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        Organization orgB = em.persist(Organization.builder()
                .name("HTX B")
                .code("HTXB-" + UUID.randomUUID().toString().substring(0, 8))
                .type(OrganizationType.COOPERATIVE)
                .build());
        orgBId = orgB.getOrganizationId();

        orgA = em.persist(Organization.builder()
                .name("HTX A")
                .code("HTXA-" + UUID.randomUUID().toString().substring(0, 8))
                .type(OrganizationType.COOPERATIVE)
                .build());

        Standard standard = em.persist(Standard.builder()
                .name("STD-" + UUID.randomUUID().toString().substring(0, 8))
                .build());

        em.persist(cert(orgA, standard, "GlobalGAP Buoi", "GG-BUOI-01", "Cuc Trong trot", today.plusDays(90)));
        em.persist(cert(orgA, standard, "VietGAP Cam", "VG-CAM-01", "Cuc An toan thuc pham", today.plusDays(10)));
        em.persist(cert(orgA, standard, "OCOP Mit", "OCOP-MIT-01", "UBND tinh", today.minusDays(5)));
        em.persist(cert(orgB, standard, "GlobalGAP Xoai", "GG-XOAI-01", "Cuc Trong trot", today.plusDays(30)));

        em.flush();
    }

    private Certification cert(Organization org, Standard standard, String name, String code,
            String issuedBy, LocalDate expiryDate) {
        return Certification.builder()
                .organization(org)
                .standard(standard)
                .name(name)
                .code(code)
                .issuedBy(issuedBy)
                .issueDate(expiryDate.minusYears(1))
                .expiryDate(expiryDate)
                .build();
    }

    @Test
    void search_withoutFilter_paginatesAndCountsTotalOfOrganization() {
        Page<Certification> page = certificationRepository.search(
                orgA.getOrganizationId(), null, null, today, today.plusDays(30), PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getNumber()).isZero();
    }

    @Test
    void search_byKeywordMatchesNameCaseInsensitive() {
        Page<Certification> page = certificationRepository.search(
                orgA.getOrganizationId(), "cam", null, today, today.plusDays(30), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCode()).isEqualTo("VG-CAM-01");
    }

    @Test
    void search_byKeywordMatchesCode() {
        Page<Certification> page = certificationRepository.search(
                orgA.getOrganizationId(), "gg-buoi", null, today, today.plusDays(30), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("GlobalGAP Buoi");
    }

    @Test
    void search_byKeywordMatchesIssuedBy() {
        Page<Certification> page = certificationRepository.search(
                orgA.getOrganizationId(), "ubnd", null, today, today.plusDays(30), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCode()).isEqualTo("OCOP-MIT-01");
    }

    @Test
    void search_statusValid_excludesExpiringAndExpired() {
        Page<Certification> page = certificationRepository.search(
                orgA.getOrganizationId(), null, "valid", today, today.plusDays(30), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
                .extracting(Certification::getCode)
                .containsExactly("GG-BUOI-01");
    }

    @Test
    void search_statusExpiring_returnsOnlyWithinThreshold() {
        Page<Certification> page = certificationRepository.search(
                orgA.getOrganizationId(), null, "expiring", today, today.plusDays(30), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCode()).isEqualTo("VG-CAM-01");
    }

    @Test
    void search_statusExpired_returnsOnlyExpired() {
        Page<Certification> page = certificationRepository.search(
                orgA.getOrganizationId(), null, "expired", today, today.plusDays(30), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCode()).isEqualTo("OCOP-MIT-01");
    }

    @Test
    void search_isolatedByOrganization() {
        Page<Certification> pageOrgB = certificationRepository.search(
                orgBId, null, null, today, today.plusDays(30), PageRequest.of(0, 10));
        assertThat(pageOrgB.getTotalElements()).isEqualTo(1);
        assertThat(pageOrgB.getContent().get(0).getCode()).isEqualTo("GG-XOAI-01");

        Page<Certification> pageOrgAKeywordXoai = certificationRepository.search(
                orgA.getOrganizationId(), "xoai", null, today, today.plusDays(30), PageRequest.of(0, 10));
        assertThat(pageOrgAKeywordXoai.getTotalElements()).isZero();
    }

    @Test
    void search_sortsByProvidedPageableSort() {
        Page<Certification> page = certificationRepository.search(
                orgA.getOrganizationId(), null, null, today, today.plusDays(30),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "expiryDate")));

        assertThat(page.getContent())
                .extracting(Certification::getCode)
                .containsExactly("OCOP-MIT-01", "VG-CAM-01", "GG-BUOI-01");
    }
}
