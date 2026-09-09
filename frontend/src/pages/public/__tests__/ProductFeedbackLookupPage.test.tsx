import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AxiosError } from "axios";

vi.mock("@/api/productFeedbackApi", () => ({
  lookupPublicProductFeedback: vi.fn(),
}));

import { lookupPublicProductFeedback } from "@/api/productFeedbackApi";
import ProductFeedbackLookupPage from "@/pages/public/ProductFeedbackLookupPage";

function renderPage() {
  return render(
    <MemoryRouter>
      <ProductFeedbackLookupPage />
    </MemoryRouter>,
  );
}

function submitLookup(code = " pa-7k2m-9q4x-h8np-3r5t ") {
  fireEvent.change(screen.getByLabelText("Mã tra cứu phản ánh"), {
    target: { value: code },
  });
  fireEvent.click(screen.getByRole("button", { name: "Tra cứu trạng thái" }));
}

function axiosError(status: number) {
  return new AxiosError(
    "Request failed",
    undefined,
    undefined,
    undefined,
    { status, statusText: "Error", headers: {}, config: {} as never, data: {} },
  );
}

describe("ProductFeedbackLookupPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("validates an empty lookup code without calling the API", () => {
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: "Tra cứu trạng thái" }));

    expect(screen.getByText("Vui lòng nhập mã tra cứu phản ánh.")).toBeInTheDocument();
    expect(lookupPublicProductFeedback).not.toHaveBeenCalled();
  });

  it("normalizes the code and renders only the public result", async () => {
    vi.mocked(lookupPublicProductFeedback).mockResolvedValue({
      status: "IN_PROGRESS",
      publicResponse: "Đơn vị phụ trách đang xác minh.",
    });
    renderPage();

    submitLookup();

    await waitFor(() => {
      expect(lookupPublicProductFeedback).toHaveBeenCalledWith({
        lookupCode: "PA-7K2M-9Q4X-H8NP-3R5T",
      });
    });
    expect(await screen.findByText("Đang xử lý")).toBeInTheDocument();
    expect(screen.getByText("Đơn vị phụ trách đang xác minh.")).toBeInTheDocument();
  });

  it("renders the neutral fallback when no public response exists", async () => {
    vi.mocked(lookupPublicProductFeedback).mockResolvedValue({
      status: "NEW",
      publicResponse: null,
    });
    renderPage();

    submitLookup();

    expect(await screen.findByText("Chưa có phản hồi công khai")).toBeInTheDocument();
  });

  it("renders a dedicated not-found state", async () => {
    vi.mocked(lookupPublicProductFeedback).mockRejectedValue(axiosError(404));
    renderPage();

    submitLookup();

    expect(await screen.findByText("Không tìm thấy phản ánh")).toBeInTheDocument();
    expect(screen.queryByText("Chưa có phản hồi công khai")).not.toBeInTheDocument();
  });

  it("renders rate limiting and does not retry automatically", async () => {
    vi.mocked(lookupPublicProductFeedback).mockRejectedValue(axiosError(429));
    renderPage();

    submitLookup();

    expect(await screen.findByText("Bạn đã tra cứu quá nhiều lần")).toBeInTheDocument();
    expect(lookupPublicProductFeedback).toHaveBeenCalledTimes(1);
  });
});
