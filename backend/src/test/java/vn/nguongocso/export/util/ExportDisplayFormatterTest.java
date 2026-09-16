package vn.nguongocso.export.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.trace.enums.ShipmentStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đơn vị cho ExportDisplayFormatter.
 * Xác nhận việc chuyển đổi các giá trị Enum và JSON chi tiết sang tiếng Việt chính xác.
 */
public class ExportDisplayFormatterTest {

    @Test
    @DisplayName("Việt hóa chính xác loại hình tổ chức")
    void testFormatOrganizationType() {
        assertThat(ExportDisplayFormatter.formatOrganizationType(OrganizationType.COOPERATIVE)).isEqualTo("Hợp tác xã");
        assertThat(ExportDisplayFormatter.formatOrganizationType(OrganizationType.ENTERPRISE)).isEqualTo("Doanh nghiệp");
        assertThat(ExportDisplayFormatter.formatOrganizationType(OrganizationType.GOVERNMENT)).isEqualTo("Cán bộ quản lý ngành");
        assertThat(ExportDisplayFormatter.formatOrganizationType(OrganizationType.SYSTEM)).isEqualTo("Tổ chức hệ thống");
        assertThat(ExportDisplayFormatter.formatOrganizationType(null)).isNull();
    }

    @Test
    @DisplayName("Việt hóa chính xác trạng thái tổ chức")
    void testFormatOrganizationStatus() {
        assertThat(ExportDisplayFormatter.formatOrganizationStatus(OrganizationStatus.ACTIVE)).isEqualTo("Đang hoạt động");
        assertThat(ExportDisplayFormatter.formatOrganizationStatus(OrganizationStatus.INACTIVE)).isEqualTo("Ngừng hoạt động");
        assertThat(ExportDisplayFormatter.formatOrganizationStatus(null)).isNull();
    }

    @Test
    @DisplayName("Việt hóa chính xác trạng thái lô sản xuất")
    void testFormatProductionLotStatus() {
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.DRAFT)).isEqualTo("Bản nháp");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.PENDING)).isEqualTo("Chờ duyệt");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.APPROVED)).isEqualTo("Đã duyệt");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.REJECTED)).isEqualTo("Bị từ chối");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.HARVESTED)).isEqualTo("Đã thu hoạch");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.PREPROCESSED)).isEqualTo("Đã sơ chế");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.PACKAGED)).isEqualTo("Đã đóng gói");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.CLOSED)).isEqualTo("Đã hoàn thành");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.RECALLED)).isEqualTo("Đã thu hồi");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.CANCELLED)).isEqualTo("Đã hủy");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(ProductionLotStatus.DISPOSED)).isEqualTo("Đã loại bỏ");
        assertThat(ExportDisplayFormatter.formatProductionLotStatus(null)).isNull();
    }

    @Test
    @DisplayName("Việt hóa chính xác trạng thái lô hàng vận chuyển")
    void testFormatShipmentStatus() {
        assertThat(ExportDisplayFormatter.formatShipmentStatus(ShipmentStatus.DRAFT)).isEqualTo("Bản nháp");
        assertThat(ExportDisplayFormatter.formatShipmentStatus(ShipmentStatus.CODE_PRINTED)).isEqualTo("Đã in mã");
        assertThat(ExportDisplayFormatter.formatShipmentStatus(ShipmentStatus.ACTIVATED)).isEqualTo("Đã kích hoạt");
        assertThat(ExportDisplayFormatter.formatShipmentStatus(ShipmentStatus.SPLIT)).isEqualTo("Đã tách lô");
        assertThat(ExportDisplayFormatter.formatShipmentStatus(ShipmentStatus.RECALLING)).isEqualTo("Đang thu hồi");
        assertThat(ExportDisplayFormatter.formatShipmentStatus(ShipmentStatus.RECALLED)).isEqualTo("Đã thu hồi");
        assertThat(ExportDisplayFormatter.formatShipmentStatus(null)).isNull();
    }

    @Test
    @DisplayName("Việt hóa chính xác hoạt động lịch canh tác")
    void testFormatFarmActivityType() {
        assertThat(ExportDisplayFormatter.formatFarmActivityType(FarmActivityType.PLANTING)).isEqualTo("Gieo giống / Xuống giống");
        assertThat(ExportDisplayFormatter.formatFarmActivityType(FarmActivityType.WATERING)).isEqualTo("Tưới nước");
        assertThat(ExportDisplayFormatter.formatFarmActivityType(FarmActivityType.FERTILIZING)).isEqualTo("Bón phân");
        assertThat(ExportDisplayFormatter.formatFarmActivityType(FarmActivityType.PESTICIDE)).isEqualTo("Phun thuốc BVTV");
        assertThat(ExportDisplayFormatter.formatFarmActivityType(FarmActivityType.WEEDING)).isEqualTo("Làm cỏ");
        assertThat(ExportDisplayFormatter.formatFarmActivityType(FarmActivityType.HARVESTING)).isEqualTo("Thu hoạch");
        assertThat(ExportDisplayFormatter.formatFarmActivityType(FarmActivityType.OTHER)).isEqualTo("Hoạt động khác");
        assertThat(ExportDisplayFormatter.formatFarmActivityType(null)).isNull();
    }

    @Test
    @DisplayName("Việt hóa chính xác loại sự kiện chuỗi cung ứng")
    void testFormatChainEventType() {
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.HARVEST)).isEqualTo("Thu hoạch");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.PREPROCESSING)).isEqualTo("Sơ chế và phân loại");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.PACKAGING)).isEqualTo("Đóng gói");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.TRANSPORT)).isEqualTo("Vận chuyển");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.PROCUREMENT)).isEqualTo("Thu mua");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.CORRECTION)).isEqualTo("Điều chỉnh dữ liệu");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.WAREHOUSE_RECEIPT)).isEqualTo("Nhập kho đối chiếu");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.STORAGE_CONDITION)).isEqualTo("Theo dõi bảo quản");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.HANDOVER)).isEqualTo("Bàn giao");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.WAREHOUSE_ENTRY)).isEqualTo("Nhập kho HTX");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.WAREHOUSE_EXIT)).isEqualTo("Xuất kho HTX");
        assertThat(ExportDisplayFormatter.formatChainEventType(ChainEventType.SPLIT)).isEqualTo("Tách lô");
        assertThat(ExportDisplayFormatter.formatChainEventType(null)).isNull();
    }

    @Test
    @DisplayName("Việt hóa chính xác đơn vị diện tích")
    void testFormatAreaUnit() {
        assertThat(ExportDisplayFormatter.formatAreaUnit(AreaUnit.HA)).isEqualTo("ha");
        assertThat(ExportDisplayFormatter.formatAreaUnit(AreaUnit.KM2)).isEqualTo("km²");
        assertThat(ExportDisplayFormatter.formatAreaUnit(null)).isNull();
    }

    @Test
    @DisplayName("Định dạng dữ liệu JSON chi tiết sự kiện thành chuỗi tiếng Việt dễ hiểu")
    void testFormatEventData() {
        String json = "{\"notes\":\"Thu mua lô cà rốt\",\"quantity\":1500,\"sanLuongKg\":1500}";
        String formatted = ExportDisplayFormatter.formatEventData(json, "; ");
        assertThat(formatted).contains("Ghi chú: Thu mua lô cà rốt");
        assertThat(formatted).contains("Số lượng: 1500");
        assertThat(formatted).contains("Sản lượng: 1500");

        // Kiểm tra xử lý rỗng hoặc null
        assertThat(ExportDisplayFormatter.formatEventData(null, "; ")).isEmpty();
        assertThat(ExportDisplayFormatter.formatEventData("", "; ")).isEmpty();
        assertThat(ExportDisplayFormatter.formatEventData("   ", "; ")).isEmpty();
    }
}
