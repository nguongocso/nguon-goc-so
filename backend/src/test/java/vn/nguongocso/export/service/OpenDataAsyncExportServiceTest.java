package vn.nguongocso.export.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;
import vn.nguongocso.export.dto.response.OpenDataExportJobResponse;
import vn.nguongocso.export.entity.OpenDataExportJob;
import vn.nguongocso.export.enums.ExportJobStatus;
import vn.nguongocso.export.repository.OpenDataExportJobRepository;
import vn.nguongocso.export.service.impl.OpenDataAsyncExportServiceImpl;
import vn.nguongocso.export.service.worker.OpenDataExportWorker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenDataAsyncExportServiceTest {

    @Mock
    private OpenDataExportJobRepository jobRepository;

    @Mock
    private OpenDataExportWorker exportWorker;

    @Mock
    private ExportService exportService;

    @InjectMocks
    private OpenDataAsyncExportServiceImpl asyncExportService;

    @TempDir
    Path tempDir;

    private CustomUserDetails mockUser;

    @BeforeEach
    void setUp() {
        mockUser = mock(CustomUserDetails.class);
        lenient().when(mockUser.getUserId()).thenReturn(UUID.randomUUID());
        lenient().when(mockUser.getUsername()).thenReturn("cuctruong01");
        ReflectionTestUtils.setField(asyncExportService, "tempDirPath", tempDir.toString());
    }

    @Test
    @DisplayName("submitJob: Khởi tạo job với trạng thái PENDING và giao việc cho worker")
    void testSubmitJob() {
        ExportOpenDataRequest request = new ExportOpenDataRequest();
        request.setFormat("CSV");

        when(jobRepository.save(any(OpenDataExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OpenDataExportJobResponse response = asyncExportService.submitJob(request, mockUser);

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ExportJobStatus.PENDING);
        assertThat(response.getFormat()).isEqualTo("CSV");

        verify(exportWorker).processExportJob(eq(response.getJobId()), eq(request), eq(mockUser), anyString());
    }

    @Test
    @DisplayName("Worker: Xử lý job ngầm thành công, lưu file trên đĩa và cập nhật trạng thái COMPLETED")
    void testWorkerProcessExportJobSuccess() throws IOException {
        UUID jobId = UUID.randomUUID();
        OpenDataExportJob job = OpenDataExportJob.builder()
                .id(jobId)
                .status(ExportJobStatus.PENDING)
                .format("JSON")
                .createdAt(LocalDateTime.now())
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(OpenDataExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        byte[] payload = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        when(exportService.exportOpenData(any(), any())).thenReturn(new ByteArrayResource(payload));

        OpenDataExportWorker worker = new OpenDataExportWorker(jobRepository, exportService);
        worker.processExportJob(jobId, new ExportOpenDataRequest(), mockUser, tempDir.toString());

        ArgumentCaptor<OpenDataExportJob> jobCaptor = ArgumentCaptor.forClass(OpenDataExportJob.class);
        verify(jobRepository, atLeast(2)).save(jobCaptor.capture());

        OpenDataExportJob finalJob = jobCaptor.getValue();
        assertThat(finalJob.getStatus()).isEqualTo(ExportJobStatus.COMPLETED);
        assertThat(finalJob.getFileSize()).isEqualTo(payload.length);
        assertThat(finalJob.getFilePath()).isNotNull();
        assertThat(Files.exists(Path.of(finalJob.getFilePath()))).isTrue();
    }

    @Test
    @DisplayName("getJobStatus: Trả về trạng thái chính xác của job khi được poll")
    void testGetJobStatus() {
        UUID jobId = UUID.randomUUID();
        OpenDataExportJob job = OpenDataExportJob.builder()
                .id(jobId)
                .status(ExportJobStatus.COMPLETED)
                .format("CSV")
                .fileName("export.csv")
                .fileSize(1024L)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        OpenDataExportJobResponse statusResponse = asyncExportService.getJobStatus(jobId);

        assertThat(statusResponse).isNotNull();
        assertThat(statusResponse.getStatus()).isEqualTo(ExportJobStatus.COMPLETED);
        assertThat(statusResponse.getFileName()).isEqualTo("export.csv");
    }

    @Test
    @DisplayName("getJobDownload: Trả về tệp kết xuất khi job COMPLETED")
    void testGetJobDownloadSuccess() throws IOException {
        Path testFile = tempDir.resolve("sample.csv");
        Files.writeString(testFile, "header1,header2\nval1,val2");

        UUID jobId = UUID.randomUUID();
        OpenDataExportJob job = OpenDataExportJob.builder()
                .id(jobId)
                .status(ExportJobStatus.COMPLETED)
                .filePath(testFile.toString())
                .fileName("sample.csv")
                .createdAt(LocalDateTime.now())
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        Resource resource = asyncExportService.getJobDownload(jobId);
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.contentLength()).isGreaterThan(0);
    }

    @Test
    @DisplayName("getJobDownload: Ném lỗi nếu job chưa COMPLETED hoặc thất bại")
    void testGetJobDownloadThrowsWhenNotReady() {
        UUID jobId = UUID.randomUUID();
        OpenDataExportJob inProgressJob = OpenDataExportJob.builder()
                .id(jobId)
                .status(ExportJobStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(inProgressJob));

        assertThatThrownBy(() -> asyncExportService.getJobDownload(jobId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tác vụ đang trong quá trình xử lý");
    }
}
