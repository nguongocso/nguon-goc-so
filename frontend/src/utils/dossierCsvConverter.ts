/**
 * Tiện ích chuyển đổi dữ liệu xem trước hồ sơ truy xuất sang định dạng CSV chuẩn GS1.
 * Đồng bộ cấu trúc 100% với backend ExportServiceImpl.convertPreviewToCsv.
 */

function escapeCsv(value: unknown): string {
  if (value === null || value === undefined) return '';
  const str = String(value);
  if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

function appendCsvRowIfPresent(
  lines: string[],
  group: string,
  field: string,
  value: unknown
): void {
  if (value !== null && value !== undefined && value !== '') {
    lines.push(`${escapeCsv(group)},${escapeCsv(field)},${escapeCsv(value)}`);
  }
}

export function convertDossierDataToCsv(preview: Record<string, unknown>): string {
  const lines: string[] = ['\uFEFF# HỒ SƠ TRUY XUẤT NGUỒN GỐC SẢN PHẨM'];

  const appliedTemplate = preview.appliedTemplate as Record<string, unknown> | undefined;
  if (appliedTemplate?.templateName) {
    lines.push(`# Mẫu hồ sơ: ${escapeCsv(appliedTemplate.templateName)}`);
  }

  const now = new Date();
  const pad = (n: number) => n.toString().padStart(2, '0');
  const dateStr = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
  lines.push(`# Thời gian xuất: ${dateStr}`);
  lines.push('');
  lines.push('Nhóm thông tin,Trường dữ liệu,Giá trị');

  // 1. Đơn vị sản xuất (HTX)
  const org = preview.organization as Record<string, unknown> | undefined;
  if (org) {
    appendCsvRowIfPresent(lines, 'Đơn vị sản xuất (HTX)', 'Tên tổ chức', org.name);
    appendCsvRowIfPresent(lines, 'Đơn vị sản xuất (HTX)', 'Mã định danh', org.code);
    appendCsvRowIfPresent(lines, 'Đơn vị sản xuất (HTX)', 'Loại hình tổ chức', org.type);
    appendCsvRowIfPresent(lines, 'Đơn vị sản xuất (HTX)', 'Trạng thái tổ chức', org.status);
    appendCsvRowIfPresent(lines, 'Đơn vị sản xuất (HTX)', 'Địa chỉ', org.address);
    appendCsvRowIfPresent(lines, 'Đơn vị sản xuất (HTX)', 'Tỉnh / Thành phố', org.province);
    appendCsvRowIfPresent(lines, 'Đơn vị sản xuất (HTX)', 'Số điện thoại', org.phone);
    appendCsvRowIfPresent(lines, 'Đơn vị sản xuất (HTX)', 'Email', org.email);
  }

  // 2. Vùng trồng
  const farmArea = preview.farmArea as Record<string, unknown> | undefined;
  if (farmArea) {
    appendCsvRowIfPresent(lines, 'Vùng trồng', 'Tên vùng trồng', farmArea.name);
    appendCsvRowIfPresent(lines, 'Vùng trồng', 'Tọa độ địa lý', farmArea.location);
    appendCsvRowIfPresent(lines, 'Vùng trồng', 'Diện tích canh tác', farmArea.area);
    appendCsvRowIfPresent(lines, 'Vùng trồng', 'Đơn vị diện tích', farmArea.areaUnit);
    appendCsvRowIfPresent(lines, 'Vùng trồng', 'Loại cây trồng', farmArea.cropType);
    appendCsvRowIfPresent(lines, 'Vùng trồng', 'Trạng thái vùng trồng', farmArea.isActive);
  }

  // 3. Lô sản xuất
  const lot = preview.productionLot as Record<string, unknown> | undefined;
  if (lot) {
    appendCsvRowIfPresent(lines, 'Lô sản xuất', 'Tên lô sản xuất', lot.name);
    appendCsvRowIfPresent(lines, 'Lô sản xuất', 'Danh mục sản phẩm', lot.productCategory);
    appendCsvRowIfPresent(lines, 'Lô sản xuất', 'Ngày xuống giống', lot.plantingDate);
    appendCsvRowIfPresent(lines, 'Lô sản xuất', 'Ngày thu hoạch', lot.harvestDate);
    appendCsvRowIfPresent(lines, 'Lô sản xuất', 'Sản lượng dự kiến', lot.expectedQuantity);
    appendCsvRowIfPresent(lines, 'Lô sản xuất', 'Đơn vị tính sản lượng', lot.expectedQuantityUnit);
    appendCsvRowIfPresent(lines, 'Lô sản xuất', 'Sản lượng thực tế', lot.actualQuantity);
    appendCsvRowIfPresent(lines, 'Lô sản xuất', 'Trạng thái', lot.status);
  }

  // 4. Lô hàng vận chuyển
  const shipment = preview.shipment as Record<string, unknown> | undefined;
  if (shipment) {
    appendCsvRowIfPresent(lines, 'Lô hàng vận chuyển', 'Tên lô hàng', shipment.name);
    appendCsvRowIfPresent(lines, 'Lô hàng vận chuyển', 'Số lượng', shipment.totalQuantity);
    appendCsvRowIfPresent(lines, 'Lô hàng vận chuyển', 'Quy cách đóng gói', shipment.packagingInfo);
    appendCsvRowIfPresent(lines, 'Lô hàng vận chuyển', 'Trạng thái', shipment.status);
    appendCsvRowIfPresent(lines, 'Lô hàng vận chuyển', 'Thời điểm tạo lô hàng', shipment.createdAt);
  }

  // 5. Chứng nhận tiêu chuẩn
  const certs = preview.certifications as Array<Record<string, unknown>> | undefined;
  if (Array.isArray(certs) && certs.length > 0) {
    lines.push('');
    lines.push('# CHỨNG NHẬN TIÊU CHUẨN');
    lines.push('STT,Tên chứng nhận,Tiêu chuẩn,Số hiệu,Ngày cấp,Hạn hiệu lực,Tổ chức chứng nhận');
    certs.forEach((c, idx) => {
      lines.push(
        [
          idx + 1,
          escapeCsv(c.name),
          escapeCsv(c.standardName),
          escapeCsv(c.certificationCode),
          escapeCsv(c.issueDate),
          escapeCsv(c.expiryDate),
          escapeCsv(c.certifier),
        ].join(',')
      );
    });
  }

  // 6. Nhật ký canh tác
  const farmLogs = preview.farmLogs as Array<Record<string, unknown>> | undefined;
  if (Array.isArray(farmLogs) && farmLogs.length > 0) {
    lines.push('');
    lines.push('# LỊCH TRÌNH CANH TÁC & CHỨNG TỪ');
    lines.push('STT,Ngày thực hiện,Hoạt động,Vật tư / Số lượng,Ghi chú,Chứng từ đính kèm');
    farmLogs.forEach((log, idx) => {
      const mat = log.material ? String(log.material) : '';
      const qty = log.quantity;
      const unit = log.unit ? String(log.unit) : '';
      const matInfo = mat + (qty !== undefined && qty !== null ? ` (${qty}${unit ? ` ${unit}` : ''})` : '');
      const attStr = Array.isArray(log.attachments) ? log.attachments.join('; ') : '';

      lines.push(
        [
          idx + 1,
          escapeCsv(log.executedDate),
          escapeCsv(log.activityType),
          escapeCsv(matInfo.trim()),
          escapeCsv(log.notes),
          escapeCsv(attStr),
        ].join(',')
      );
    });
  }

  // 7. Kiểm nghiệm chất lượng
  const inspections = preview.inspections as Array<Record<string, unknown>> | undefined;
  if (Array.isArray(inspections) && inspections.length > 0) {
    lines.push('');
    lines.push('# LỊCH SỬ KIỂM NGHIỆM');
    lines.push('STT,Ngày gửi mẫu,Đơn vị kiểm nghiệm,Chỉ tiêu / Tiêu chuẩn,Kết quả,Ngày cấp kết quả,Hạn hiệu lực');
    inspections.forEach((insp, idx) => {
      const res = insp.passed ?? insp.status ?? '';
      lines.push(
        [
          idx + 1,
          escapeCsv(insp.sampleSentDate),
          escapeCsv(insp.inspectionUnit),
          escapeCsv(insp.criterionName),
          escapeCsv(res),
          escapeCsv(insp.resultDate),
          escapeCsv(insp.expiryDate),
        ].join(',')
      );
    });
  }

  // 8. Dòng sự kiện chuỗi cung ứng
  const timelineEvents = preview.timelineEvents as Array<Record<string, unknown>> | undefined;
  if (Array.isArray(timelineEvents) && timelineEvents.length > 0) {
    lines.push('');
    lines.push('# DÒNG SỰ KIỆN CHUỖI CUNG ỨNG');
    lines.push('STT,Thời điểm ghi nhận,Loại sự kiện,Tọa độ địa điểm,Chi tiết sự kiện,Người ghi nhận');
    timelineEvents.forEach((ev, idx) => {
      lines.push(
        [
          idx + 1,
          escapeCsv(ev.recordedAt),
          escapeCsv(ev.eventType),
          escapeCsv(ev.location),
          escapeCsv(ev.eventData),
          escapeCsv(ev.recordedBy),
        ].join(',')
      );
    });
  }

  return lines.join('\n');
}
