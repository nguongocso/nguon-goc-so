package vn.nguongocso.export.constant;

import vn.nguongocso.export.dto.response.FieldGroupDefinition;
import vn.nguongocso.export.dto.response.FieldItemDefinition;
import vn.nguongocso.export.enums.ProfileFieldGroup;

import java.util.*;

/**
 * Định nghĩa danh mục trường và danh sách các trường bắt buộc theo QTN-11.
 */
public final class MandatoryFields {

    private MandatoryFields() {
        // Utility class
    }

    /**
     * Tập hợp 8 trường bắt buộc cốt lõi theo quy tắc QTN-11.
     * Mọi mẫu hồ sơ truy xuất bắt buộc phải chứa tất cả các trường này.
     */
    public static final Set<String> QTN11_MANDATORY_FIELD_KEYS = Set.of(
            "organization.name",
            "farmArea.name",
            "productionLot.name",
            "productionLot.productCategory",
            "shipment.name",
            "shipment.totalQuantity",
            "farmLog.activityType",
            "chainEvent.eventType"
    );

    /**
     * Bản đồ ánh xạ mã trường sang tên hiển thị tiếng Việt.
     */
    public static final Map<String, String> FIELD_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("organization.name", "Tên tổ chức / HTX"),
            Map.entry("organization.code", "Mã tổ chức"),
            Map.entry("organization.taxCode", "Mã số thuế"),
            Map.entry("organization.address", "Địa chỉ trụ sở"),
            Map.entry("organization.phone", "Số điện thoại"),
            Map.entry("organization.email", "Email"),
            Map.entry("organization.representative", "Người đại diện"),
            Map.entry("farmArea.name", "Tên vùng trồng"),
            Map.entry("farmArea.code", "Mã vùng trồng"),
            Map.entry("farmArea.location", "Tọa độ địa lý"),
            Map.entry("farmArea.area", "Diện tích canh tác"),
            Map.entry("farmArea.areaUnit", "Đơn vị diện tích"),
            Map.entry("farmArea.cropType", "Loại cây trồng"),
            Map.entry("productionLot.name", "Tên / Mã lô sản xuất"),
            Map.entry("productionLot.productCategory", "Danh mục sản phẩm"),
            Map.entry("productionLot.plantingDate", "Ngày xuống giống"),
            Map.entry("productionLot.harvestDate", "Ngày thu hoạch"),
            Map.entry("productionLot.expectedQuantity", "Sản lượng dự kiến"),
            Map.entry("productionLot.actualQuantity", "Sản lượng thực tế"),
            Map.entry("productionLot.status", "Trạng thái lô SX"),
            Map.entry("shipment.name", "Tên lô hàng"),
            Map.entry("shipment.totalQuantity", "Số lượng lô hàng"),
            Map.entry("shipment.packagingInfo", "Quy cách đóng gói"),
            Map.entry("shipment.status", "Trạng thái lô hàng"),
            Map.entry("farmLog.activityType", "Loại hoạt động canh tác"),
            Map.entry("farmLog.executedDate", "Ngày thực hiện canh tác"),
            Map.entry("farmLog.material", "Vật tư nông nghiệp"),
            Map.entry("farmLog.quantity", "Liều lượng / Số lượng vật tư"),
            Map.entry("farmLog.notes", "Ghi chú kỹ thuật canh tác"),
            Map.entry("farmLog.attachments", "Chứng từ đính kèm"),
            Map.entry("inspection.sampleSentDate", "Ngày gửi mẫu"),
            Map.entry("inspection.inspectionUnit", "Đơn vị kiểm nghiệm"),
            Map.entry("inspection.criterionName", "Chỉ tiêu kiểm nghiệm"),
            Map.entry("inspection.passed", "Kết quả Đạt / Không đạt"),
            Map.entry("inspection.resultDate", "Ngày cấp kết quả"),
            Map.entry("inspection.expiryDate", "Hạn hiệu lực kiểm nghiệm"),
            Map.entry("certification.standardName", "Tên tiêu chuẩn chứng nhận"),
            Map.entry("certification.certificationCode", "Mã số chứng nhận"),
            Map.entry("certification.issueDate", "Ngày cấp chứng nhận"),
            Map.entry("certification.expiryDate", "Ngày hết hạn chứng nhận"),
            Map.entry("certification.certifier", "Tổ chức chứng nhận"),
            Map.entry("chainEvent.eventType", "Loại sự kiện chuỗi"),
            Map.entry("chainEvent.recordedAt", "Thời điểm sự kiện"),
            Map.entry("chainEvent.recordedBy", "Người ghi nhận sự kiện"),
            Map.entry("chainEvent.location", "Tọa độ địa điểm sự kiện"),
            Map.entry("chainEvent.eventData", "Chi tiết sự kiện")
    );

    /**
     * Kiểm tra và trả về danh sách các trường bắt buộc QTN-11 bị thiếu trong tập trường cung cấp.
     */
    public static List<String> findMissingMandatoryFields(Collection<String> selectedFieldKeys) {
        if (selectedFieldKeys == null || selectedFieldKeys.isEmpty()) {
            return QTN11_MANDATORY_FIELD_KEYS.stream()
                    .map(key -> key + " (" + FIELD_DISPLAY_NAMES.getOrDefault(key, key) + ")")
                    .toList();
        }
        List<String> missing = new ArrayList<>();
        for (String mandatoryKey : QTN11_MANDATORY_FIELD_KEYS) {
            if (!selectedFieldKeys.contains(mandatoryKey)) {
                missing.add(mandatoryKey + " (" + FIELD_DISPLAY_NAMES.getOrDefault(mandatoryKey, mandatoryKey) + ")");
            }
        }
        return missing;
    }

    /**
     * Kiểm tra xem một mã trường có phải là bắt buộc theo QTN-11 hay không.
     */
    public static boolean isMandatory(String fieldKey) {
        return QTN11_MANDATORY_FIELD_KEYS.contains(fieldKey);
    }

    /**
     * Sinh danh mục tất cả các trường có thể chọn trong hệ thống, phân nhóm phục vụ frontend.
     */
    public static List<FieldGroupDefinition> buildFullCatalog() {
        List<FieldGroupDefinition> catalog = new ArrayList<>();

        // 1. ORGANIZATION
        catalog.add(FieldGroupDefinition.builder()
                .fieldGroup(ProfileFieldGroup.ORGANIZATION)
                .groupLabel(ProfileFieldGroup.ORGANIZATION.getLabel())
                .fields(List.of(
                        createItem("organization.name", "Đơn vị sản xuất và chịu trách nhiệm pháp lý"),
                        createItem("organization.code", "Mã định danh nội bộ của tổ chức"),
                        createItem("organization.taxCode", "Mã số thuế doanh nghiệp / HTX"),
                        createItem("organization.address", "Địa chỉ trụ sở hành chính"),
                        createItem("organization.phone", "Số điện thoại liên hệ"),
                        createItem("organization.email", "Địa chỉ email giao dịch"),
                        createItem("organization.representative", "Họ tên người đại diện pháp luật")
                ))
                .build());

        // 2. FARM_AREA
        catalog.add(FieldGroupDefinition.builder()
                .fieldGroup(ProfileFieldGroup.FARM_AREA)
                .groupLabel(ProfileFieldGroup.FARM_AREA.getLabel())
                .fields(List.of(
                        createItem("farmArea.name", "Khu vực địa lý canh tác nông sản"),
                        createItem("farmArea.code", "Mã vùng trồng đã đăng ký"),
                        createItem("farmArea.location", "Tọa độ GPS vùng canh tác"),
                        createItem("farmArea.area", "Quy mô diện tích vùng trồng"),
                        createItem("farmArea.areaUnit", "Đơn vị tính diện tích (m2, ha...)"),
                        createItem("farmArea.cropType", "Chủng loại cây trồng chủ lực")
                ))
                .build());

        // 3. PRODUCTION_LOT
        catalog.add(FieldGroupDefinition.builder()
                .fieldGroup(ProfileFieldGroup.PRODUCTION_LOT)
                .groupLabel(ProfileFieldGroup.PRODUCTION_LOT.getLabel())
                .fields(List.of(
                        createItem("productionLot.name", "Mã định danh lô sản xuất nguồn"),
                        createItem("productionLot.productCategory", "Chủng loại sản phẩm nông sản"),
                        createItem("productionLot.plantingDate", "Thời điểm gieo cấy / xuống giống"),
                        createItem("productionLot.harvestDate", "Thời điểm thu hoạch sản phẩm"),
                        createItem("productionLot.expectedQuantity", "Sản lượng dự kiến thu hoạch"),
                        createItem("productionLot.actualQuantity", "Sản lượng thu hoạch thực tế"),
                        createItem("productionLot.status", "Trạng thái vận hành của lô sản xuất")
                ))
                .build());

        // 4. SHIPMENT
        catalog.add(FieldGroupDefinition.builder()
                .fieldGroup(ProfileFieldGroup.SHIPMENT)
                .groupLabel(ProfileFieldGroup.SHIPMENT.getLabel())
                .fields(List.of(
                        createItem("shipment.name", "Tên chuyến hàng / lô hàng vận chuyển"),
                        createItem("shipment.totalQuantity", "Tổng sản lượng lô hàng xuất kho"),
                        createItem("shipment.packagingInfo", "Thông tin quy cách đóng gói"),
                        createItem("shipment.status", "Trạng thái vận hành của lô hàng")
                ))
                .build());

        // 5. FARM_LOG
        catalog.add(FieldGroupDefinition.builder()
                .fieldGroup(ProfileFieldGroup.FARM_LOG)
                .groupLabel(ProfileFieldGroup.FARM_LOG.getLabel())
                .fields(List.of(
                        createItem("farmLog.activityType", "Hoạt động: gieo cấy, bón phân, phun thuốc, thu hoạch"),
                        createItem("farmLog.executedDate", "Thời điểm nông hộ ghi nhận hoạt động"),
                        createItem("farmLog.material", "Tên vật tư phân bón / thuốc BVTV"),
                        createItem("farmLog.quantity", "Liều lượng / khối lượng vật tư đã sử dụng"),
                        createItem("farmLog.notes", "Ghi chú kỹ thuật canh tác"),
                        createItem("farmLog.attachments", "Tệp hóa đơn, ảnh chứng từ đính kèm")
                ))
                .build());

        // 6. INSPECTION
        catalog.add(FieldGroupDefinition.builder()
                .fieldGroup(ProfileFieldGroup.INSPECTION)
                .groupLabel(ProfileFieldGroup.INSPECTION.getLabel())
                .fields(List.of(
                        createItem("inspection.sampleSentDate", "Thời điểm gửi mẫu kiểm nghiệm"),
                        createItem("inspection.inspectionUnit", "Trung tâm phân tích kiểm nghiệm"),
                        createItem("inspection.criterionName", "Chỉ tiêu kiểm nghiệm dư lượng / vi sinh"),
                        createItem("inspection.passed", "Đánh giá kết quả Đạt / Không đạt"),
                        createItem("inspection.resultDate", "Ngày phòng kiểm nghiệm trả kết quả"),
                        createItem("inspection.expiryDate", "Hạn hiệu lực của phiếu phân tích")
                ))
                .build());

        // 7. CERTIFICATION
        catalog.add(FieldGroupDefinition.builder()
                .fieldGroup(ProfileFieldGroup.CERTIFICATION)
                .groupLabel(ProfileFieldGroup.CERTIFICATION.getLabel())
                .fields(List.of(
                        createItem("certification.standardName", "Tên tiêu chuẩn: VietGAP, GlobalGAP..."),
                        createItem("certification.certificationCode", "Số hiệu chứng chỉ được cấp"),
                        createItem("certification.issueDate", "Ngày cấp chứng chỉ"),
                        createItem("certification.expiryDate", "Ngày hết hạn hiệu lực chứng chỉ"),
                        createItem("certification.certifier", "Đơn vị đánh giá và cấp chứng chỉ")
                ))
                .build());

        // 8. CHAIN_EVENT
        catalog.add(FieldGroupDefinition.builder()
                .fieldGroup(ProfileFieldGroup.CHAIN_EVENT)
                .groupLabel(ProfileFieldGroup.CHAIN_EVENT.getLabel())
                .fields(List.of(
                        createItem("chainEvent.eventType", "Loại sự kiện: thu hoạch, đóng gói, vận chuyển, thu mua"),
                        createItem("chainEvent.recordedAt", "Thời gian ghi nhận sự kiện trên chuỗi"),
                        createItem("chainEvent.recordedBy", "Họ tên người ghi nhận sự kiện"),
                        createItem("chainEvent.location", "Tọa độ địa điểm nơi diễn ra sự kiện"),
                        createItem("chainEvent.eventData", "Dữ liệu chi tiết đặc thù của sự kiện")
                ))
                .build());

        return catalog;
    }

    private static FieldItemDefinition createItem(String fieldKey, String description) {
        return FieldItemDefinition.builder()
                .fieldKey(fieldKey)
                .displayName(FIELD_DISPLAY_NAMES.getOrDefault(fieldKey, fieldKey))
                .mandatory(isMandatory(fieldKey))
                .description(description)
                .build();
    }
}
