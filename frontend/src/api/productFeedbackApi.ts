import apiClient from "@/api/axiosConfig";
import type {
  CreateProductFeedbackPayload,
  PublicProductFeedbackCreated,
  AssignProductFeedbackPayload,
  UpdateProductFeedbackProcessingPayload,
  CloseProductFeedbackPayload,
  CreateProductFeedbackRecallPayload,
  ProductFeedbackRecall,
  ProductFeedback,
  ProductFeedbackSeverity,
  ProductFeedbackStatus,
} from "@/types/productFeedback";
import type { PageResponse } from "@/types/common";

export const createProductFeedback = async (
  productionLotId: string,
  payload: CreateProductFeedbackPayload,
): Promise<PublicProductFeedbackCreated> => {
  const response = await apiClient.post<{ data: PublicProductFeedbackCreated }>(
    `/public/production-lots/${productionLotId}/feedbacks`,
    payload,
  );

  return response.data.data;
};

export const getProductFeedbacks = async (params?: {
  page?: number;
  size?: number;
  sort?: string;
  keyword?: string;
  status?: ProductFeedbackStatus;
  severity?: ProductFeedbackSeverity;
  productionLotId?: string;
  assignedToUserId?: string;
}): Promise<PageResponse<ProductFeedback>> => {
  const response = await apiClient.get<{ data: PageResponse<ProductFeedback> }>(
    "/product-feedbacks",
    { params },
  );
  return response.data.data;
};

export const assignProductFeedback = async (
  feedbackId: string,
  payload: AssignProductFeedbackPayload,
): Promise<ProductFeedback> => {
  const response = await apiClient.put<{ data: ProductFeedback }>(
    `/product-feedbacks/${feedbackId}/assignment`,
    payload,
  );
  return response.data.data;
};

export const updateProductFeedbackProcessing = async (
  feedbackId: string,
  payload: UpdateProductFeedbackProcessingPayload,
): Promise<ProductFeedback> => {
  const response = await apiClient.put<{ data: ProductFeedback }>(
    `/product-feedbacks/${feedbackId}/processing`,
    payload,
  );
  return response.data.data;
};

export const closeProductFeedback = async (
  feedbackId: string,
  payload: CloseProductFeedbackPayload,
): Promise<ProductFeedback> => {
  const response = await apiClient.put<{ data: ProductFeedback }>(
    `/product-feedbacks/${feedbackId}/close`,
    payload,
  );
  return response.data.data;
};

export const createProductFeedbackRecall = async (
  feedbackId: string,
  payload: CreateProductFeedbackRecallPayload,
): Promise<ProductFeedbackRecall> => {
  const response = await apiClient.post<{ data: ProductFeedbackRecall }>(
    `/product-feedbacks/${feedbackId}/recall-requests`,
    payload,
  );
  return response.data.data;
};

export const getProductFeedbackById = async (
  feedbackId: string,
): Promise<ProductFeedback> => {
  const response = await apiClient.get<{ data: ProductFeedback }>(
    `/product-feedbacks/${feedbackId}`,
  );
  return response.data.data;
};
