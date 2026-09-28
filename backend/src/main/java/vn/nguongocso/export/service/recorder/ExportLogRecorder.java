package vn.nguongocso.export.service.recorder;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.export.entity.ExportLog;
import vn.nguongocso.export.entity.ProfileTemplate;
import vn.nguongocso.export.repository.ExportLogRepository;
import vn.nguongocso.trace.entity.Shipment;

/** Thành phần chuyên trách mở write-transaction độc lập để ghi nhận nhật ký xuất hồ sơ (ExportLog). */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportLogRecorder {
    private final ExportLogRepository exportLogRepository;
    private final UserRepository userRepository;

    /** Ghi nhận nhật ký xuất hồ sơ trong một write-transaction ngắn độc lập. */
    @Transactional
    public void recordExportLog(Shipment shipment, ProfileTemplate template, UUID userId) {
        User user = userId != null
                ? userRepository.findById(userId).orElse(null)
                : null;

        ExportLog exportLog = ExportLog.builder()
                .shipment(shipment)
                .template(template)
                .exportedBy(user)
                .exportedAt(LocalDateTime.now())
                .build();
        exportLogRepository.save(exportLog);

        log.info("Đã ghi nhận nhật ký xuất hồ sơ (ExportLog ID: {}) cho shipment ID: {}, template: {}",
                exportLog.getId(),
                shipment != null ? shipment.getId() : null,
                template != null ? template.getName() : "Mặc định hệ thống");
    }
}
