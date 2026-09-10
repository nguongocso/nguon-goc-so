package vn.nguongocso.trace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;

/**
 * Kiểm thử tích hợp tầng dữ liệu NCL-782 cho quan hệ lô cha - lô con.
 *
 * <p>Tập trung vào khả năng lưu lineage, truy vấn lô con, khóa lô nguồn và
 * cách ly lô con theo tổ chức nhận. Migration sản xuất được kiểm tra riêng
 * trên MySQL vì profile test dùng Hibernate tạo schema H2.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShipmentSplitRelationshipRepositoryTest {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductionLotRepository productionLotRepository;

    @Autowired
    private UserRepository userRepository;

    private Organization sourceOrganization;
    private Organization firstRecipient;
    private Organization secondRecipient;
    private ProductionLot productionLot;
    private User splitBy;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        sourceOrganization = saveOrganization(
                "HTX nguồn " + suffix,
                "SOURCE-" + suffix,
                OrganizationType.COOPERATIVE);
        firstRecipient = saveOrganization(
                "Doanh nghiệp nhận A " + suffix,
                "RECIPIENT-A-" + suffix,
                OrganizationType.ENTERPRISE);
        secondRecipient = saveOrganization(
                "Doanh nghiệp nhận B " + suffix,
                "RECIPIENT-B-" + suffix,
                OrganizationType.ENTERPRISE);

        ProductCategory category = productCategoryRepository.save(ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Nông sản NCL-782 " + suffix)
                .group("Nông sản")
                .isActive(true)
                .build());

        productionLot = productionLotRepository.save(ProductionLot.builder()
                .organization(sourceOrganization)
                .productCategory(category)
                .name("Lô sản xuất NCL-782 " + suffix)
                .expectedQuantity(1000D)
                .expectedQuantityUnit("tem")
                .status(ProductionLotStatus.CLOSED)
                .build());

        splitBy = userRepository.save(User.builder()
                .userName("split-ncl782-" + suffix)
                .passwordHash("{noop}mat-khau")
                .fullName("Quản lý HTX NCL-782")
                .email("split-ncl782-" + suffix + "@test.local")
                .status(UserStatus.ACTIVE)
                .build());
    }

    @Test
    void saveAndFindChildren_shouldPreserveSplitLineageAndAuditFields() throws InterruptedException {
        LocalDateTime splitAt = LocalDateTime.of(2026, 9, 10, 10, 30);
        Shipment parent = shipmentRepository.saveAndFlush(
                shipment("Lô cha", 1000L, ShipmentStatus.SPLIT, null, null, splitAt));

        Shipment firstChild = shipmentRepository.saveAndFlush(
                shipment("Lô con A", 400L, ShipmentStatus.CODE_PRINTED,
                        parent, firstRecipient, splitAt));
        Thread.sleep(20);
        Shipment secondChild = shipmentRepository.saveAndFlush(
                shipment("Lô con B", 600L, ShipmentStatus.CODE_PRINTED,
                        parent, secondRecipient, splitAt));

        List<Shipment> children = shipmentRepository
                .findAllByParentShipment_IdOrderByCreatedAtAsc(parent.getId());

        assertThat(children).extracting(Shipment::getId)
                .containsExactly(firstChild.getId(), secondChild.getId());
        assertThat(children).allSatisfy(child -> {
            assertThat(child.getParentShipment().getId()).isEqualTo(parent.getId());
            assertThat(child.getOrganization().getOrganizationId())
                    .isEqualTo(sourceOrganization.getOrganizationId());
            assertThat(child.getProductionLot().getId()).isEqualTo(productionLot.getId());
            assertThat(child.getSplitAt()).isEqualTo(splitAt);
            assertThat(child.getSplitBy().getUserId()).isEqualTo(splitBy.getUserId());
        });
        assertThat(children).extracting(child -> child.getRecipientOrganization().getOrganizationId())
                .containsExactly(firstRecipient.getOrganizationId(), secondRecipient.getOrganizationId());
        assertThat(children).extracting(Shipment::getTotalQuantity)
                .containsExactly(400L, 600L);
        assertThat(shipmentRepository.existsByParentShipment_Id(parent.getId())).isTrue();
    }

    @Test
    void findOwnedByIdForSplitUpdate_shouldRespectSourceOrganization() {
        Shipment parent = shipmentRepository.saveAndFlush(
                shipment("Lô nguồn cần khóa", 1000L, ShipmentStatus.CODE_PRINTED,
                        null, null, null));

        assertThat(shipmentRepository.findOwnedByIdForSplitUpdate(
                parent.getId(), sourceOrganization.getOrganizationId()))
                .contains(parent);
        assertThat(shipmentRepository.findOwnedByIdForSplitUpdate(
                parent.getId(), firstRecipient.getOrganizationId()))
                .isEmpty();
    }

    @Test
    void findByIdAndRecipientOrganization_shouldPreventCrossTenantLookup() {
        LocalDateTime splitAt = LocalDateTime.of(2026, 9, 10, 10, 30);
        Shipment parent = shipmentRepository.saveAndFlush(
                shipment("Lô cha", 1000L, ShipmentStatus.SPLIT, null, null, splitAt));
        Shipment child = shipmentRepository.saveAndFlush(
                shipment("Lô con A", 1000L, ShipmentStatus.CODE_PRINTED,
                        parent, firstRecipient, splitAt));

        assertThat(shipmentRepository.findByIdAndRecipientOrganization_OrganizationId(
                child.getId(), firstRecipient.getOrganizationId()))
                .contains(child);
        assertThat(shipmentRepository.findByIdAndRecipientOrganization_OrganizationId(
                child.getId(), secondRecipient.getOrganizationId()))
                .isEmpty();
    }

    private Organization saveOrganization(String name, String code, OrganizationType type) {
        return organizationRepository.save(Organization.builder()
                .name(name)
                .code(code)
                .type(type)
                .status(OrganizationStatus.ACTIVE)
                .build());
    }

    private Shipment shipment(
            String name,
            long totalQuantity,
            ShipmentStatus status,
            Shipment parent,
            Organization recipient,
            LocalDateTime splitAt) {
        Shipment shipment = new Shipment();
        shipment.setOrganization(sourceOrganization);
        shipment.setProductionLot(productionLot);
        shipment.setName(name);
        shipment.setTotalQuantity(totalQuantity);
        shipment.setStatus(status);
        shipment.setParentShipment(parent);
        shipment.setRecipientOrganization(recipient);
        shipment.setSplitAt(splitAt);
        shipment.setSplitBy(splitAt == null ? null : splitBy);
        return shipment;
    }
}
