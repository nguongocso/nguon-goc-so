import type { FieldSelectionItem } from '@/types/profileTemplate';

/** Xây dựng dữ liệu xem trước theo các trường đã chọn. */
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

  const farmAreaData: Record<string, unknown> = {};
  if (selectedKeySet.has('farmArea.name')) farmAreaData.name = 'Vùng chuyên canh Cà Rốt Đơn Dương';
  if (selectedKeySet.has('farmArea.location')) farmAreaData.location = '11.8345, 108.4567';
  if (selectedKeySet.has('farmArea.area')) farmAreaData.area = 5.2;
  if (selectedKeySet.has('farmArea.areaUnit')) farmAreaData.areaUnit = 'ha';
  if (selectedKeySet.has('farmArea.cropType')) farmAreaData.cropType = 'Cà rốt F1';
  if (selectedKeySet.has('farmArea.isActive')) farmAreaData.isActive = 'Đang hoạt động';
  if (Object.keys(farmAreaData).length > 0) mock.farmArea = farmAreaData;

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

  const shipmentData: Record<string, unknown> = {};
  if (selectedKeySet.has('shipment.name')) shipmentData.name = 'Chuyến hàng xuất siêu thị Go! - Đà Lạt';
  if (selectedKeySet.has('shipment.totalQuantity')) shipmentData.totalQuantity = 2000;
  if (selectedKeySet.has('shipment.packagingInfo')) shipmentData.packagingInfo = 'Thùng carton 10kg, dán tem QR GS1';
  if (selectedKeySet.has('shipment.status')) shipmentData.status = 'Đã kích hoạt';
  if (selectedKeySet.has('shipment.createdAt')) shipmentData.createdAt = '2026-09-12 08:00:00';
  if (Object.keys(shipmentData).length > 0) mock.shipment = shipmentData;

  const hasFarmLogs = selectedFields.some((f) => f.fieldKey.startsWith('farmLog.'));
  if (hasFarmLogs) {
    mock.farmLogs = [
      {
        executedDate: selectedKeySet.has('farmLog.executedDate') ? '2026-06-15' : undefined,
        activityType: selectedKeySet.has('farmLog.activityType') ? 'Gieo giống / Xuống giống' : undefined,
        material: selectedKeySet.has('farmLog.material') ? 'Giống cà rốt F1 Kuroda' : undefined,
        quantity: selectedKeySet.has('farmLog.quantity') ? 2.5 : undefined,
        unit: selectedKeySet.has('farmLog.unit') ? 'gói' : undefined,
        notes: selectedKeySet.has('farmLog.notes')
          ? 'Gieo hạt vụ thu đông, độ ẩm đất 75%, xử lý vi sinh Trichoderma'
          : undefined,
        attachments: selectedKeySet.has('farmLog.attachments') ? ['BienBan_GieoGiong_2026.pdf'] : undefined,
      },
      {
        executedDate: selectedKeySet.has('farmLog.executedDate') ? '2026-07-02' : undefined,
        activityType: selectedKeySet.has('farmLog.activityType') ? 'Tưới tiêu' : undefined,
        material: selectedKeySet.has('farmLog.material') ? 'Nước giếng khoan kiểm nghiệm đạt chuẩn' : undefined,
        quantity: selectedKeySet.has('farmLog.quantity') ? 50 : undefined,
        unit: selectedKeySet.has('farmLog.unit') ? 'm3' : undefined,
        notes: selectedKeySet.has('farmLog.notes')
          ? 'Tưới phun mưa tự động duy trì độ ẩm 70%, làm sạch cỏ luống'
          : undefined,
        attachments: selectedKeySet.has('farmLog.attachments') ? ['KetQua_NuocTuoi_2026.pdf'] : undefined,
      },
      {
        executedDate: selectedKeySet.has('farmLog.executedDate') ? '2026-07-10' : undefined,
        activityType: selectedKeySet.has('farmLog.activityType') ? 'Bón phân' : undefined,
        material: selectedKeySet.has('farmLog.material') ? 'Phân trùn quế vi sinh' : undefined,
        quantity: selectedKeySet.has('farmLog.quantity') ? 500 : undefined,
        unit: selectedKeySet.has('farmLog.unit') ? 'kg' : undefined,
        notes: selectedKeySet.has('farmLog.notes')
          ? 'Bón thúc lần 1 theo quy trình hữu cơ VietGAP'
          : undefined,
        attachments: selectedKeySet.has('farmLog.attachments') ? ['HoaDon_VatTu_TrunQue.pdf'] : undefined,
      },
      {
        executedDate: selectedKeySet.has('farmLog.executedDate') ? '2026-07-28' : undefined,
        activityType: selectedKeySet.has('farmLog.activityType') ? 'Phòng trừ sâu bệnh' : undefined,
        material: selectedKeySet.has('farmLog.material') ? 'Chế phẩm Neem Oil thảo mộc' : undefined,
        quantity: selectedKeySet.has('farmLog.quantity') ? 10 : undefined,
        unit: selectedKeySet.has('farmLog.unit') ? 'lít' : undefined,
        notes: selectedKeySet.has('farmLog.notes')
          ? 'Phun phòng ngừa sâu tơ và rệp muội định kỳ VietGAP'
          : undefined,
        attachments: selectedKeySet.has('farmLog.attachments') ? ['NhatKy_BVTV_NeemOil.pdf'] : undefined,
      },
      {
        executedDate: selectedKeySet.has('farmLog.executedDate') ? '2026-08-15' : undefined,
        activityType: selectedKeySet.has('farmLog.activityType') ? 'Bón phân' : undefined,
        material: selectedKeySet.has('farmLog.material') ? 'Phân Kali hữu cơ khoáng' : undefined,
        quantity: selectedKeySet.has('farmLog.quantity') ? 300 : undefined,
        unit: selectedKeySet.has('farmLog.unit') ? 'kg' : undefined,
        notes: selectedKeySet.has('farmLog.notes')
          ? 'Bón thúc lần 2 nuôi củ, tăng tích lũy đường và màu sắc'
          : undefined,
        attachments: selectedKeySet.has('farmLog.attachments') ? ['PhieuXuatKho_KaliHuuCo.pdf'] : undefined,
      },
      {
        executedDate: selectedKeySet.has('farmLog.executedDate') ? '2026-08-30' : undefined,
        activityType: selectedKeySet.has('farmLog.activityType') ? 'Chăm sóc' : undefined,
        material: selectedKeySet.has('farmLog.material') ? 'Không sử dụng vật tư' : undefined,
        quantity: selectedKeySet.has('farmLog.quantity') ? 0 : undefined,
        unit: selectedKeySet.has('farmLog.unit') ? '-' : undefined,
        notes: selectedKeySet.has('farmLog.notes')
          ? 'Đảm bảo thời gian cách ly an toàn 15 ngày trước thu hoạch'
          : undefined,
        attachments: selectedKeySet.has('farmLog.attachments') ? ['BienBan_KiemTra_CachLy.pdf'] : undefined,
      },
    ];
  }

  const hasCertifications = selectedFields.some((f) => f.fieldKey.startsWith('certification.'));
  if (hasCertifications) {
    mock.certifications = [
      {
        name: selectedKeySet.has('certification.name') ? 'Chứng nhận tiêu chuẩn VietGAP Trồng trọt' : undefined,
        standardName: selectedKeySet.has('certification.standardName') ? 'VietGAP' : undefined,
        certificationCode: selectedKeySet.has('certification.certificationCode') ? 'VG-2026-LD-0018' : undefined,
        issueDate: selectedKeySet.has('certification.issueDate') ? '2026-01-10' : undefined,
        expiryDate: selectedKeySet.has('certification.expiryDate') ? '2028-01-10' : undefined,
        certifier: selectedKeySet.has('certification.certifier')
          ? 'Trung tâm Chứng nhận Phù hợp Quacert'
          : undefined,
      },
      {
        name: selectedKeySet.has('certification.name')
          ? 'Chứng nhận Chuỗi Cung ứng Thực phẩm An toàn'
          : undefined,
        standardName: selectedKeySet.has('certification.standardName') ? 'Chuỗi ATTP' : undefined,
        certificationCode: selectedKeySet.has('certification.certificationCode') ? 'ATTP-LD-2026-089' : undefined,
        issueDate: selectedKeySet.has('certification.issueDate') ? '2026-02-15' : undefined,
        expiryDate: selectedKeySet.has('certification.expiryDate') ? '2029-02-15' : undefined,
        certifier: selectedKeySet.has('certification.certifier')
          ? 'Chi cục Trồng trọt & BVTV Lâm Đồng'
          : undefined,
      },
    ];
  }

  const hasInspections = selectedFields.some((f) => f.fieldKey.startsWith('inspection.'));
  if (hasInspections) {
    mock.inspections = [
      {
        sampleSentDate: selectedKeySet.has('inspection.sampleSentDate') ? '2026-09-05' : undefined,
        inspectionUnit: selectedKeySet.has('inspection.inspectionUnit') ? 'Trung tâm Phân tích Quatest 3' : undefined,
        criterionName: selectedKeySet.has('inspection.criterionName')
          ? 'Kim loại nặng trong đất & nước (Pb, Cd)'
          : undefined,
        passed: selectedKeySet.has('inspection.passed') ? 'Đạt' : undefined,
        status: selectedKeySet.has('inspection.passed') ? 'Đạt' : undefined,
        resultDate: selectedKeySet.has('inspection.resultDate') ? '2026-09-06' : undefined,
        expiryDate: selectedKeySet.has('inspection.expiryDate') ? '2027-03-06' : undefined,
      },
      {
        sampleSentDate: selectedKeySet.has('inspection.sampleSentDate') ? '2026-09-08' : undefined,
        inspectionUnit: selectedKeySet.has('inspection.inspectionUnit') ? 'Trung tâm Phân tích Quatest 3' : undefined,
        criterionName: selectedKeySet.has('inspection.criterionName')
          ? 'Dư lượng Nitrat (NO3-) trên nông sản'
          : undefined,
        passed: selectedKeySet.has('inspection.passed') ? 'Đạt' : undefined,
        status: selectedKeySet.has('inspection.passed') ? 'Đạt' : undefined,
        resultDate: selectedKeySet.has('inspection.resultDate') ? '2026-09-09' : undefined,
        expiryDate: selectedKeySet.has('inspection.expiryDate') ? '2027-03-09' : undefined,
      },
      {
        sampleSentDate: selectedKeySet.has('inspection.sampleSentDate') ? '2026-09-08' : undefined,
        inspectionUnit: selectedKeySet.has('inspection.inspectionUnit') ? 'Trung tâm Phân tích Quatest 3' : undefined,
        criterionName: selectedKeySet.has('inspection.criterionName')
          ? 'Dư lượng thuốc BVTV (Cypermethrin, Chlorpyrifos)'
          : undefined,
        passed: selectedKeySet.has('inspection.passed') ? 'Đạt (Không phát hiện)' : undefined,
        status: selectedKeySet.has('inspection.passed') ? 'Đạt' : undefined,
        resultDate: selectedKeySet.has('inspection.resultDate') ? '2026-09-09' : undefined,
        expiryDate: selectedKeySet.has('inspection.expiryDate') ? '2027-03-09' : undefined,
      },
      {
        sampleSentDate: selectedKeySet.has('inspection.sampleSentDate') ? '2026-09-08' : undefined,
        inspectionUnit: selectedKeySet.has('inspection.inspectionUnit') ? 'Trung tâm Phân tích Quatest 3' : undefined,
        criterionName: selectedKeySet.has('inspection.criterionName')
          ? 'Vi sinh vật gây hại (Salmonella, E.coli)'
          : undefined,
        passed: selectedKeySet.has('inspection.passed') ? 'Đạt' : undefined,
        status: selectedKeySet.has('inspection.passed') ? 'Đạt' : undefined,
        resultDate: selectedKeySet.has('inspection.resultDate') ? '2026-09-09' : undefined,
        expiryDate: selectedKeySet.has('inspection.expiryDate') ? '2027-03-09' : undefined,
      },
    ];
  }

  const hasTimeline = selectedFields.some((f) => f.fieldKey.startsWith('chainEvent.'));
  if (hasTimeline) {
    mock.timelineEvents = [
      {
        recordedAt: selectedKeySet.has('chainEvent.recordedAt') ? '2026-09-10 07:30:00' : undefined,
        eventType: selectedKeySet.has('chainEvent.eventType') ? 'Thu hoạch' : undefined,
        location: selectedKeySet.has('chainEvent.location') ? '11.8345, 108.4567' : undefined,
        eventData: selectedKeySet.has('chainEvent.eventData')
          ? 'Sản lượng: 2500 kg; Thu hoạch thủ công sáng sớm, sơ tuyển củ loại 1'
          : undefined,
        recordedBy: selectedKeySet.has('chainEvent.recordedBy') ? 'Nguyễn Văn Quản Lý' : undefined,
      },
      {
        recordedAt: selectedKeySet.has('chainEvent.recordedAt') ? '2026-09-10 11:00:00' : undefined,
        eventType: selectedKeySet.has('chainEvent.eventType') ? 'Vận chuyển nội bộ' : undefined,
        location: selectedKeySet.has('chainEvent.location') ? '11.8348, 108.4569' : undefined,
        eventData: selectedKeySet.has('chainEvent.eventData')
          ? 'Vận chuyển về trạm sơ chế bằng xe chuyên dụng 49C-123.45; Nhiệt độ: 20°C'
          : undefined,
        recordedBy: selectedKeySet.has('chainEvent.recordedBy') ? 'Lê Văn Vận Chuyển' : undefined,
      },
      {
        recordedAt: selectedKeySet.has('chainEvent.recordedAt') ? '2026-09-10 14:30:00' : undefined,
        eventType: selectedKeySet.has('chainEvent.eventType') ? 'Sơ chế & Làm sạch' : undefined,
        location: selectedKeySet.has('chainEvent.location') ? '11.8350, 108.4570' : undefined,
        eventData: selectedKeySet.has('chainEvent.eventData')
          ? 'Rửa sạch sục khí Ozone diệt khuẩn, làm ráo tự nhiên; Phân loại củ 16-20cm'
          : undefined,
        recordedBy: selectedKeySet.has('chainEvent.recordedBy') ? 'Phạm Thị Sơ Chế' : undefined,
      },
      {
        recordedAt: selectedKeySet.has('chainEvent.recordedAt') ? '2026-09-11 09:00:00' : undefined,
        eventType: selectedKeySet.has('chainEvent.eventType') ? 'Kiểm tra chất lượng (QC)' : undefined,
        location: selectedKeySet.has('chainEvent.location') ? '11.8350, 108.4570' : undefined,
        eventData: selectedKeySet.has('chainEvent.eventData')
          ? 'Kiểm tra cảm quan độ giòn tươi và test nhanh tồn dư nitrate; Đạt 100% chuẩn xuất hàng'
          : undefined,
        recordedBy: selectedKeySet.has('chainEvent.recordedBy') ? 'Đỗ Văn Kiểm Phẩm' : undefined,
      },
      {
        recordedAt: selectedKeySet.has('chainEvent.recordedAt') ? '2026-09-12 10:00:00' : undefined,
        eventType: selectedKeySet.has('chainEvent.eventType') ? 'Đóng gói' : undefined,
        location: selectedKeySet.has('chainEvent.location') ? '11.8350, 108.4570' : undefined,
        eventData: selectedKeySet.has('chainEvent.eventData')
          ? 'Số thùng: 200; Quy cách: Thùng carton chuyên dụng 10kg, bọc màng PE thoáng khí, dán tem QR GS1'
          : undefined,
        recordedBy: selectedKeySet.has('chainEvent.recordedBy') ? 'Trần Thị Đóng Gói' : undefined,
      },
      {
        recordedAt: selectedKeySet.has('chainEvent.recordedAt') ? '2026-09-12 14:00:00' : undefined,
        eventType: selectedKeySet.has('chainEvent.eventType') ? 'Kích hoạt tem truy xuất' : undefined,
        location: selectedKeySet.has('chainEvent.location') ? '11.8350, 108.4570' : undefined,
        eventData: selectedKeySet.has('chainEvent.eventData')
          ? 'Kích hoạt 200 mã định danh tem QR GS1 công khai trên Cổng thông tin Nguồn Gốc Số'
          : undefined,
        recordedBy: selectedKeySet.has('chainEvent.recordedBy') ? 'Nguyễn Văn Quản Lý' : undefined,
      },
      {
        recordedAt: selectedKeySet.has('chainEvent.recordedAt') ? '2026-09-12 16:30:00' : undefined,
        eventType: selectedKeySet.has('chainEvent.eventType') ? 'Xuất kho vận chuyển' : undefined,
        location: selectedKeySet.has('chainEvent.location') ? '11.8352, 108.4572' : undefined,
        eventData: selectedKeySet.has('chainEvent.eventData')
          ? 'Bàn giao xe container lạnh 49A-888.99 giao Đại siêu thị Go! Đà Lạt; Nhiệt độ thùng: 8°C - 12°C'
          : undefined,
        recordedBy: selectedKeySet.has('chainEvent.recordedBy') ? 'Hoàng Văn Kho' : undefined,
      },
      {
        recordedAt: selectedKeySet.has('chainEvent.recordedAt') ? '2026-09-13 06:30:00' : undefined,
        eventType: selectedKeySet.has('chainEvent.eventType') ? 'Tiếp nhận & Phân phối' : undefined,
        location: selectedKeySet.has('chainEvent.location') ? '11.9404, 108.4583' : undefined,
        eventData: selectedKeySet.has('chainEvent.eventData')
          ? 'Đại siêu thị Go! Đà Lạt nghiệm thu quét mã QR, xác nhận nhập kho 200 thùng và phân phối lên kệ'
          : undefined,
        recordedBy: selectedKeySet.has('chainEvent.recordedBy') ? 'Quản lý Tiếp nhận Siêu thị' : undefined,
      },
    ];
  }

  return mock;
}

