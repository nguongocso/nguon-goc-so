export interface CreateProductFeedbackPayload {
  content: string;
  traceCodeValue?: string;
}

export interface PublicProductFeedbackCreated {
  id: string;
  productionLotId: string;
  status: ProductFeedbackStatus;
  createdAt: string;
}

export type ProductFeedbackStatus =
  | "NEW"
  | "IN_PROGRESS"
  | "CLOSED"
  | "ESCALATED_TO_RECALL";

export type ProductFeedbackSeverity =
  | "INFORMATION"
  | "QUALITY_SUSPECTED"
  | "COUNTERFEIT_SUSPECTED";

export interface ProductFeedback {
  id: string;
  productionLotId: string;
  productionLotName: string;
  content: string;
  createdAt: string;
  organizationId?: string;
  organizationName?: string;
  productCategoryName?: string;
  traceCodeId?: string;
  traceCodeValue?: string;
  status: ProductFeedbackStatus;
  severity: ProductFeedbackSeverity;
  assignedToUserId?: string;
  assignedToName?: string;
  assignedAt?: string;
  processingContent?: string;
  publicResponse?: string;
  closeReason?: string;
  closedByUserId?: string;
  closedByName?: string;
  closedAt?: string;
  latestRecallRequestId?: string;
  latestRecallRequestStatus?: "PENDING" | "APPROVED" | "REJECTED";
  hasPendingRecallRequest: boolean;
  updatedAt: string;
}

export interface AssignProductFeedbackPayload {
  assignedToUserId: string;
}

export interface UpdateProductFeedbackProcessingPayload {
  severity: ProductFeedbackSeverity;
  traceCodeId?: string | null;
  processingContent?: string;
  publicResponse?: string;
}

export interface CloseProductFeedbackPayload {
  processingContent?: string;
  publicResponse?: string;
  closeReason: string;
}

export interface CreateProductFeedbackRecallPayload {
  reason: string;
  evidence?: string;
}

export interface ProductFeedbackRecall {
  id: string;
  lotId: string;
  sourceFeedbackId: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  reason: string;
  evidence?: string;
  requestedAt: string;
}
