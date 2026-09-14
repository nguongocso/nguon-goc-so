package vn.nguongocso.alert_reclaim_history.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.alert.dto.request.ActivityLogExportFilterRequest;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.entity.ActivityLogExportItem;
import vn.nguongocso.alert.entity.ActivityLogExportJob;
import vn.nguongocso.alert.repository.ActivityLogExportItemRepository;
import vn.nguongocso.alert.repository.ActivityLogExportJobRepository;
import vn.nguongocso.alert.repository.ActivityLogRepository;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;

/**
 * Kiểm thử liên tầng (E2E Integration Test) cho chức năng NCL-08-CN-015:
 * Xuất nhật ký hoạt động phục vụ kiểm tra.
 *
 * Kiểm tra toàn bộ luồng nghiệp vụ thực tế với cơ sở dữ liệu test:
 * - VT-02 đăng nhập, xem nhật ký, lọc dữ liệu, xem trước số lượng (preview), xuất CSV.
 * - Kiểm tra định dạng CSV (9 cột, UTF-8 BOM, escape ký tự, tiếng Việt, chống CSV Formula Injection).
 * - Kiểm tra ghi nhận audit log EXPORT_ACTIVITY_LOG sau khi xuất và không lọt vào chính file CSV đó.
 * - Kiểm tra các trường hợp biên: không có dữ liệu (400), phân quyền VT-02 vs VT-03 (403), tenant isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.activity-log-export.direct-limit=2")
@Transactional
public class ActivityLogExportE2EIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ActivityLogExportJobRepository exportJobRepository;

    @Autowired
    private ActivityLogExportItemRepository exportItemRepository;

    @Autowired
    private vn.nguongocso.alert.service.impl.ActivityLogExportWorker activityLogExportWorker;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID orgIdA;
    private UUID orgIdB;

    private CustomUserDetails userManagerOrgA;
    private CustomUserDetails userRecorderOrgA;
    private CustomUserDetails userManagerOrgB;

    @BeforeEach
    void setUp() {
        orgIdA = UUID.randomUUID();
        orgIdB = UUID.randomUUID();

        userManagerOrgA = createCustomUserDetails(UUID.randomUUID(), orgIdA, "manager_a", "Nguyễn Văn An", "VT-02");
        userRecorderOrgA = createCustomUserDetails(UUID.randomUUID(), orgIdA, "recorder_a", "Trần Thị Bình", "VT-03");
        userManagerOrgB = createCustomUserDetails(UUID.randomUUID(), orgIdB, "manager_b", "Lê Văn B", "VT-02");
    }

    private CustomUserDetails createCustomUserDetails(UUID userId, UUID orgId, String username, String fullName, String roleCode) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName(username);
        user.setFullName(fullName);
        user.setPasswordHash("hash123");

        Organization org = new Organization();
        org.setOrganizationId(orgId);
        org.setName("Tổ chức " + username);
        org.setCode("ORG_" + username.toUpperCase());

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setOrganization(org);
        orgUser.setUser(user);

        Role role = new Role();
        role.setCode(roleCode);
        role.setName("Vai trò " + roleCode);

        return new CustomUserDetails(user, orgUser, role);
    }

    private ActivityLog createLog(UUID orgId, String username, String fullName, String action, String entityType, String entityId, LocalDateTime createdAt) {
        return ActivityLog.builder()
                .organizationId(orgId)
                .userId(UUID.randomUUID())
                .username(username)
                .fullName(fullName)
                .action(action)
                .description("Mô tả thao tác " + action)
                .actorRole(username.startsWith("recorder") ? "VT-03" : "VT-02")
                .entityType(entityType)
                .entityId(entityId)
                .beforeValue("{\"status\":\"OLD\"}")
                .afterValue("{\"status\":\"NEW\"}")
                .createdAt(createdAt)
                .build();
    }

    @Test
    @DisplayName("E2E Happy Path: VT-02 đăng nhập -> Xem danh sách -> Áp dụng bộ lọc -> Preview -> Tải CSV -> Kiểm tra nội dung CSV -> Refresh danh sách thấy EXPORT_ACTIVITY_LOG nhưng không lọt vào CSV")
    void testE2E_HappyPath_FullFlow() throws Exception {
        // 1. Chuẩn bị dữ liệu ban đầu cho Tổ chức A (3 bản ghi) và Tổ chức B (2 bản ghi)
        ActivityLog log1 = createLog(orgIdA, "manager_a", "Nguyễn Văn An", "UPDATE_PRODUCTION_LOT", "PRODUCTION_LOT", "LOT-001", LocalDateTime.of(2026, 9, 10, 8, 30));
        ActivityLog log2 = createLog(orgIdA, "recorder_a", "Trần Thị Bình", "UPDATE_PRODUCTION_LOT", "PRODUCTION_LOT", "LOT-002", LocalDateTime.of(2026, 9, 11, 9, 15));
        ActivityLog log3 = createLog(orgIdA, "manager_a", "Nguyễn Văn An", "CREATE_PRODUCTION_LOT", "PRODUCTION_LOT", "LOT-003", LocalDateTime.of(2026, 9, 12, 14, 0));
        
        ActivityLog logOrgB1 = createLog(orgIdB, "manager_b", "Lê Văn B", "UPDATE_PRODUCTION_LOT", "PRODUCTION_LOT", "LOT-B01", LocalDateTime.of(2026, 9, 10, 8, 30));
        ActivityLog logOrgB2 = createLog(orgIdB, "manager_b", "Lê Văn B", "CREATE_PRODUCTION_LOT", "PRODUCTION_LOT", "LOT-B02", LocalDateTime.of(2026, 9, 11, 9, 0));

        activityLogRepository.saveAll(List.of(log1, log2, log3, logOrgB1, logOrgB2));

        // 2. VT-02 xem danh sách nhật ký qua API GET (bước "mở Nhật ký hoạt động")
        mockMvc.perform(get("/api/v1/organizations/activity-logs")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(3));

        // 3. Áp dụng bộ lọc (action = UPDATE_PRODUCTION_LOT) và gọi Preview
        ActivityLogExportFilterRequest filter = new ActivityLogExportFilterRequest();
        filter.setAction("UPDATE_PRODUCTION_LOT");

        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports/preview")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.mode").value("DIRECT"));

        // 4. Bấm tải CSV (gọi POST /exports)
        MvcResult exportResult = mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        // Kiểm tra header Content-Disposition chứa tên tệp theo quy chuẩn
        String disposition = exportResult.getResponse().getHeader("Content-Disposition");
        assertThat(disposition).isNotNull();
        assertThat(disposition).startsWith("attachment;");
        assertThat(disposition).contains("activity-logs-");
        assertThat(disposition).contains(".csv");

        // 5. Kiểm tra nội dung tệp CSV
        byte[] csvBytes = exportResult.getResponse().getContentAsByteArray();
        assertThat(csvBytes.length).isGreaterThan(3);
        // Kiểm tra BOM UTF-8 (\uFEFF)
        assertThat(csvBytes[0]).isEqualTo((byte) 0xEF);
        assertThat(csvBytes[1]).isEqualTo((byte) 0xBB);
        assertThat(csvBytes[2]).isEqualTo((byte) 0xBF);

        String csvString = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(new InputStreamReader(
                        new ByteArrayInputStream(csvBytes, 3, csvBytes.length - 3), StandardCharsets.UTF_8))) {
            List<String> headerNames = parser.getHeaderNames();
            assertThat(headerNames).containsExactly(
                    "Thời gian",
                    "Người thực hiện",
                    "Tên đăng nhập",
                    "Vai trò",
                    "Hành động",
                    "Loại đối tượng",
                    "Mã đối tượng",
                    "Dữ liệu trước",
                    "Dữ liệu sau"
            );

            List<CSVRecord> records = parser.getRecords();
            assertThat(records).hasSize(2);

            CSVRecord r1 = records.get(0);
            assertThat(r1.get("Người thực hiện")).isEqualTo("Nguyễn Văn An");
            assertThat(r1.get("Tên đăng nhập")).isEqualTo("manager_a");
            assertThat(r1.get("Vai trò")).isEqualTo("Quản lý hợp tác xã (VT-02)");
            assertThat(r1.get("Hành động")).isEqualTo("Cập nhật lô sản xuất");
            assertThat(r1.get("Loại đối tượng")).isEqualTo("Lô sản xuất");
            assertThat(r1.get("Mã đối tượng")).isEqualTo("LOT-001");
            assertThat(r1.get("Dữ liệu trước")).isEqualTo("{\"status\":\"OLD\"}");
            assertThat(r1.get("Dữ liệu sau")).isEqualTo("{\"status\":\"NEW\"}");

            CSVRecord r2 = records.get(1);
            assertThat(r2.get("Người thực hiện")).isEqualTo("Trần Thị Bình");
            assertThat(r2.get("Tên đăng nhập")).isEqualTo("recorder_a");
            assertThat(r2.get("Hành động")).isEqualTo("Cập nhật lô sản xuất");
            assertThat(r2.get("Mã đối tượng")).isEqualTo("LOT-002");
        }

        // 6. Refresh danh sách qua API GET: Xác nhận xuất hiện sự kiện EXPORT_ACTIVITY_LOG
        MvcResult refreshResult = mockMvc.perform(get("/api/v1/organizations/activity-logs")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andReturn();

        // 7. Xác nhận bản ghi EXPORT_ACTIVITY_LOG mới sinh KHÔNG nằm trong nội dung CSV của chính lần xuất đó
        assertThat(csvString).doesNotContain("EXPORT_ACTIVITY_LOG");
    }

    @Test
    @DisplayName("Dữ liệu vượt ngưỡng tạo job và snapshot bất biến, không lộ job sang tenant khác")
    void testE2E_AsyncSnapshotAndTenantScopedJob() throws Exception {
        activityLogRepository.saveAll(List.of(
                createLog(orgIdA, "manager_a", "Nguyễn Văn An", "UPDATE_LOT", "LOT", "LOT-1", LocalDateTime.now()),
                createLog(orgIdA, "manager_a", "Nguyễn Văn An", "UPDATE_LOT", "LOT", "LOT-2", LocalDateTime.now()),
                createLog(orgIdA, "manager_a", "Nguyễn Văn An", "UPDATE_LOT", "LOT", "LOT-3", LocalDateTime.now())));

        ActivityLogExportFilterRequest filter = new ActivityLogExportFilterRequest();
        MvcResult result = mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.mode").value("ASYNC"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.recordCount").value(3))
                .andExpect(jsonPath("$.data.exportId").exists())
                .andReturn();

        UUID jobId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("exportId").asText());
        ActivityLogExportJob job = exportJobRepository.findById(jobId).orElseThrow();
        List<ActivityLogExportItem> snapshot = exportItemRepository
                .findByJobId(jobId, PageRequest.of(0, 10)).getContent();

        assertThat(job.getOrganizationId()).isEqualTo(orgIdA);
        assertThat(snapshot).hasSize(3);
        assertThat(snapshot).extracting(ActivityLogExportItem::getActionType)
                .doesNotContain("EXPORT_ACTIVITY_LOG");

        mockMvc.perform(get("/api/v1/organizations/activity-logs/exports/{exportId}", jobId)
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgB, null, userManagerOrgB.getAuthorities())))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Kiểm tra trường hợp Không có dữ liệu: Preview trả count 0, Export trả HTTP 400 và không ghi audit log")
    void testE2E_EmptyData_ShouldReturn400_NoAudit() throws Exception {
        ActivityLogExportFilterRequest emptyFilter = new ActivityLogExportFilterRequest();
        emptyFilter.setAction("NON_EXISTING_ACTION");

        // 1. Preview trả về count = 0
        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports/preview")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyFilter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0))
                .andExpect(jsonPath("$.data.mode").value("DIRECT"));

        // 2. Export bị từ chối với HTTP 400
        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyFilter)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không có nhật ký hoạt động trong phạm vi lọc."));

        // 3. Không có bản ghi audit nào được sinh thêm
        long exportAuditCount = activityLogRepository.findAll().stream()
                .filter(log -> "EXPORT_ACTIVITY_LOG".equals(log.getAction()))
                .count();
        assertThat(exportAuditCount).isZero();
    }

    @Test
    @DisplayName("Kiểm tra Tenant Isolation: Tuyệt đối không xuất dữ liệu của tổ chức khác")
    void testE2E_TenantIsolation() throws Exception {
        ActivityLog logOrgA = createLog(orgIdA, "manager_a", "Nguyễn Văn A", "UPDATE_LOT", "PRODUCTION_LOT", "LOT-A", LocalDateTime.now());
        ActivityLog logOrgB = createLog(orgIdB, "manager_b", "Trần Văn B (Khác Tổ Chức)", "UPDATE_LOT", "PRODUCTION_LOT", "LOT-B", LocalDateTime.now());
        activityLogRepository.saveAll(List.of(logOrgA, logOrgB));

        // Org A xuất dữ liệu không có bộ lọc
        ActivityLogExportFilterRequest filter = new ActivityLogExportFilterRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andReturn();

        byte[] csvBytes = result.getResponse().getContentAsByteArray();
        String csvString = new String(csvBytes, StandardCharsets.UTF_8);

        assertThat(csvString).contains("manager_a");
        assertThat(csvString).contains("LOT-A");
        assertThat(csvString).doesNotContain("manager_b");
        assertThat(csvString).doesNotContain("LOT-B");
        assertThat(csvString).doesNotContain("Trần Văn B (Khác Tổ Chức)");
    }

    @Test
    @DisplayName("Kiểm tra Phân quyền (Authorization): VT-03 và Anonymous bị từ chối truy cập 403")
    void testE2E_Authorization() throws Exception {
        ActivityLogExportFilterRequest filter = new ActivityLogExportFilterRequest();

        // 1. VT-03 bị từ chối trên cả 3 endpoint
        mockMvc.perform(get("/api/v1/organizations/activity-logs")
                .with(authentication(new UsernamePasswordAuthenticationToken(userRecorderOrgA, null, userRecorderOrgA.getAuthorities())))
                .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports/preview")
                .with(authentication(new UsernamePasswordAuthenticationToken(userRecorderOrgA, null, userRecorderOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                .with(authentication(new UsernamePasswordAuthenticationToken(userRecorderOrgA, null, userRecorderOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isForbidden());

        // 2. Anonymous bị từ chối
        mockMvc.perform(get("/api/v1/organizations/activity-logs").with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports/preview")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Kiểm tra CSV Formula Injection và Tiếng Việt UTF-8: Ký tự nguy hiểm được chèn nháy đơn, tiếng Việt hiển thị chính xác")
    void testE2E_FormulaInjection_And_Utf8Vietnamese() throws Exception {
        ActivityLog logWithFormulas = ActivityLog.builder()
                .organizationId(orgIdA)
                .userId(UUID.randomUUID())
                .username("=cmd|' /C calc'!A0")
                .fullName(" @Võ Thị Sáu - Giám đốc HTX Nông nghiệp")
                .action("+UPDATE_STATUS")
                .description("Thao tác kiểm tra công thức")
                .entityType("-SPECIAL_ENTITY")
                .entityId("\t=1+1")
                .createdAt(LocalDateTime.of(2026, 9, 14, 10, 0))
                .build();

        activityLogRepository.save(logWithFormulas);

        ActivityLogExportFilterRequest filter = new ActivityLogExportFilterRequest();
        filter.setStartDate(java.time.LocalDate.of(2026, 9, 14));
        filter.setEndDate(java.time.LocalDate.of(2026, 9, 14));

        MvcResult result = mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andReturn();

        byte[] csvBytes = result.getResponse().getContentAsByteArray();
        String csvString = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);

        // Kiểm tra tiếng Việt hiển thị chuẩn xác
        assertThat(csvString).contains("Võ Thị Sáu - Giám đốc HTX Nông nghiệp");

        // Kiểm tra chống formula injection với dấu nháy đơn đứng đầu chuỗi
        assertThat(csvString).contains("'=cmd|' /C calc'!A0");
        assertThat(csvString).contains("' @Võ Thị Sáu");
        assertThat(csvString).contains("'+UPDATE_STATUS");
        assertThat(csvString).contains("'-SPECIAL_ENTITY");
        assertThat(csvString).contains("'\t=1+1");
    }

    @Test
    @DisplayName("Kiểm tra lọc theo ngày không hợp lệ: startDate > endDate trả HTTP 400")
    void testE2E_InvalidDateRange_ShouldReturn400() throws Exception {
        ActivityLogExportFilterRequest filter = new ActivityLogExportFilterRequest();
        filter.setStartDate(java.time.LocalDate.of(2026, 9, 20));
        filter.setEndDate(java.time.LocalDate.of(2026, 9, 10));

        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports/preview")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ngày bắt đầu không được sau ngày kết thúc."));

        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ngày bắt đầu không được sau ngày kết thúc."));
    }

    @Test
    @DisplayName("Kiểm tra giới hạn khoảng thời gian: Vượt quá maxRangeDays (365 ngày) trả HTTP 400 và không ghi audit")
    void testE2E_ExceedsMaxRangeDays_ShouldReturn400_NoAudit() throws Exception {
        ActivityLogExportFilterRequest filter = new ActivityLogExportFilterRequest();
        filter.setStartDate(java.time.LocalDate.of(2024, 1, 1));
        filter.setEndDate(java.time.LocalDate.of(2025, 1, 2)); // 367 days > 365 days

        // 1. Preview bị từ chối với 400
        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports/preview")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Khoảng thời gian xuất nhật ký không được vượt quá 365 ngày."));

        // 2. Export bị từ chối với 400
        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Khoảng thời gian xuất nhật ký không được vượt quá 365 ngày."));

        // 3. Khẳng định không tạo bất kỳ audit log nào
        long auditCount = activityLogRepository.findAll().stream()
                .filter(log -> "EXPORT_ACTIVITY_LOG".equals(log.getAction()))
                .count();
        assertThat(auditCount).isZero();
    }

    @Test
    @DisplayName("Kiểm tra tính bất biến của Snapshot (Immutability): Log mới chèn sau snapshot không lọt vào tệp worker xuất")
    void testE2E_SnapshotImmutability_NewLogsAfterSnapshotNotIncludedInWorkerOutput() throws Exception {
        // 1. Tạo 3 log ban đầu cho Org A (vượt directLimit=2 để kích hoạt ASYNC)
        activityLogRepository.saveAll(List.of(
                createLog(orgIdA, "manager_a", "Nguyễn Văn An", "ACTION_1", "LOT", "LOT-1", LocalDateTime.now().minusHours(3)),
                createLog(orgIdA, "manager_a", "Nguyễn Văn An", "ACTION_2", "LOT", "LOT-2", LocalDateTime.now().minusHours(2)),
                createLog(orgIdA, "manager_a", "Nguyễn Văn An", "ACTION_3", "LOT", "LOT-3", LocalDateTime.now().minusHours(1))));

        ActivityLogExportFilterRequest filter = new ActivityLogExportFilterRequest();

        // 2. Gửi request export để chốt snapshot
        MvcResult result = mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.mode").value("ASYNC"))
                .andExpect(jsonPath("$.data.recordCount").value(3))
                .andReturn();

        UUID jobId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("exportId").asText());

        // 3. Chèn thêm log thứ 4 VÀO DATABASE SAU KHI SNAPSHOT ĐÃ TẠO
        ActivityLog postSnapshotLog = createLog(orgIdA, "manager_a", "Nguyễn Văn An", "POST_SNAPSHOT_ACTION", "LOT", "LOT-4", LocalDateTime.now());
        activityLogRepository.save(postSnapshotLog);

        // 4. Cho worker xử lý job
        activityLogExportWorker.process(jobId);

        // 5. Tải file CSV qua endpoint download và kiểm tra nội dung
        MvcResult downloadResult = mockMvc.perform(get("/api/v1/organizations/activity-logs/exports/{jobId}/download", jobId)
                .with(authentication(new UsernamePasswordAuthenticationToken(userManagerOrgA, null, userManagerOrgA.getAuthorities())))
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        byte[] csvBytes = downloadResult.getResponse().getContentAsByteArray();
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        // Khẳng định: File CSV chứa 3 log ban đầu, TUYỆT ĐỐI KHÔNG chứa log thứ 4 mới chèn sau snapshot
        assertThat(csvContent).contains("ACTION_1", "ACTION_2", "ACTION_3");
        assertThat(csvContent).doesNotContain("POST_SNAPSHOT_ACTION");
        assertThat(csvContent).doesNotContain("LOT-4");
    }
}
