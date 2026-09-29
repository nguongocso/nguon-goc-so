export type PublicCertificationStatus = 'VALID' | 'EXPIRED';

export interface PublicCertification {
  certificationId: string;
  certificationName: string;
  certificationNameEn?: string | null;
  certificationCode: string;
  issuedBy: string | null;
  issueDate: string | null;
  expiryDate: string;
  status: PublicCertificationStatus;
  statusLabel: string;
  /** Chứng nhận có tệp đính kèm để hiển thị ảnh hay không. */
  hasDocument?: boolean;
  documentFileName?: string | null;
  /** Kiểu nội dung tệp: image/jpeg, image/png hoặc application/pdf. */
  documentContentType?: string | null;
  documentFileSize?: number | null;
  /** Đường dẫn tương đối tới endpoint tải tệp công khai. */
  documentUrl?: string | null;
}

export interface PublicLotCertificationsResponse {
  productionLotId: string;
  lotName: string;
  hasCertification: boolean;
  certifications: PublicCertification[];
}
