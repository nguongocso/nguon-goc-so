/**
 * Kiểm thử component AiMarkdownRenderer:
 * Xác minh khả năng phân tích và render Markdown an toàn, hỗ trợ văn bản thuần,
 * in đậm, inline code, tiêu đề, danh sách có/không thứ tự, trích dẫn, và liên kết nội bộ / ngoại bộ.
 */
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AiMarkdownRenderer } from "../AiMarkdownRenderer";

describe("AiMarkdownRenderer", () => {
  it("hiển thị văn bản thuần bình thường", () => {
    render(
      <MemoryRouter>
        <AiMarkdownRenderer content="Xin chào trợ lý ảo" />
      </MemoryRouter>
    );
    expect(screen.getByText("Xin chào trợ lý ảo")).toBeInTheDocument();
  });

  it("hiển thị chữ in đậm với thẻ strong", () => {
    render(
      <MemoryRouter>
        <AiMarkdownRenderer content="Vui lòng nhấn **Bước 1** để tiếp tục" />
      </MemoryRouter>
    );
    const boldEl = screen.getByText("Bước 1");
    expect(boldEl.tagName).toBe("STRONG");
    expect(boldEl).toHaveClass("font-semibold");
  });

  it("hiển thị inline code với thẻ code", () => {
    render(
      <MemoryRouter>
        <AiMarkdownRenderer content="Sử dụng mã `LOT-12345` để tra cứu" />
      </MemoryRouter>
    );
    const codeEl = screen.getByText("LOT-12345");
    expect(codeEl.tagName).toBe("CODE");
    expect(codeEl).toHaveClass("font-mono");
  });

  it("hiển thị tiêu đề cấp 3 (###) thành thẻ h4", () => {
    render(
      <MemoryRouter>
        <AiMarkdownRenderer content="### Hướng dẫn kiểm định" />
      </MemoryRouter>
    );
    const headerEl = screen.getByRole("heading", { level: 4, name: "Hướng dẫn kiểm định" });
    expect(headerEl).toBeInTheDocument();
  });

  it("hiển thị khối trích dẫn / ghi chú (>)", () => {
    render(
      <MemoryRouter>
        <AiMarkdownRenderer content="> Đây là thông tin lưu ý quan trọng" />
      </MemoryRouter>
    );
    expect(screen.getByText("Đây là thông tin lưu ý quan trọng")).toBeInTheDocument();
  });

  it("hiển thị danh sách gạch đầu dòng với dấu '-' hoặc '*'", () => {
    render(
      <MemoryRouter>
        <AiMarkdownRenderer content={"- Mục thứ nhất\n* Mục thứ hai"} />
      </MemoryRouter>
    );
    expect(screen.getByText("Mục thứ nhất")).toBeInTheDocument();
    expect(screen.getByText("Mục thứ hai")).toBeInTheDocument();
  });

  it("hiển thị danh sách đánh số thứ tự", () => {
    render(
      <MemoryRouter>
        <AiMarkdownRenderer content={"1. Bước đầu tiên\n2. Bước thứ hai"} />
      </MemoryRouter>
    );
    expect(screen.getByText("Bước đầu tiên")).toBeInTheDocument();
    expect(screen.getByText("Bước thứ hai")).toBeInTheDocument();
    expect(screen.getByText("1.")).toBeInTheDocument();
    expect(screen.getByText("2.")).toBeInTheDocument();
  });

  it("chuyển đổi link nội bộ thành React Router Link", () => {
    render(
      <MemoryRouter>
        <AiMarkdownRenderer content="Xem tại [Quản lý lô sản xuất](/production-lots)" />
      </MemoryRouter>
    );
    const link = screen.getByRole("link", { name: "Quản lý lô sản xuất" });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute("href", "/production-lots");
    expect(link).not.toHaveAttribute("target");
  });

  it("chuyển đổi link ngoại bộ thành thẻ a mở tab mới", () => {
    render(
      <MemoryRouter>
        <AiMarkdownRenderer content="Tra cứu tại [Cổng thông tin](https://nguongocso.vn/docs)" />
      </MemoryRouter>
    );
    const link = screen.getByRole("link", { name: "Cổng thông tin" });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute("href", "https://nguongocso.vn/docs");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  it("không render gì khi content rỗng", () => {
    const { container } = render(
      <MemoryRouter>
        <AiMarkdownRenderer content="" />
      </MemoryRouter>
    );
    expect(container.firstChild).toBeNull();
  });
});
