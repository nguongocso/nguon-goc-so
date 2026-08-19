export type PublicCertificationStatus = "VALID" | "EXPIRED";

export interface PublicCertification {
  certificationId: string;
  certificationName: string;
  certificationCode: string;
  issuedBy: string | null;
  issueDate: string | null;
  expiryDate: string;
  status: PublicCertificationStatus;
  statusLabel: string;
}

export interface PublicLotCertificationsResponse {
  productionLotId: string;
  lotName: string;
  hasCertification: boolean;
  certifications: PublicCertification[];
}