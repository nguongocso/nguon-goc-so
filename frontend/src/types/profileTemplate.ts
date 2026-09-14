/**
 * Định nghĩa kiểu dữ liệu cho Mẫu hồ sơ truy xuất theo đối tác (NCL-07-CN-007)
 */

export interface ProfileTemplateField {
  id?: string;
  fieldKey: string;
  fieldGroup: string;
  isMandatory: boolean;
  sortOrder?: number;
}

export interface ProfileTemplate {
  id: string;
  organizationId: string;
  name: string;
  partnerName?: string | null;
  isDefault: boolean;
  fields: ProfileTemplateField[];
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
}

export interface FieldSelectionItem {
  fieldKey: string;
  fieldGroup: string;
  isMandatory?: boolean;
  sortOrder?: number;
}

export interface CreateProfileTemplateRequest {
  name: string;
  partnerName?: string;
  isDefault: boolean;
  selectedFields: FieldSelectionItem[];
}

export interface UpdateProfileTemplateRequest {
  name: string;
  partnerName?: string;
  isDefault: boolean;
  selectedFields: FieldSelectionItem[];
}

export interface AvailableFieldItem {
  key: string;
  label: string;
  isMandatory: boolean;
}

export interface FieldGroupDefinition {
  group: string;
  groupLabel: string;
  fields: AvailableFieldItem[];
}
