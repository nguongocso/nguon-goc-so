package vn.nguongocso.alert_reclaim_history.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.service.impl.ActivityLogCsvWriter;
import vn.nguongocso.alert.service.impl.ActivityLogExportValueSanitizer;

class ActivityLogCsvWriterTest {

    @Test
    void writeActivities_shouldCreateExcelCompatibleVietnameseTable() throws Exception {
        ActivityLog log = ActivityLog.builder()
                .createdAt(LocalDateTime.of(2026, 9, 14, 20, 25, 20))
                .fullName("Nguyễn Văn Minh")
                .username("nguyenvanminh")
                .actorRole("VT-02")
                .action("CREATE_PRODUCTION_LOT")
                .entityType("PRODUCTION_LOT")
                .entityId("LOT-001")
                .build();

        byte[] output = writer().writeActivities(List.of(log));

        assertThat(output).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setDelimiter(',')
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(new InputStreamReader(
                        new ByteArrayInputStream(output, 3, output.length - 3), StandardCharsets.UTF_8))) {
            assertThat(parser.getHeaderNames()).containsExactly(
                    "Thời gian", "Người thực hiện", "Tên đăng nhập", "Vai trò", "Hành động",
                    "Loại đối tượng", "Mã đối tượng", "Dữ liệu trước", "Dữ liệu sau");

            List<CSVRecord> records = parser.getRecords();
            assertThat(records).hasSize(1);
            CSVRecord record = records.get(0);
            assertThat(record.get("Thời gian")).isEqualTo("14/09/2026 20:25:20");
            assertThat(record.get("Người thực hiện")).isEqualTo("Nguyễn Văn Minh");
            assertThat(record.get("Vai trò")).isEqualTo("Quản lý hợp tác xã (VT-02)");
            assertThat(record.get("Hành động")).isEqualTo("Tạo lô sản xuất");
            assertThat(record.get("Loại đối tượng")).isEqualTo("Lô sản xuất");
            assertThat(record.get("Dữ liệu trước")).isEqualTo("Không có dữ liệu");
            assertThat(record.get("Dữ liệu sau")).isEqualTo("Không có dữ liệu");
        }
    }

    @Test
    void writeActivities_shouldPreserveUnknownCodesAndProtectFormulaValues() throws Exception {
        ActivityLog log = ActivityLog.builder()
                .createdAt(LocalDateTime.of(2026, 9, 14, 20, 25, 20))
                .fullName("=cmd")
                .username("unknown")
                .actorRole("CUSTOM_ROLE")
                .action("CUSTOM_ACTION")
                .entityType("CUSTOM_OBJECT")
                .entityId("+1")
                .build();

        String csv = new String(writer().writeActivities(List.of(log)), StandardCharsets.UTF_8);

        assertThat(csv).contains("'=cmd", "CUSTOM_ROLE", "CUSTOM_ACTION", "CUSTOM_OBJECT", "'+1");
    }

    private ActivityLogCsvWriter writer() {
        return new ActivityLogCsvWriter(new ActivityLogExportValueSanitizer(new ObjectMapper()));
    }
}
