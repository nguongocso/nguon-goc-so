/** Định nghĩa kiểu dữ liệu cho Mẫu hồ sơ truy xuất theo đối tác (NCL-07-CN-007). */

/** Chi tiết cấu hình một trường dữ liệu trong mẫu hồ sơ. */
export interface ProfileTemplateField {
  id?: string;
  fieldKey: string;
  fieldGroup: string;
  isMandatory: boolean;
  sortOrder?: number;
}

/** Đối tượng mẫu hồ sơ truy xuất của tổ chức. */
export interface ProfileTemplate {
  id: string;
  organizationId: string;
  name: string;
  partnerName?: string | null;
  isDefault: boolean;
  default?: boolean;
  fields: ProfileTemplateField[];
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
}

/** Mục trường được chọn khi tạo hoặc cập nhật mẫu hồ sơ. */
export interface FieldSelectionItem {
  fieldKey: string;
  fieldGroup: string;
  isMandatory?: boolean;
  sortOrder?: number;
}

/** Dữ liệu yêu cầu tạo mới mẫu hồ sơ truy xuất. */
export interface CreateProfileTemplateRequest {
  name: string;
  partnerName?: string;
  isDefault: boolean;
  selectedFields: FieldSelectionItem[];
}

/** Dữ liệu yêu cầu cập nhật mẫu hồ sơ truy xuất. */
export interface UpdateProfileTemplateRequest {
  name: string;
  partnerName?: string;
  isDefault: boolean;
  selectedFields: FieldSelectionItem[];
}

/** Thông tin một trường dữ liệu khả dụng trong catalog hệ thống. */
export interface AvailableFieldItem {
  fieldKey: string;
  displayName: string;
  mandatory: boolean;
  description?: string;
  // Các bí danh (aliases) phục vụ tương thích ngược với code cũ
  key: string;
  label: string;
  isMandatory: boolean;
}

/** Định nghĩa một nhóm trường dữ liệu khả dụng. */
export interface FieldGroupDefinition {
  fieldGroup: string;
  groupLabel: string;
  fields: AvailableFieldItem[];
  // Bí danh phục vụ tương thích ngược
  group: string;
}
