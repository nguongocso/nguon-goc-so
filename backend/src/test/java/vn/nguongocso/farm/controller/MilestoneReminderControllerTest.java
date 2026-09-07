package vn.nguongocso.farm.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.farm.dto.response.MilestoneReminderResponse;
import vn.nguongocso.farm.dto.response.MilestoneScanResult;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;
import vn.nguongocso.farm.service.MilestoneReminderService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.permission.service.PermissionChecker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MilestoneReminderController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class MilestoneReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MilestoneReminderService milestoneReminderService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    private CustomUserDetails vt01Admin;
    private CustomUserDetails vt02Manager;
    private CustomUserDetails vt03Recorder;

    private CustomUserDetails createUserDetails(String username, String roleCode) {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName(username);
        user.setFullName("User " + username);
        user.setPasswordHash("password");

        Organization org = new Organization();
        org.setOrganizationId(UUID.randomUUID());
        org.setName("Tổ chức thử nghiệm");
        org.setCode("TEST_ORG");

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setOrganization(org);

        Role role = new Role();
        role.setCode(roleCode);
        role.setName("Role " + roleCode);

        return new CustomUserDetails(user, orgUser, role);
    }

    @BeforeEach
    void setUp() {
        vt01Admin = createUserDetails("admin", "VT-01");
        vt02Manager = createUserDetails("manager", "VT-02");
        vt03Recorder = createUserDetails("recorder", "VT-03");
    }

    @Test
    @DisplayName("POST /api/v1/milestone-reminders/scan - VT-01 kích hoạt quét thành công")
    void testTriggerScan_VT01_Success() throws Exception {
        MilestoneScanResult result = MilestoneScanResult.builder()
                .scannedLotsCount(3)
                .remindersCreatedCount(1)
                .message("Đã quét 3 lô sản xuất, tạo mới 1 nhắc việc quá hạn.")
                .build();

        when(milestoneReminderService.scanOverdueMilestones()).thenReturn(result);

        mockMvc.perform(post("/api/v1/milestone-reminders/scan")
                        .with(user(vt01Admin))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scannedLotsCount").value(3))
                .andExpect(jsonPath("$.data.remindersCreatedCount").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/milestone-reminders/scan - VT-03 không có quyền bị từ chối 403")
    void testTriggerScan_VT03_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/milestone-reminders/scan")
                        .with(user(vt03Recorder))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/milestone-reminders - VT-03 lấy danh sách nhắc việc thành công")
    void testGetReminders_VT03_Success() throws Exception {
        MilestoneReminderResponse reminder = MilestoneReminderResponse.builder()
                .id(UUID.randomUUID())
                .lotId(UUID.randomUUID())
                .lotName("Lô Lúa ST25")
                .milestoneId(1L)
                .milestoneName("Bón phân đợt một")
                .activityType("FERTILIZING")
                .overdueDays(3)
                .expectedDate(LocalDate.now().minusDays(3))
                .status(MilestoneReminderStatus.OPEN)
                .reminderDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<MilestoneReminderResponse> pageResponse = PageResponse.<MilestoneReminderResponse>builder()
                .items(List.of(reminder))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(milestoneReminderService.getReminders(any(), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/milestone-reminders")
                        .with(user(vt03Recorder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].milestoneName").value("Bón phân đợt một"))
                .andExpect(jsonPath("$.data.items[0].overdueDays").value(3))
                .andExpect(jsonPath("$.data.items[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /api/v1/milestone-reminders/my-active - VT-03 lấy nhắc việc đang mở")
    void testGetMyActiveReminders_VT03_Success() throws Exception {
        MilestoneReminderResponse reminder = MilestoneReminderResponse.builder()
                .id(UUID.randomUUID())
                .lotId(UUID.randomUUID())
                .lotName("Lô Lúa ST25")
                .milestoneId(1L)
                .milestoneName("Bón phân đợt một")
                .activityType("FERTILIZING")
                .overdueDays(3)
                .status(MilestoneReminderStatus.OPEN)
                .reminderDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(milestoneReminderService.getMyActiveReminders(any()))
                .thenReturn(List.of(reminder));

        mockMvc.perform(get("/api/v1/milestone-reminders/my-active")
                        .with(user(vt03Recorder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].milestoneName").value("Bón phân đợt một"))
                .andExpect(jsonPath("$.data[0].overdueDays").value(3))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"));
    }
}
