import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { NotificationBell } from "../NotificationBell";
import type { NotificationResponse } from "@/types/notification";

const mockNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({
    user: {
      userId: "user-1",
      roleCode: "VT-02",
      email: "manager@example.com",
    },
    isLoading: false,
  }),
}));

const mockMarkAsRead = vi.fn().mockResolvedValue({ id: "notif-1", isRead: true, readAt: "2026-09-10T10:00:00Z" });
const mockRefreshUnreadCount = vi.fn().mockResolvedValue(undefined);

const mockNotificationItem: NotificationResponse = {
  id: "notif-expiring-1",
  type: "ALERT",
  title: "Cảnh báo: Kết quả kiểm nghiệm sắp hết hiệu lực",
  content: 'Lô sản xuất "Lô chè Shan Tuyết A1" có kết quả kiểm nghiệm sẽ hết hiệu lực sau 10 ngày (ngày hết hạn: 2026-09-20). Vui lòng chủ động lập kế hoạch kiểm nghiệm mới.',
  isRead: false,
  readAt: null,
  createdAt: "2026-09-10T08:00:00Z",
};

vi.mock("@/hooks/useNotifications", () => ({
  useNotifications: () => ({
    items: [mockNotificationItem],
    isLoading: false,
    load: vi.fn(),
    markAsRead: mockMarkAsRead,
  }),
}));

vi.mock("@/hooks/useUnreadCount", () => ({
  useUnreadCount: () => ({
    unreadCount: 1,
    refresh: mockRefreshUnreadCount,
  }),
}));

describe("NCL-11-CN-004: Notification Inspection Navigation", () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mockMarkAsRead.mockClear();
    mockRefreshUnreadCount.mockClear();
  });

  it("điều hướng đến /production-lots khi người dùng nhấp vào thông báo kiểm nghiệm sắp hết hiệu lực", async () => {
    render(
      <MemoryRouter>
        <NotificationBell />
      </MemoryRouter>,
    );

    // Mở popover thông báo bằng cách bấm vào chuông
    const bellButton = screen.getByTitle("Thông báo");
    fireEvent.click(bellButton);

    // Kiểm tra title thông báo hiển thị đúng
    await waitFor(() => {
      expect(
        screen.getByText("Cảnh báo: Kết quả kiểm nghiệm sắp hết hiệu lực"),
      ).toBeDefined();
    });

    // Bấm vào item thông báo
    const itemButton = screen.getByText(
      "Cảnh báo: Kết quả kiểm nghiệm sắp hết hiệu lực",
    ).closest("button");
    expect(itemButton).toBeDefined();
    fireEvent.click(itemButton!);

    // Kiểm tra markAsRead được gọi
    expect(mockMarkAsRead).toHaveBeenCalledWith("notif-expiring-1");

    // Kiểm tra điều hướng tới /production-lots
    expect(mockNavigate).toHaveBeenCalledWith("/production-lots");
  });
});
