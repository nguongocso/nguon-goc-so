package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.AssignProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CloseProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRecallRequest;
import vn.nguongocso.farm.dto.request.UpdateProductFeedbackProcessingRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.ProductFeedbackServiceImpl;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.recall.enums.RecallRequestStatus;
import vn.nguongocso.recall.repository.RecallRequestRepository;
import vn.nguongocso.recall.service.RecallRequestService;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;
import vn.nguongocso.trace.repository.TraceCodeRepository;

class ProductFeedbackProcessingServiceTest {

    private ProductFeedbackRepository productFeedbackRepository;
    private RecallRequestRepository recallRequestRepository;
    private OrganizationUserRepository organizationUserRepository;
    private RecallRequestService recallRequestService;
    private UserRepository userRepository;
    private ProductFeedbackServiceImpl service;
    private CustomUserDetails currentUser;
    private UUID organizationId;
    private UUID feedbackId;
    private ProductFeedback feedback;

    @BeforeEach
    void setUp() {
        productFeedbackRepository = mock(ProductFeedbackRepository.class);
        recallRequestRepository = mock(RecallRequestRepository.class);
        organizationUserRepository = mock(OrganizationUserRepository.class);
        recallRequestService = mock(RecallRequestService.class);
        userRepository = mock(UserRepository.class);
        service = new ProductFeedbackServiceImpl(
                productFeedbackRepository,
                mock(ProductionLotRepository.class),
                mock(TraceCodeRepository.class),
                recallRequestRepository,
                recallRequestService,
                organizationUserRepository,
                userRepository,
                mock(org.springframework.context.ApplicationEventPublisher.class),
                mock(NotificationService.class),
                mock(ProductFeedbackLookupCodeGenerator.class));

        organizationId = UUID.randomUUID();
        feedbackId = UUID.randomUUID();
        Organization organization = Organization.builder()
                .organizationId(organizationId)
                .name("HTX kiểm thử")
                .build();
        ProductionLot lot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô kiểm thử")
                .organization(organization)
                .build();
        feedback = ProductFeedback.builder()
                .id(feedbackId)
                .productionLot(lot)
                .content("Nghi ngờ chất lượng sản phẩm")
                .status(ProductFeedbackStatus.NEW)
                .severity(ProductFeedbackSeverity.INFORMATION)
                .build();

        currentUser = mock(CustomUserDetails.class);
        when(currentUser.getRoleCode()).thenReturn("VT-02");
        when(currentUser.getOrganizationId()).thenReturn(organizationId);
        when(productFeedbackRepository.findByIdAndProductionLot_Organization_OrganizationId(
                feedbackId, organizationId)).thenReturn(Optional.of(feedback));
        when(productFeedbackRepository.save(any(ProductFeedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void assign_shouldMoveNewFeedbackToInProgress_whenAssigneeIsEligible() {
        UUID assigneeId = UUID.randomUUID();
        User assignee = User.builder().userId(assigneeId).fullName("Người ghi sự kiện").build();
        Role eventRecorderRole = new Role(3, "VT-03", "Người ghi sự kiện");
        OrganizationUser membership = new OrganizationUser();
        membership.setUser(assignee);
        membership.setRole(eventRecorderRole);
        membership.setStatus(OrganizationUserStatus.ACTIVE);
        when(organizationUserRepository.findByOrganization_OrganizationIdAndUser_UserId(
                organizationId, assigneeId)).thenReturn(Optional.of(membership));

        AssignProductFeedbackRequest request = new AssignProductFeedbackRequest();
        request.setAssignedToUserId(assigneeId);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);
            ProductFeedbackResponse response = service.assign(feedbackId, request);

            assertThat(response.getStatus()).isEqualTo(ProductFeedbackStatus.IN_PROGRESS);
            assertThat(response.getAssignedToUserId()).isEqualTo(assigneeId);
            assertThat(feedback.getAssignedAt()).isNotNull();
        }
    }

    @Test
    void assign_shouldRejectActiveMember_whenAssigneeIsNotEventRecorder() {
        UUID assigneeId = UUID.randomUUID();
        User assignee = User.builder().userId(assigneeId).fullName("Quản lý HTX").build();
        Role managerRole = new Role(2, "VT-02", "Quản lý tổ chức");
        OrganizationUser membership = new OrganizationUser();
        membership.setUser(assignee);
        membership.setRole(managerRole);
        membership.setStatus(OrganizationUserStatus.ACTIVE);
        when(organizationUserRepository.findByOrganization_OrganizationIdAndUser_UserId(
                organizationId, assigneeId)).thenReturn(Optional.of(membership));

        AssignProductFeedbackRequest request = new AssignProductFeedbackRequest();
        request.setAssignedToUserId(assigneeId);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);
            assertThatThrownBy(() -> service.assign(feedbackId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Người được chọn không đủ điều kiện xử lý phản ánh");
        }
    }

    @Test
    void assign_shouldRejectEventRecorder_whenMembershipIsInactive() {
        UUID assigneeId = UUID.randomUUID();
        User assignee = User.builder().userId(assigneeId).fullName("Người ghi sự kiện").build();
        Role eventRecorderRole = new Role(3, "VT-03", "Người ghi sự kiện");
        OrganizationUser membership = new OrganizationUser();
        membership.setUser(assignee);
        membership.setRole(eventRecorderRole);
        membership.setStatus(OrganizationUserStatus.INACTIVE);
        when(organizationUserRepository.findByOrganization_OrganizationIdAndUser_UserId(
                organizationId, assigneeId)).thenReturn(Optional.of(membership));

        AssignProductFeedbackRequest request = new AssignProductFeedbackRequest();
        request.setAssignedToUserId(assigneeId);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);
            assertThatThrownBy(() -> service.assign(feedbackId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Người được chọn không đủ điều kiện xử lý phản ánh");
        }
    }

    @Test
    void updateProcessing_shouldRejectCounterfeitClassificationWithoutTraceCode() {
        feedback.setStatus(ProductFeedbackStatus.IN_PROGRESS);
        feedback.setAssignedTo(User.builder().userId(UUID.randomUUID()).build());
        UpdateProductFeedbackProcessingRequest request = new UpdateProductFeedbackProcessingRequest();
        request.setSeverity(ProductFeedbackSeverity.COUNTERFEIT_SUSPECTED);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);
            assertThatThrownBy(() -> service.updateProcessing(feedbackId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Phản ánh phải được liên kết với mã tem cụ thể");
        }
    }

    @Test
    void close_shouldRejectWhenProcessingContentIsMissing() {
        feedback.setStatus(ProductFeedbackStatus.IN_PROGRESS);
        feedback.setAssignedTo(User.builder().userId(UUID.randomUUID()).build());
        CloseProductFeedbackRequest request = new CloseProductFeedbackRequest();
        request.setCloseReason("Đã kiểm tra");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);
            assertThatThrownBy(() -> service.close(feedbackId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Vui lòng nhập nội dung xử lý trước khi đóng phản ánh");
        }
        verify(productFeedbackRepository, org.mockito.Mockito.never()).save(feedback);
    }

    @Test
    void close_shouldRejectWhenLinkedRecallIsPending() {
        feedback.setStatus(ProductFeedbackStatus.ESCALATED_TO_RECALL);
        feedback.setAssignedTo(User.builder().userId(UUID.randomUUID()).build());
        feedback.setProcessingContent("Đã xác minh hồ sơ");
        when(recallRequestRepository.existsBySourceFeedback_IdAndStatus(
                feedbackId, RecallRequestStatus.PENDING)).thenReturn(true);
        CloseProductFeedbackRequest request = new CloseProductFeedbackRequest();
        request.setCloseReason("Đã kiểm tra");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);
            assertThatThrownBy(() -> service.close(feedbackId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Phải xử lý xong đề nghị thu hồi trước khi đóng phản ánh");
        }
    }

    @Test
    void close_shouldStoreHandlerAndTimestamp_whenRequiredDataIsPresent() {
        UUID currentUserId = UUID.randomUUID();
        User manager = User.builder().userId(currentUserId).fullName("Quản lý xử lý").build();
        feedback.setStatus(ProductFeedbackStatus.IN_PROGRESS);
        feedback.setAssignedTo(manager);
        feedback.setProcessingContent("Đã đối chiếu hồ sơ");
        when(currentUser.getUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(manager));
        CloseProductFeedbackRequest request = new CloseProductFeedbackRequest();
        request.setCloseReason("Đã xử lý xong");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);
            ProductFeedbackResponse response = service.close(feedbackId, request);

            assertThat(response.getStatus()).isEqualTo(ProductFeedbackStatus.CLOSED);
            assertThat(response.getClosedByUserId()).isEqualTo(currentUserId);
            assertThat(response.getClosedAt()).isNotNull();
        }
    }

    @Test
    void createRecall_shouldRejectDuplicatePendingRequest() {
        feedback.setStatus(ProductFeedbackStatus.IN_PROGRESS);
        feedback.setSeverity(ProductFeedbackSeverity.QUALITY_SUSPECTED);
        feedback.setAssignedTo(User.builder().userId(UUID.randomUUID()).build());
        when(recallRequestRepository.existsBySourceFeedback_IdAndStatus(
                feedbackId, RecallRequestStatus.PENDING)).thenReturn(true);
        CreateProductFeedbackRecallRequest request = new CreateProductFeedbackRecallRequest();
        request.setShipmentId(UUID.randomUUID());
        request.setReason("Nghi ngờ chất lượng");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);
            assertThatThrownBy(() -> service.createRecallRequest(feedbackId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Phản ánh đã có đề nghị thu hồi đang chờ duyệt");
        }
    }

    @Test
    void createRecall_shouldLinkFeedbackAndEscalateStatus() {
        UUID recallId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        feedback.setStatus(ProductFeedbackStatus.IN_PROGRESS);
        feedback.setSeverity(ProductFeedbackSeverity.QUALITY_SUSPECTED);
        feedback.setAssignedTo(User.builder().userId(UUID.randomUUID()).build());
        CreateProductFeedbackRecallRequest request = new CreateProductFeedbackRecallRequest();
        request.setShipmentId(shipmentId);
        request.setReason("Nghi ngờ chất lượng");
        RecallRequestResponse recallResponse = RecallRequestResponse.builder()
                .id(recallId)
                .sourceFeedbackId(feedbackId)
                .status("PENDING")
                .build();
        when(recallRequestService.createFromFeedback(
                feedback, request.getShipmentId(), request.getReason(), null, currentUser)).thenReturn(recallResponse);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);
            RecallRequestResponse response = service.createRecallRequest(feedbackId, request);

            assertThat(response.getSourceFeedbackId()).isEqualTo(feedbackId);
            assertThat(feedback.getStatus()).isEqualTo(ProductFeedbackStatus.ESCALATED_TO_RECALL);
            verify(productFeedbackRepository).save(feedback);
        }
    }
}
