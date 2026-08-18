package vn.nguongocso.unit.help;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.help.dto.response.HelpContentResponse;
import vn.nguongocso.help.entity.HelpContent;
import vn.nguongocso.help.repository.HelpContentRepository;
import vn.nguongocso.help.service.impl.HelpServiceImpl;

/**
 * Unit test cho {@link HelpServiceImpl} (NCL-01-CN-006).
 */
@ExtendWith(MockitoExtension.class)
class HelpServiceImplTest {

    private static final String SCREEN_KEY = "farm-log-create";
    private static final String ROLE_CODE_VT03 = "VT-03";
    private static final String GENERAL_ROLE_CODE = "GENERAL";

    private static final String STEPS_JSON = "[\"Bước 1\",\"Bước 2\"]";
    private static final List<String> STEPS = List.of("Bước 1", "Bước 2");

    @Mock
    private HelpContentRepository helpContentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private HelpServiceImpl helpService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        CustomUserDetails currentUser = mock(CustomUserDetails.class);
        // lenient: test screenKey rỗng sẽ không dùng đến stub này
        lenient().when(currentUser.getRoleCode()).thenReturn(ROLE_CODE_VT03);
        lenient().when(SecurityUtils.getCurrentUserDetails()).thenReturn(currentUser);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void getHelp_returnsRoleSpecificContent_whenRoleRowExists() throws Exception {
        HelpContent roleContent = buildHelpContent(SCREEN_KEY, ROLE_CODE_VT03, "Hướng dẫn VT-03");
        when(helpContentRepository.findByScreenKeyAndRoleCodeOrderBySortOrderAsc(SCREEN_KEY, ROLE_CODE_VT03))
                .thenReturn(List.of(roleContent));
        doReturn(STEPS).when(objectMapper)
                .readValue(anyString(), any(TypeReference.class));

        HelpContentResponse response = helpService.getHelp(SCREEN_KEY);

        assertThat(response).isNotNull();
        assertThat(response.getScreenKey()).isEqualTo(SCREEN_KEY);
        assertThat(response.getRoleCode()).isEqualTo(ROLE_CODE_VT03);
        assertThat(response.getTitle()).isEqualTo("Hướng dẫn VT-03");
        assertThat(response.getSteps()).containsExactly("Bước 1", "Bước 2");
    }

    @Test
    void getHelp_fallsBackToGeneralContent_whenNoRoleSpecificRow() throws Exception {
        when(helpContentRepository.findByScreenKeyAndRoleCodeOrderBySortOrderAsc(SCREEN_KEY, ROLE_CODE_VT03))
                .thenReturn(List.of());

        HelpContent generalContent = buildHelpContent(SCREEN_KEY, GENERAL_ROLE_CODE, "Hướng dẫn chung");
        when(helpContentRepository.findByScreenKeyAndRoleCodeOrderBySortOrderAsc(SCREEN_KEY, GENERAL_ROLE_CODE))
                .thenReturn(List.of(generalContent));
        doReturn(STEPS).when(objectMapper)
                .readValue(anyString(), any(TypeReference.class));

        HelpContentResponse response = helpService.getHelp(SCREEN_KEY);

        assertThat(response).isNotNull();
        assertThat(response.getRoleCode()).isEqualTo(GENERAL_ROLE_CODE);
        assertThat(response.getTitle()).isEqualTo("Hướng dẫn chung");
        assertThat(response.getScreenKey()).isEqualTo(SCREEN_KEY);
        assertThat(response.getSteps()).containsExactly("Bước 1", "Bước 2");
    }

    @Test
    void getHelp_returnsNull_whenNoContentForScreen() {
        when(helpContentRepository.findByScreenKeyAndRoleCodeOrderBySortOrderAsc(anyString(), anyString()))
                .thenReturn(List.of());

        HelpContentResponse response = helpService.getHelp(SCREEN_KEY);

        assertThat(response).isNull();
    }

    @Test
    void getHelp_returnsNull_whenScreenKeyBlank() {
        HelpContentResponse response = helpService.getHelp("   ");

        assertThat(response).isNull();
    }

    @Test
    void getHelp_returnsEmptySteps_whenStepsJsonInvalid() throws Exception {
        HelpContent brokenContent = buildHelpContent(SCREEN_KEY, ROLE_CODE_VT03, "Hướng dẫn lỗi JSON");
        brokenContent.setSteps("not-valid-json");
        when(helpContentRepository.findByScreenKeyAndRoleCodeOrderBySortOrderAsc(SCREEN_KEY, ROLE_CODE_VT03))
                .thenReturn(List.of(brokenContent));
        doThrow(new JsonProcessingException("invalid") {
        }).when(objectMapper)
                .readValue(anyString(), any(TypeReference.class));

        HelpContentResponse response = helpService.getHelp(SCREEN_KEY);

        assertThat(response).isNotNull();
        assertThat(response.getSteps()).isEmpty();
        assertThat(response.getTitle()).isEqualTo("Hướng dẫn lỗi JSON");
    }

    private HelpContent buildHelpContent(String screenKey, String roleCode, String title) {
        HelpContent content = new HelpContent();
        content.setId(UUID.randomUUID());
        content.setScreenKey(screenKey);
        content.setRoleCode(roleCode);
        content.setTitle(title);
        content.setSteps(STEPS_JSON);
        content.setExampleData(null);
        content.setSortOrder(0);
        return content;
    }
}