/**
 * Kiểm thử toàn diện component AiChatWidget:
 * - Hiển thị nút nổi FAB ban đầu và mở cửa sổ chat khi nhấn.
 * - Gửi tin nhắn qua input/nút gửi và hiển thị câu trả lời từ AI.
 * - Gửi tin nhắn bằng phím Enter và xuống dòng với Shift+Enter.
 * - Bấm vào chip gợi ý nhanh (QuickPromptChips) để gửi câu hỏi.
 * - Thu nhỏ và đóng cửa sổ chat.
 * - Xóa lịch sử trò chuyện và cập nhật sessionStorage.
 * - Xử lý lỗi kết nối API và hỗ trợ bấm thử lại.
 * - Khôi phục lịch sử trò chuyện từ sessionStorage khi khởi tạo.
 */
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AiChatWidget } from "../AiChatWidget";
import { aiChatApi } from "@/api/aiChatApi";

vi.mock("@/api/aiChatApi", () => ({
  aiChatApi: {
    sendMessage: vi.fn(),
    getSuggestedPrompts: vi.fn().mockResolvedValue([
      {
        category: "Gợi ý chung",
        prompts: ["Cách tạo lô sản xuất mới?", "Quy trình cấp mã QR?"],
      },
    ]),
  },
}));

describe("AiChatWidget", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    // Giả lập scrollIntoView cho môi trường jsdom
    window.HTMLElement.prototype.scrollIntoView = vi.fn();
  });

  it("render nút nổi FAB ban đầu và mở cửa sổ chat khi click vào FAB", () => {
    render(
      <MemoryRouter>
        <AiChatWidget />
      </MemoryRouter>
    );

    // Nút FAB hiển thị ban đầu với aria-label
    const fabButton = screen.getByLabelText("Mở Trợ lý AI");
    expect(fabButton).toBeInTheDocument();

    // Cửa sổ chat chưa hiển thị
    expect(screen.queryByText("Trợ lý ảo Nguồn Gốc Số")).not.toBeInTheDocument();

    // Nhấn mở FAB
    fireEvent.click(fabButton);

    // Cửa sổ chat xuất hiện
    expect(screen.getByText("Trợ lý ảo Nguồn Gốc Số")).toBeInTheDocument();
    expect(screen.getByText("Trực tuyến 24/7")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Nhập câu hỏi của bạn...")).toBeInTheDocument();
  });

  it("gửi tin nhắn qua ô nhập và hiển thị câu trả lời của AI", async () => {
    vi.mocked(aiChatApi.sendMessage).mockResolvedValueOnce({
      reply: "Đây là câu trả lời chi tiết từ AI.",
      timestamp: "2026-09-24T18:00:00",
      suggestedQuestions: ["Bạn có cần thêm thông tin gì không?"],
    });

    render(
      <MemoryRouter>
        <AiChatWidget />
      </MemoryRouter>
    );

    // Mở widget
    fireEvent.click(screen.getByLabelText("Mở Trợ lý AI"));

    // Nhập câu hỏi
    const input = screen.getByPlaceholderText("Nhập câu hỏi của bạn...");
    fireEvent.change(input, { target: { value: "Hướng dẫn tạo lô sản xuất" } });
    expect(input).toHaveValue("Hướng dẫn tạo lô sản xuất");

    // Bấm nút gửi
    const sendButton = screen.getByLabelText("Gửi tin nhắn");
    fireEvent.click(sendButton);

    // Ô input được làm trống sau khi gửi
    expect(input).toHaveValue("");

    // Hiển thị tin nhắn người dùng và phản hồi từ AI
    await waitFor(() => {
      expect(screen.getByText("Hướng dẫn tạo lô sản xuất")).toBeInTheDocument();
      expect(screen.getByText("Đây là câu trả lời chi tiết từ AI.")).toBeInTheDocument();
    });

    // Xác nhận API sendMessage được gọi với đúng nội dung
    expect(aiChatApi.sendMessage).toHaveBeenCalledWith({
      message: "Hướng dẫn tạo lô sản xuất",
      history: expect.any(Array),
    });
  });

  it("gửi tin nhắn bằng phím Enter và không gửi khi nhấn Shift+Enter", async () => {
    vi.mocked(aiChatApi.sendMessage).mockResolvedValueOnce({
      reply: "Phản hồi qua Enter.",
      timestamp: "2026-09-24T18:00:00",
      suggestedQuestions: [],
    });

    render(
      <MemoryRouter>
        <AiChatWidget />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByLabelText("Mở Trợ lý AI"));
    const input = screen.getByPlaceholderText("Nhập câu hỏi của bạn...");

    // Thử Shift+Enter -> không gửi
    fireEvent.change(input, { target: { value: "Tin nhắn nhiều dòng" } });
    fireEvent.keyDown(input, { key: "Enter", shiftKey: true });
    expect(aiChatApi.sendMessage).not.toHaveBeenCalled();

    // Nhấn Enter đơn thuần -> gửi tin nhắn
    fireEvent.keyDown(input, { key: "Enter", shiftKey: false });

    await waitFor(() => {
      expect(screen.getByText("Phản hồi qua Enter.")).toBeInTheDocument();
    });
    expect(aiChatApi.sendMessage).toHaveBeenCalledTimes(1);
  });

  it("gửi câu hỏi khi người dùng bấm vào chip gợi ý nhanh", async () => {
    vi.mocked(aiChatApi.sendMessage).mockResolvedValueOnce({
      reply: "Quy trình cấp mã QR gồm 3 bước...",
      timestamp: "2026-09-24T18:00:00",
      suggestedQuestions: [],
    });

    render(
      <MemoryRouter>
        <AiChatWidget />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByLabelText("Mở Trợ lý AI"));

    // Chờ danh sách gợi ý tải xong
    const promptChip = await screen.findByRole("button", {
      name: "Quy trình cấp mã QR?",
    });
    expect(promptChip).toBeInTheDocument();

    // Nhấn vào chip gợi ý
    fireEvent.click(promptChip);

    await waitFor(() => {
      expect(screen.getByText("Quy trình cấp mã QR gồm 3 bước...")).toBeInTheDocument();
    });

    expect(aiChatApi.sendMessage).toHaveBeenCalledWith({
      message: "Quy trình cấp mã QR?",
      history: expect.any(Array),
    });
  });

  it("thu nhỏ cửa sổ chat khi bấm nút thu nhỏ", () => {
    render(
      <MemoryRouter>
        <AiChatWidget />
      </MemoryRouter>
    );

    // Mở widget
    fireEvent.click(screen.getByLabelText("Mở Trợ lý AI"));
    expect(screen.getByText("Trợ lý ảo Nguồn Gốc Số")).toBeInTheDocument();

    // Bấm nút thu nhỏ
    const minimizeButton = screen.getByTitle("Thu nhỏ");
    fireEvent.click(minimizeButton);

    // Cửa sổ chat ẩn đi, FAB hiện lại
    expect(screen.queryByText("Trợ lý ảo Nguồn Gốc Số")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Mở Trợ lý AI")).toBeInTheDocument();
  });

  it("xóa lịch sử hội thoại khi bấm nút Clear History", async () => {
    render(
      <MemoryRouter>
        <AiChatWidget />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByLabelText("Mở Trợ lý AI"));

    // Nhấn nút xóa lịch sử
    const clearButton = screen.getByTitle("Xóa lịch sử trò chuyện");
    fireEvent.click(clearButton);

    // Tin nhắn thông báo làm mới phiên hội thoại hiển thị
    expect(
      await screen.findByText(
        "Phiên hội thoại đã được làm mới. Tôi có thể hỗ trợ gì thêm cho bạn?"
      )
    ).toBeInTheDocument();
  });

  it("hiển thị tin nhắn lỗi khi API sendMessage thất bại và cho phép thử lại", async () => {
    vi.mocked(aiChatApi.sendMessage)
      .mockRejectedValueOnce(new Error("Mất kết nối mạng"))
      .mockResolvedValueOnce({
        reply: "Đã kết nối lại thành công!",
        timestamp: "2026-09-24T18:00:00",
        suggestedQuestions: [],
      });

    render(
      <MemoryRouter>
        <AiChatWidget />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByLabelText("Mở Trợ lý AI"));

    const input = screen.getByPlaceholderText("Nhập câu hỏi của bạn...");
    fireEvent.change(input, { target: { value: "Câu hỏi cần thử lại" } });
    fireEvent.click(screen.getByLabelText("Gửi tin nhắn"));

    // Chờ thông báo lỗi xuất hiện
    const errorMessage = await screen.findByText(
      /Rất tiếc, kết nối đến Trợ lý AI đang gặp sự cố gián đoạn tạm thời/i
    );
    expect(errorMessage).toBeInTheDocument();

    // Bấm nút thử lại
    const retryButton = screen.getByRole("button", {
      name: /Thử gửi lại câu hỏi/i,
    });
    expect(retryButton).toBeInTheDocument();
    fireEvent.click(retryButton);

    // Phản hồi thành công xuất hiện sau khi thử lại
    await waitFor(() => {
      expect(screen.getByText("Đã kết nối lại thành công!")).toBeInTheDocument();
    });
    expect(aiChatApi.sendMessage).toHaveBeenCalledTimes(2);
  });

  it("khôi phục tin nhắn từ sessionStorage khi khởi tạo", () => {
    const savedMessages = [
      {
        id: "msg-saved-1",
        role: "user",
        content: "Tin nhắn đã lưu từ trước",
        timestamp: "09:00",
      },
      {
        id: "msg-saved-2",
        role: "model",
        content: "Câu trả lời đã lưu từ trước",
        timestamp: "09:01",
      },
    ];
    sessionStorage.setItem(
      "nguongocso_ai_chat_session_v1",
      JSON.stringify(savedMessages)
    );

    render(
      <MemoryRouter>
        <AiChatWidget />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByLabelText("Mở Trợ lý AI"));

    expect(screen.getByText("Tin nhắn đã lưu từ trước")).toBeInTheDocument();
    expect(screen.getByText("Câu trả lời đã lưu từ trước")).toBeInTheDocument();
  });
});
