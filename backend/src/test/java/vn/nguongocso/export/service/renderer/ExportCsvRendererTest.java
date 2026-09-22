package vn.nguongocso.export.service.renderer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Kiểm thử đơn vị và bộ test bất biến (Golden Master) cho ExportCsvRenderer (NCL-12-CN-003).
 * Xác thực việc kết xuất CSV độc lập với giao dịch, hỗ trợ BOM UTF-8 và bảo vệ cấu trúc byte-for-byte.
 */
class ExportCsvRendererTest {

    private ExportCsvRenderer exportCsvRenderer;
    private ObjectMapper objectMapper;
    private Map<String, Object> materializedSnapshot;

    @BeforeEach
    void setUp() {
        exportCsvRenderer = new ExportCsvRenderer();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        materializedSnapshot = new LinkedHashMap<>();
        materializedSnapshot.put("shipmentId", UUID.randomUUID());

        Map<String, Object> templateInfo = new LinkedHashMap<>();
        templateInfo.put("templateName", "Mẫu chuẩn VietGAP");
        templateInfo.put("partnerName", "Siêu thị Co.op");
        templateInfo.put("isDefault", false);
        templateInfo.put("totalFieldsConfigured", 10);
        materializedSnapshot.put("appliedTemplate", templateInfo);

        Map<String, Object> orgData = new LinkedHashMap<>();
        orgData.put("name", "Hợp tác xã Nông nghiệp Lam Đồng");
        orgData.put("code", "HTX-LD-001");
        orgData.put("address", "123 Đường Nông Nghiệp, Đơn Dương");
        materializedSnapshot.put("organization", orgData);

        Map<String, Object> farmAreaData = new LinkedHashMap<>();
        farmAreaData.put("name", "Vùng chuyên canh Cà rốt");
        farmAreaData.put("area", new BigDecimal("15.50"));
        materializedSnapshot.put("farmArea", farmAreaData);

        Map<String, Object> lotData = new LinkedHashMap<>();
        lotData.put("name", "Lô Cà rốt 2026-01");
        lotData.put("productCategory", "Củ quả sạch");
        materializedSnapshot.put("productionLot", lotData);

        Map<String, Object> shipmentData = new LinkedHashMap<>();
        shipmentData.put("name", "Chuyến hàng Đà Lạt - Sài Gòn #01");
        shipmentData.put("totalQuantity", 2500L);
        shipmentData.put("status", "Đang lưu thông");
        materializedSnapshot.put("shipment", shipmentData);

        List<Map<String, Object>> farmLogs = new ArrayList<>();
        Map<String, Object> log1 = new LinkedHashMap<>();
        log1.put("activityType", "Tưới nước");
        log1.put("executedDate", LocalDate.of(2026, 9, 15));
        log1.put("material", "Hệ thống tưới nhỏ giọt");
        log1.put("attachments", List.of("hinh-anh-tuoi-nuoc.jpg"));
        farmLogs.add(log1);
        materializedSnapshot.put("farmLogs", farmLogs);

        List<Map<String, Object>> timelineEvents = new ArrayList<>();
        Map<String, Object> event1 = new LinkedHashMap<>();
        event1.put("eventType", "Thu hoạch");
        event1.put("recordedAt", LocalDateTime.of(2026, 9, 20, 8, 30, 0));
        event1.put("recordedBy", "Nguyễn Văn Kiểm Kê");
        event1.put("location", "11.85, 108.45");
        timelineEvents.add(event1);
        materializedSnapshot.put("timelineEvents", timelineEvents);
    }

    @Test
    @DisplayName("Snapshot rời transaction: Xác nhận không chứa JPA Entity hoặc Hibernate Proxy")
    void shouldContainNoJpaEntitiesOrHibernateProxiesWhenSnapshotMaterialized() {
        assertPureMaterializedStructure(materializedSnapshot);
    }

    @Test
    @DisplayName("Renderer hoạt động độc lập sau transaction: ExportCsvRenderer tạo CSV chuẩn với BOM UTF-8")
    void shouldGenerateCsvWithBomAndHeadersWhenRenderingPreviewWithoutTransaction() {
        assertThatCode(() -> {
            String csv = exportCsvRenderer.renderPreviewToCsv(materializedSnapshot);
            assertThat(csv).isNotNull();

            assertThat(csv).startsWith("\uFEFF");

            assertThat(csv).contains("# HỒ SƠ TRUY XUẤT NGUỒN GỐC SẢN PHẨM");
            assertThat(csv).contains("# Mẫu hồ sơ: Mẫu chuẩn VietGAP");
            assertThat(csv).contains("Nhóm thông tin,Trường dữ liệu,Giá trị");

            assertThat(csv).contains("# LỊCH TRÌNH CANH TÁC & CHỨNG TỪ");
            assertThat(csv).contains("# DÒNG SỰ KIỆN CHUỖI CUNG ỨNG");

            assertThat(csv).contains("Hợp tác xã Nông nghiệp Lam Đồng");
            assertThat(csv).contains("Vùng chuyên canh Cà rốt");
            assertThat(csv).contains("Lô Cà rốt 2026-01");
            assertThat(csv).contains("Chuyến hàng Đà Lạt - Sài Gòn #01");
            assertThat(csv).contains("Tưới nước");
            assertThat(csv).contains("Thu hoạch");

            byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
            assertThat(bytes).isNotEmpty();
            assertThat(bytes[0]).isEqualTo((byte) 0xEF);
            assertThat(bytes[1]).isEqualTo((byte) 0xBB);
            assertThat(bytes[2]).isEqualTo((byte) 0xBF);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Renderer hoạt động độc lập sau transaction: JSON serializer tuần tự hóa snapshot hoàn hảo")
    void shouldSerializeJsonSuccessfullyWhenMaterializedWithoutTransaction() {
        assertThatCode(() -> {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(materializedSnapshot);
            assertThat(json).isNotNull();
            assertThat(json).contains("\"Hợp tác xã Nông nghiệp Lam Đồng\"");
            assertThat(json).contains("\"Vùng chuyên canh Cà rốt\"");
            assertThat(json).contains("\"appliedTemplate\"");
            assertThat(json).contains("\"farmLogs\"");
            assertThat(json).contains("\"timelineEvents\"");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Golden-Master CSV: So sánh byte-for-byte toàn bộ byte[] với fixture tham chiếu")
    void shouldMatchReferenceGoldenMasterByteForByteWhenRenderingComplexCsv() {
        LocalDateTime fixedExportTime = LocalDateTime.of(2026, 9, 21, 10, 0, 0);

        Map<String, Object> fixtureSnapshot = new LinkedHashMap<>();
        fixtureSnapshot.put("exportedAt", fixedExportTime);

        Map<String, Object> tpl = new LinkedHashMap<>();
        tpl.put("templateName", "Mẫu \"Đặc Biệt\", Xuất Khẩu EU");
        fixtureSnapshot.put("appliedTemplate", tpl);

        Map<String, Object> org = new LinkedHashMap<>();
        org.put("name", "Hợp tác xã Nông nghiệp Cầu Đất, Đà Lạt");
        org.put("code", "HTX-CD-001");
        org.put("address", "Số 45, Đường Trần Hưng Đạo, Phường 10, TP. Đà Lạt");
        fixtureSnapshot.put("organization", org);

        Map<String, Object> farmArea = new LinkedHashMap<>();
        farmArea.put("name", "Vùng trồng Cà phê Arabica Cầu Đất");
        farmArea.put("area", new BigDecimal("25.75"));
        fixtureSnapshot.put("farmArea", farmArea);

        Map<String, Object> lot = new LinkedHashMap<>();
        lot.put("name", "Lô Cà phê Arabica \"Thượng Hạng\"");
        lot.put("productCategory", "Cà phê nhân đặc sản");
        fixtureSnapshot.put("productionLot", lot);

        Map<String, Object> shipment = new LinkedHashMap<>();
        shipment.put("name", "Chuyến hàng đi Hamburg, Đức");
        shipment.put("totalQuantity", 5000L);
        shipment.put("packagingInfo", "Bao đay 60kg, đóng gói chuẩn xuất khẩu");
        fixtureSnapshot.put("shipment", shipment);

        List<Map<String, Object>> certs = new ArrayList<>();
        Map<String, Object> cert1 = new LinkedHashMap<>();
        cert1.put("name", "Chứng nhận Hữu cơ Châu Âu (EU Organic)");
        cert1.put("standardName", "Tiêu chuẩn Nông nghiệp Hữu cơ EU 2018/848");
        cert1.put("certificationCode", "VN-BIO-149");
        cert1.put("issueDate", LocalDate.of(2025, 1, 15));
        cert1.put("expiryDate", LocalDate.of(2027, 1, 14));
        cert1.put("certifier", "Control Union Vietnam, Co. Ltd.");
        certs.add(cert1);
        fixtureSnapshot.put("certifications", certs);

        List<Map<String, Object>> farmLogs = new ArrayList<>();
        Map<String, Object> log1 = new LinkedHashMap<>();
        log1.put("executedDate", LocalDate.of(2026, 8, 10));
        log1.put("activityType", "Bón phân hữu cơ sinh học");
        log1.put("material", "Phân trùn quế vi sinh");
        log1.put("quantity", 500);
        log1.put("unit", "kg");
        log1.put("notes", "Ghi chú canh tác:\n- Độ ẩm đất đạt 65%, đạt chuẩn VietGAP\n- Thời tiết nắng ráo");
        log1.put("attachments", List.of("phieu-kiem-tra,phan-bon.pdf", "hinh-anh-bon-phan.jpg"));
        farmLogs.add(log1);
        fixtureSnapshot.put("farmLogs", farmLogs);

        List<Map<String, Object>> inspections = new ArrayList<>();
        Map<String, Object> insp1 = new LinkedHashMap<>();
        insp1.put("sampleSentDate", LocalDate.of(2026, 9, 1));
        insp1.put("inspectionUnit", "Trung tâm Phân tích & Giám định Eurofins Sắc Ký Hải Đăng");
        insp1.put("criterionName", "Dư lượng hóa chất Glyphosate & Kim loại nặng");
        insp1.put("passed", "Đạt");
        insp1.put("resultDate", LocalDate.of(2026, 9, 5));
        insp1.put("expiryDate", LocalDate.of(2027, 9, 5));
        inspections.add(insp1);
        fixtureSnapshot.put("inspections", inspections);

        List<Map<String, Object>> timeline = new ArrayList<>();
        Map<String, Object> event1 = new LinkedHashMap<>();
        event1.put("recordedAt", LocalDateTime.of(2026, 9, 20, 7, 15, 0));
        event1.put("eventType", "Thu hoạch (HARVEST)");
        event1.put("location", "11.9404, 108.4583");
        event1.put("eventData", "Nhiệt độ: 18°C; Độ ẩm: 70%");
        event1.put("recordedBy", "Trần Văn Quản Lý");
        timeline.add(event1);
        fixtureSnapshot.put("timelineEvents", timeline);

        String expectedCsv = "\uFEFF# HỒ SƠ TRUY XUẤT NGUỒN GỐC SẢN PHẨM\n"
                + "# Mẫu hồ sơ: \"Mẫu \"\"Đặc Biệt\"\", Xuất Khẩu EU\"\n"
                + "# Thời gian xuất: 2026-09-21 10:00:00\n\n"
                + "Nhóm thông tin,Trường dữ liệu,Giá trị\n"
                + "Đơn vị sản xuất (HTX),Tên tổ chức,\"Hợp tác xã Nông nghiệp Cầu Đất, Đà Lạt\"\n"
                + "Đơn vị sản xuất (HTX),Mã định danh,HTX-CD-001\n"
                + "Đơn vị sản xuất (HTX),Địa chỉ,\"Số 45, Đường Trần Hưng Đạo, Phường 10, TP. Đà Lạt\"\n"
                + "Vùng trồng,Tên vùng trồng,Vùng trồng Cà phê Arabica Cầu Đất\n"
                + "Vùng trồng,Diện tích canh tác,25.75\n"
                + "Lô sản xuất,Tên lô sản xuất,\"Lô Cà phê Arabica \"\"Thượng Hạng\"\"\"\n"
                + "Lô sản xuất,Danh mục sản phẩm,Cà phê nhân đặc sản\n"
                + "Lô hàng vận chuyển,Tên lô hàng,\"Chuyến hàng đi Hamburg, Đức\"\n"
                + "Lô hàng vận chuyển,Số lượng,5000\n"
                + "Lô hàng vận chuyển,Quy cách đóng gói,\"Bao đay 60kg, đóng gói chuẩn xuất khẩu\"\n"
                + "\n# CHỨNG NHẬN TIÊU CHUẨN\n"
                + "STT,Tên chứng nhận,Tiêu chuẩn,Số hiệu,Ngày cấp,Hạn hiệu lực,Tổ chức chứng nhận\n"
                + "1,Chứng nhận Hữu cơ Châu Âu (EU Organic),Tiêu chuẩn Nông nghiệp Hữu cơ EU 2018/848,VN-BIO-149,2025-01-15,2027-01-14,\"Control Union Vietnam, Co. Ltd.\"\n"
                + "\n# LỊCH TRÌNH CANH TÁC & CHỨNG TỪ\n"
                + "STT,Ngày thực hiện,Hoạt động,Vật tư / Số lượng,Ghi chú,Chứng từ đính kèm\n"
                + "1,2026-08-10,Bón phân hữu cơ sinh học,Phân trùn quế vi sinh (500 kg),\"Ghi chú canh tác:\n"
                + "- Độ ẩm đất đạt 65%, đạt chuẩn VietGAP\n"
                + "- Thời tiết nắng ráo\",\"phieu-kiem-tra,phan-bon.pdf; hinh-anh-bon-phan.jpg\"\n"
                + "\n# LỊCH SỬ KIỂM NGHIỆM\n"
                + "STT,Ngày gửi mẫu,Đơn vị kiểm nghiệm,Chỉ tiêu / Tiêu chuẩn,Kết quả,Ngày cấp kết quả,Hạn hiệu lực\n"
                + "1,2026-09-01,Trung tâm Phân tích & Giám định Eurofins Sắc Ký Hải Đăng,Dư lượng hóa chất Glyphosate & Kim loại nặng,Đạt,2026-09-05,2027-09-05\n"
                + "\n# DÒNG SỰ KIỆN CHUỖI CUNG ỨNG\n"
                + "STT,Thời điểm ghi nhận,Loại sự kiện,Tọa độ địa điểm,Chi tiết sự kiện,Người ghi nhận\n"
                + "1,2026-09-20T07:15,Thu hoạch (HARVEST),\"11.9404, 108.4583\",Nhiệt độ: 18°C; Độ ẩm: 70%,Trần Văn Quản Lý\n";

        String actualCsv = exportCsvRenderer.renderPreviewToCsv(fixtureSnapshot, fixedExportTime);
        byte[] actualBytes = actualCsv.getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = expectedCsv.getBytes(StandardCharsets.UTF_8);

        assertThat(actualCsv).isEqualTo(expectedCsv);
        assertThat(actualBytes).isEqualTo(expectedBytes);

        assertThat(actualBytes[0]).isEqualTo((byte) 0xEF);
        assertThat(actualBytes[1]).isEqualTo((byte) 0xBB);
        assertThat(actualBytes[2]).isEqualTo((byte) 0xBF);
    }

    private void assertPureMaterializedStructure(Object node) {
        if (node == null) {
            return;
        }

        Class<?> clazz = node.getClass();
        String className = clazz.getName();

        assertThat(className)
                .as("Lớp '%s' không được thuộc gói org.hibernate", className)
                .doesNotContain("org.hibernate");

        assertThat(clazz.isAnnotationPresent(jakarta.persistence.Entity.class))
                .as("Đối tượng '%s' không được là @Entity", className)
                .isFalse();

        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                assertThat(entry.getKey()).isInstanceOf(String.class);
                assertPureMaterializedStructure(entry.getValue());
            }
        } else if (node instanceof Collection<?> coll) {
            for (Object item : coll) {
                assertPureMaterializedStructure(item);
            }
        }
    }
}
