import type { ProductFeedback, ProductFeedbackSeverity } from "@/types/productFeedback";

export interface ProductFeedbackProcessingDraft {
  severity: ProductFeedbackSeverity;
  traceCodeId: string;
  processingContent: string;
  publicResponse: string;
}

export function hasUnsavedClassification(
  feedback: ProductFeedback,
  draft: ProductFeedbackProcessingDraft,
): boolean {
  return draft.severity !== feedback.severity
    || (draft.traceCodeId.trim() || undefined) !== (feedback.traceCodeId ?? undefined);
}

export function hasUnsavedProcessing(
  feedback: ProductFeedback,
  draft: ProductFeedbackProcessingDraft,
): boolean {
  return hasUnsavedClassification(feedback, draft)
    || draft.processingContent !== (feedback.processingContent ?? "")
    || draft.publicResponse !== (feedback.publicResponse ?? "");
}
