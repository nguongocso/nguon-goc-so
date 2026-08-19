// frontend/src/utils/constants.ts

// Regex cho số điện thoại Việt Nam (bắt đầu bằng 0 hoặc +84, sau đó 9-10 chữ số)
export const PHONE_REGEX = /^(0|\+84)[0-9]{9,10}$/;

// Regex cho mật khẩu mạnh: 8-50 ký tự, ít nhất 1 chữ hoa, 1 chữ thường, 1 số, 1 ký tự đặc biệt
export const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_])[A-Za-z\d\W_]{8,50}$/;

// Regex cho mã tổ chức: chỉ chữ hoa, số, gạch dưới, gạch ngang
export const ORGANIZATION_CODE_REGEX = /^[A-Z0-9_-]+$/;

// Enum OrganizationType (có thể dùng object để ánh xạ nhãn hiển thị)
export const ORGANIZATION_TYPES = {
  COOPERATIVE: 'Hợp tác xã',
  ENTERPRISE: 'Doanh nghiệp',
  GOVERNMENT: 'Cơ quan quản lý',
  SYSTEM: 'Tổ chức hệ thống',
} as const;

export type OrganizationTypeKey = keyof typeof ORGANIZATION_TYPES;