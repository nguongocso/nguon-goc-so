package vn.nguongocso.unit.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.organization.controller.InvitationController;
import vn.nguongocso.organization.dto.request.AcceptInvitationRequest;
import vn.nguongocso.organization.dto.request.CreateInvitationRequest;
import vn.nguongocso.organization.dto.response.AcceptInvitationResponse;
import vn.nguongocso.organization.dto.response.InvitationPublicResponse;
import vn.nguongocso.organization.dto.response.InvitationResponse;
import vn.nguongocso.organization.service.InvitationService;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvitationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createInvitation_shouldReturnCreated_whenManager() throws Exception {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("member.new@gmail.com");
        request.setRoleId(3);
        request.setExpiryDays(7);

        CustomUserDetails mockUser = mock(CustomUserDetails.class);
        when(mockUser.getUsername()).thenReturn("manager1");
        when(mockUser.getAuthorities()).thenAnswer(inv -> 
            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VT-02"))
        );

        InvitationResponse response = InvitationResponse.builder()
                .id(UUID.randomUUID())
                .email("member.new@gmail.com")
                .organizationName("HTX Sạch")
                .roleName("Người ghi sự kiện")
                .status("PENDING")
                .token("test_token")
                .build();

        when(invitationService.createInvitation(any(CreateInvitationRequest.class), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/organization/invitations")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.email").value("member.new@gmail.com"))
                .andExpect(jsonPath("$.data.organizationName").value("HTX Sạch"));
    }

    @Test
    void getInvitationDetails_shouldReturnOk_withoutAuth() throws Exception {
        // Given
        String token = "test_token";
        InvitationPublicResponse response = InvitationPublicResponse.builder()
                .email("member.new@gmail.com")
                .organizationName("HTX Sạch")
                .roleName("Người ghi sự kiện")
                .status("PENDING")
                .expiryDate(LocalDateTime.now().plusDays(5))
                .build();

        when(invitationService.getInvitationDetails(token)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/public/organization/invitations/{token}", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("member.new@gmail.com"))
                .andExpect(jsonPath("$.data.organizationName").value("HTX Sạch"));
    }

    @Test
    void acceptInvitation_shouldReturnOk_withoutAuth() throws Exception {
        // Given
        String token = "test_token";
        AcceptInvitationRequest request = new AcceptInvitationRequest();
        request.setUserName("newuser");
        request.setPassword("SecureP@ss123");
        request.setFullName("Nguyễn Văn A");

        AcceptInvitationResponse response = AcceptInvitationResponse.builder()
                .userId(UUID.randomUUID())
                .userName("newuser")
                .fullName("Nguyễn Văn A")
                .organizationName("HTX Sạch")
                .roleCode("VT-03")
                .build();

        when(invitationService.acceptInvitation(eq(token), any(AcceptInvitationRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/public/organization/invitations/{token}/accept", token)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userName").value("newuser"))
                .andExpect(jsonPath("$.data.roleCode").value("VT-03"));
    }
}
