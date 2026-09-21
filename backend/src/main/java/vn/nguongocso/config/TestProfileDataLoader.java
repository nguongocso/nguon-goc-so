package vn.nguongocso.config;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
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
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/**
 * Bộ nạp dữ liệu khởi tạo cho môi trường kiểm thử runtime (profile test).
 * <p>
 * Nạp người dùng quản lý HTX (VT-02), người ghi sự kiện (VT-03), các khóa thử nghiệm mẫu
 * và xuất token JWT ra file tạm để phục vụ kiểm thử runtime curl tự động.
 */
@Slf4j
@Component
@Profile("runtime-test")
@RequiredArgsConstructor
public class TestProfileDataLoader implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final PartnerApiKeyRepository partnerApiKeyRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductionLotRepository productionLotRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void run(String... args) throws Exception {
        log.info("==> [RUNTIME-TEST] Khởi tạo dữ liệu mẫu cho profile test...");

        // 1. Tạo tổ chức
        UUID orgId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Organization org = organizationRepository.findById(orgId).orElseGet(() -> {
            Organization o = new Organization();
            o.setOrganizationId(orgId);
            o.setName("Hợp tác xã Nông nghiệp Mẫu Song Hành");
            o.setCode("HTX-TEST-DEMO");
            o.setType(OrganizationType.COOPERATIVE);
            o.setStatus(OrganizationStatus.ACTIVE);
            return organizationRepository.save(o);
        });

        // 2. Tạo vai trò
        Role roleManager = roleRepository.findByCode("VT-02").orElseGet(() -> {
            Role r = new Role();
            r.setCode("VT-02");
            r.setName("Quản lý Hợp tác xã");
            return roleRepository.save(r);
        });

        Role roleRecorder = roleRepository.findByCode("VT-03").orElseGet(() -> {
            Role r = new Role();
            r.setCode("VT-03");
            r.setName("Người ghi sự kiện nông nghiệp");
            return roleRepository.save(r);
        });

        // 3. Tạo User Quản lý HTX (VT-02)
        User managerUser = userRepository.findByUserName("cooperative_manager").orElseGet(() -> {
            User u = new User();
            u.setUserName("cooperative_manager");
            u.setEmail("manager@nguongocso.vn");
            u.setFullName("Quản Lý Hợp Tác Xã Demo");
            u.setPasswordHash(passwordEncoder.encode("password123"));
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        OrganizationUser managerOrgUser = organizationUserRepository
                .findByUser_UserIdAndOrganization_OrganizationId(managerUser.getUserId(), org.getOrganizationId())
                .orElseGet(() -> {
                    OrganizationUser ou = new OrganizationUser();
                    ou.setId(UUID.randomUUID());
                    ou.setUser(managerUser);
                    ou.setOrganization(org);
                    ou.setRole(roleManager);
                    ou.setStatus(OrganizationUserStatus.ACTIVE);
                    ou.setJoinedAt(LocalDateTime.now());
                    return organizationUserRepository.save(ou);
                });

        // 4. Tạo User Người ghi sự kiện (VT-03)
        User recorderUser = userRepository.findByUserName("event_recorder").orElseGet(() -> {
            User u = new User();
            u.setUserName("event_recorder");
            u.setEmail("recorder@nguongocso.vn");
            u.setFullName("Người Ghi Sự Kiện Demo");
            u.setPasswordHash(passwordEncoder.encode("password123"));
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        OrganizationUser recorderOrgUser = organizationUserRepository
                .findByUser_UserIdAndOrganization_OrganizationId(recorderUser.getUserId(), org.getOrganizationId())
                .orElseGet(() -> {
                    OrganizationUser ou = new OrganizationUser();
                    ou.setId(UUID.randomUUID());
                    ou.setUser(recorderUser);
                    ou.setOrganization(org);
                    ou.setRole(roleRecorder);
                    ou.setStatus(OrganizationUserStatus.ACTIVE);
                    ou.setJoinedAt(LocalDateTime.now());
                    return organizationUserRepository.save(ou);
                });

        // 5. Sinh JWT Token cho từng vai trò
        CustomUserDetails managerDetails = new CustomUserDetails(managerUser, managerOrgUser, roleManager);
        String managerToken = jwtTokenProvider.generateAccessToken(managerDetails);

        CustomUserDetails recorderDetails = new CustomUserDetails(recorderUser, recorderOrgUser, roleRecorder);
        String recorderToken = jwtTokenProvider.generateAccessToken(recorderDetails);



        // 6. Tạo khóa thử nghiệm active và expired trong DB H2
        String activeRawKey = "nks_test_e8a1b2c3d4e5f678901234567890abcdef";
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

        String expiredRawKey = "nks_test_expired1234567890abcdef12345678";
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

        // 7. Tạo danh mục và Lô sản xuất thật
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
        lot = productionLotRepository.save(lot);

        // Ghi tokens và IDs ra file để script curl đọc
        try (FileWriter writer = new FileWriter("/tmp/test_tokens.env", StandardCharsets.UTF_8)) {
            writer.write("COOPERATIVE_MANAGER_TOKEN=" + managerToken + "\n");
            writer.write("EVENT_RECORDER_TOKEN=" + recorderToken + "\n");
            writer.write("SAMPLE_LOT_ID=00000000-0000-0000-0000-000000000001\n");
            writer.write("REAL_LOT_ID=" + lot.getId().toString() + "\n");
            writer.write("ACTIVE_TEST_KEY=" + activeRawKey + "\n");
            writer.write("EXPIRED_TEST_KEY=" + expiredRawKey + "\n");
        }

        log.info("==> [RUNTIME-TEST] Đã lưu thông tin kiểm thử vào /tmp/test_tokens.env (realLotId={})", lot.getId());
        log.info("==> [RUNTIME-TEST] Khởi tạo dữ liệu mẫu hoàn tất!");
    }
}
