/** Định nghĩa kiểu dữ liệu cho chức năng Xuất dữ liệu mở và Hồ sơ truy xuất (NCL-07-CN-007). */

/** Tham số yêu cầu kết xuất dữ liệu mở theo bộ lọc. */
export interface ExportOpenDataRequest {
  organizationId?: string;
  fromDate?: string; // ISO datetime
  toDate?: string;
  productCategoryIds?: string[];
  shipmentIds?: string[];
  /** NCL-742 §8 — Lọc theo danh sách ID đơn vị hành chính địa bàn quản lý */
  unitIds?: string[];
  format?: 'JSON' | 'CSV' | 'XML';
  /** NCL-07-CN-007 — Mã mẫu hồ sơ truy xuất áp dụng (tùy chọn) */
  templateId?: string;
}

/** Cấu trúc phản hồi kết quả xuất dữ liệu mở. */
export interface ExportOpenDataResponse {
  success: boolean;
  status: number;
  message?: string;
}