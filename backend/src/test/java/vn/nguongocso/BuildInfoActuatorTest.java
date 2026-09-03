package vn.nguongocso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-04 — Release/Deployment Traceability.
 *
 * Xác minh rằng bản build luôn mang thông tin truy vết:
 *
 *     Version  → build.version  (pom.xml)
 *     Commit   → build.git.commit (CI truyền -Dgit.commit=&lt;github.sha&gt;)
 *     Build Time → build.time
 *
 * và thông tin này được expose qua {@code /actuator/info} mà KHÔNG thay đổi
 * security policy hiện tại:
 *
 *     - /actuator/info VẪN yêu cầu xác thực (anyRequest().authenticated()).
 *     - Chỉ /actuator/health là public.
 *
 * Build-info được sinh bởi spring-boot-maven-plugin:build-info ở phase
 * process-resources, nên goal này chạy trong cả {@code mvn test} và
 * {@code mvn package} (bao gồm Docker build với -Pprod).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BuildInfoActuatorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BuildProperties buildProperties;

    @Test
    @DisplayName("TC-04: build-info sinh ra với version, build time và git commit")
    void buildInfoShouldContainVersionBuildTimeAndCommit() {
        assertThat(buildProperties).isNotNull();
        assertThat(buildProperties.getVersion()).isEqualTo("01");
        assertThat(buildProperties.getTime()).isNotNull();
        // CI truyền -Dgit.commit=<github.sha>; build local mặc định "local".
        assertThat(String.valueOf(buildProperties.get("git.commit"))).isNotBlank();
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("TC-04: /actuator/info trả build info (version/time/commit) cho user đã xác thực")
    void actuatorInfoShouldExposeBuildInfoToAuthenticatedUser() throws Exception {
        String commit = String.valueOf(buildProperties.get("git.commit"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.build.version").value("01"))
                .andExpect(jsonPath("$.build.time").isNotEmpty())
                // Commit phải xuất hiện trong response (bất kể shape lồng nhau).
                .andExpect(content().string(containsString(commit)));
    }

    @Test
    @DisplayName("TC-04: /actuator/info vẫn được bảo vệ — không token → 403 (security giữ nguyên)")
    void actuatorInfoShouldRemainProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isForbidden());
    }
}
