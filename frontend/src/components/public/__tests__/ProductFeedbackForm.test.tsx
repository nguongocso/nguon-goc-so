import "@testing-library/jest-dom/vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import {
  createProductFeedback,
  lookupPublicProductFeedback,
} from "../../../api/productFeedbackApi";
import { ProductFeedbackForm } from "../ProductFeedbackForm";

vi.mock("@/api/productFeedbackApi", () => ({
  createProductFeedback: vi.fn(),
  lookupPublicProductFeedback: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

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

  it("shows the one-time lookup code after a successful submission and copies code to clipboard", async () => {
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: {
        writeText: writeTextMock,
      },
    });

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

    const copyBtn = screen.getByRole("button", { name: "Sao chép mã" });
    const lookupStatusBtn = screen.getByRole("button", {
      name: "Tra cứu trạng thái",
    });
    expect(copyBtn).toBeInTheDocument();
    expect(lookupStatusBtn).toBeInTheDocument();

    // Thao tác sao chép mã
    fireEvent.click(copyBtn);
    expect(writeTextMock).toHaveBeenCalledWith("PA-7K2M-9Q4X-H8NP-3R5T");
    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith("Đã sao chép mã tra cứu.");
    });
  });

  it("opens lookup dialog with prefilled code when clicking lookup status button after submission", async () => {
    vi.mocked(createProductFeedback).mockResolvedValue({
      id: "feedback-1",
      productionLotId: "lot-1",
      status: "NEW",
      createdAt: "2026-09-08T08:30:00",
      lookupCode: "PA-7K2M-9Q4X-H8NP-3R5T",
    });
    vi.mocked(lookupPublicProductFeedback).mockResolvedValue({
      status: "NEW",
      publicResponse: null,
    });

    renderForm();

    fireEvent.change(screen.getByLabelText("Nội dung phản ánh *"), {
      target: { value: "Thông tin trên tem không chính xác" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Gửi phản ánh" }));

    await screen.findByText("Đã gửi phản ánh");

    const lookupStatusBtn = screen.getByRole("button", {
      name: "Tra cứu trạng thái",
    });
    fireEvent.click(lookupStatusBtn);

    expect(screen.getByText("Tra cứu trạng thái phản ánh")).toBeInTheDocument();
    await waitFor(() => {
      expect(lookupPublicProductFeedback).toHaveBeenCalledWith({
        lookupCode: "PA-7K2M-9Q4X-H8NP-3R5T",
      });
    });
  });

  it("resets submission view and returns to form when clicking submit another button", async () => {
    vi.mocked(createProductFeedback).mockResolvedValue({
      id: "feedback-1",
      productionLotId: "lot-1",
      status: "NEW",
      createdAt: "2026-09-08T08:30:00",
      lookupCode: "PA-7K2M-9Q4X-H8NP-3R5T",
    });

    renderForm();

    fireEvent.change(screen.getByLabelText("Nội dung phản ánh *"), {
      target: { value: "Nội dung phản ánh mẫu" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Gửi phản ánh" }));

    await screen.findByText("Đã gửi phản ánh");

    const submitAnotherBtn = screen.getByRole("button", {
      name: "Gửi phản ánh khác",
    });
    fireEvent.click(submitAnotherBtn);

    expect(
      screen.getByRole("button", { name: "Gửi phản ánh" }),
    ).toBeInTheDocument();
  });

  it("renders lookup button before submit button and opens dialog", () => {
    renderForm();

    const lookupBtn = screen.getByRole("button", { name: "Tra cứu phản ánh" });
    const submitBtn = screen.getByRole("button", { name: "Gửi phản ánh" });
    expect(lookupBtn).toBeInTheDocument();
    expect(submitBtn).toBeInTheDocument();

    fireEvent.click(lookupBtn);
    expect(screen.getByText("Tra cứu trạng thái phản ánh")).toBeInTheDocument();
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
    expect(toast.error).toHaveBeenCalled();
  });
});
