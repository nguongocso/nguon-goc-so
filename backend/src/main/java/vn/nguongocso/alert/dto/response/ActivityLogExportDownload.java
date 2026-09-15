package vn.nguongocso.alert.dto.response;

import java.nio.file.Path;

import lombok.Builder;
import lombok.Getter;

/** Thông tin tệp export đã được kiểm tra quyền tải. */
@Getter
@Builder
public class ActivityLogExportDownload {
    private Path path;
    private String fileName;
    private long fileSize;
}
