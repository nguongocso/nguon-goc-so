package vn.nguongocso.export.service.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.export.schema.OpenDataSchema;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OpenDataExportProcessorCsvTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ChainEventRepository chainEventRepository;
    @Mock
    private FarmLogRepository farmLogRepository;
    @Mock
    private ProductionLotCertificationRepository productionLotCertificationRepository;

    private OpenDataExportProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OpenDataExportProcessor(
                shipmentRepository,
                chainEventRepository,
                farmLogRepository,
                productionLotCertificationRepository);
    }

    @Test
    @DisplayName("convertToCsv: Mỗi dòng shipment chỉ chứa timeline của chính shipment đó, không bị rò rỉ timeline của shipment khác")
    void convertToCsv_shouldIsolateTimelinePerShipment() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        UUID idC = UUID.randomUUID();

        OpenDataSchema.TimelineEvent eventA = OpenDataSchema.TimelineEvent.builder()
                .eventType("HARVEST")
                .recordedAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .recordedBy("Nguyen Van A")
                .data(Map.of("note", "Thu hoach lo A"))
                .build();

        OpenDataSchema.TimelineEvent eventB = OpenDataSchema.TimelineEvent.builder()
                .eventType("TRANSPORT")
                .recordedAt(LocalDateTime.of(2026, 9, 2, 9, 0))
                .recordedBy("Tran Van B")
                .data(Map.of("note", "Van chuyen lo B"))
                .build();

        OpenDataSchema.TimelineEvent eventC = OpenDataSchema.TimelineEvent.builder()
                .eventType("PROCUREMENT")
                .recordedAt(LocalDateTime.of(2026, 9, 3, 10, 0))
                .recordedBy("Le Van C")
                .data(Map.of("note", "Thu mua lo C"))
                .build();

        OpenDataSchema.ShipmentData shipmentA = OpenDataSchema.ShipmentData.builder()
                .id(idA)
                .name("Lô hàng Cam Xoàn A")
                .productionLotName("Lô sản xuất Cam 01")
                .productCategory("Cam Xoàn")
                .totalQuantity(1500.5)
                .unit("kg")
                .status("APPROVED")
                .timeline(List.of(eventA))
                .build();

        OpenDataSchema.ShipmentData shipmentB = OpenDataSchema.ShipmentData.builder()
                .id(idB)
                .name("Lô hàng Xoài Cát B")
                .productionLotName("Lô sản xuất Xoài 02")
                .productCategory("Xoài Cát")
                .totalQuantity(2000.0)
                .unit("kg")
                .status("IN_TRANSIT")
                .timeline(List.of(eventB))
                .build();

        OpenDataSchema.ShipmentData shipmentC = OpenDataSchema.ShipmentData.builder()
                .id(idC)
                .name("Lô hàng Sầu Riêng C")
                .productionLotName("Lô sản xuất Sầu Riêng 03")
                .productCategory("Sầu Riêng Ri6")
                .totalQuantity(3000.75)
                .unit("kg")
                .status("DELIVERED")
                .timeline(List.of(eventC))
                .build();

        OpenDataSchema schema = OpenDataSchema.builder()
                .exportedAt(LocalDateTime.of(2026, 9, 24, 12, 0))
                .shipments(List.of(shipmentA, shipmentB, shipmentC))
                .build();

        String csv = processor.convertToCsv(schema);

        assertThat(csv).isNotNull();
        assertThat(csv).startsWith("\uFEFFshipmentId,name,productionLotName");

        String[] lines = csv.split("\n");
        // 1 header + 3 data lines = 4 lines
        assertThat(lines).hasSize(4);

        String lineA = lines[1];
        String lineB = lines[2];
        String lineC = lines[3];

        // Line A contains event A and does NOT contain event B or event C
        assertThat(lineA).contains(idA.toString());
        assertThat(lineA).contains("Thu hoach lo A");
        assertThat(lineA).doesNotContain("Van chuyen lo B");
        assertThat(lineA).doesNotContain("Thu mua lo C");

        // Line B contains event B and does NOT contain event A or event C
        assertThat(lineB).contains(idB.toString());
        assertThat(lineB).contains("Van chuyen lo B");
        assertThat(lineB).doesNotContain("Thu hoach lo A");
        assertThat(lineB).doesNotContain("Thu mua lo C");

        // Line C contains event C and does NOT contain event A or event B
        assertThat(lineC).contains(idC.toString());
        assertThat(lineC).contains("Thu mua lo C");
        assertThat(lineC).doesNotContain("Thu hoach lo A");
        assertThat(lineC).doesNotContain("Van chuyen lo B");
    }
}
