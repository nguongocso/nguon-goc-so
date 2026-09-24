package vn.nguongocso.export.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;
import vn.nguongocso.export.dto.response.OpenDataExportJobResponse;
import vn.nguongocso.export.enums.ExportJobStatus;
import vn.nguongocso.export.service.ExportService;
import vn.nguongocso.export.service.OpenDataAsyncExportService;
import vn.nguongocso.export.service.ProfileTemplateService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenDataExportAsyncControllerTest {

    @Mock
    private ExportService exportService;

    @Mock
    private ProfileTemplateService profileTemplateService;

    @Mock
    private OpenDataAsyncExportService openDataAsyncExportService;

    @InjectMocks
    private ExportController exportController;

    private CustomUserDetails mockUser;

    @BeforeEach
    void setUp() {
        mockUser = mock(CustomUserDetails.class);
        lenient().when(mockUser.getUsername()).thenReturn("cuctruong01");
    }

    @Test
    @DisplayName("exportOpenData với async=true trả về 202 Accepted kèm thông tin Job")
    void exportOpenData_async_returns202Accepted() {
        UUID jobId = UUID.randomUUID();
        OpenDataExportJobResponse jobResponse = OpenDataExportJobResponse.builder()
                .jobId(jobId)
                .status(ExportJobStatus.PENDING)
                .format("CSV")
                .downloadUrl("/api/v1/export/jobs/" + jobId + "/download")
                .build();

        when(openDataAsyncExportService.submitJob(any(ExportOpenDataRequest.class), any())).thenReturn(jobResponse);

        ExportOpenDataRequest request = new ExportOpenDataRequest();
        request.setFormat("CSV");

        ResponseEntity<?> response = exportController.exportOpenData(request, true, mockUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isInstanceOf(ApiResult.class);

        @SuppressWarnings("unchecked")
        ApiResult<OpenDataExportJobResponse> apiResult = (ApiResult<OpenDataExportJobResponse>) response.getBody();
        assertThat(apiResult.getData()).isNotNull();
        assertThat(apiResult.getData().getJobId()).isEqualTo(jobId);
        assertThat(apiResult.getData().getStatus()).isEqualTo(ExportJobStatus.PENDING);
    }

    @Test
    @DisplayName("getExportJob trả về thông tin trạng thái job để polling")
    void getExportJob_returns200Ok() {
        UUID jobId = UUID.randomUUID();
        OpenDataExportJobResponse jobResponse = OpenDataExportJobResponse.builder()
                .jobId(jobId)
                .status(ExportJobStatus.COMPLETED)
                .format("JSON")
                .fileName("export.json")
                .fileSize(2048L)
                .build();

        when(openDataAsyncExportService.getJobStatus(jobId)).thenReturn(jobResponse);

        var response = exportController.getExportJob(jobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo(ExportJobStatus.COMPLETED);
        assertThat(response.getBody().getData().getFileName()).isEqualTo("export.json");
    }

    @Test
    @DisplayName("downloadExportJob trả về file đính kèm khi job COMPLETED")
    void downloadExportJob_returnsFileAttachment() {
        UUID jobId = UUID.randomUUID();
        OpenDataExportJobResponse jobResponse = OpenDataExportJobResponse.builder()
                .jobId(jobId)
                .status(ExportJobStatus.COMPLETED)
                .format("CSV")
                .fileName("open_data_test.csv")
                .fileSize(100L)
                .build();

        when(openDataAsyncExportService.getJobStatus(jobId)).thenReturn(jobResponse);
        when(openDataAsyncExportService.getJobDownload(jobId)).thenReturn(new ByteArrayResource("test,data".getBytes()));

        var response = exportController.downloadExportJob(jobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("open_data_test.csv");
        assertThat(response.getBody()).isNotNull();
    }
}
