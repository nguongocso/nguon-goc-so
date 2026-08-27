export type MemberStatus = 'ACTIVE' | 'INACTIVE';

export interface RoleOption {
  roleId: number;
  code: string;
  name: string;
}

export interface OrganizationMember {
  id: string;
  organizationId: string;
  userId: string;
  username: string;
  fullName: string;
  roleId: number;
  roleCode: string | null;
  roleName: string | null;
  status: MemberStatus;
  /**
   * Trạng thái membership trong tổ chức hiện tại (organization_users.status).
   * Khác `status` (trạng thái toàn cục users.status) — nguồn sự thật cho
   * việc vô hiệu hóa/kích hoạt lại thành viên (NCL-01-CN-009).
   */
  membershipStatus?: MemberStatus;
  joinedAt: string;
  email?: string | null;
  phone?: string | null;
}

export interface AssignRoleRequest {
  userId: string;
  roleId: number;
}

export interface AddMemberRequest {
  username: string;
  password: string;
  fullName: string;
  phone?: string | null;
  email?: string | null;
  roleId: number;
}

export interface ApiResult<T> {
  success: boolean;
  status: number;
  message?: string;
  data: T;
  errors?: unknown;
  path?: string;
  timestamp?: string;
}

// ====================================================================
// NCL-01-CN-009 — Vô hiệu hóa / kích hoạt lại thành viên (QTN-32)
// ====================================================================

/** Thông tin tóm tắt một lô sản xuất đang phân công cho thành viên. */
export interface MemberLotSummary {
  lotId: string;
  lotName: string;
  lotStatus: string;
  plantingDate?: string | null;
  harvestDate?: string | null;
}

/** Kết quả precheck lô chưa hoàn thành (GET /members/{userId}/unfinished-lots). */
export interface UnfinishedLotsResponse {
  userId: string;
  hasUnfinishedLots: boolean;
  total: number;
  replacementRequired: boolean;
  lots: MemberLotSummary[];
}

/** Ứng viên thay thế hợp lệ (GET /members/{userId}/replacement-candidates). */
export interface ReplacementCandidate {
  userId: string;
  username: string;
  fullName: string;
  roleCode: string | null;
  roleName: string | null;
  /** Các lô chưa hoàn thành mà ứng viên đủ điều kiện tiếp nhận. */
  eligibleLotIds: string[];
}

export interface DeactivateMemberRequest {
  /** Lý do vô hiệu hóa — bắt buộc, tối đa 500 ký tự. */
  reason: string;
  /**
   * Người thay thế tiếp nhận các lô chưa hoàn thành — bắt buộc khi
   * thành viên còn lô chưa hoàn thành (409 requiresReplacement).
   */
  replacementUserId?: string;
}

export interface ReactivateMemberRequest {
  /** Lý do kích hoạt lại — bắt buộc, tối đa 500 ký tự. */
  reason: string;
}

/** Payload `errors` của lỗi 409 khi thành viên còn lô chưa hoàn thành. */
export interface ReplacementRequiredError {
  code: string;
  requiresReplacement: boolean;
  pendingLots: MemberLotSummary[];
}