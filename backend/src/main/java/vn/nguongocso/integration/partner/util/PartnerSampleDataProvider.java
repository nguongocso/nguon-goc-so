package vn.nguongocso.integration.partner.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.integration.partner.dto.response.PartnerCertificationResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerFarmAreaResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerFarmLogSummaryResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerLotDossierResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerLotInfoResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerOrgInfoResponse;
import vn.nguongocso.publicapi.dto.response.PublicChainEventItem;
import vn.nguongocso.publicapi.dto.response.PublicInspectionCriterionResultDto;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;
import vn.nguongocso.report.dto.response.Gs1DossierExportResponse;
import vn.nguongocso.report.dto.response.Gs1Event;
import vn.nguongocso.report.dto.response.Gs1EventLocation;
import vn.nguongocso.report.dto.response.Gs1Inspection;
import vn.nguongocso.report.dto.response.Gs1InspectionCriterion;
import vn.nguongocso.report.dto.response.Gs1ShipmentInfo;
/**
 * Lớp tiện ích cung cấp bộ dữ liệu mẫu cho chế độ thử nghiệm.
*/
public final class PartnerSampleDataProvider {
    public static final String TEST_NOTICE = "Dữ liệu thử nghiệm (Sandbox Mode) - Không phải dữ liệu thực tế";

    public static final String SAMPLE_LOT_ID = "sample-lot-001";
    /**
     * Khởi tạo trình cung cấp dữ liệu mẫu.
     */
    private PartnerSampleDataProvider() {
    }
    /**
     * Tạo hồ sơ truy xuất lô sản xuất mẫu.
     */
    public static PartnerLotDossierResponse getSampleLotDossier() {
        PartnerLotInfoResponse lotInfo = PartnerLotInfoResponse.builder()
                .lotId("00000000-0000-0000-0000-000000000001")
                .lotName("[DỮ LIỆU MẪU] Lô Xoài Cát Chu Thử Nghiệm")
                .productCategoryName("Xoài Cát Chu")
                .expectedQuantity(10000.0)
                .actualQuantity(9800.0)
                .quantityUnit("KG")
                .plantingDate(LocalDate.of(2026, 2, 1))
                .harvestDate(LocalDate.of(2026, 7, 15))
                .status(ProductionLotStatus.HARVESTED)
                .build();

        PartnerOrgInfoResponse orgInfo = PartnerOrgInfoResponse.builder()
                .organizationId("00000000-0000-0000-0000-000000000002")
                .organizationName("[DỮ LIỆU MẪU] Hợp Tác Xã Trái Cây Mẫu Nguồn Gốc Số")
                .organizationCode("HTX-TEST-DEMO")
                .address("Khu Thực Nghiệm Công Nghệ Nông Nghiệp Số")
                .phone("0901234567")
                .email("sandbox@nguongocso.vn")
                .build();

        PartnerFarmAreaResponse farmAreaInfo = PartnerFarmAreaResponse.builder()
                .farmAreaId("00000000-0000-0000-0000-000000000003")
                .farmAreaName("[DỮ LIỆU MẪU] Vùng Canh Tác Thực Nghiệm A1")
                .area(2.0)
                .areaUnit("HECTARE")
                .build();

        List<PartnerCertificationResponse> certifications = List.of(
                PartnerCertificationResponse.builder()
                        .certificationName("[DỮ LIỆU MẪU] Chứng nhận VietGAP Mẫu")
                        .standardName("VietGAP")
                        .certificateCode("VG-TEST-9999")
                        .issueDate(LocalDate.of(2026, 1, 1))
                        .expiryDate(LocalDate.of(2027, 1, 1))
                        .issuedBy("Hệ Thống Kiểm Nghiệm Thử Nghiệm")
                        .build()
        );

        PartnerFarmLogSummaryResponse logSummary = PartnerFarmLogSummaryResponse.builder()
                .totalLogsRecorded(25)
                .lastActivityAt(LocalDateTime.of(2026, 7, 15, 10, 0, 0))
                .build();

        return PartnerLotDossierResponse.builder()
                .lotInfo(lotInfo)
                .organizationInfo(orgInfo)
                .farmAreaInfo(farmAreaInfo)
                .certifications(certifications)
                .farmLogSummary(logSummary)
                .isTest(true)
                .testNotice(TEST_NOTICE)
                .build();
    }
    /**
     * Tạo dữ liệu tra cứu hành trình theo mã tem mẫu.
     */
    public static PublicTraceResponse getSampleTraceResponse() {
        Map<String, Object> harvestData = new LinkedHashMap<>();
        harvestData.put("field", "Đồi chè Long Cốc Thử Nghiệm");
        harvestData.put("technique", "Hái thủ công 1 tôm 2 lá");

        Map<String, Object> packagingData = new LinkedHashMap<>();
        packagingData.put("packagingType", "Hút chân không túi thiếc 100g");
        packagingData.put("facility", "Xưởng chế biến chè thử nghiệm");

        Map<String, Object> transportData = new LinkedHashMap<>();
        transportData.put("fromLocation", "Tân Sơn, Phú Thọ");
        transportData.put("toLocation", "Kho trung chuyển Hà Nội");

        List<PublicChainEventItem> events = List.of(
                PublicChainEventItem.builder()
                        .eventType("HARVESTING")
                        .eventData(harvestData)
                        .recordedAt(LocalDateTime.of(2026, 7, 20, 8, 0, 0))
                        .build(),
                PublicChainEventItem.builder()
                        .eventType("PACKAGING")
                        .eventData(packagingData)
                        .recordedAt(LocalDateTime.of(2026, 7, 21, 14, 30, 0))
                        .build(),
                PublicChainEventItem.builder()
                        .eventType("TRANSPORT")
                        .eventData(transportData)
                        .recordedAt(LocalDateTime.of(2026, 7, 22, 9, 0, 0))
                        .build()
        );

        List<PublicInspectionCriterionResultDto> inspections = List.of(
                PublicInspectionCriterionResultDto.builder()
                        .id("00000000-0000-0000-0000-000000000005")
                        .criterionName("Dư lượng thuốc bảo vệ thực vật")
                        .passed(true)
                        .inspectionDate(LocalDate.of(2026, 7, 19))
                        .build()
        );

        return PublicTraceResponse.builder()
                .codeValue("TEST-TRACE-001")
                .productName("[DỮ LIỆU MẪU] Chè Xanh Long Cốc Thử Nghiệm")
                .shipmentCode("LH-TEST-2026")
                .shipmentStatus("ACTIVATED")
                .recalled(false)
                .locked(false)
                .events(events)
                .inspections(inspections)
                .isTest(true)
                .testNotice(TEST_NOTICE)
                .build();
    }
    /**
     * Tạo hồ sơ xuất theo lược đồ GS1 mô phỏng mẫu.
     */
    public static Gs1DossierExportResponse getSampleGs1DossierResponse() {
        Gs1ShipmentInfo shipmentInfo = Gs1ShipmentInfo.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                .name("[DỮ LIỆU MẪU] Lô Hàng Xoài Cát Xuất Khẩu Thử Nghiệm")
                .codeValues(List.of("TEST-TRACE-001"))
                .productCategory("Xoài Cát Chu")
                .totalQuantity(5000L)
                .unit("KG")
                .status("ACTIVATED")
                .organization(Gs1ShipmentInfo.OrganizationInfo.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                        .code("HTX-TEST-DEMO")
                        .name("[DỮ LIỆU MẪU] Hợp Tác Xã Trái Cây Mẫu Nguồn Gốc Số")
                        .build())
                .build();

        Map<String, Object> eventDetails = new LinkedHashMap<>();
        eventDetails.put("yield", "5000 KG");

        Gs1Event event = Gs1Event.builder()
                .eventId(UUID.fromString("00000000-0000-0000-0000-000000000021"))
                .eventType("HARVESTING")
                .eventTypeLabel("Thu hoạch")
                .recordedAt(LocalDateTime.of(2026, 7, 20, 8, 0, 0))
                .recordedBy("Kỹ thuật viên Thử nghiệm")
                .location(Gs1EventLocation.builder()
                        .latitude(10.352)
                        .longitude(105.987)
                        .address(null)
                        .build())
                .details(eventDetails)
                .build();

        Gs1Inspection inspection = Gs1Inspection.builder()
                .requestId(UUID.fromString("00000000-0000-0000-0000-000000000031"))
                .inspectionUnit("Trung Tâm Kiểm Nghiệm Thực Nghiệm")
                .sampleSentDate(LocalDate.of(2026, 7, 18))
                .status("PASSED")
                .criteria(List.of(
                        Gs1InspectionCriterion.builder()
                                .criterionCode("CT-TEST-01")
                                .criterionName("Dư lượng kim loại nặng")
                                .standardName("VietGAP")
                                .passed(true)
                                .resultDate(LocalDate.of(2026, 7, 19))
                                .expiryDate(LocalDate.of(2027, 7, 19))
                                .build()
                ))
                .build();

        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("standard", "GS1_SIMULATED_V1");
        mapping.put("complianceNote", "Mô phỏng lược đồ GS1, không phải chứng nhận tuân thủ chính thức GS1");

        return Gs1DossierExportResponse.builder()
                .shipment(shipmentInfo)
                .events(List.of(event))
                .inspections(List.of(inspection))
                .mapping(mapping)
                .warnings(new ArrayList<>())
                .exportedAt(LocalDateTime.now())
                .exportedBy("Hệ Thống Thử Nghiệm Nguồn Gốc Số")
                .build();
    }
}
