package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.response.AttachmentResponse;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private FarmLogAttachmentRepository attachmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AttachmentService attachmentService;

    private UUID orgId;
    private UUID userId;
    private UUID logId;

    private CustomUserDetails userDetails;
    private FarmLog farmLog;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                attachmentService,
                "maxFileSize",
                MAX_FILE_SIZE
        );

        ReflectionTestUtils.setField(
                attachmentService,
                "baseDir",
                System.getProperty("java.io.tmpdir")
        );

        ReflectionTestUtils.setField(
                attachmentService,
                "farmLogRelativePath",
                "farm-logs"
        );

        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        logId = UUID.randomUUID();

        Organization organization = new Organization();
        organization.setOrganizationId(orgId);

        ProductionLot productionLot = new ProductionLot();
        productionLot.setId(UUID.randomUUID());
        productionLot.setOrganization(organization);

        farmLog = new FarmLog();
        farmLog.setId(logId);
        farmLog.setProductionLotId(productionLot);

        user = new User();
        user.setUserId(userId);
        user.setFullName("Test user");

        userDetails = mock(CustomUserDetails.class);
    }

    @Test
    void uploadAttachment_shouldSuccess_whenJpgFileValid()
            throws Exception {

        // Stub userDetails trong test
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getOrganizationId()).thenReturn(orgId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        when(farmLogRepository.findById(logId))
                .thenReturn(Optional.of(farmLog));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(attachmentRepository.save(any(FarmLogAttachment.class)))
                .thenAnswer(invocation -> {
                    FarmLogAttachment a = invocation.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        AttachmentResponse response =
                attachmentService.uploadAttachment(
                        logId,
                        file,
                        "test",
                        userDetails
                );

        assertThat(response).isNotNull();
        assertThat(response.getFileName()).isEqualTo("image.jpg");
        assertThat(response.getFileSize()).isEqualTo(file.getSize());
        assertThat(response.getFileType()).isEqualTo("image/jpeg");
        assertThat(response.getDescription()).isEqualTo("test");
    }

    @Test
    void uploadAttachment_shouldSuccess_whenPdfFileValid()
            throws Exception {

        // Stub userDetails trong test
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getOrganizationId()).thenReturn(orgId);


        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        when(farmLogRepository.findById(logId))
                .thenReturn(Optional.of(farmLog));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(attachmentRepository.save(any(FarmLogAttachment.class)))
                .thenAnswer(invocation -> {
                    FarmLogAttachment a = invocation.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        AttachmentResponse response =
                attachmentService.uploadAttachment(
                        logId,
                        file,
                        "PDF document",
                        userDetails
                );

        assertThat(response).isNotNull();
        assertThat(response.getFileName()).isEqualTo("document.pdf");
        assertThat(response.getFileType())
                .isEqualTo("application/pdf");
    }

    @Test
    void uploadAttachment_shouldThrow_whenFileTooLarge() {

        when(userDetails.getOrganizationId()).thenReturn(orgId);

        byte[] largeContent = new byte[6 * 1024 * 1024];

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        when(farmLogRepository.findById(logId))
                .thenReturn(Optional.of(farmLog));

        assertThatThrownBy(() ->
                attachmentService.uploadAttachment(
                        logId,
                        file,
                        null,
                        userDetails
                )
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "File vượt quá dung lượng"
                );
    }

    @Test
    void uploadAttachment_shouldThrow_whenUnsupportedFileType() {

        when(userDetails.getOrganizationId()).thenReturn(orgId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "virus.exe",
                "application/x-msdownload",
                "bad content".getBytes()
        );

        when(farmLogRepository.findById(logId))
                .thenReturn(Optional.of(farmLog));

        assertThatThrownBy(() ->
                attachmentService.uploadAttachment(
                        logId,
                        file,
                        null,
                        userDetails
                )
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "Loại file không hỗ trợ"
                );
    }

    @Test
    void uploadAttachment_shouldThrow_whenFileEmpty() {

        when(userDetails.getOrganizationId()).thenReturn(orgId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        when(farmLogRepository.findById(logId))
                .thenReturn(Optional.of(farmLog));

        assertThatThrownBy(() ->
                attachmentService.uploadAttachment(
                        logId,
                        file,
                        null,
                        userDetails
                )
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "File không được để trống"
                );
    }

    @Test
    void uploadAttachment_shouldThrow_whenFarmLogNotFound() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        when(farmLogRepository.findById(logId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                attachmentService.uploadAttachment(
                        logId,
                        file,
                        null,
                        userDetails
                )
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "Không tìm thấy nhật ký canh tác"
                );
    }

    @Test
    void uploadAttachment_shouldThrow_whenNotBelongToOrganization() {
        when(userDetails.getOrganizationId()).thenReturn(UUID.randomUUID()); // khác với otherOrgId

        UUID otherOrgId = UUID.randomUUID();
        Organization otherOrganization = new Organization();
        otherOrganization.setOrganizationId(otherOrgId);
        ProductionLot otherProductionLot = new ProductionLot();
        otherProductionLot.setOrganization(otherOrganization);
        FarmLog otherFarmLog = new FarmLog();
        otherFarmLog.setId(logId);
        otherFarmLog.setProductionLotId(otherProductionLot);

        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "test".getBytes());
        when(farmLogRepository.findById(logId)).thenReturn(Optional.of(otherFarmLog));

        assertThatThrownBy(() -> attachmentService.uploadAttachment(logId, file, null, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thuộc tổ chức của bạn");
    }

}