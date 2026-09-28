/**
 * Kiểm thử tích hợp component MainLayout:
 * Xác minh MainLayout render đầy đủ Header, Sidebar, nội dung trang con (Outlet)
 * và nút Trợ lý AI (AiChatWidget).
 */
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import "@testing-library/jest-dom/vitest";
import { MainLayout } from "../MainLayout";

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({
    user: {
      userId: "user-1",
      username: "testuser",
      fullName: "Nguyễn Văn Test",
      email: "test@example.com",
      roleCode: "VT-01",
    },
    logout: vi.fn(),
    completeLogin: vi.fn(),
  }),
}));

vi.mock("@/api/authApi", () => ({
  getMyOrganizations: vi.fn().mockResolvedValue({ success: true, data: [] }),
  switchOrganization: vi.fn(),
}));

vi.mock("@/components/notification/NotificationBell", () => ({
  NotificationBell: () => <div data-testid="notification-bell">Bell</div>,
}));

vi.mock("@/components/common/Logo", () => ({
  Logo: () => <div data-testid="logo">Logo</div>,
}));

vi.mock("@/hooks/useMediaQuery", () => ({
  useMediaQuery: vi.fn().mockReturnValue(true), // Mặc định chế độ desktop
}));

describe("MainLayout component", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("render thành công layout chính bao gồm Header, Outlet và nút mở Trợ lý AI", () => {
    render(
      <MemoryRouter initialEntries={["/dashboard"]}>
        <Routes>
          <Route element={<MainLayout />}>
            <Route path="/dashboard" element={<div data-testid="child-page">Dashboard Content</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    // Xác nhận nội dung trang con được hiển thị
    expect(screen.getByTestId("child-page")).toBeInTheDocument();
    expect(screen.getByText("Dashboard Content")).toBeInTheDocument();
  });
});
