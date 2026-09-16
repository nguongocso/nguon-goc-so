package vn.nguongocso.publicapi;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import vn.nguongocso.integration.apikey.dto.request.CreateTestApiKeyRequest;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;
import vn.nguongocso.integration.partner.util.PartnerSampleDataProvider;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/**
 * Kiểm thử tích hợp toàn diện cho User Story NCL-12-CN-004:
 * "Trang tài liệu cổng dữ liệu và khóa thử nghiệm".
 * <p>
 * Bao gồm các ca kiểm thử:
 * <ul>
 *     <li>TC-01: Gọi thử bằng khóa thử nghiệm -> nhận đúng dữ liệu mẫu như trong tài liệu.</li>
 *     <li>TC-02: Dùng khóa thử nghiệm gọi lấy lô thật -> chỉ nhận dữ liệu mẫu, đánh dấu is_test=true.</li>
 *     <li>TC-03: Khóa thử nghiệm hết hạn -> bị từ chối 401 với thông báo hết hạn rõ ràng.</li>
 *     <li>TC-04: Vai trò Người ghi sự kiện (VT-03) yêu cầu cấp khóa thử nghiệm -> bị từ chối 403 Forbidden.</li>
 *     <li>Happy Path: Vai trò Quản lý HTX (VT-02) cấp khóa thử nghiệm thành công -> nhận 201 Created và rawApiKey (tiền tố nks_test_).</li>
 *     <li>Rate Limit: Vượt quá hạn mức số lượt gọi trong 1 giờ -> bị từ chối 429 Too Many Requests (QTN-20).</li>
 *     <li>Revoke: Khóa thử nghiệm đã bị thu hồi -> bị từ chối 401 Unauthorized.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TestApiKeyIntegrationTest {

    private static final String ACTIVE_TEST_RAW_KEY = "nks_test_e8a1b2c3d4e5f678901234567890abcdef";
    private static final String EXPIRED_TEST_RAW_KEY = "nks_test_expired1234567890abcdef12345678";
    private static final String REVOKED_TEST_RAW_KEY = "nks_test_revoked1234567890abcdef12345678";
    private static final String RATE_LIMITED_TEST_RAW_KEY = "nks_test_ratelimit1234567890abcdef123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartnerApiKeyRepository partnerApiKeyRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationUserRepository organizationUserRepository;

    @Autowired
    private ProductionLotRepository productionLotRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    private Organization testOrg;
    private ProductionLot realProductionLot;
    private User testManagerUser;
    private User testRecorderUser;
    private Role managerRole;
    private Role recorderRole;

    @BeforeEach
    void setUp() {
        // Dọn dẹp context bảo mật trước mỗi ca kiểm thử
        SecurityContextHolder.clearContext();

        // 1. Tạo tổ chức mẫu
        testOrg = organizationRepository.save(Organization.builder()
                .name("Hợp tác xã Nông nghiệp Thử Nghiệm")
                .code("HTX-TEST-" + UUID.randomUUID().toString().substring(0, 6))
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Tạo ngành hàng và lô sản xuất thật
        ProductCategory category = productCategoryRepository.save(ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Xoài cát")
                .group("Trái cây")
                .isActive(true)
                .build());

        realProductionLot = productionLotRepository.save(ProductionLot.builder()
                .name("Lô Xoài Thực Tế Tại Vườn Số 9")
                .organization(testOrg)
                .productCategory(category)
                .expectedQuantity(5000.0)
                .status(ProductionLotStatus.APPROVED)
                .build());

        // 3. Tạo các vai trò VT-02 (Quản lý) và VT-03 (Người ghi sự kiện) nếu chưa có
        managerRole = roleRepository.findByCode("VT-02")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setCode("VT-02");
                    r.setName("Quản lý Hợp tác xã");
                    return roleRepository.save(r);
                });

        recorderRole = roleRepository.findByCode("VT-03")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setCode("VT-03");
                    r.setName("Người ghi sự kiện nông nghiệp");
                    return roleRepository.save(r);
                });

        // 4. Tạo người dùng Quản lý HTX
        testManagerUser = userRepository.save(User.builder()
                .userName("manager_test_" + UUID.randomUUID().toString().substring(0, 6))
                .email("manager_" + UUID.randomUUID().toString().substring(0, 6) + "@nguongocso.vn")
                .fullName("Nguyễn Văn Quản Lý")
                .passwordHash("hashed_password")
                .status(UserStatus.ACTIVE)
                .build());

        OrganizationUser managerOrgUser = new OrganizationUser();
        managerOrgUser.setUser(testManagerUser);
        managerOrgUser.setOrganization(testOrg);
        managerOrgUser.setRole(managerRole);
        organizationUserRepository.save(managerOrgUser);

        // 5. Tạo người dùng Người ghi sự kiện
        testRecorderUser = userRepository.save(User.builder()
                .userName("recorder_test_" + UUID.randomUUID().toString().substring(0, 6))
                .email("recorder_" + UUID.randomUUID().toString().substring(0, 6) + "@nguongocso.vn")
                .fullName("Trần Thị Ghi Sự Kiện")
                .passwordHash("hashed_password")
                .status(UserStatus.ACTIVE)
                .build());

        OrganizationUser recorderOrgUser = new OrganizationUser();
        recorderOrgUser.setUser(testRecorderUser);
        recorderOrgUser.setOrganization(testOrg);
        recorderOrgUser.setRole(recorderRole);
        organizationUserRepository.save(recorderOrgUser);

        // 6. Tạo Khóa Thử Nghiệm Đang Hoạt Động (ACTIVE)
        partnerApiKeyRepository.save(PartnerApiKey.builder()
                .organization(testOrg)
                .partnerName("Doanh Nghiệp Thu Mua Test Key")
                .keyHash(PartnerApiKeyService.hashSha256(ACTIVE_TEST_RAW_KEY))
                .keyPrefix("nks_test_e8a1")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().plusDays(15))
                .status(PartnerApiKeyStatus.ACTIVE)
                .isTest(true)
                .createdBy(testManagerUser)
                .build());

        // 7. Tạo Khóa Thử Nghiệm Đã Hết Hạn (EXPIRED)
        partnerApiKeyRepository.save(PartnerApiKey.builder()
                .organization(testOrg)
                .partnerName("Doanh Nghiệp Thu Mua Expired Key")
                .keyHash(PartnerApiKeyService.hashSha256(EXPIRED_TEST_RAW_KEY))
                .keyPrefix("nks_test_expi")
                .rateLimitPerHour(60)
                .expiresAt(LocalDateTime.now().minusDays(2))
                .status(PartnerApiKeyStatus.EXPIRED)
                .isTest(true)
                .createdBy(testManagerUser)
                .build());

        // 8. Tạo Khóa Thử Nghiệm Bị Thu Hồi (REVOKED)
        partnerApiKeyRepository.save(PartnerApiKey.builder()
                .organization(testOrg)
                .partnerName("Doanh Nghiệp Thu Mua Revoked Key")
                .keyHash(PartnerApiKeyService.hashSha256(REVOKED_TEST_RAW_KEY))
                .keyPrefix("nks_test_revo")
                .rateLimitPerHour(60)
                .expiresAt(LocalDateTime.now().plusDays(10))
                .status(PartnerApiKeyStatus.REVOKED)
                .isTest(true)
                .createdBy(testManagerUser)
                .build());

        // 9. Tạo Khóa Thử Nghiệm Hạn Mức Thấp (1 lượt/giờ) để kiểm thử Rate Limit
        partnerApiKeyRepository.save(PartnerApiKey.builder()
                .organization(testOrg)
                .partnerName("Doanh Nghiệp Thu Mua Rate Limited Key")
                .keyHash(PartnerApiKeyService.hashSha256(RATE_LIMITED_TEST_RAW_KEY))
                .keyPrefix("nks_test_rate")
                .rateLimitPerHour(1)
                .expiresAt(LocalDateTime.now().plusDays(10))
                .status(PartnerApiKeyStatus.ACTIVE)
                .isTest(true)
                .createdBy(testManagerUser)
                .build());
    }

    private void authenticateAs(User user, Role role) {
        OrganizationUser membership = organizationUserRepository
                .findByUser_UserIdAndOrganization_OrganizationId(user.getUserId(), testOrg.getOrganizationId())
                .orElseThrow();

        CustomUserDetails details = new CustomUserDetails(user, membership, role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                details, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.getCode())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    /**
     * TC-01: Đối tác gọi thử bằng khóa thử nghiệm -> nhận đúng dữ liệu mẫu như trong tài liệu.
     */
    @Test
    @DisplayName("TC-01: Đối tác gọi endpoint bằng khóa thử nghiệm -> Nhận đúng dữ liệu mẫu Sandbox và is_test=true")
    void testTC01_CallEndpointWithTestApiKey_ReturnsSampleData() throws Exception {
        // 1. Gọi qua đường dẫn /api/publicapi/v1/lots/SAMPLE_LOT_ID với header X-API-KEY
        mockMvc.perform(get("/api/publicapi/v1/lots/SAMPLE_LOT_ID")
                        .header("X-API-KEY", ACTIVE_TEST_RAW_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_test").value(true))
                .andExpect(jsonPath("$.data.lotInfo.lotName").value(PartnerSampleDataProvider.getSampleLotDossier().getLotInfo().getLotName()))
                .andExpect(jsonPath("$.data.organizationInfo.organizationName").value(PartnerSampleDataProvider.getSampleLotDossier().getOrganizationInfo().getOrganizationName()))
                .andExpect(jsonPath("$.data.testNotice").value(PartnerSampleDataProvider.TEST_NOTICE));

        // 2. Kiểm tra tương thích với header X-Api-Key (viết hoa thường linh hoạt)
        mockMvc.perform(get("/api/publicapi/v1/lots/sample-lot-001")
                        .header("X-Api-Key", ACTIVE_TEST_RAW_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_test").value(true));
    }

    /**
     * TC-02: Khóa thử nghiệm -> gọi lấy lô thật -> chỉ nhận dữ liệu mẫu, đánh dấu is_test=true.
     */
    @Test
    @DisplayName("TC-02: Khóa thử nghiệm gọi lấy lô thật -> Chỉ nhận dữ liệu mẫu Sandbox, đánh dấu is_test=true")
    void testTC02_CallRealLotWithTestApiKey_ReturnsSampleDataOnly() throws Exception {
        UUID realLotId = realProductionLot.getId();

        // Gọi lấy thông tin lô thật qua cổng publicapi với khóa thử nghiệm
        mockMvc.perform(get("/api/publicapi/v1/lots/" + realLotId)
                        .header("X-API-KEY", ACTIVE_TEST_RAW_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_test").value(true))
                // Dữ liệu trả về PHẢI là dữ liệu mẫu Sandbox, KHÔNG PHẢI dữ liệu thật của realProductionLot
                .andExpect(jsonPath("$.data.lotInfo.lotName").value(PartnerSampleDataProvider.getSampleLotDossier().getLotInfo().getLotName()))
                .andExpect(jsonPath("$.data.testNotice").value(PartnerSampleDataProvider.TEST_NOTICE));

        // Kiểm tra tương tự trên endpoint đối tác /api/v1/partner/production-lots/{lotId}/dossier
        mockMvc.perform(get("/api/v1/partner/production-lots/" + realLotId + "/dossier")
                        .header("X-API-KEY", ACTIVE_TEST_RAW_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_test").value(true))
                .andExpect(jsonPath("$.data.lotInfo.lotName").value(PartnerSampleDataProvider.getSampleLotDossier().getLotInfo().getLotName()));
    }

    /**
     * TC-03: Khóa hết hạn -> gọi -> bị từ chối với thông báo hết hạn.
     */
    @Test
    @DisplayName("TC-03: Khóa thử nghiệm hết hạn -> Bị từ chối HTTP 401 với thông báo hết hạn")
    void testTC03_ExpiredTestApiKey_RejectedWith401() throws Exception {
        mockMvc.perform(get("/api/publicapi/v1/lots/SAMPLE_LOT_ID")
                        .header("X-API-KEY", EXPIRED_TEST_RAW_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("hết hạn")));
    }

    /**
     * TC-04: Vai trò Người ghi sự kiện -> cấp khóa thử nghiệm -> bị từ chối 403.
     */
    @Test
    @DisplayName("TC-04: Vai trò Người ghi sự kiện (VT-03) cấp khóa thử nghiệm -> Bị từ chối HTTP 403 Forbidden")
    void testTC04_EventRecorderRole_CannotCreateTestApiKey_Forbidden() throws Exception {
        authenticateAs(testRecorderUser, recorderRole);

        CreateTestApiKeyRequest request = CreateTestApiKeyRequest.builder()
                .partnerName("Đối tác thử nghiệm XYZ")
                .rateLimitPerHour(50)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        // Gọi endpoint chuẩn /api/v1/organization/api-keys/test
        mockMvc.perform(post("/api/v1/organization/api-keys/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Gọi endpoint alias /api/v1/cooperative/test-api-keys
        mockMvc.perform(post("/api/v1/cooperative/test-api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /**
     * Happy path: Quản lý Hợp tác xã (VT-02) cấp khóa thử nghiệm thành công.
     */
    @Test
    @DisplayName("Happy path: Quản lý HTX (VT-02) cấp khóa thử nghiệm thành công -> HTTP 201 và tiền tố nks_test_")
    void testHappyPath_CooperativeManager_CreatesTestApiKeySuccessfully() throws Exception {
        authenticateAs(testManagerUser, managerRole);

        CreateTestApiKeyRequest request = CreateTestApiKeyRequest.builder()
                .partnerName("Công ty Xuất Khẩu Nông Sản Sạch")
                .rateLimitPerHour(60)
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build();

        mockMvc.perform(post("/api/v1/organization/api-keys/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.partnerName").value("Công ty Xuất Khẩu Nông Sản Sạch"))
                .andExpect(jsonPath("$.data.is_test").value(true))
                .andExpect(jsonPath("$.data.keyPrefix").value(containsString("nks_test_")))
                .andExpect(jsonPath("$.data.rawApiKey").value(containsString("nks_test_")));
    }

    /**
     * Rate limit: Vượt quá hạn mức số lượt gọi trong 1 giờ -> HTTP 429 Too Many Requests (QTN-20).
     */
    @Test
    @DisplayName("Rate limit: Khóa thử nghiệm vượt quá hạn mức gọi -> Bị từ chối HTTP 429 Too Many Requests")
    void testRateLimit_Exceeded_RejectedWith429() throws Exception {
        // Lần gọi 1: Chưa vượt hạn mức (1/1) -> thành công 200 OK
        mockMvc.perform(get("/api/publicapi/v1/lots/SAMPLE_LOT_ID")
                        .header("X-API-KEY", RATE_LIMITED_TEST_RAW_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Lần gọi 2: Vượt hạn mức (> 1) -> bị từ chối 429 Too Many Requests
        mockMvc.perform(get("/api/publicapi/v1/lots/SAMPLE_LOT_ID")
                        .header("X-API-KEY", RATE_LIMITED_TEST_RAW_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("vượt quá hạn mức")));
    }

    /**
     * Thu hồi khóa: Khóa thử nghiệm đã bị thu hồi -> HTTP 401 Unauthorized.
     */
    @Test
    @DisplayName("Revoke: Khóa thử nghiệm đã bị thu hồi -> Bị từ chối HTTP 401 Unauthorized")
    void testRevokedApiKey_RejectedWith401() throws Exception {
        mockMvc.perform(get("/api/publicapi/v1/lots/SAMPLE_LOT_ID")
                        .header("X-API-KEY", REVOKED_TEST_RAW_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("thu hồi")));
    }
}
