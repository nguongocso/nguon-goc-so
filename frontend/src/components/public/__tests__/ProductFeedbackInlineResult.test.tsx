import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "@/context/LanguageContext";
import { ProductFeedbackInlineResult } from "../ProductFeedbackInlineResult";

describe("ProductFeedbackInlineResult", () => {
  it("renders loading state correctly", () => {
    render(
      <ProductFeedbackInlineResult
        isLoading={true}
        lookupCode="PA-7K2M-9Q4X-H8NP-3R5T"
        result={null}
        errorKind={null}
        onReset={vi.fn()}
      />,
    );

    expect(
      screen.getByText(/Đang tra cứu trạng thái phản ánh/),
    ).toBeInTheDocument();
  });

  it("renders success result with status badge and public response", () => {
    const onReset = vi.fn();

    render(
      <ProductFeedbackInlineResult
        isLoading={false}
        lookupCode="PA-7K2M-9Q4X-H8NP-3R5T"
        result={{
          status: "IN_PROGRESS",
          publicResponse: "Đang tiến hành kiểm tra mẫu sản phẩm.",
        }}
        errorKind={null}
        onReset={onReset}
      />,
    );

    expect(screen.getByText("PA-7K2M-9Q4X-H8NP-3R5T")).toBeInTheDocument();
    expect(screen.getByText("Đang xử lý")).toBeInTheDocument();
    expect(
      screen.getByText("Đang tiến hành kiểm tra mẫu sản phẩm."),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /Tra cứu mã khác/i }));
    expect(onReset).toHaveBeenCalled();
  });

  it("renders fallback text when publicResponse is empty", () => {
    render(
      <ProductFeedbackInlineResult
        isLoading={false}
        lookupCode="PA-7K2M-9Q4X-H8NP-3R5T"
        result={{
          status: "NEW",
          publicResponse: null,
        }}
        errorKind={null}
        onReset={vi.fn()}
      />,
    );

    expect(screen.getByText("Đã tiếp nhận")).toBeInTheDocument();
    expect(
      screen.getByText("Chưa có phản hồi công khai"),
    ).toBeInTheDocument();
  });

  it("renders error state for not-found", () => {
    const onReset = vi.fn();

    render(
      <ProductFeedbackInlineResult
        isLoading={false}
        lookupCode="PA-INVALID"
        result={null}
        errorKind="not-found"
        onReset={onReset}
      />,
    );

    expect(screen.getByText("Không tìm thấy phản ánh")).toBeInTheDocument();
    expect(
      screen.getByText("Vui lòng kiểm tra lại mã tra cứu và thử lại."),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    expect(onReset).toHaveBeenCalled();
  });

  it("renders error state for rate-limit", () => {
    render(
      <ProductFeedbackInlineResult
        isLoading={false}
        lookupCode="PA-7K2M-9Q4X-H8NP-3R5T"
        result={null}
        errorKind="rate-limit"
        onReset={vi.fn()}
      />,
    );

    expect(screen.getByText("Bạn đã tra cứu quá nhiều lần")).toBeInTheDocument();
  });

  it("renders english content when language is set to EN", () => {
    sessionStorage.setItem("public_lookup_lang", "en");

    render(
      <LanguageProvider>
        <ProductFeedbackInlineResult
          isLoading={false}
          lookupCode="PA-B35W-UZFW-G5XD-4KM4"
          result={{
            status: "NEW",
            publicResponse: null,
          }}
          errorKind={null}
          onReset={vi.fn()}
        />
      </LanguageProvider>,
    );

    expect(screen.getByText("Feedback Lookup Result")).toBeInTheDocument();
    expect(screen.getByText("Feedback Status")).toBeInTheDocument();
    expect(screen.getByText("Received")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Feedback has been recorded and is pending review.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByText("Public Response")).toBeInTheDocument();
    expect(screen.getByText("No public response yet")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /Search Another Code/i }),
    ).toBeInTheDocument();

    sessionStorage.removeItem("public_lookup_lang");
  });
});
