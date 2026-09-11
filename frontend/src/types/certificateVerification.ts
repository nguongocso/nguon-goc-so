export type CertificateVerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export type CertificateValidityStatus = 'VALID' | 'EXPIRED';

export interface CertificateReviewer {
  userId: string;
  fullName: string;
}

export interface CertificateDocument {
  fileName: string;
  contentType: 'application/pdf' | 'image/jpeg' | 'image/png';
  fileSize: number;
  viewUrl: string;
}

export interface CertificateVerification {
  id: string;
  organizationId: string;
  organizationName: string;
  standardId: string;
  standardName: string;
  code: string;
  issuedBy: string;
  issueDate: string;
  expiryDate: string;
  verificationStatus: CertificateVerificationStatus;
  validityStatus: CertificateValidityStatus;
  document: CertificateDocument | null;
  reviewedBy: CertificateReviewer | null;
  reviewedAt: string | null;
  reviewNote: string | null;
  rejectionReason: string | null;
  createdAt: string;
  updatedAt: string | null;
  notifiedCount?: number;
}

export interface CertificateVerificationPage {
  items: CertificateVerification[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface CertificateVerificationListParams {
  verificationStatus?: CertificateVerificationStatus;
  keyword?: string;
  organizationId?: string;
  page?: number;
  size?: number;
  sortBy?: 'createdAt' | 'reviewedAt' | 'expiryDate';
  sortDir?: 'asc' | 'desc';
}

export interface VerifyCertificatePayload {
  reviewNote?: string;
}

export interface RejectCertificatePayload {
  rejectionReason: string;
}
