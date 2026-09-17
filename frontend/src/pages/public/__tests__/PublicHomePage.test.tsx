import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AxiosError } from "axios";

const mockNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("@/api/productFeedbackApi", () => ({
  lookupPublicProductFeedback: vi.fn(),
}));

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({ user: null, isLoading: false }),
}));

import { lookupPublicProductFeedback } from "@/api/productFeedbackApi";
import PublicHomePage, {
  isProductFeedbackLookupCode,
} from "../PublicHomePage";

function renderPage(initialEntries = ["/"]) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <PublicHomePage />
    </MemoryRouter>,
  );
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

describe("PublicHomePage unified search", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("identifies feedback codes correctly via isProductFeedbackLookupCode helper", () => {
    expect(isProductFeedbackLookupCode("PA-7K2M-9Q4X-H8NP-3R5T")).toBe(true);
    expect(isProductFeedbackLookupCode("pa-7k2m-9q4x-h8np-3r5t")).toBe(true);
    expect(isProductFeedbackLookupCode("PA7K2M9Q4XH8NP3R5T")).toBe(true);

    expect(isProductFeedbackLookupCode("HTX00000001")).toBe(false);
    expect(isProductFeedbackLookupCode("LOT-2026-001")).toBe(false);
    expect(isProductFeedbackLookupCode("")).toBe(false);
  });

  it("navigates to /public/trace/:codeValue when entering normal trace code", () => {
    renderPage();

    const input = screen.getByPlaceholderText(/Nhập mã tra cứu hoặc mã phản ánh/i);
    fireEvent.change(input, { target: { value: "HTX00000001" } });
    fireEvent.click(screen.getByRole("button", { name: "Tìm kiếm" }));

    expect(mockNavigate).toHaveBeenCalledWith("/public/trace/HTX00000001");
    expect(lookupPublicProductFeedback).not.toHaveBeenCalled();
  });

  it("executes feedback lookup inline without navigating when entering PA code", async () => {
    vi.mocked(lookupPublicProductFeedback).mockResolvedValue({
      status: "IN_PROGRESS",
      publicResponse: "Đang tiến hành xác minh thông tin phản ánh.",
    });

    renderPage();

    const input = screen.getByPlaceholderText(/Nhập mã tra cứu hoặc mã phản ánh/i);
    fireEvent.change(input, { target: { value: "PA-7K2M-9Q4X-H8NP-3R5T" } });
    fireEvent.click(screen.getByRole("button", { name: "Tìm kiếm" }));

    expect(mockNavigate).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(lookupPublicProductFeedback).toHaveBeenCalledWith({
        lookupCode: "PA-7K2M-9Q4X-H8NP-3R5T",
      });
    });

    expect(await screen.findByText("Đang xử lý")).toBeInTheDocument();
    expect(
      screen.getByText("Đang tiến hành xác minh thông tin phản ánh."),
    ).toBeInTheDocument();
  });

  it("displays 404 error inline when feedback code is not found", async () => {
    vi.mocked(lookupPublicProductFeedback).mockRejectedValue(axiosError(404));

    renderPage();

    const input = screen.getByPlaceholderText(/Nhập mã tra cứu hoặc mã phản ánh/i);
    fireEvent.change(input, { target: { value: "PA-0000-0000-0000-0000" } });
    fireEvent.click(screen.getByRole("button", { name: "Tìm kiếm" }));

    expect(await screen.findByText("Không tìm thấy phản ánh")).toBeInTheDocument();
    expect(
      screen.getByText("Vui lòng kiểm tra lại mã tra cứu và thử lại."),
    ).toBeInTheDocument();
  });

  it("automatically triggers inline lookup when feedbackCode query param is present", async () => {
    vi.mocked(lookupPublicProductFeedback).mockResolvedValue({
      status: "CLOSED",
      publicResponse: "Phản ánh đã được xử lý xong.",
    });

    renderPage(["/?feedbackCode=PA-7K2M-9Q4X-H8NP-3R5T"]);

    await waitFor(() => {
      expect(lookupPublicProductFeedback).toHaveBeenCalledWith({
        lookupCode: "PA-7K2M-9Q4X-H8NP-3R5T",
      });
    });

    expect(await screen.findByText("Đã đóng")).toBeInTheDocument();
    expect(screen.getByText("Phản ánh đã được xử lý xong.")).toBeInTheDocument();
  });
});
