import { describe, expect, it, vi, beforeEach } from "vitest";

vi.mock("@/api/axiosConfig", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

import apiClient from "@/api/axiosConfig";
import {
  createProductFeedback,
  getProductFeedbacks,
  getProductFeedbackById,
  assignProductFeedback,
  updateProductFeedbackProcessing,
  closeProductFeedback,
  createProductFeedbackRecall,
  lookupPublicProductFeedback,
} from "@/api/productFeedbackApi";

describe("productFeedbackApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("createProductFeedback should post to public endpoint", async () => {
    const lotId = "lot-123";
    const payload = { content: "Tem có dấu hiệu mờ", traceCodeValue: "NGS-001" };
    const mockData = { id: "fb-1", productionLotId: lotId, status: "NEW" as const };
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { data: mockData } });

    const result = await createProductFeedback(lotId, payload);

    expect(apiClient.post).toHaveBeenCalledWith(
      `/public/production-lots/${lotId}/feedbacks`,
      payload,
    );
    expect(result).toEqual(mockData);
  });

  it("lookupPublicProductFeedback should post the public lookup code", async () => {
    const payload = { lookupCode: "PA-7K2M-9Q4X-H8NP-3R5T" };
    const mockData = {
      status: "IN_PROGRESS" as const,
      publicResponse: "Đơn vị phụ trách đang xác minh.",
    };
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { data: mockData } });

    const result = await lookupPublicProductFeedback(payload);

    expect(apiClient.post).toHaveBeenCalledWith(
      "/public/product-feedbacks/lookup",
      payload,
    );
    expect(result).toEqual(mockData);
  });

  it("getProductFeedbacks should get list with query params", async () => {
    const params = { page: 0, size: 10, status: "NEW" as const };
    const mockData = {
      items: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    };
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: { data: mockData } });

    const result = await getProductFeedbacks(params);

    expect(apiClient.get).toHaveBeenCalledWith("/product-feedbacks", { params });
    expect(result).toEqual(mockData);
  });

  it("getProductFeedbackById should fetch single feedback detail", async () => {
    const feedbackId = "fb-123";
    const mockData = { id: feedbackId, content: "Chi tiết" };
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: { data: mockData } });

    const result = await getProductFeedbackById(feedbackId);

    expect(apiClient.get).toHaveBeenCalledWith(`/product-feedbacks/${feedbackId}`);
    expect(result).toEqual(mockData);
  });

  it("assignProductFeedback should put to assignment endpoint", async () => {
    const feedbackId = "fb-123";
    const payload = { assignedToUserId: "user-456" };
    const mockData = { id: feedbackId, assignedToUserId: "user-456" };
    vi.mocked(apiClient.put).mockResolvedValueOnce({ data: { data: mockData } });

    const result = await assignProductFeedback(feedbackId, payload);

    expect(apiClient.put).toHaveBeenCalledWith(
      `/product-feedbacks/${feedbackId}/assignment`,
      payload,
    );
    expect(result).toEqual(mockData);
  });

  it("updateProductFeedbackProcessing should put to processing endpoint", async () => {
    const feedbackId = "fb-123";
    const payload = {
      severity: "QUALITY_SUSPECTED" as const,
      processingContent: "Đã xác minh",
    };
    const mockData = { id: feedbackId, severity: "QUALITY_SUSPECTED" };
    vi.mocked(apiClient.put).mockResolvedValueOnce({ data: { data: mockData } });

    const result = await updateProductFeedbackProcessing(feedbackId, payload);

    expect(apiClient.put).toHaveBeenCalledWith(
      `/product-feedbacks/${feedbackId}/processing`,
      payload,
    );
    expect(result).toEqual(mockData);
  });

  it("closeProductFeedback should put to close endpoint", async () => {
    const feedbackId = "fb-123";
    const payload = { closeReason: "Đã xử lý xong" };
    const mockData = { id: feedbackId, status: "CLOSED" };
    vi.mocked(apiClient.put).mockResolvedValueOnce({ data: { data: mockData } });

    const result = await closeProductFeedback(feedbackId, payload);

    expect(apiClient.put).toHaveBeenCalledWith(
      `/product-feedbacks/${feedbackId}/close`,
      payload,
    );
    expect(result).toEqual(mockData);
  });

  it("createProductFeedbackRecall should post to recall-requests endpoint", async () => {
    const feedbackId = "fb-123";
    const payload = {
      shipmentId: "ship-1",
      reason: "Nghi ngờ chất lượng",
    };
    const mockData = { id: "rec-1", shipmentId: "ship-1", status: "PENDING" };
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { data: mockData } });

    const result = await createProductFeedbackRecall(feedbackId, payload);

    expect(apiClient.post).toHaveBeenCalledWith(
      `/product-feedbacks/${feedbackId}/recall-requests`,
      payload,
    );
    expect(result).toEqual(mockData);
  });
});
