/**
 * Nội dung hướng dẫn sử dụng trong ứng dụng (NCL-01-CN-006).
 */
export interface HelpContent {
  /** Mã định danh màn hình (ví dụ: "farm-log-create"). */
  screenKey: string;

  /** Mã vai trò (ví dụ: "VT-03") hoặc "GENERAL". */
  roleCode: string;

  /** Tiêu đề hướng dẫn. */
  title: string;

  /** Danh sách các bước hướng dẫn. */
  steps: string[];

  /** Ví dụ minh hoạ (tuỳ chọn). */
  exampleData?: string | null;
}