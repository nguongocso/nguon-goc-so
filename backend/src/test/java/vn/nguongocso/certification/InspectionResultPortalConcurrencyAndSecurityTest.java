package vn.nguongocso.certification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionResultRequest;
import vn.nguongocso.certification.dto.request.IssueInspectionResultEntryLinkRequest;
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
import vn.nguongocso.exception.BusinessException;
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
 * Kiểm thử chuyên sâu cho tính đồng thời (Concurrency) và an toàn bảo mật (Security)
 * của Cổng nhập kết quả kiểm nghiệm (NCL-11-CN-007, BLOCKER 1, BLOCKER 2, MAJOR 2, MAJOR 6).
 */
@SpringBootTest
@ActiveProfiles("test")
class InspectionResultPortalConcurrencyAndSecurityTest {

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
                .name("HTX Nông Sản An Toàn Concurrency")
                .code("HTX-CONC-" + UUID.randomUUID().toString().substring(0, 6))
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
                .userName("mgr_conc_" + UUID.randomUUID().toString().substring(0, 8))
                .passwordHash("$2a$10$dummyHashValueForIntegrationTestingPurposeOnly")
                .fullName("Nguyễn Quản Lý")
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
                .name("Trung tâm Giám định Chất lượng Độc lập")
                .accreditationCode("TU-CONC-" + UUID.randomUUID().toString().substring(0, 6))
                .isActive(true)
                .contactInfo("lab@quality.vn")
                .createdAt(LocalDateTime.now())
                .build());

        ProductCategory category = categoryRepository.save(ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Thanh Long")
                .isActive(true)
                .build());

        productionLot = lotRepository.save(ProductionLot.builder()
                .name("Lô Thanh Long Ruột Đỏ")
                .organization(organization)
                .productCategory(category)
                .status(ProductionLotStatus.APPROVED)
                .expectedQuantity(2000.0)
                .expectedQuantityUnit("kg")
                .createdAt(LocalDateTime.now())
                .build());

        inspectionRequest = requestRepository.save(InspectionRequest.builder()
                .productionLot(productionLot)
                .inspectionUnit(testingUnit.getName())
                .testingUnitId(testingUnit.getId())
                .sampleSentDate(LocalDate.now().minusDays(3))
                .status(InspectionRequestStatus.PENDING_RESULT)
                .createdBy(managerUser)
                .createdAt(LocalDateTime.now())
                .build());

        criterion1 = criterionRepository.save(InspectionCriterion.builder()
                .id(UUID.randomUUID())
                .inspectionRequest(inspectionRequest)
                .criterionName("Dư lượng thuốc trừ sâu gốc Clo")
                .criterionCode("CRIT-C1")
                .criterionId(101L)
                .build());

        criterion2 = criterionRepository.save(InspectionCriterion.builder()
                .id(UUID.randomUUID())
                .inspectionRequest(inspectionRequest)
                .criterionName("Hàm lượng vi sinh vật hiếu khí")
                .criterionCode("CRIT-C2")
                .criterionId(102L)
                .build());

        inspectionRequest.getCriteria().add(criterion1);
        inspectionRequest.getCriteria().add(criterion2);
        inspectionRequest = requestRepository.save(inspectionRequest);
    }

    @AfterEach
    void tearDown() {
        criterionResultRepository.deleteAll();
        linkRepository.deleteAll();
        criterionRepository.deleteAll();
        requestRepository.deleteAll();
        lotRepository.deleteAll();
        categoryRepository.deleteAll();
        testingUnitRepository.deleteAll();
        orgUserRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("BLOCKER 2 / QTN-14: Khi concurrent issueLink diễn ra, sau cùng chỉ có duy nhất 1 link ở trạng thái ACTIVE")
    void testConcurrentIssueLink_onlyOneActiveLinkRemains() throws Exception {
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        IssueInspectionResultEntryLinkRequest issueRequest = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@quality.vn")
                .expiryDays(7)
                .build();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    linkService.issueLink(inspectionRequest.getId(), issueRequest, customUserDetails);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Bắn đồng thời các luồng
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).isTrue();

        // Kiểm tra Invariant QTN-14: số link ACTIVE trong DB cho request này chính xác là 1
        long activeCount = linkRepository.countByInspectionRequest_IdAndStatus(
                inspectionRequest.getId(), InspectionResultEntryLinkStatus.ACTIVE);
        assertThat(activeCount).isEqualTo(1L);

        // Tổng số link đã tạo
        List<InspectionResultEntryLink> allLinks = linkRepository.findByInspectionRequest_Id(inspectionRequest.getId());
        assertThat(allLinks).hasSize(threadCount);

        // Các link còn lại phải ở trạng thái REVOKED
        long revokedCount = allLinks.stream()
                .filter(l -> l.getStatus() == InspectionResultEntryLinkStatus.REVOKED)
                .count();
        assertThat(revokedCount).isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("MAJOR 2 / TC-03: Hai luồng nộp kết quả portal đồng thời với cùng một token -> chỉ 1 luồng thành công, 1 luồng bị từ chối")
    void testConcurrentDoubleSubmit_onlyOneSucceeds_TC03() throws Exception {
        // Cấp link portal
        IssueInspectionResultEntryLinkRequest issueRequest = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@quality.vn")
                .expiryDays(7)
                .build();
        InspectionResultEntryLinkResponse linkResponse = linkService.issueLink(
                inspectionRequest.getId(), issueRequest, customUserDetails);
        String rawToken = linkResponse.getEntryUrl().substring(linkResponse.getEntryUrl().lastIndexOf('/') + 1);

        List<InspectionCriterionResultRequest> payload = List.of(
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
                        .build());

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictOrGoneCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    resultService.recordPortalResults(rawToken, payload, "192.168.1.50", "Test-Browser");
                    successCount.incrementAndGet();
                } catch (BusinessException be) {
                    if (be.getStatus() == HttpStatus.GONE || be.getStatus() == HttpStatus.CONFLICT) {
                        conflictOrGoneCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).isTrue();

        // Đúng 1 luồng thành công và 1 luồng bị từ chối
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictOrGoneCount.get()).isEqualTo(1);

        // Kiểm tra database: link phải ở trạng thái USED
        InspectionResultEntryLink linkInDb = linkRepository.findById(linkResponse.getId()).orElseThrow();
        assertThat(linkInDb.getStatus()).isEqualTo(InspectionResultEntryLinkStatus.USED);

        // Kết quả chỉ có 2 bản ghi
        List<InspectionCriterionResult> results = criterionResultRepository
                .findByInspectionCriterion_InspectionRequest_Id(inspectionRequest.getId());
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("MAJOR 2: Đua race condition giữa Manual Entry và Portal Submit -> bảo toàn tính nhất quán và one-time semantics")
    void testManualVsPortalRace_consistentState() throws Exception {
        // Cấp link portal
        IssueInspectionResultEntryLinkRequest issueRequest = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@quality.vn")
                .expiryDays(7)
                .build();
        InspectionResultEntryLinkResponse linkResponse = linkService.issueLink(
                inspectionRequest.getId(), issueRequest, customUserDetails);
        String rawToken = linkResponse.getEntryUrl().substring(linkResponse.getEntryUrl().lastIndexOf('/') + 1);

        List<InspectionCriterionResultRequest> portalPayload = List.of(
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
                        .build());

        List<InspectionCriterionResultRequest> manualPayload = List.of(
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterion1.getId().toString())
                        .passed(false)
                        .resultDate(LocalDate.now())
                        .build(),
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterion2.getId().toString())
                        .passed(false)
                        .resultDate(LocalDate.now())
                        .build());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger completedCount = new AtomicInteger(0);

        // Thread 1: Portal submit
        executor.submit(() -> {
            try {
                startLatch.await();
                resultService.recordPortalResults(rawToken, portalPayload, "192.168.1.51", "Portal-Client");
                completedCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Manual record
        executor.submit(() -> {
            try {
                startLatch.await();
                resultService.recordResults(inspectionRequest.getId(), manualPayload, customUserDetails);
                completedCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(finished).isTrue();

        // Đúng 1 luồng thành công (luồng kia bị chặn do status không còn là PENDING_RESULT)
        assertThat(completedCount.get()).isEqualTo(1);

        // Kiểm tra tính nhất quán: Toàn bộ kết quả trong DB phải có cùng 1 entrySource duy nhất
        List<InspectionCriterionResult> results = criterionResultRepository
                .findByInspectionCriterion_InspectionRequest_Id(inspectionRequest.getId());
        assertThat(results).hasSize(2);

        InspectionResultEntrySource firstSource = results.get(0).getEntrySource();
        assertThat(results.get(1).getEntrySource()).isEqualTo(firstSource);
    }

    @Test
    @DisplayName("BLOCKER 1: Không thể submit với Opaque File Handle của request hoặc chỉ tiêu khác (Foreign File Ownership Denied)")
    void testForeignFileOwnership_deniedWithForbidden() {
        // Cấp link cho Request 1
        IssueInspectionResultEntryLinkRequest issueRequest = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@quality.vn")
                .expiryDays(7)
                .build();
        InspectionResultEntryLinkResponse linkResp1 = linkService.issueLink(
                inspectionRequest.getId(), issueRequest, customUserDetails);
        String rawToken1 = linkResp1.getEntryUrl().substring(linkResp1.getEntryUrl().lastIndexOf('/') + 1);

        // Upload file hợp lệ cho Request 1 - Criterion 1
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "cert.pdf", "application/pdf", "%PDF-sample-content".getBytes());
        String fileHandle1 = resultService.uploadPortalResultFile(
                rawToken1, criterion1.getId().toString(), file1, "192.168.1.1");
        assertThat(fileHandle1).startsWith("pfh_");

        // Tạo Request 2 độc lập
        InspectionRequest req2 = requestRepository.save(InspectionRequest.builder()
                .productionLot(productionLot)
                .inspectionUnit(testingUnit.getName())
                .testingUnitId(testingUnit.getId())
                .sampleSentDate(LocalDate.now().minusDays(1))
                .status(InspectionRequestStatus.PENDING_RESULT)
                .createdBy(managerUser)
                .createdAt(LocalDateTime.now())
                .build());
        InspectionCriterion crit2A = criterionRepository.save(InspectionCriterion.builder()
                .id(UUID.randomUUID())
                .inspectionRequest(req2)
                .criterionName("Chỉ tiêu của Request 2")
                .criterionCode("CRIT-REQ2")
                .criterionId(201L)
                .build());
        req2.getCriteria().add(crit2A);
        req2 = requestRepository.save(req2);

        // Cấp link cho Request 2
        InspectionResultEntryLinkResponse linkResp2 = linkService.issueLink(
                req2.getId(), issueRequest, customUserDetails);
        String rawToken2 = linkResp2.getEntryUrl().substring(linkResp2.getEntryUrl().lastIndexOf('/') + 1);

        // Kẻ tấn công mang fileHandle1 của Request 1 submit vào Request 2
        List<InspectionCriterionResultRequest> maliciousPayload = List.of(
                InspectionCriterionResultRequest.builder()
                        .criterionId(crit2A.getId().toString())
                        .passed(true)
                        .resultDate(LocalDate.now())
                        .expiryDate(LocalDate.now().plusMonths(6))
                        .filePath(fileHandle1) // File handle của Request 1!
                        .build());

        // Phải bị từ chối với HTTP 403 FORBIDDEN
        assertThatThrownBy(() -> resultService.recordPortalResults(rawToken2, maliciousPayload, "192.168.1.2", "Malicious"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getMessage()).contains("không thuộc về chỉ tiêu hoặc yêu cầu");
                });
    }

    @Test
    @DisplayName("MAJOR 3: Tải file vượt quá 5MB ném HTTP 413, sai định dạng MIME ném HTTP 415")
    void testFileValidation_sizeAndMime() {
        IssueInspectionResultEntryLinkRequest issueRequest = IssueInspectionResultEntryLinkRequest.builder()
                .recipientEmail("lab@quality.vn")
                .expiryDays(7)
                .build();
        InspectionResultEntryLinkResponse linkResp = linkService.issueLink(
                inspectionRequest.getId(), issueRequest, customUserDetails);
        String rawToken = linkResp.getEntryUrl().substring(linkResp.getEntryUrl().lastIndexOf('/') + 1);

        // 1. File quá 5MB (> 5242880 bytes)
        byte[] oversizedBytes = new byte[5 * 1024 * 1024 + 1024]; // 5MB + 1KB
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", oversizedBytes);

        assertThatThrownBy(() -> resultService.uploadPortalResultFile(
                rawToken, criterion1.getId().toString(), oversizedFile, "192.168.1.1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
                    assertThat(be.getMessage()).contains("vượt quá dung lượng");
                });

        // 2. MIME type không hỗ trợ (.txt hoặc .exe)
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "danger.exe", "application/x-msdownload", "content".getBytes());

        assertThatThrownBy(() -> resultService.uploadPortalResultFile(
                rawToken, criterion1.getId().toString(), textFile, "192.168.1.1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                    assertThat(be.getMessage()).contains("Loại file không hỗ trợ");
                });
    }
}
