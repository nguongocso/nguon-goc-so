package vn.nguongocso.backup.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.backup.dto.response.BackupHistoryResponse;
import vn.nguongocso.backup.entity.BackupRestoreHistory;
import vn.nguongocso.backup.enums.BackupOperationType;
import vn.nguongocso.backup.enums.BackupStatus;
import vn.nguongocso.backup.enums.BackupType;
import vn.nguongocso.backup.repository.BackupRestoreHistoryRepository;
import vn.nguongocso.backup.service.BackupService;
import vn.nguongocso.backup.service.RestoreService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

// commnet bằng tiếng việt
/**
 * Lớp triển khai phục hồi dữ liệu
 */
@Service
@Slf4j
public class RestoreServiceImpl implements RestoreService {
    private final BackupRestoreHistoryRepository backupRestoreHistoryRepository;
    private final BackupService backupService;
    private final TaskExecutor taskExecutor;

    private RestoreService self;

    /**
     * Setter để inject chính bản thân bean này, cho phép gọi các phương
     * thức @Transactional từ bên trong.
     * 
     * @param self RestoreServiceImpl bean được Spring quản lý
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(@org.springframework.context.annotation.Lazy RestoreService self) {
        this.self = self;
    }

    /**
     * Constructor để khởi tạo các repository và service cần thiết.
     * 
     * @param backupRestoreHistoryRepository Repository cho lịch sử sao lưu và phục
     *                                       hồi
     * @param backupService                  Service sao lưu
     * @param taskExecutor                   Executor tác vụ
     */
    public RestoreServiceImpl(
            BackupRestoreHistoryRepository backupRestoreHistoryRepository,
            BackupService backupService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.backupRestoreHistoryRepository = backupRestoreHistoryRepository;
        this.backupService = backupService;
        this.taskExecutor = taskExecutor;
    }

    private final AtomicBoolean maintenanceMode = new AtomicBoolean(false);

    // Database Connection Parameters
    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:3306}")
    private String dbPort;

    @Value("${DB_NAME:nguon_goc_so}")
    private String dbName;

    @Value("${DB_USERNAME:root}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    // Restore CLI configurations
    @Value("${app.backup.mysql-path:mysql}")
    private String mysqlPath;

    /**
     * Kiểm tra xem hệ thống có đang ở chế độ bảo trì hay không.
     * 
     * @return true nếu đang ở chế độ bảo trì, false nếu không
     */
    @Override
    public boolean isMaintenanceMode() {
        return maintenanceMode.get();
    }

    /**
     * Thiết lập chế độ bảo trì của hệ thống.
     * 
     * @param mode true để bật chế độ bảo trì, false để tắt
     */
    @Override
    public void setMaintenanceMode(boolean mode) {
        log.info("Setting system maintenance mode to: {}", mode);
        maintenanceMode.set(mode);
    }

    /**
     * Cập nhật trạng thái của một bản ghi lịch sử sao lưu/phục hồi.
     * 
     * @param id ID của bản ghi cần cập nhật
     */
    @Override
    @Transactional
    public void updateStatus(Integer id, BackupStatus status, String errorMessage) {
        backupRestoreHistoryRepository.findById(id).ifPresent(history -> {
            history.setStatus(status);
            history.setErrorMessage(errorMessage);
            backupRestoreHistoryRepository.save(history);
        });
    }

    /**
     * Kích hoạt quá trình phục hồi dữ liệu từ một bản sao lưu đã tồn tại.
     * 
     * @param backupHistoryId ID của bản ghi sao lưu cần phục hồi
     * @param creator         Người dùng kích hoạt quá trình phục hồi
     * @return Phản hồi chứa thông tin lịch sử phục hồi vừa được tạo
     */
    @Override
    @Transactional
    public BackupHistoryResponse triggerRestore(Integer backupHistoryId, User creator) {
        log.info("Triggering restore process for history ID: {} by user: {}", backupHistoryId, creator.getFullName());

        // Check lock
        if (backupRestoreHistoryRepository.existsByStatus(BackupStatus.IN_PROGRESS)) {
            throw new BusinessException(
                    "Hệ thống đang có một tiến trình sao lưu hoặc khôi phục khác đang diễn ra. Vui lòng thử lại sau.");
        }

        BackupRestoreHistory targetBackup = backupRestoreHistoryRepository.findById(backupHistoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy bản ghi sao lưu để khôi phục với ID: " + backupHistoryId));

        if (targetBackup.getOperationType() != BackupOperationType.BACKUP
                || targetBackup.getStatus() != BackupStatus.SUCCESS) {
            throw new BusinessException("Bản ghi chỉ định không phải là một bản sao lưu thành công.");
        }

        File backupFile = new File(targetBackup.getFilePath());
        if (!backupFile.exists()) {
            throw new ResourceNotFoundException("Tập tin sao lưu vật lý không tồn tại trên máy chủ.");
        }

        // Create Restore record
        BackupRestoreHistory restoreRecord = BackupRestoreHistory.builder()
                .operationType(BackupOperationType.RESTORE)
                .status(BackupStatus.IN_PROGRESS)
                .reference(targetBackup)
                .createdBy(creator)
                .build();

        BackupRestoreHistory saved = backupRestoreHistoryRepository.save(restoreRecord);
        setMaintenanceMode(true);

        // Đảm bảo async chỉ chạy SAU KHI transaction đã commit thành công
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            CompletableFuture.runAsync(() -> runRestoreProcess(saved, backupFile, creator),
                                    taskExecutor);
                        }
                    });
        } else {
            CompletableFuture.runAsync(() -> runRestoreProcess(saved, backupFile, creator), taskExecutor);
        }

        return BackupHistoryResponse.fromEntity(saved);
    }

    /**
     * Executes the restore process in the background.
     */
    private void runRestoreProcess(BackupRestoreHistory restoreHistory, File backupFile, User creator) {
        log.info("Starting restore processing for restore log ID: {}", restoreHistory.getId());
        BackupRestoreHistory quickBackup = null;

        try {
            // 1. Take a quick safety backup of the database before overriding it
            log.info("Taking a quick safety backup of current database state before restore...");
            try {
                quickBackup = backupService.executeBackupWithoutLock(BackupType.MANUAL, creator);
                if (quickBackup.getStatus() != BackupStatus.SUCCESS) {
                    throw new RuntimeException(
                            "Tạo bản sao lưu khẩn cấp thất bại. Dừng quá trình phục hồi để bảo vệ dữ liệu. Chi tiết: "
                                    + quickBackup.getErrorMessage());
                }
            } catch (Exception ex) {
                log.error("Emergency backup failed. Restoring aborted.", ex);
                throw new RuntimeException(
                        "Không thể tạo bản sao lưu khẩn cấp trước khi khôi phục: " + ex.getMessage());
            }

            // 2. Perform the database restoration
            log.info("Executing database restoration from file: {}", backupFile.getAbsolutePath());
            performRestore(backupFile);

            // 3. Update status to SUCCESS
            self.updateStatus(restoreHistory.getId(), BackupStatus.SUCCESS, null);
            log.info("Database restoration completed successfully.");

        } catch (Exception e) {
            log.error("Database restoration failed for log ID: {}. Attempting rollback...", restoreHistory.getId(), e);
            self.updateStatus(restoreHistory.getId(), BackupStatus.FAILED, e.getMessage());

            // 4. Fallback/Rollback: restore database back to the quickBackup state
            if (quickBackup != null && quickBackup.getFilePath() != null) {
                File quickBackupFile = new File(quickBackup.getFilePath());
                if (quickBackupFile.exists()) {
                    log.warn("Attempting database rollback using emergency backup file: {}",
                            quickBackupFile.getAbsolutePath());
                    try {
                        performRestore(quickBackupFile);
                        log.info("Database rollback completed successfully.");
                    } catch (Exception rollbackException) {
                        log.error("CRITICAL ERROR: Database rollback failed. System database is corrupted!",
                                rollbackException);
                    }
                }
            }
        } finally {
            // Ensure maintenance mode is turned off after execution
            setMaintenanceMode(false);
        }
    }

    /**
     * Reads sql.gz and feeds it directly into mysql client process.
     */
    private void performRestore(File backupFile) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(mysqlPath);
        command.add("-h");
        command.add(dbHost);
        command.add("-P");
        command.add(dbPort);
        command.add("-u");
        command.add(dbUsername);
        command.add(dbName);

        ProcessBuilder pb = new ProcessBuilder(command);
        if (dbPassword != null && !dbPassword.isEmpty()) {
            pb.environment().put("MYSQL_PWD", dbPassword);
        }

        // Gộp luồng lỗi (stderr) vào luồng xuất chuẩn (stdout) để đọc chung 1 stream
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Feed decompressed SQL directly into mysql's stdin
        try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(backupFile));
                OutputStream os = process.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } // Đóng os (stdin) để báo cho mysql biết đã truyền xong dữ liệu

        // Đọc sạch dữ liệu output (cảnh báo/lỗi) để giải phóng buffer của OS, tránh
        // deadlock treo tiến trình
        StringBuilder outputMsg = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputMsg.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(
                    "mysql CLI exited with code: " + exitCode + ". Detail: " + outputMsg.toString().trim());
        }
    }
}
