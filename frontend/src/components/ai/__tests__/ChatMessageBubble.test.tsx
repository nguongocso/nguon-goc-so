/**
 * Kiểm thử component ChatMessageBubble:
 * Xác minh hiển thị tin nhắn của người dùng, tin nhắn của trợ lý AI (hỗ trợ Markdown),
 * chức năng sao chép nội dung vào clipboard và nút thử lại khi xảy ra lỗi.
 */
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ChatMessageBubble } from "../ChatMessageBubble";
import type { ChatMessageItem } from "@/types/aiChat";

describe("ChatMessageBubble", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("hiển thị tin nhắn người dùng với căn phải, nội dung và thời gian", () => {
    const userMessage: ChatMessageItem = {
      id: "msg-1",
      role: "user",
      content: "Tôi muốn tìm hiểu về quy trình cấp mã số vùng trồng",
      timestamp: "10:30",
    };

    render(
      <MemoryRouter>
        <ChatMessageBubble message={userMessage} />
      </MemoryRouter>
    );

    // Xác minh nội dung văn bản hiển thị
    expect(
      screen.getByText("Tôi muốn tìm hiểu về quy trình cấp mã số vùng trồng")
    ).toBeInTheDocument();
    // Xác minh mốc thời gian hiển thị
    expect(screen.getByText("10:30")).toBeInTheDocument();
    // Không có nút sao chép hoặc nút thử lại với tin nhắn người dùng
    expect(
      screen.queryByTitle("Sao chép câu trả lời")
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Thử gửi lại câu hỏi/i })
    ).not.toBeInTheDocument();
  });

  it("hiển thị tin nhắn trợ lý AI với Markdown và hỗ trợ sao chép nội dung", async () => {
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: {
        writeText: writeTextMock,
      },
    });

    const botMessage: ChatMessageItem = {
      id: "msg-2",
      role: "model",
      content: "Để đăng ký mã số, bạn truy cập mục **Vùng trồng**.",
      timestamp: "10:31",
    };

    render(
      <MemoryRouter>
        <ChatMessageBubble message={botMessage} />
      </MemoryRouter>
    );

    // Xác minh Markdown được render (chữ in đậm)
    expect(screen.getByText("Vùng trồng")).toBeInTheDocument();
    expect(screen.getByText("10:31")).toBeInTheDocument();

    // Tìm và nhấn nút sao chép
    const copyButton = screen.getByTitle("Sao chép câu trả lời");
    expect(copyButton).toBeInTheDocument();

    fireEvent.click(copyButton);

    expect(writeTextMock).toHaveBeenCalledWith(
      "Để đăng ký mã số, bạn truy cập mục **Vùng trồng**."
    );
    // Sau khi nhấn sao chép, trạng thái chuyển sang hiển thị "Đã chép"
    expect(await screen.findByText(/Đã chép/i)).toBeInTheDocument();
  });

  it("hiển thị trạng thái lỗi và nút gửi lại khi tin nhắn bị lỗi", () => {
    const handleRetry = vi.fn();
    const errorMessage: ChatMessageItem = {
      id: "msg-err",
      role: "model",
      content: "Không thể kết nối đến máy chủ AI.",
      timestamp: "10:32",
      isError: true,
    };

    render(
      <MemoryRouter>
        <ChatMessageBubble message={errorMessage} onRetry={handleRetry} />
      </MemoryRouter>
    );

    // Xác minh nội dung lỗi hiển thị
    expect(
      screen.getByText("Không thể kết nối đến máy chủ AI.")
    ).toBeInTheDocument();

    // Không hiển thị nút sao chép khi tin nhắn là lỗi
    expect(
      screen.queryByTitle("Sao chép câu trả lời")
    ).not.toBeInTheDocument();

    // Nút thử lại xuất hiện và gọi callback khi được nhấn
    const retryButton = screen.getByRole("button", {
      name: /Thử gửi lại câu hỏi/i,
    });
    expect(retryButton).toBeInTheDocument();

    fireEvent.click(retryButton);
    expect(handleRetry).toHaveBeenCalledTimes(1);
  });

  it("không hiển thị nút thử lại khi tin nhắn lỗi nhưng không truyền hàm onRetry", () => {
    const errorMessage: ChatMessageItem = {
      id: "msg-err-2",
      role: "model",
      content: "Đã có lỗi xảy ra.",
      timestamp: "10:33",
      isError: true,
    };

    render(
      <MemoryRouter>
        <ChatMessageBubble message={errorMessage} />
      </MemoryRouter>
    );

    expect(
      screen.queryByRole("button", { name: /Thử gửi lại câu hỏi/i })
    ).not.toBeInTheDocument();
  });
});
