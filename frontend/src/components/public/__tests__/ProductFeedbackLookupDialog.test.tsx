import "@testing-library/jest-dom/vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AxiosError } from "axios";
import { lookupPublicProductFeedback } from "../../../api/productFeedbackApi";
import { ProductFeedbackLookupDialog } from "../ProductFeedbackLookupDialog";

vi.mock("@/api/productFeedbackApi", () => ({
  lookupPublicProductFeedback: vi.fn(),
}));

function axiosError(status: number) {
  return new AxiosError(
    "Request failed",
    undefined,
    undefined,
    undefined,
    { status, statusText: "Error", headers: {}, config: {} as never, data: {} },
  );
}

describe("ProductFeedbackLookupDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the dialog with input and title when open", () => {
    render(
      <ProductFeedbackLookupDialog
        open={true}
        onOpenChange={vi.fn()}
      />,
    );

    expect(screen.getByText("Tra cứu trạng thái phản ánh")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("PA-XXXX-XXXX-XXXX-XXXX"),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Tra cứu" })).toBeInTheDocument();
  });

  it("validates empty lookup code without calling API", () => {
    render(
      <ProductFeedbackLookupDialog
        open={true}
        onOpenChange={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Tra cứu" }));

    expect(
      screen.getByText("Vui lòng nhập mã tra cứu phản ánh."),
    ).toBeInTheDocument();
    expect(lookupPublicProductFeedback).not.toHaveBeenCalled();
  });

  it("successfully looks up feedback and renders status and response", async () => {
    vi.mocked(lookupPublicProductFeedback).mockResolvedValue({
      status: "IN_PROGRESS",
      publicResponse: "Đơn vị đang xác minh mẫu sản phẩm.",
    });

    render(
      <ProductFeedbackLookupDialog
        open={true}
        onOpenChange={vi.fn()}
      />,
    );

    const input = screen.getByPlaceholderText("PA-XXXX-XXXX-XXXX-XXXX");
    fireEvent.change(input, { target: { value: "PA-7K2M-9Q4X-H8NP-3R5T" } });
    fireEvent.click(screen.getByRole("button", { name: "Tra cứu" }));

    await waitFor(() => {
      expect(lookupPublicProductFeedback).toHaveBeenCalledWith({
        lookupCode: "PA-7K2M-9Q4X-H8NP-3R5T",
      });
    });

    expect(await screen.findByText("Đang xử lý")).toBeInTheDocument();
    expect(
      screen.getByText("Đơn vị đang xác minh mẫu sản phẩm."),
    ).toBeInTheDocument();
  });

  it("automatically queries when initialCode is provided", async () => {
    vi.mocked(lookupPublicProductFeedback).mockResolvedValue({
      status: "NEW",
      publicResponse: null,
    });

    render(
      <ProductFeedbackLookupDialog
        open={true}
        onOpenChange={vi.fn()}
        initialCode="PA-7K2M-9Q4X-H8NP-3R5T"
      />,
    );

    await waitFor(() => {
      expect(lookupPublicProductFeedback).toHaveBeenCalledWith({
        lookupCode: "PA-7K2M-9Q4X-H8NP-3R5T",
      });
    });

    expect(await screen.findByText("Đã tiếp nhận")).toBeInTheDocument();
  });

  it("renders 404 error when code not found", async () => {
    vi.mocked(lookupPublicProductFeedback).mockRejectedValue(axiosError(404));

    render(
      <ProductFeedbackLookupDialog
        open={true}
        onOpenChange={vi.fn()}
      />,
    );

    const input = screen.getByPlaceholderText("PA-XXXX-XXXX-XXXX-XXXX");
    fireEvent.change(input, { target: { value: "PA-0000-0000-0000-0000" } });
    fireEvent.click(screen.getByRole("button", { name: "Tra cứu" }));

    expect(await screen.findByText("Không tìm thấy phản ánh")).toBeInTheDocument();
  });
});
