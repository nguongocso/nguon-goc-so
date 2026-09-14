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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.alert.dto.request.ActivityLogExportFilterRequest;
import vn.nguongocso.alert.entity.ActivityLog;
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
@Transactional
public class ActivityLogExportE2EIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityLogRepository activityLogRepository;

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
                .entityType(entityType)
                .entityId(entityId)
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
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(new InputStreamReader(new ByteArrayInputStream(csvBytes, 3, csvBytes.length - 3), StandardCharsets.UTF_8))) {
            List<String> headerNames = parser.getHeaderNames();
            assertThat(headerNames).containsExactly(
                    "occurredAt",
                    "actorName",
                    "actorUsername",
                    "actorRole",
                    "actionType",
                    "objectType",
                    "objectIdentifier",
                    "beforeValue",
                    "afterValue"
            );

            List<CSVRecord> records = parser.getRecords();
            assertThat(records).hasSize(2);

            CSVRecord r1 = records.get(0);
            assertThat(r1.get("actorName")).isEqualTo("Nguyễn Văn An");
            assertThat(r1.get("actorUsername")).isEqualTo("manager_a");
            assertThat(r1.get("actorRole")).isEqualTo("null");
            assertThat(r1.get("actionType")).isEqualTo("UPDATE_PRODUCTION_LOT");
            assertThat(r1.get("objectType")).isEqualTo("PRODUCTION_LOT");
            assertThat(r1.get("objectIdentifier")).isEqualTo("LOT-001");
            assertThat(r1.get("beforeValue")).isEqualTo("null");
            assertThat(r1.get("afterValue")).isEqualTo("null");

            CSVRecord r2 = records.get(1);
            assertThat(r2.get("actorName")).isEqualTo("Trần Thị Bình");
            assertThat(r2.get("actorUsername")).isEqualTo("recorder_a");
            assertThat(r2.get("actionType")).isEqualTo("UPDATE_PRODUCTION_LOT");
            assertThat(r2.get("objectIdentifier")).isEqualTo("LOT-002");
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
}
