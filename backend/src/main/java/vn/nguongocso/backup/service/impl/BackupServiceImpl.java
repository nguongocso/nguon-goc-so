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
     */
    @Override
    @Transactional
    public BackupScheduleResponse configureSchedule(BackupScheduleRequest request, User updater) {
        log.info("Configuring backup schedule. Cron: {}, Active: {}", request.getCronExpression(),
                request.getIsActive());
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

        eventPublisher.publishEvent(new BackupScheduleChangedEvent(this, saved));

        return BackupScheduleResponse.fromEntity(saved);
    }

    /**
     * Lấy thông tin lịch trình sao lưu hiện tại.
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
     */
    @Override
    @Transactional
    public BackupHistoryResponse triggerManualBackup(User creator) {
        log.info("Triggering manual backup by user: {}", creator.getFullName());

        if (backupRestoreHistoryRepository.existsByStatus(BackupStatus.IN_PROGRESS)) {
            throw new BusinessException(
                    "Hệ thống đang có một tiến trình sao lưu hoặc khôi phục khác đang diễn ra. Vui lòng thử lại sau.");
        }
        BackupRestoreHistory history = BackupRestoreHistory.builder()
                .operationType(BackupOperationType.BACKUP)
                .backupType(BackupType.MANUAL)
                .status(BackupStatus.IN_PROGRESS)
                .createdBy(creator)
                .build();

        BackupRestoreHistory saved = backupRestoreHistoryRepository.save(history);
        CompletableFuture.runAsync(() -> runBackupProcess(saved), taskExecutor);

        return BackupHistoryResponse.fromEntity(saved);
    }

    /**
     * Thực hiện sao lưu cơ sở dữ liệu.
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
     * Thực hiện sao lưu cơ sở dữ liệu.
     */
    private BackupRestoreHistory runBackupProcess(BackupRestoreHistory history) {
        log.info("Starting mysqldump database dump for history ID: {}", history.getId());

        File dir = new File(backupDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "backup_" + timestamp + ".sql.gz";
        File file = new File(dir, fileName);
        File errFile = new File(dir, "mysqldump_error_" + timestamp + ".log");

        String resolvedExecutable;
        try {
            resolvedExecutable = resolveMysqldumpPath();
        } catch (IOException e) {
            log.error("Failed to resolve mysqldump path", e);
            if (file.exists()) {
                file.delete();
            }
            if (errFile.exists()) {
                errFile.delete();
            }
            self.updateStatus(history.getId(), BackupStatus.FAILED, null, null, null, e.getMessage());
            return backupRestoreHistoryRepository.findById(history.getId()).orElse(history);
        }

        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append(resolvedExecutable).append(" ");
        cmdBuilder.append("-h ").append(dbHost).append(" ");
        cmdBuilder.append("-P ").append(dbPort).append(" ");
        cmdBuilder.append("-u ").append(dbUsername).append(" ");
        cmdBuilder.append("--single-transaction ");
        cmdBuilder.append("--skip-lock-tables ");
        cmdBuilder.append("--no-tablespaces ");
        cmdBuilder.append("--set-gtid-purged=OFF ");
        cmdBuilder.append("--ignore-table=").append(dbName).append(".backup_restore_history ");
        cmdBuilder.append("--ignore-table=").append(dbName).append(".backup_schedules ");
        cmdBuilder.append(dbName);

        List<String> command = new ArrayList<>();
        command.add("/bin/sh");
        command.add("-c");
        command.add(cmdBuilder.toString());

        ProcessBuilder pb = new ProcessBuilder(command);

        if (dbPassword != null && !dbPassword.isEmpty()) {
            pb.environment().put("MYSQL_PWD", dbPassword);
        }

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

            readerThread.join(5000);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
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

            if (errFile.exists()) {
                errFile.delete();
            }
            self.updateStatus(history.getId(), BackupStatus.SUCCESS, fileName, file.getAbsolutePath(), file.length(),
                    null);
            log.info("Database backup finished successfully. File: {}", file.getAbsolutePath());
            BackupRestoreHistory saved = backupRestoreHistoryRepository.findById(history.getId()).orElse(history);
            try {
                cleanOldBackups();
            } catch (Exception e) {
                log.error("Error during cleaning old backups", e);
            }

            return saved;

        } catch (Exception e) {
            log.error("Backup execution failed for history ID: {}", history.getId(), e);
            if (file.exists()) {
                file.delete();
            }

            self.updateStatus(history.getId(), BackupStatus.FAILED, null, null, null, e.getMessage());
            return backupRestoreHistoryRepository.findById(history.getId()).orElse(history);
        }
    }

    /**
     * Xóa bản sao lưu cũ.
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

    /**
     * Tự động giải quyết đường dẫn công cụ mysqldump.
     * Thứ tự ưu tiên: Cấu hình app.backup.mysql-dump-path -> System PATH via
     * `where`/`which` -> Thư mục mặc định -> Gọi trực tiếp mysqldump -> Báo lỗi.
     */
    public String resolveMysqldumpPath() throws IOException {
        String configured = mysqlDumpPath != null ? mysqlDumpPath.trim() : "";

        // 1. Nếu cấu hình được truyền và file tồn tại trên đĩa
        if (!configured.isEmpty()) {
            File file = new File(configured);
            if (file.exists() && file.isFile()) {
                log.info("Sử dụng đường dẫn mysqldump từ cấu hình: {}", file.getAbsolutePath());
                return file.getAbsolutePath();
            }
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String execName = isWindows ? "mysqldump.exe" : "mysqldump";

        // 2. Tìm kiếm trên System PATH thông qua `where` (Windows) hoặc `which`
        // (Linux/macOS)
        String foundByPath = findExecutableOnSystemPath(isWindows ? "where" : "which", execName);
        if (foundByPath != null) {
            log.info("Tìm thấy mysqldump trên System PATH: {}", foundByPath);
            return foundByPath;
        }

        // 3. Thử các đường dẫn cài đặt mặc định phổ biến
        List<String> commonPaths = getCommonMysqlPaths(isWindows, execName);
        for (String path : commonPaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                log.info("Tìm thấy mysqldump tại vị trí mặc định: {}", file.getAbsolutePath());
                return file.getAbsolutePath();
            }
        }

        // 4. Kiểm tra xem lệnh "mysqldump" hoặc "mysqldump.exe" có thể thực thi trực
        // tiếp từ PATH không
        if (canExecuteCommand(execName)) {
            log.info("Sử dụng mysqldump trực tiếp từ môi trường hệ thống");
            return execName;
        }

        // 5. Ném ngoại lệ chi tiết nếu không tìm thấy
        if (!configured.isEmpty()) {
            throw new IOException("Đường dẫn mysqldump được cấu hình (" + configured
                    + ") không tồn tại hoặc không phải là file hợp lệ. Vui lòng kiểm tra lại cấu hình MYSQL_DUMP_PATH.");
        }

        throw new IOException("Không tìm thấy công cụ mysqldump trên hệ thống. "
                + "Vui lòng cài đặt MySQL Client hoặc cấu hình biến môi trường MYSQL_DUMP_PATH / thuộc tính app.backup.mysql-dump-path.");
    }

    private String findExecutableOnSystemPath(String finderTool, String binaryName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(finderTool, binaryName);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    File file = new File(line.trim());
                    if (file.exists() && file.isFile()) {
                        return file.getAbsolutePath();
                    }
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
        }
        return null;
    }

    private List<String> getCommonMysqlPaths(boolean isWindows, String binaryName) {
        List<String> paths = new ArrayList<>();
        if (isWindows) {
            paths.add("C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\" + binaryName);
            paths.add("C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\" + binaryName);
            paths.add("C:\\Program Files\\MySQL\\MySQL Server 8.1\\bin\\" + binaryName);
            paths.add("C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\" + binaryName);
            paths.add("C:\\Program Files (x86)\\MySQL\\MySQL Server 5.7\\bin\\" + binaryName);
            paths.add("C:\\xampp\\mysql\\bin\\" + binaryName);
            paths.add("C:\\inetpub\\mysql\\bin\\" + binaryName);
        } else {
            paths.add("/usr/bin/" + binaryName);
            paths.add("/usr/local/bin/" + binaryName);
            paths.add("/usr/local/mysql/bin/" + binaryName);
            paths.add("/opt/homebrew/bin/" + binaryName);
        }
        return paths;
    }

    private boolean canExecuteCommand(String binaryName) {
        try {
            Process process = new ProcessBuilder(binaryName, "--version").start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
