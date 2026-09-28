import { describe, expect, it } from "vitest";

import type { ProductFeedback } from "@/types/productFeedback";
import {
  hasUnsavedClassification,
  hasUnsavedProcessing,
} from "./productFeedbackDraft";

const feedback: ProductFeedback = {
  id: "feedback-1",
  productionLotId: "lot-1",
  productionLotName: "Lô sản xuất 1",
  content: "Nội dung phản ánh",
  createdAt: "2026-09-07T10:00:00Z",
  updatedAt: "2026-09-07T10:00:00Z",
  status: "IN_PROGRESS",
  severity: "INFORMATION",
  assignedToUserId: "user-1",
  processingContent: "Đang xác minh",
  publicResponse: "Đã tiếp nhận",
  hasPendingRecallRequest: false,
};

describe("productFeedbackDraft", () => {
  it("detects an unsaved severity and trace-code classification", () => {
    const draft = {
      severity: "COUNTERFEIT_SUSPECTED" as const,
      traceCodeId: "trace-1",
      processingContent: "Đang xác minh",
      publicResponse: "Đã tiếp nhận",
    };

    expect(hasUnsavedClassification(feedback, draft)).toBe(true);
    expect(hasUnsavedProcessing(feedback, draft)).toBe(true);
  });

  it("detects unsaved processing text without marking classification dirty", () => {
    const draft = {
      severity: "INFORMATION" as const,
      traceCodeId: "",
      processingContent: "Đã xác minh xong",
      publicResponse: "Đã tiếp nhận",
    };

    expect(hasUnsavedClassification(feedback, draft)).toBe(false);
    expect(hasUnsavedProcessing(feedback, draft)).toBe(true);
  });

  it("treats nullable trace-code values as the same empty value", () => {
    const draft = {
      severity: "INFORMATION" as const,
      traceCodeId: "",
      processingContent: "Đang xác minh",
      publicResponse: "Đã tiếp nhận",
    };

    expect(hasUnsavedClassification({ ...feedback, traceCodeId: undefined }, draft)).toBe(false);
    expect(hasUnsavedProcessing(feedback, draft)).toBe(false);
  });
});
