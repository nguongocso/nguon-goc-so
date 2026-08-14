package vn.nguongocso.unit.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.DuplicateResourceException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.mail.service.EmailService;
import vn.nguongocso.organization.dto.request.AcceptInvitationRequest;
import vn.nguongocso.organization.dto.request.CreateInvitationRequest;
import vn.nguongocso.organization.dto.response.AcceptInvitationResponse;
import vn.nguongocso.organization.dto.response.InvitationPublicResponse;
import vn.nguongocso.organization.dto.response.InvitationResponse;
import vn.nguongocso.organization.entity.Invitation;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.InvitationStatus;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.InvitationRepository;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.organization.service.impl.InvitationServiceImpl;

@ExtendWith(MockitoExtension.class)
public class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    private CustomUserDetails currentUser;
    private User creator;
    private Organization organization;
    private Role role;
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setUserId(UUID.randomUUID());
        creator.setUserName("manager1");
        creator.setFullName("HTX Manager");

        organization = new Organization();
        organization.setOrganizationId(orgId);
        organization.setName("HTX Nông Sản Sạch");

        role = new Role();
        role.setRoleId(3);
        role.setCode("VT-03");
        role.setName("Người ghi sự kiện");

        currentUser = mock(CustomUserDetails.class);
    }

    @Test
    void createInvitation_shouldCreateSuccessfully_whenValidRequest() {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("member.new@gmail.com");
        request.setRoleId(3);
        request.setExpiryDays(7);

        when(currentUser.getOrganizationId()).thenReturn(orgId);
        when(currentUser.getUser()).thenReturn(creator);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(roleRepository.findById(3)).thenReturn(Optional.of(role));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(invitationRepository.findByEmailAndOrganizationOrganizationIdAndStatus(
                request.getEmail(), orgId, InvitationStatus.PENDING))
                .thenReturn(Collections.emptyList());

        // When
        InvitationResponse response = invitationService.createInvitation(request, currentUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("member.new@gmail.com");
        assertThat(response.getOrganizationName()).isEqualTo("HTX Nông Sản Sạch");
        assertThat(response.getRoleName()).isEqualTo("Người ghi sự kiện");
        assertThat(response.getStatus()).isEqualTo("PENDING");

        verify(invitationRepository, times(1)).save(any(Invitation.class));
        verify(emailService, times(1)).sendInvitationEmail(anyString(), anyString(), anyString(), anyString(), anyInt());
        verify(eventPublisher, times(1)).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    void createInvitation_shouldThrowException_whenEmailAlreadyMember() {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("existing@gmail.com");
        request.setRoleId(3);

        User existingUser = new User();
        existingUser.setUserId(UUID.randomUUID());

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setStatus(OrganizationUserStatus.ACTIVE);

        when(currentUser.getOrganizationId()).thenReturn(orgId);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(roleRepository.findById(3)).thenReturn(Optional.of(role));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUser));
        when(organizationUserRepository.findByOrganization_OrganizationIdAndUser_UserId(orgId, existingUser.getUserId()))
                .thenReturn(Optional.of(orgUser));

        // When & Then
        assertThatThrownBy(() -> invitationService.createInvitation(request, currentUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Người dùng có email này đã là thành viên của tổ chức");
    }

    @Test
    void getInvitationDetails_shouldReturnDetails_whenTokenValid() {
        // Given
        String token = "valid_token";
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email("member.new@gmail.com")
                .role(role)
                .token(token)
                .status(InvitationStatus.PENDING)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmail("member.new@gmail.com")).thenReturn(false);

        // When
        InvitationPublicResponse response = invitationService.getInvitationDetails(token);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("member.new@gmail.com");
        assertThat(response.getOrganizationName()).isEqualTo("HTX Nông Sản Sạch");
        assertThat(response.getRoleName()).isEqualTo("Người ghi sự kiện");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.isExistingUser()).isFalse();
    }

    @Test
    void getInvitationDetails_shouldChangeStatusToExpired_whenTokenExpired() {
        // Given
        String token = "expired_token";
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email("member.new@gmail.com")
                .role(role)
                .token(token)
                .status(InvitationStatus.PENDING)
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        // When & Then
        assertThatThrownBy(() -> invitationService.getInvitationDetails(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Thư mời đã quá hạn hoặc đã được sử dụng");

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        verify(invitationRepository, times(1)).save(invitation);
    }

    @Test
    void acceptInvitation_shouldCreateUserAndOrganizationUser_whenValid() {
        // Given
        String token = "valid_token";
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email("member.new@gmail.com")
                .role(role)
                .token(token)
                .status(InvitationStatus.PENDING)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .build();

        AcceptInvitationRequest request = new AcceptInvitationRequest();
        request.setUserName("newmember");
        request.setPassword("SecureP@ss123");
        request.setFullName("Nguyễn Văn A");

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("member.new@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUserName("newmember")).thenReturn(false);
        when(passwordEncoder.encode("SecureP@ss123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AcceptInvitationResponse response = invitationService.acceptInvitation(token, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserName()).isEqualTo("newmember");
        assertThat(response.getOrganizationName()).isEqualTo("HTX Nông Sản Sạch");
        assertThat(response.getRoleCode()).isEqualTo("VT-03");

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getUsedAt()).isNotNull();

        verify(userRepository, times(1)).save(any(User.class));
        verify(organizationUserRepository, times(1)).save(any(OrganizationUser.class));
        verify(invitationRepository, times(1)).save(invitation);
        verify(eventPublisher, times(1)).publishEvent(any(ActivityLogEvent.class));
    }
}
