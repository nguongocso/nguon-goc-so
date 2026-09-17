package vn.nguongocso.certification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import vn.nguongocso.certification.dto.request.InspectionCriterionResultRequest;
import vn.nguongocso.certification.dto.request.IssueInspectionResultEntryLinkRequest;
import vn.nguongocso.certification.dto.request.RecordInspectionResultsRequest;
import vn.nguongocso.certification.dto.response.InspectionResultEntryLinkResponse;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.InspectionResultEntryLink;
import vn.nguongocso.certification.entity.TestingUnit;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.enums.InspectionResultEntryLinkStatus;
import vn.nguongocso.certification.enums.InspectionResultEntrySource;
import vn.nguongocso.certification.repository.InspectionCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.InspectionResultEntryLinkRepository;
import vn.nguongocso.certification.repository.TestingUnitRepository;
import vn.nguongocso.certification.service.InspectionCriterionResultService;
import vn.nguongocso.certification.service.InspectionResultEntryLinkService;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/**
 * Kiểm thử tích hợp toàn diện luồng Cổng nhập kết quả kiểm nghiệm (NCL-11-CN-007).
 * Bao gồm các ca kiểm thử:
 * <ul>
 *     <li>TC-01: Cấp liên kết, truy cập công khai và nộp kết quả với nguồn TESTING_UNIT_PORTAL.</li>
 *     <li>TC-02: Liên kết hết hạn trả về HTTP 410 GONE kèm hướng dẫn liên hệ HTX.</li>
 *     <li>TC-03: Chống double-submit, liên kết đã dùng trả về HTTP 410 GONE.</li>
 *     <li>TC-04: HTX nhập tay tự động thu hồi liên kết portal active và gán nguồn COOPERATIVE_MANUAL.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InspectionResultEntryPortalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InspectionResultEntryLinkService linkService;

    @Autowired
    private InspectionCriterionResultService resultService;

    @Autowired
    private InspectionResultEntryLinkRepository linkRepository;

    @Autowired
    private InspectionRequestRepository requestRepository;

    @Autowired
    private InspectionCriterionRepository criterionRepository;

    @Autowired
    private InspectionCriterionResultRepository criterionResultRepository;

    @Autowired
    private TestingUnitRepository testingUnitRepository;

    @Autowired
    private ProductionLotRepository lotRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationUserRepository orgUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Organization organization;
    private User managerUser;
    private CustomUserDetails customUserDetails;
    private TestingUnit testingUnit;
    private ProductionLot productionLot;
    private InspectionRequest inspectionRequest;
    private InspectionCriterion criterion1;
    private InspectionCriterion criterion2;

    @BeforeEach
    void setUp() {
        organization = organizationRepository.save(Organization.builder()
                .name("HTX Nông Sản Hữu Cơ Miền Tây")
                .code("HTX-TEST-" + UUID.randomUUID().toString().substring(0, 6))
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        Role roleVt02 = roleRepository.findByCode("VT-02")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setCode("VT-02");
                    r.setName("Quản lý HTX");
                    return roleRepository.save(r);
                });

        managerUser = userRepository.save(User.builder()
                .userId(UUID.randomUUID())
                .userName("manager_" + UUID.randomUUID().toString().substring(0, 8))
                .passwordHash("$2a$10$dummyHashValueForIntegrationTestingPurposeOnly")
                .fullName("Trần Quản Lý")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setId(UUID.randomUUID());
        orgUser.setOrganization(organization);
        orgUser.setUser(managerUser);
        orgUser.setRole(roleVt02);
        orgUser.setStatus(vn.nguongocso.organization.enums.OrganizationUserStatus.ACTIVE);
        orgUser.setJoinedAt(LocalDateTime.now());
        orgUser = orgUserRepository.save(orgUser);

        customUserDetails = new CustomUserDetails(managerUser, orgUser, roleVt02);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                customUserDetails, null, List.of(new SimpleGrantedAuthority("ROLE_VT-02")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        testingUnit = testingUnitRepository.save(TestingUnit.builder()
                .id(UUID.randomUUID())
                .name("Trung tâm Kỹ thuật Tiêu chuẩn Đo lường Chất lượng 3 (QUATEST 3)")
                .accreditationCode("TU-" + UUID.randomUUID().toString().substring(0, 6))
                .isActive(true)
                .contactInfo("contact@quatest3.vn")
                .createdAt(LocalDateTime.now())
                .build());

        ProductCategory category = categoryRepository.save(ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Xoài")
                .isActive(true)
                .build());

        productionLot = lotRepository.save(ProductionLot.builder()
                .name("Lô Xoài Cát Chu Xuất Khẩu")
                .organization(organization)
                .productCategory(category)
                .status(ProductionLotStatus.APPROVED)
                .expectedQuantity(1000.0)
                .expectedQuantityUnit("kg")
                .createdAt(LocalDateTime.now())
                .build());

        inspectionRequest = requestRepository.save(InspectionRequest.builder()
                .productionLot(productionLot)
                .inspectionUnit(testingUnit.getName())
                .testingUnitId(testingUnit.getId())
                .sampleSentDate(LocalDate.now().minusDays(5))
                .status(InspectionRequestStatus.PENDING_RESULT)
                .createdBy(managerUser)
                .createdAt(LocalDateTime.now())
                .build());

        criterion1 = InspectionCriterion.builder()
                .id(UUID.randomUUID())
                .inspectionRequest(inspectionRequest)
                .criterionName("Dư lượng thuốc BVTV (Carbendazim)")
                .criterionCode("CRIT-01")
                .criterionId(1L)
                .build();

        criterion2 = InspectionCriterion.builder()
                .id(UUID.randomUUID())
                .inspectionRequest(inspectionRequest)
                .criterionName("Hàm lượng kim loại nặng (Chì)")
                .criterionCode("CRIT-02")
                .criterionId(2L)
                .build();

        inspectionRequest.getCriteria().add(criterion1);
        inspectionRequest.getCriteria().add(criterion2);
        inspectionRequest = requestRepository.save(inspectionRequest);
        criterion1 = inspectionRequest.getCriteria().get(0);
        criterion2 = inspectionRequest.getCriteria().get(1);
    }

    @Test
    @DisplayName("TC-01: Cấp liên kết, truy cập công khai và nộp kết quả portal thành công")
    void testEndToEndPortalResultEntry_Success_TC01() throws Exception {
        // 1. Cấp liên kết bởi Quản lý HTX (VT-02)
        IssueInspectionResultEntryLinkRequest issueRequest = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@quatest3.vn")
                .expiryDays(7)
                .build();

        InspectionResultEntryLinkResponse linkResponse = linkService.issueLink(
                inspectionRequest.getId(), issueRequest, customUserDetails);

        assertThat(linkResponse).isNotNull();
        assertThat(linkResponse.getEntryUrl()).isNotBlank();
        assertThat(linkResponse.getStatus()).isEqualTo(InspectionResultEntryLinkStatus.ACTIVE);

        // Trích xuất rawToken từ entryUrl
        String entryUrl = linkResponse.getEntryUrl();
        String rawToken = entryUrl.substring(entryUrl.lastIndexOf('/') + 1);

        // 2. Đơn vị kiểm nghiệm mở liên kết qua cổng công khai (không cần auth/JWT)
        mockMvc.perform(get("/api/v1/public/inspection-result-entry/{token}", rawToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.testingUnit").value(testingUnit.getName()))
                .andExpect(jsonPath("$.data.lotName").value(productionLot.getName()))
                .andExpect(jsonPath("$.data.criteria").isArray())
                .andExpect(jsonPath("$.data.criteria.length()").value(2));

        // 3. Đơn vị kiểm nghiệm nộp kết quả cho toàn bộ chỉ tiêu
        RecordInspectionResultsRequest submitRequest = RecordInspectionResultsRequest.builder()
                .results(List.of(
                        InspectionCriterionResultRequest.builder()
                                .criterionId(criterion1.getId().toString())
                                .passed(true)
                                .resultDate(LocalDate.now())
                                .expiryDate(LocalDate.now().plusMonths(6))
                                .build(),
                        InspectionCriterionResultRequest.builder()
                                .criterionId(criterion2.getId().toString())
                                .passed(true)
                                .resultDate(LocalDate.now())
                                .expiryDate(LocalDate.now().plusMonths(6))
                                .build()))
                .build();

        mockMvc.perform(put("/api/v1/public/inspection-result-entry/{token}/results", rawToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].entrySource").value("TESTING_UNIT_PORTAL"))
                .andExpect(jsonPath("$.data[1].entrySource").value("TESTING_UNIT_PORTAL"));

        // 4. Kiểm tra Database Invariants
        InspectionResultEntryLink linkInDb = linkRepository.findById(linkResponse.getId()).orElseThrow();
        assertThat(linkInDb.getStatus()).isEqualTo(InspectionResultEntryLinkStatus.USED);
        assertThat(linkInDb.getUsedAt()).isNotNull();

        List<InspectionCriterionResult> resultsInDb = criterionResultRepository
                .findByInspectionCriterion_InspectionRequest_Id(inspectionRequest.getId());
        assertThat(resultsInDb).hasSize(2);
        for (InspectionCriterionResult r : resultsInDb) {
            assertThat(r.getEntrySource()).isEqualTo(InspectionResultEntrySource.TESTING_UNIT_PORTAL);
            assertThat(r.getPortalLink()).isNotNull();
            assertThat(r.getPortalLink().getId()).isEqualTo(linkInDb.getId());
            assertThat(r.getCreatedBy()).isNull(); // Invariant 4: createdBy == null khi qua portal
            assertThat(r.getPassed()).isTrue();
        }

        InspectionRequest updatedRequest = requestRepository.findById(inspectionRequest.getId()).orElseThrow();
        assertThat(updatedRequest.getStatus()).isEqualTo(InspectionRequestStatus.PASSED);
    }

    @Test
    @DisplayName("TC-02: Liên kết hết hạn trả về HTTP 410 GONE kèm hướng dẫn")
    void testPortalResultEntry_ExpiredLink_Returns410_TC02() throws Exception {
        IssueInspectionResultEntryLinkRequest issueRequest = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@quatest3.vn")
                .expiryDays(7)
                .build();

        InspectionResultEntryLinkResponse linkResponse = linkService.issueLink(
                inspectionRequest.getId(), issueRequest, customUserDetails);

        String entryUrl = linkResponse.getEntryUrl();
        String rawToken = entryUrl.substring(entryUrl.lastIndexOf('/') + 1);

        // Làm giả link hết hạn bằng cách lùi expiresAt
        InspectionResultEntryLink linkInDb = linkRepository.findById(linkResponse.getId()).orElseThrow();
        linkInDb.setExpiresAt(LocalDateTime.now().minusHours(1));
        linkInDb.setStatus(InspectionResultEntryLinkStatus.EXPIRED);
        linkRepository.save(linkInDb);

        mockMvc.perform(get("/api/v1/public/inspection-result-entry/{token}", rawToken))
                .andExpect(status().isGone())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.message", containsString("hết hạn")));
    }

    @Test
    @DisplayName("TC-03: Chống double-submit, liên kết đã dùng trả về HTTP 410 GONE")
    void testPortalResultEntry_DoubleSubmit_Returns410_TC03() throws Exception {
        IssueInspectionResultEntryLinkRequest issueRequest = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@quatest3.vn")
                .expiryDays(7)
                .build();

        InspectionResultEntryLinkResponse linkResponse = linkService.issueLink(
                inspectionRequest.getId(), issueRequest, customUserDetails);

        String entryUrl = linkResponse.getEntryUrl();
        String rawToken = entryUrl.substring(entryUrl.lastIndexOf('/') + 1);

        RecordInspectionResultsRequest submitRequest = RecordInspectionResultsRequest.builder()
                .results(List.of(
                        InspectionCriterionResultRequest.builder()
                                .criterionId(criterion1.getId().toString())
                                .passed(true)
                                .resultDate(LocalDate.now())
                                .expiryDate(LocalDate.now().plusMonths(6))
                                .build(),
                        InspectionCriterionResultRequest.builder()
                                .criterionId(criterion2.getId().toString())
                                .passed(true)
                                .resultDate(LocalDate.now())
                                .expiryDate(LocalDate.now().plusMonths(6))
                                .build()))
                .build();

        // Submit lần 1 thành công
        mockMvc.perform(put("/api/v1/public/inspection-result-entry/{token}/results", rawToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk());

        // Submit lần 2 bị chặn với HTTP 410 GONE
        mockMvc.perform(put("/api/v1/public/inspection-result-entry/{token}/results", rawToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isGone())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.message", containsString("đã được sử dụng")));

        // Truy cập GET lại link cũng bị 410 GONE
        mockMvc.perform(get("/api/v1/public/inspection-result-entry/{token}", rawToken))
                .andExpect(status().isGone())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    @DisplayName("TC-04: HTX nhập tay tự động thu hồi link active và lưu nguồn COOPERATIVE_MANUAL")
    void testCooperativeManualEntry_RevokesActiveLink_TC04() {
        // 1. Đang có 1 link ACTIVE cấp trước đó
        IssueInspectionResultEntryLinkRequest issueRequest = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@quatest3.vn")
                .expiryDays(7)
                .build();

        InspectionResultEntryLinkResponse linkResponse = linkService.issueLink(
                inspectionRequest.getId(), issueRequest, customUserDetails);

        assertThat(linkResponse.getStatus()).isEqualTo(InspectionResultEntryLinkStatus.ACTIVE);

        // 2. HTX quyết định nhập tay kết quả
        List<InspectionCriterionResultRequest> manualResults = List.of(
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterion1.getId().toString())
                        .passed(true)
                        .resultDate(LocalDate.now())
                        .expiryDate(LocalDate.now().plusMonths(6))
                        .build(),
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterion2.getId().toString())
                        .passed(false) // Không đạt
                        .build());

        resultService.recordResults(inspectionRequest.getId(), manualResults, customUserDetails);

        // 3. Kiểm tra link ACTIVE cũ đã bị tự động thu hồi (REVOKED)
        InspectionResultEntryLink linkInDb = linkRepository.findById(linkResponse.getId()).orElseThrow();
        assertThat(linkInDb.getStatus()).isEqualTo(InspectionResultEntryLinkStatus.REVOKED);
        assertThat(linkInDb.getRevokedBy()).isNotNull();
        assertThat(linkInDb.getRevokedBy().getUserId()).isEqualTo(managerUser.getUserId());

        // 4. Kiểm tra nguồn kết quả là COOPERATIVE_MANUAL, có createdBy, không có portalLink
        List<InspectionCriterionResult> results = criterionResultRepository
                .findByInspectionCriterion_InspectionRequest_Id(inspectionRequest.getId());
        assertThat(results).hasSize(2);
        for (InspectionCriterionResult r : results) {
            assertThat(r.getEntrySource()).isEqualTo(InspectionResultEntrySource.COOPERATIVE_MANUAL);
            assertThat(r.getCreatedBy()).isNotNull();
            assertThat(r.getCreatedBy().getUserId()).isEqualTo(managerUser.getUserId());
            assertThat(r.getPortalLink()).isNull();
        }

        // 5. Trạng thái yêu cầu kiểm nghiệm trở thành FAILED do criterion2 không đạt
        InspectionRequest updatedRequest = requestRepository.findById(inspectionRequest.getId()).orElseThrow();
        assertThat(updatedRequest.getStatus()).isEqualTo(InspectionRequestStatus.FAILED);
    }
}
