export interface CreateProductFeedbackPayload {
  content: string;
}

export interface ProductFeedback {
  id: string;
  productionLotId: string;
  productionLotName: string;
  content: string;
  createdAt: string;
  organizationId?: string;
  organizationName?: string;
  productCategoryName?: string;
}
