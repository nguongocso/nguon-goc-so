package vn.nguongocso.alert.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.entity.ActivityLogExportItem;
import vn.nguongocso.exception.BusinessException;

/** Sinh CSV nhật ký hoạt động nhất quán cho cả direct và async export. */
@Component
@RequiredArgsConstructor
public class ActivityLogCsvWriter {
    private static final String NULL_VALUE = "null";
    private static final String[] CSV_HEADERS = {
            "occurredAt", "actorName", "actorUsername", "actorRole", "actionType",
            "objectType", "objectIdentifier", "beforeValue", "afterValue"
    };
    private final ActivityLogExportValueSanitizer valueSanitizer;

    /** Sinh tệp CSV trong bộ nhớ cho nhánh xuất trực tiếp có giới hạn. */
    public byte[] writeActivities(List<ActivityLog> logs) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            output.write(0xEF);
            output.write(0xBB);
            output.write(0xBF);
            try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
                    CSVPrinter printer = new CSVPrinter(writer, csvFormat())) {
                for (ActivityLog log : logs) {
                    print(printer, log.getCreatedAt(), actorName(log), log.getUsername(), log.getActorRole(),
                            log.getAction(), log.getEntityType(), log.getEntityId(),
                            valueSanitizer.sanitize(log.getBeforeValue()),
                            valueSanitizer.sanitize(log.getAfterValue()));
                }
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo tệp nhật ký hoạt động.");
        }
    }

    /** Mở tệp CSV mới và ghi BOM cùng header. */
    public CSVPrinter openFile(Path path) throws IOException {
        Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        writer.write('\ufeff');
        return new CSVPrinter(writer, csvFormat());
    }

    /** Ghi một dòng snapshot vào CSV nền. */
    public void print(CSVPrinter printer, ActivityLogExportItem item) throws IOException {
        print(printer, item.getOccurredAt(), item.getActorName(), item.getActorUsername(), item.getActorRole(),
                item.getActionType(), item.getObjectType(), item.getObjectIdentifier(),
                valueSanitizer.sanitize(item.getBeforeValue()),
                valueSanitizer.sanitize(item.getAfterValue()));
    }

    private void print(CSVPrinter printer, Object... values) throws IOException {
        Object[] protectedValues = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            protectedValues[i] = values[i] == null ? NULL_VALUE : protectFormula(String.valueOf(values[i]));
        }
        printer.printRecord(protectedValues);
    }

    private CSVFormat csvFormat() {
        return CSVFormat.DEFAULT.builder().setHeader(CSV_HEADERS).setRecordSeparator("\r\n").get();
    }

    private String actorName(ActivityLog log) {
        return log.getFullName() != null && !log.getFullName().isBlank() ? log.getFullName() : log.getUsername();
    }

    /** Bảo vệ giá trị khỏi bị bảng tính diễn giải thành công thức. */
    public String protectFormula(String value) {
        if (value == null || value.isEmpty()) return value;
        String stripped = value.stripLeading();
        if (stripped.isEmpty()) return value;
        char first = stripped.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@' ? "'" + value : value;
    }
}
