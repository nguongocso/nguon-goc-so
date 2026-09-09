import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/api/productFeedbackApi", () => ({
  createProductFeedback: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

import { createProductFeedback } from "@/api/productFeedbackApi";
import { ProductFeedbackForm } from "@/components/public/ProductFeedbackForm";

function renderForm() {
  return render(
    <MemoryRouter>
      <ProductFeedbackForm
        productionLotId="lot-1"
        productName="Xoài Cát Chu"
        traceCodeValue="NGS-001"
      />
    </MemoryRouter>,
  );
}

describe("ProductFeedbackForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows the one-time lookup code after a successful submission", async () => {
    vi.mocked(createProductFeedback).mockResolvedValue({
      id: "feedback-1",
      productionLotId: "lot-1",
      status: "NEW",
      createdAt: "2026-09-08T08:30:00",
      lookupCode: "PA-7K2M-9Q4X-H8NP-3R5T",
    });
    renderForm();

    fireEvent.change(screen.getByLabelText("Nội dung phản ánh *"), {
      target: { value: "Thông tin trên tem không chính xác" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Gửi phản ánh" }));

    await waitFor(() => {
      expect(createProductFeedback).toHaveBeenCalledWith("lot-1", {
        content: "Thông tin trên tem không chính xác",
        traceCodeValue: "NGS-001",
      });
    });
    expect(await screen.findByText("Đã gửi phản ánh")).toBeInTheDocument();
    expect(screen.getByText("PA-7K2M-9Q4X-H8NP-3R5T")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Tra cứu trạng thái" }))
      .toHaveAttribute("href", "/public/product-feedbacks/lookup");
  });

  it("does not replace the form when submission fails", async () => {
    vi.mocked(createProductFeedback).mockRejectedValue(new Error("network"));
    renderForm();

    fireEvent.change(screen.getByLabelText("Nội dung phản ánh *"), {
      target: { value: "Thông tin sai" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Gửi phản ánh" }));

    await waitFor(() => expect(createProductFeedback).toHaveBeenCalled());
    expect(screen.getByRole("button", { name: "Gửi phản ánh" })).toBeInTheDocument();
    expect(screen.queryByText("Mã tra cứu phản ánh của bạn")).not.toBeInTheDocument();
  });
});
