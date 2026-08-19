export interface Invitation {
  id: string;
  email: string;
  organizationId: string;
  organizationName: string;
  roleId: number;
  roleName: string;
  status: 'PENDING' | 'ACCEPTED' | 'EXPIRED';
  token: string;
  expiryDate: string;
  createdBy: string;
  createdAt: string;
}

export interface CreateInvitationRequest {
  email: string;
  roleId: number;
  expiryDays?: number; // mặc định 7
}

export interface InvitationResponse {
  id: string;
  email: string;
  organizationId: string;
  organizationName: string;
  roleId: number;
  roleName: string;
  status: string;
  token: string;
  joinUrl?: string;
  expiryDate: string;
  createdBy: string;
  createdAt: string;
}

export interface InvitationPublicResponse {
  email: string;
  organizationName: string;
  roleName: string;
  status: string;
  expiryDate: string;
  isExistingUser?: boolean;
}

export interface AcceptInvitationRequest {
  userName: string;
  password: string;
  fullName: string;
  phone?: string;
}

export interface AcceptInvitationResponse {
  userId: string;
  userName: string;
  fullName: string;
  organizationId: string;
  organizationName: string;
  roleCode: string;
}