import apiClient from "@/api/axiosConfig";
import type {
  CreateProductFeedbackPayload,
  ProductFeedback,
} from "@/types/productFeedback";
import type { PageResponse } from "@/types/common";

export const createProductFeedback = async (
  productionLotId: string,
  payload: CreateProductFeedbackPayload,
): Promise<ProductFeedback> => {
  const response = await apiClient.post<{ data: ProductFeedback }>(
    `/public/production-lots/${productionLotId}/feedbacks`,
    payload,
  );

  return response.data.data;
};

export const getProductFeedbacks = async (params?: {
  page?: number;
  size?: number;
  sort?: string;
}): Promise<PageResponse<ProductFeedback>> => {
  const response = await apiClient.get<{ data: PageResponse<ProductFeedback> }>(
    "/product-feedbacks",
    { params },
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
