export interface Certification {
  id: string;
  name: string;
  code: string;
  issuedBy: string;
  issueDate: string;  // YYYY-MM-DD
  expiryDate: string; // YYYY-MM-DD
  isValid: boolean;
}

export interface ProductionLotCertification {
  id: string;
  certificationId: string;
  certificationName: string;
  certificationCode: string;
  issuedBy: string;
  issueDate: string;
  expiryDate: string;
  isValid: boolean;
  attachedAt: string; // ISO datetime
  attachedBy: string;
  note: string | null;
}

export interface AttachCertificationRequest {
  certificationId: string;
  note?: string;
}

export interface CreateCertificationRequest {
  standardId: string;
  code: string;
  issuedBy?: string;
  issueDate: string;   // YYYY-MM-DD
  expiryDate: string;  // YYYY-MM-DD
}

// CertificationResponse đã có (hoặc thêm nếu chưa có)
export interface CertificationResponse {
  id: string;
  name: string;        // tên standard
  code: string;
  issuedBy: string;
  issueDate: string;
  expiryDate: string;
  isValid: boolean;
}