package vn.nguongocso.farm.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.response.AttachmentResponse;
import vn.nguongocso.farm.service.AttachmentService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FarmLogAttachmentController.class)   // ✅ Đúng controller
@WithMockUser(roles = "VT-03")
@ActiveProfiles("test")
public class FarmLogAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttachmentService attachmentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

        @MockitoBean
        private PermissionChecker permissionChecker;

    private final UUID logId = UUID.randomUUID();

    @Test
    void uploadAttachment_shouldReturn201_whenValidFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", "test".getBytes()
        );

        AttachmentResponse response = AttachmentResponse.builder()
                .id(UUID.randomUUID())
                .farmLogId(logId)
                .fileName("image.jpg")
                .fileSize(4L)
                .fileType("image/jpeg")
                .build();

        when(attachmentService.uploadAttachment(any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/v1/farm-logs/{logId}/attachments", logId)
                        .file(file)
                        .param("description", "test")
                        .with(csrf()))   // ✅ Thêm CSRF
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("image.jpg"));
    }

    @Test
    void uploadAttachment_shouldReturn400_whenFileTooLarge() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", new byte[6 * 1024 * 1024]
        );

        when(attachmentService.uploadAttachment(any(), any(), any(), any()))
                .thenThrow(new BusinessException("File vượt quá dung lượng cho phép (5MB)"));

        mockMvc.perform(multipart("/api/v1/farm-logs/{logId}/attachments", logId)
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File vượt quá dung lượng cho phép (5MB)"));
    }

    @Test
    void uploadAttachment_shouldReturn400_whenUnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", "bad".getBytes()
        );

        when(attachmentService.uploadAttachment(any(), any(), any(), any()))
                .thenThrow(new BusinessException("Loại file không hỗ trợ. Chỉ chấp nhận JPG, PNG, PDF"));

        mockMvc.perform(multipart("/api/v1/farm-logs/{logId}/attachments", logId)
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Loại file không hỗ trợ. Chỉ chấp nhận JPG, PNG, PDF"));
    }

    @Test
    void uploadAttachment_shouldReturn400_whenFileEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        when(attachmentService.uploadAttachment(any(), any(), any(), any()))
                .thenThrow(new BusinessException("File không được để trống"));

        mockMvc.perform(multipart("/api/v1/farm-logs/{logId}/attachments", logId)
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File không được để trống"));
    }

    @Test
    void uploadAttachment_shouldReturn400_whenFarmLogNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", "test".getBytes()
        );

        when(attachmentService.uploadAttachment(any(), any(), any(), any()))
                .thenThrow(new BusinessException("Không tìm thấy nhật ký canh tác"));

        mockMvc.perform(multipart("/api/v1/farm-logs/{logId}/attachments", logId)
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không tìm thấy nhật ký canh tác"));
    }

    @Test
    void uploadAttachment_shouldReturn400_whenNotBelongToOrganization() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", "test".getBytes()
        );

        when(attachmentService.uploadAttachment(any(), any(), any(), any()))
                .thenThrow(new BusinessException("Nhật ký không thuộc tổ chức của bạn"));

        mockMvc.perform(multipart("/api/v1/farm-logs/{logId}/attachments", logId)
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nhật ký không thuộc tổ chức của bạn"));
    }
}