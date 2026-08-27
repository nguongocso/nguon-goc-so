/**
 * Message backend trả (contract NCL-742 §8, rule 2) khi VT-05 chưa được
 * gán địa bàn nào: HTTP 200 + dữ liệu rỗng kèm message này.
 * Frontend so khớp chuỗi để hiển thị empty-state thân thiện thay vì lỗi.
 */
export const NO_ASSIGNED_AREA_MESSAGE =
  'Bạn chưa được phân công địa bàn quản lý nào.';

export const NO_ASSIGNED_AREA_HINT =
  'Vui lòng liên hệ quản trị viên để được phân công địa bàn quản lý.';
