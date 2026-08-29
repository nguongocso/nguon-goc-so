package vn.nguongocso.trace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.entity.CodeRange;

/**
 * Kiểm thử tầng repository của dải mã truy xuất ({@link CodeRange}).
 *
 * <p>
 * Tập trung vào
 * {@link CodeRangeRepository#findFirstReadOnlyByOrganizationOrganizationIdOrderByCreatedAtDesc(UUID)}
 * — biến thể KHÔNG khoá (không sinh {@code FOR UPDATE}) dành cho luồng chỉ
 * đọc. MySQL không cho phép thực thi {@code SELECT ... FOR UPDATE} trong
 * transaction READ ONLY (lỗi 1792 / SQLState 25006), khiến endpoint số lượng
 * mã truy xuất còn lại của "Tạo lô hàng mới" trả lỗi 500.
 * </p>
 *
 * <p>
 * Sử dụng {@code @SpringBootTest} + profile {@code test} + {@code @Transactional}
 * theo quy ước hạ tầng kiểm thử hiện có (H2, Flyway tắt, ddl-auto=create-drop).
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CodeRangeRepositoryTest {

    @Autowired
    private CodeRangeRepository codeRangeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void findFirstReadOnlyByOrganization_shouldReturnLatestRange_whenOrganizationHasMultipleRanges()
            throws InterruptedException {

        // Given — một tổ chức có 2 dải mã (bảng code_ranges chưa có UNIQUE trên organization_id)
        Organization organization = organizationRepository.save(Organization.builder()
                .name("HTX Kiểm thử dải mã")
                .code("TEST-ORG-" + UUID.randomUUID().toString().substring(0, 8))
                .type(OrganizationType.COOPERATIVE)
                .build());

        CodeRange oldRange = codeRangeRepository.save(CodeRange.builder()
                .organization(organization)
                .prefix("893001")
                .totalLimit(1000L)
                .usedCount(100L)
                .build());

        // @PrePersist luôn ghi đè createdAt bằng now(), nên chờ để đảm bảo createdAt của
        // dải mã sau lớn hơn dải mã trước
        Thread.sleep(20);

        CodeRange latestRange = codeRangeRepository.save(CodeRange.builder()
                .organization(organization)
                .prefix("893002")
                .totalLimit(2000L)
                .usedCount(50L)
                .build());

        // When
        CodeRange found = codeRangeRepository
                .findFirstReadOnlyByOrganizationOrganizationIdOrderByCreatedAtDesc(organization.getOrganizationId())
                .orElse(null);

        // Then — trả về đúng 1 dòng là dải mã MỚI NHẤT (không ném NonUniqueResultException)
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(latestRange.getId());
        assertThat(found.getPrefix()).isEqualTo("893002");
        assertThat(found.getCreatedAt()).isAfter(oldRange.getCreatedAt());
    }

    @Test
    void findFirstReadOnlyByOrganization_shouldReturnEmpty_whenOrganizationHasNoRange() {

        // Given
        Organization organization = organizationRepository.save(Organization.builder()
                .name("HTX chưa được cấp dải mã")
                .code("TEST-ORG-" + UUID.randomUUID().toString().substring(0, 8))
                .type(OrganizationType.COOPERATIVE)
                .build());

        // When & Then
        assertThat(codeRangeRepository
                .findFirstReadOnlyByOrganizationOrganizationIdOrderByCreatedAtDesc(organization.getOrganizationId()))
                .isEmpty();
    }
}
