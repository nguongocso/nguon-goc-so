package vn.nguongocso.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.repository.OrganizationRepository;

/** Thành phần khởi tạo tổ chức, khóa API và lô sản xuất mẫu cho môi trường kiểm thử runtime. */
@Slf4j
@Component
@Profile("runtime-test")
@RequiredArgsConstructor
public class TestOrganizationBootstrap {
    public static final UUID TEST_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final OrganizationRepository organizationRepository;
    private final PartnerApiKeyRepository partnerApiKeyRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductionLotRepository productionLotRepository;

    /** Khởi tạo tổ chức hợp tác xã mẫu. */
    public Organization bootstrapOrganization() {
        return organizationRepository.findById(TEST_ORG_ID).orElseGet(() -> {
            Organization o = new Organization();
            o.setOrganizationId(TEST_ORG_ID);
            o.setName("Hợp tác xã Nông nghiệp Mẫu Song Hành");
            o.setCode("HTX-TEST-DEMO");
            o.setType(OrganizationType.COOPERATIVE);
            o.setStatus(OrganizationStatus.ACTIVE);
            return organizationRepository.save(o);
        });
    }

    /** Khởi tạo các khóa API đối tác mẫu. */
    public void bootstrapPartnerApiKeys(
            Organization org,
            User managerUser,
            String activeRawKey,
            String expiredRawKey) {
        String activeHash = PartnerApiKeyService.hashSha256(activeRawKey);
        if (partnerApiKeyRepository.findByKeyHash(activeHash).isEmpty()) {
            partnerApiKeyRepository.save(PartnerApiKey.builder()
                    .organization(org)
                    .partnerName("Doanh Nghiệp Thu Mua Mẫu")
                    .keyHash(activeHash)
                    .keyPrefix("nks_test_e8a1")
                    .rateLimitPerHour(100)
                    .expiresAt(LocalDateTime.now().plusDays(15))
                    .status(PartnerApiKeyStatus.ACTIVE)
                    .isTest(true)
                    .createdBy(managerUser)
                    .build());
        }

        String expiredHash = PartnerApiKeyService.hashSha256(expiredRawKey);
        if (partnerApiKeyRepository.findByKeyHash(expiredHash).isEmpty()) {
            partnerApiKeyRepository.save(PartnerApiKey.builder()
                    .organization(org)
                    .partnerName("Doanh Nghiệp Thu Mua Khóa Hết Hạn")
                    .keyHash(expiredHash)
                    .keyPrefix("nks_test_expi")
                    .rateLimitPerHour(60)
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .status(PartnerApiKeyStatus.EXPIRED)
                    .isTest(true)
                    .createdBy(managerUser)
                    .build());
        }
    }

    /** Khởi tạo danh mục sản phẩm và lô sản xuất mẫu. */
    public ProductionLot bootstrapProductLot(Organization org) {
        ProductCategory category = productCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            ProductCategory c = new ProductCategory();
            c.setId(UUID.randomUUID());
            c.setName("Xoài cát Chu");
            c.setGroup("Trái cây");
            c.setIsActive(true);
            return productCategoryRepository.save(c);
        });

        ProductionLot lot = new ProductionLot();
        lot.setName("Lô Xoài Thực Tế Tại Vườn Hợp Tác Xã");
        lot.setOrganization(org);
        lot.setProductCategory(category);
        lot.setExpectedQuantity(5000.0);
        lot.setStatus(ProductionLotStatus.APPROVED);
        lot.setPlantingDate(LocalDate.now().minusMonths(6));
        lot.setHarvestDate(LocalDate.now().plusMonths(1));
        return productionLotRepository.save(lot);
    }
}
