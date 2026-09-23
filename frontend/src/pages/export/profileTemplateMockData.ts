import type { FieldSelectionItem } from '@/types/profileTemplate';

/**
 * Xây dựng dữ liệu giả lập mẫu (mock data) phản ánh đầy đủ cấu trúc các trường dữ liệu được chọn
 * để truyền vào DossierPreviewDialog khi người dùng bấm "Xem trước hồ sơ".
 *
 * @param templateName Tên mẫu hồ sơ đang nhập trên form.
 * @param isDefault Trạng thái đặt làm mặc định.
 * @param selectedFields Danh sách các trường được chọn.
 * @returns Đối tượng chứa dữ liệu xem trước phân cấp.
 */
export function buildProfileTemplateMockData(
  templateName: string,
  isDefault: boolean,
  selectedFields: FieldSelectionItem[],
): Record<string, unknown> {
  const selectedKeySet = new Set(selectedFields.map((f) => f.fieldKey));

  const mock: Record<string, unknown> = {
    shipmentId: 'SHIP-MOCK-2026-DEMO',
    appliedTemplate: {
      templateName: templateName || 'Mẫu đang tạo',
      totalFields: selectedFields.length,
      isDefault,
    },
  };

  // 1. Organization
  const orgData: Record<string, unknown> = {};
  if (selectedKeySet.has('organization.name')) orgData.name = 'Hợp tác xã Nông nghiệp Xanh Lam Đồng';
  if (selectedKeySet.has('organization.code')) orgData.code = 'HTX-LAMDONG-01';
  if (selectedKeySet.has('organization.type')) orgData.type = 'Hợp tác xã';
  if (selectedKeySet.has('organization.status')) orgData.status = 'Đang hoạt động';
  if (selectedKeySet.has('organization.address')) orgData.address = 'Thôn 3, Xã Đạ Ròn, Huyện Đơn Dương, Tỉnh Lâm Đồng';
  if (selectedKeySet.has('organization.province')) orgData.province = 'Tỉnh Lâm Đồng';
  if (selectedKeySet.has('organization.phone')) orgData.phone = '0263.3888.999';
  if (selectedKeySet.has('organization.email')) orgData.email = 'lienhe@htxxanh.vn';
  if (Object.keys(orgData).length > 0) mock.organization = orgData;

  // 2. FarmArea
  const farmAreaData: Record<string, unknown> = {};
  if (selectedKeySet.has('farmArea.name')) farmAreaData.name = 'Vùng chuyên canh Cà Rốt Đơn Dương';
  if (selectedKeySet.has('farmArea.location')) farmAreaData.location = '11.8345, 108.4567';
  if (selectedKeySet.has('farmArea.area')) farmAreaData.area = 5.2;
  if (selectedKeySet.has('farmArea.areaUnit')) farmAreaData.areaUnit = 'ha';
  if (selectedKeySet.has('farmArea.cropType')) farmAreaData.cropType = 'Cà rốt F1';
  if (selectedKeySet.has('farmArea.isActive')) farmAreaData.isActive = 'Đang hoạt động';
  if (Object.keys(farmAreaData).length > 0) mock.farmArea = farmAreaData;

  // 3. ProductionLot
  const lotData: Record<string, unknown> = {};
  if (selectedKeySet.has('productionLot.name')) lotData.name = 'Lô Cà Rốt hữu cơ VietGAP 2026';
  if (selectedKeySet.has('productionLot.productCategory')) lotData.productCategory = 'Rau củ quả tươi';
  if (selectedKeySet.has('productionLot.plantingDate')) lotData.plantingDate = '2026-06-15';
  if (selectedKeySet.has('productionLot.harvestDate')) lotData.harvestDate = '2026-09-10';
  if (selectedKeySet.has('productionLot.expectedQuantity')) lotData.expectedQuantity = 12500;
  if (selectedKeySet.has('productionLot.expectedQuantityUnit')) lotData.expectedQuantityUnit = 'kg';
  if (selectedKeySet.has('productionLot.actualQuantity')) lotData.actualQuantity = 12800;
  if (selectedKeySet.has('productionLot.status')) lotData.status = 'Đang đóng gói';
  if (Object.keys(lotData).length > 0) mock.productionLot = lotData;

  // 4. Shipment
  const shipmentData: Record<string, unknown> = {};
  if (selectedKeySet.has('shipment.name')) shipmentData.name = 'Chuyến hàng xuất siêu thị Go! - Đà Lạt';
  if (selectedKeySet.has('shipment.totalQuantity')) shipmentData.totalQuantity = 2000;
  if (selectedKeySet.has('shipment.packagingInfo')) shipmentData.packagingInfo = 'Thùng carton 10kg, dán tem QR GS1';
  if (selectedKeySet.has('shipment.status')) shipmentData.status = 'Đã kích hoạt';
  if (selectedKeySet.has('shipment.createdAt')) shipmentData.createdAt = '2026-09-12 08:00:00';
  if (Object.keys(shipmentData).length > 0) mock.shipment = shipmentData;

  // 5. FarmLogs
  const hasFarmLogs = selectedFields.some((f) => f.fieldKey.startsWith('farmLog.'));
  if (hasFarmLogs) {
    mock.farmLogs = [
      {
        executedDate: selectedKeySet.has('farmLog.executedDate') ? '2026-06-15' : undefined,
        activityType: selectedKeySet.has('farmLog.activityType') ? 'Gieo giống / Xuống giống' : undefined,
        material: selectedKeySet.has('farmLog.material') ? 'Giống cà rốt F1 Kuroda' : undefined,
        quantity: selectedKeySet.has('farmLog.quantity') ? 2.5 : undefined,
        unit: selectedKeySet.has('farmLog.unit') ? 'gói' : undefined,
        notes: selectedKeySet.has('farmLog.notes') ? 'Gieo hạt vụ thu đông, độ ẩm đất 75%' : undefined,
        attachments: selectedKeySet.has('farmLog.attachments') ? ['BienBan_GieoGiong_2026.pdf'] : undefined,
      },
      {
        executedDate: selectedKeySet.has('farmLog.executedDate') ? '2026-07-10' : undefined,
        activityType: selectedKeySet.has('farmLog.activityType') ? 'Bón phân' : undefined,
        material: selectedKeySet.has('farmLog.material') ? 'Phân trùn quế vi sinh' : undefined,
        quantity: selectedKeySet.has('farmLog.quantity') ? 500 : undefined,
        unit: selectedKeySet.has('farmLog.unit') ? 'kg' : undefined,
        notes: selectedKeySet.has('farmLog.notes') ? 'Bón thúc lần 1 theo quy trình hữu cơ' : undefined,
        attachments: selectedKeySet.has('farmLog.attachments') ? ['HoaDon_VatTu_TrunQue.pdf'] : undefined,
      },
    ];
  }

  // 6. Certifications
  const hasCertifications = selectedFields.some((f) => f.fieldKey.startsWith('certification.'));
  if (hasCertifications) {
    mock.certifications = [
      {
        name: selectedKeySet.has('certification.name') ? 'Chứng nhận tiêu chuẩn VietGAP Trồng trọt' : undefined,
        standardName: selectedKeySet.has('certification.standardName') ? 'VietGAP' : undefined,
        certificationCode: selectedKeySet.has('certification.certificationCode') ? 'VG-2026-LD-0018' : undefined,
        issueDate: selectedKeySet.has('certification.issueDate') ? '2026-01-10' : undefined,
        expiryDate: selectedKeySet.has('certification.expiryDate') ? '2028-01-10' : undefined,
        certifier: selectedKeySet.has('certification.certifier') ? 'Trung tâm Chứng nhận Phù hợp Quacert' : undefined,
      },
    ];
  }

  // 7. Inspections
  const hasInspections = selectedFields.some((f) => f.fieldKey.startsWith('inspection.'));
  if (hasInspections) {
    mock.inspections = [
      {
        sampleSentDate: selectedKeySet.has('inspection.sampleSentDate') ? '2026-09-08' : undefined,
        inspectionUnit: selectedKeySet.has('inspection.inspectionUnit') ? 'Trung tâm Phân tích Quatest 3' : undefined,
        criterionName: selectedKeySet.has('inspection.criterionName') ? 'Dư lượng Nitrat (NO3-)' : undefined,
        passed: selectedKeySet.has('inspection.passed') ? 'Đạt' : undefined,
        status: selectedKeySet.has('inspection.passed') ? 'Đạt' : undefined,
        resultDate: selectedKeySet.has('inspection.resultDate') ? '2026-09-09' : undefined,
        expiryDate: selectedKeySet.has('inspection.expiryDate') ? '2027-03-09' : undefined,
      },
    ];
  }

  // 8. Timeline
  const hasTimeline = selectedFields.some((f) => f.fieldKey.startsWith('chainEvent.'));
  if (hasTimeline) {
    mock.timelineEvents = [
      {
        recordedAt: selectedKeySet.has('chainEvent.recordedAt') ? '2026-09-10 08:30:00' : undefined,
        eventType: selectedKeySet.has('chainEvent.eventType') ? 'Thu hoạch' : undefined,
        location: selectedKeySet.has('chainEvent.location') ? '11.8345, 108.4567' : undefined,
        eventData: selectedKeySet.has('chainEvent.eventData')
          ? 'Sản lượng: 2500 kg; Phương thức: Thu hoạch thủ công'
          : undefined,
        recordedBy: selectedKeySet.has('chainEvent.recordedBy') ? 'Nguyễn Văn Quản Lý' : undefined,
      },
      {
        recordedAt: selectedKeySet.has('chainEvent.recordedAt') ? '2026-09-12 14:00:00' : undefined,
        eventType: selectedKeySet.has('chainEvent.eventType') ? 'Đóng gói' : undefined,
        location: selectedKeySet.has('chainEvent.location') ? '11.8350, 108.4570' : undefined,
        eventData: selectedKeySet.has('chainEvent.eventData')
          ? 'Số thùng: 200; Quy cách: Thùng carton 10kg'
          : undefined,
        recordedBy: selectedKeySet.has('chainEvent.recordedBy') ? 'Trần Thị Đóng Gói' : undefined,
      },
    ];
  }

  return mock;
}
