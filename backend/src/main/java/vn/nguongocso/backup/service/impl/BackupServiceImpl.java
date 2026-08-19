package vn.nguongocso.backup.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.backup.dto.request.BackupScheduleRequest;
import vn.nguongocso.backup.dto.response.BackupHistoryResponse;
import vn.nguongocso.backup.dto.response.BackupScheduleResponse;
import vn.nguongocso.backup.entity.BackupRestoreHistory;
import vn.nguongocso.backup.entity.BackupSchedule;
import vn.nguongocso.backup.enums.BackupOperationType;
import vn.nguongocso.backup.enums.BackupStatus;
import vn.nguongocso.backup.enums.BackupType;
import vn.nguongocso.backup.event.BackupScheduleChangedEvent;
import vn.nguongocso.backup.repository.BackupRestoreHistoryRepository;
import vn.nguongocso.backup.repository.BackupScheduleRepository;
import vn.nguongocso.backup.service.BackupService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

// commnet bằng tiếng việt
/*
* Lớp triển khai sao lưu dữ liệu
 */
@Service
@Slf4j
public class BackupServiceImpl implements BackupService {
    private final BackupScheduleRepository backupScheduleRepository;
    private final BackupRestoreHistoryRepository backupRestoreHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskExecutor taskExecutor;

    private BackupService self;

    /*
     * Thiết lập tự tham chiếu để gọi các phương thức @Transactional trong cùng một
     * bean.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(@org.springframework.context.annotation.Lazy BackupService self) {
        this.self = self;
    }

    /*
     * Constructor để khởi tạo các repository và event publisher.
     */
    public BackupServiceImpl(
            BackupScheduleRepository backupScheduleRepository,
            BackupRestoreHistoryRepository backupRestoreHistoryRepository,
            ApplicationEventPublisher eventPublisher,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.backupScheduleRepository = backupScheduleRepository;
        this.backupRestoreHistoryRepository = backupRestoreHistoryRepository;
        this.eventPublisher = eventPublisher;
        this.taskExecutor = taskExecutor;
    }

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

    // Backup Configurations
    @Value("${app.backup.local-dir:./backups}")
    private String backupDir;

    @Value("${app.backup.mysql-dump-path:mysqldump}")
    private String mysqlDumpPath;

    @Value("${app.backup.retention-count:30}")
    private int retentionCount;

    /**
     * Cấu hình lịch trình sao lưu dựa trên yêu cầu từ người dùng.
     * Phương thức này sẽ lưu cấu hình vào cơ sở dữ liệu và phát ra sự kiện để cập
     * nhật lịch trình động.
     *
     * @param request Yêu cầu cấu hình lịch trình sao lưu
     * @param updater Người dùng thực hiện thay đổi
     * @return Phản hồi chứa thông tin lịch trình đã được lưu
     */
    @Override
    @Transactional
    public BackupScheduleResponse configureSchedule(BackupScheduleRequest request, User updater) {
        log.info("Configuring backup schedule. Cron: {}, Active: {}", request.getCronExpression(),
                request.getIsActive());

        // Validate Cron expression
        if (!CronExpression.isValidExpression(request.getCronExpression())) {
            throw new BusinessException("Định dạng biểu thức cron không hợp lệ");
        }

        BackupSchedule schedule = backupScheduleRepository.findFirstByIsActiveTrue()
                .orElse(backupScheduleRepository.findById(1).orElse(new BackupSchedule()));

        schedule.setCronExpression(request.getCronExpression());
        schedule.setDescription(request.getDescription());
        schedule.setActive(request.getIsActive());
        schedule.setUpdatedBy(updater);

        BackupSchedule saved = backupScheduleRepository.save(schedule);

        // Publish event to dynamically reload the scheduler
        eventPublisher.publishEvent(new BackupScheduleChangedEvent(this, saved));

        return BackupScheduleResponse.fromEntity(saved);
    }

    /**
     * Lấy thông tin lịch trình sao lưu hiện tại.
     *
     * @return Phản hồi chứa thông tin lịch trình hiện tại hoặc null nếu không có
     *         lịch trình nào được kích hoạt
     */
    @Override
    @Transactional(readOnly = true)
    public BackupScheduleResponse getActiveSchedule() {
        return backupScheduleRepository.findFirstByIsActiveTrue()
                .map(BackupScheduleResponse::fromEntity)
                .orElse(null);
    }

    /**
     * Kích hoạt sao lưu thủ công. Phương thức này sẽ kiểm tra xem có tiến trình sao
     * lưu hoặc phục hồi nào đang diễn ra hay không.
     * Nếu không, nó sẽ tạo một bản ghi lịch sử với trạng thái IN_PROGRESS và thực
     * hiện sao lưu trong nền.
     *
     * @param creator Người dùng kích hoạt sao lưu
     * @return Phản hồi chứa thông tin lịch sử sao lưu vừa được tạo
     */
    @Override
    @Transactional
    public BackupHistoryResponse triggerManualBackup(User creator) {
        log.info("Triggering manual backup by user: {}", creator.getFullName());

        // Check if there is any pending BACKUP or RESTORE operation in progress
        // (Resource locking)
        if (backupRestoreHistoryRepository.existsByStatus(BackupStatus.IN_PROGRESS)) {
            throw new BusinessException(
                    "Hệ thống đang có một tiến trình sao lưu hoặc khôi phục khác đang diễn ra. Vui lòng thử lại sau.");
        }

        // Create log record with IN_PROGRESS status
        BackupRestoreHistory history = BackupRestoreHistory.builder()
                .operationType(BackupOperationType.BACKUP)
                .backupType(BackupType.MANUAL)
                .status(BackupStatus.IN_PROGRESS)
                .createdBy(creator)
                .build();

        BackupRestoreHistory saved = backupRestoreHistoryRepository.save(history);

        // Execute backup asynchronously in the background using TaskExecutor to avoid
        // thread blocking
        CompletableFuture.runAsync(() -> runBackupProcess(saved), taskExecutor);

        return BackupHistoryResponse.fromEntity(saved);
    }

    /**
     * Thực hiện sao lưu cơ sở dữ liệu.
     *
     * @param backupType Loại sao lưu
     * @param creator    Người dùng thực hiện sao lưu
     * @return Lịch sử sao lưu vừa được tạo
     */
    @Override
    @Transactional
    public BackupRestoreHistory executeBackup(BackupType backupType, User creator) {
        log.info("Executing database backup. Type: {}", backupType);

        if (backupRestoreHistoryRepository.existsByStatus(BackupStatus.IN_PROGRESS)) {
            throw new BusinessException("Hệ thống đang có tiến trình bảo trì hoặc sao lưu khác diễn ra");
        }

        return executeBackupWithoutLock(backupType, creator);
    }

    /**
     * Thực hiện sao lưu cơ sở dữ liệu mà không kiểm tra khóa. Phương thức này được
     * sử dụng nội bộ khi đã đảm bảo rằng không có tiến trình nào đang diễn ra.
     *
     * @param backupType Loại sao lưu
     * @param creator    Người dùng thực hiện sao lưu
     * @return Lịch sử sao lưu vừa được tạo
     */
    @Override
    @Transactional
    public BackupRestoreHistory executeBackupWithoutLock(BackupType backupType, User creator) {
        log.info("Executing database backup without lock checking. Type: {}", backupType);

        BackupRestoreHistory history = BackupRestoreHistory.builder()
                .operationType(BackupOperationType.BACKUP)
                .backupType(backupType)
                .status(BackupStatus.IN_PROGRESS)
                .createdBy(creator)
                .build();

        BackupRestoreHistory saved = backupRestoreHistoryRepository.save(history);
        return runBackupProcess(saved);
    }

    /**
     * Cập nhật trạng thái của bản ghi lịch sử sao lưu/phục hồi.
     *
     * @param id           ID của bản ghi lịch sử
     * @param status       Trạng thái mới
     * @param fileName     Tên tập tin sao lưu (nếu có)
     * @param filePath     Đường dẫn tập tin sao lưu (nếu có)
     * @param fileSize     Kích thước tập tin sao lưu (nếu có)
     * @param errorMessage Thông báo lỗi (nếu có)
     */
    @Override
    @Transactional
    public void updateStatus(Integer id, BackupStatus status, String fileName, String filePath, Long fileSize,
            String errorMessage) {
        backupRestoreHistoryRepository.findById(id).ifPresent(history -> {
            history.setStatus(status);
            history.setFileName(fileName);
            history.setFilePath(filePath);
            history.setFileSize(fileSize);
            history.setErrorMessage(errorMessage);
            backupRestoreHistoryRepository.save(history);
        });
    }

    /**
     * Lấy danh sách lịch sử sao lưu/phục hồi với các bộ lọc và phân trang.
     *
     * @param operationType Loại thao tác (sao lưu hoặc phục hồi)
     * @param status        Trạng thái của thao tác
     * @param pageable      Thông tin phân trang
     * @return Trang chứa danh sách lịch sử sao lưu/phục hồi
     */
    @Override
    @Transactional(readOnly = true)
    public Page<BackupHistoryResponse> getHistory(BackupOperationType operationType, BackupStatus status,
            Pageable pageable) {
        return backupRestoreHistoryRepository.findHistoryWithFilters(operationType, status, pageable)
                .map(BackupHistoryResponse::fromEntity);
    }

    /*
     * Lấy tập tin sao lưu dựa trên ID lịch sử. Phương thức này sẽ kiểm tra xem bản
     * ghi có phải là một bản sao lưu thành công hay không và trả về tập tin vật lý
     * nếu tồn tại.
     */
    @Override
    @Transactional(readOnly = true)
    public File getBackupFile(Integer historyId) {
        BackupRestoreHistory history = backupRestoreHistoryRepository.findById(historyId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy lịch sử sao lưu với ID: " + historyId));

        if (history.getOperationType() != BackupOperationType.BACKUP || history.getStatus() != BackupStatus.SUCCESS) {
            throw new BusinessException("Yêu cầu không hợp lệ. Bản ghi không phải là một bản sao lưu thành công.");
        }

        File file = new File(history.getFilePath());
        if (!file.exists()) {
            throw new ResourceNotFoundException("Tập tin sao lưu vật lý không tồn tại trên máy chủ.");
        }

        return file;
    }

    /**
     * Xóa bản ghi lịch sử sao lưu và tập tin vật lý liên quan.
     *
     * @param historyId ID của bản ghi lịch sử cần xóa
     */
    @Override
    @Transactional
    public void deleteBackup(Integer historyId) {
        log.info("Deleting backup file and log with ID: {}", historyId);
        BackupRestoreHistory history = backupRestoreHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử với ID: " + historyId));

        if (history.getFilePath() != null) {
            File file = new File(history.getFilePath());
            if (file.exists()) {
                if (file.delete()) {
                    log.info("Physical backup file deleted successfully: {}", history.getFilePath());
                } else {
                    log.error("Failed to delete physical backup file: {}", history.getFilePath());
                }
            }
        }

        backupRestoreHistoryRepository.delete(history);
    }

    /**
     * Executes the actual mysqldump command line process.
     */
    private BackupRestoreHistory runBackupProcess(BackupRestoreHistory history) {
        log.info("Starting mysqldump database dump for history ID: {}", history.getId());

        // Ensure local directory exists
        File dir = new File(backupDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "backup_" + timestamp + ".sql.gz";
        File file = new File(dir, fileName);
        File errFile = new File(dir, "mysqldump_error_" + timestamp + ".log");

        List<String> command = new ArrayList<>();
        command.add(mysqlDumpPath);
        command.add("-h");
        command.add(dbHost);
        command.add("-P");
        command.add(dbPort);
        command.add("-u");
        command.add(dbUsername);
        command.add("--single-transaction");
        command.add("--skip-lock-tables");
        command.add("--ignore-table=" + dbName + ".backup_restore_history");
        command.add("--ignore-table=" + dbName + ".backup_schedules"); // nếu có bảng lịch trình cũng nên loại trừ
        command.add(dbName);

        ProcessBuilder pb = new ProcessBuilder(command);
        // Securely pass MySQL password via environment variable
        if (dbPassword != null && !dbPassword.isEmpty()) {
            pb.environment().put("MYSQL_PWD", dbPassword);
        }

        // Chuyển hướng stderr ra file log tạm để tránh nghẹt buffer OS làm treo tiến
        // trình
        pb.redirectError(errFile);

        try {
            Process process = pb.start();

            Thread readerThread = new Thread(() -> {
                try (InputStream is = process.getInputStream();
                        FileOutputStream fos = new FileOutputStream(file);
                        GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        gzos.write(buffer, 0, len);
                    }
                } catch (IOException e) {
                    log.error("Error reading mysqldump output stream", e);
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                readerThread.interrupt();
                throw new IOException(
                        "mysqldump timeout: quá trình dump vượt quá thời gian cho phép, có thể do bị khóa (lock) bởi tiến trình khác.");
            }

            readerThread.join(5000); // đợi thread đọc ghi nốt phần còn lại (nếu process đã thoát nhanh)

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // Đọc lỗi từ file log tạm
                StringBuilder errorMsg = new StringBuilder();
                if (errFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(errFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorMsg.append(line).append("\n");
                        }
                    }
                    errFile.delete();
                }
                throw new IOException(
                        "mysqldump CLI exited with code: " + exitCode + ". Error: " + errorMsg.toString().trim());
            }

            // Xóa file log tạm khi chạy thành công
            if (errFile.exists()) {
                errFile.delete();
            }

            // Successfully backed up database
            self.updateStatus(history.getId(), BackupStatus.SUCCESS, fileName, file.getAbsolutePath(), file.length(),
                    null);
            log.info("Database backup finished successfully. File: {}", file.getAbsolutePath());

            // Save status (fetch updated from DB to return)
            BackupRestoreHistory saved = backupRestoreHistoryRepository.findById(history.getId()).orElse(history);

            // Execute cleanup of old files in a separate block to ensure it doesn't fail
            // the backup
            try {
                cleanOldBackups();
            } catch (Exception e) {
                log.error("Error during cleaning old backups", e);
            }

            return saved;

        } catch (Exception e) {
            log.error("Backup execution failed for history ID: {}", history.getId(), e);

            // Delete corrupt file if created
            if (file.exists()) {
                file.delete();
            }

            self.updateStatus(history.getId(), BackupStatus.FAILED, null, null, null, e.getMessage());
            return backupRestoreHistoryRepository.findById(history.getId()).orElse(history);
        }
    }

    /**
     * Deletes old backups exceeding the retention threshold to free disk space.
     */
    private void cleanOldBackups() {
        log.info("Checking for old backups exceeding retention limit of {}", retentionCount);
        List<BackupRestoreHistory> backups = backupRestoreHistoryRepository
                .findByOperationTypeAndStatusOrderByCreatedAtDesc(BackupOperationType.BACKUP, BackupStatus.SUCCESS);

        if (backups.size() > retentionCount) {
            List<BackupRestoreHistory> toDelete = backups.subList(retentionCount, backups.size());
            log.info("Deleting {} old backups from disk and database", toDelete.size());
            for (BackupRestoreHistory history : toDelete) {
                if (history.getFilePath() != null) {
                    File file = new File(history.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                }
                backupRestoreHistoryRepository.delete(history);
            }
        }
    }
}
