import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import "@testing-library/jest-dom/vitest";
import { Header } from "../Header";

const mockLogout = vi.fn();
const mockCompleteLogin = vi.fn();
let mockUser: any = null;

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({
    user: mockUser,
    logout: mockLogout,
    completeLogin: mockCompleteLogin,
  }),
}));

vi.mock("@/api/authApi", () => ({
  getMyOrganizations: vi.fn().mockResolvedValue({ success: true, data: [] }),
  switchOrganization: vi.fn(),
}));

vi.mock("@/components/notification/NotificationBell", () => ({
  NotificationBell: () => <div data-testid="notification-bell">Bell</div>,
}));

vi.mock("@/components/layout/SyncBadge", () => ({
  SyncBadge: () => <div data-testid="sync-badge">Sync</div>,
}));

vi.mock("@/components/common/Logo", () => ({
  Logo: () => <div data-testid="logo">Logo</div>,
}));

describe("Header component - Avatar & Notification Badge", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("hiển thị avatar image và fallback sang icon User khi onError xảy ra", () => {
    mockUser = {
      userId: "user-1",
      username: "testuser",
      fullName: "Nguyễn Văn Test",
      email: "test@example.com",
      roleCode: "VT-03",
      avatarUrl: "/uploads/avatars/user-1.png",
    };

    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    );

    const img = screen.getByRole("img", { name: "Nguyễn Văn Test" });
    expect(img).toBeInTheDocument();
    expect(img.getAttribute("src")).toContain("/uploads/avatars/user-1.png");

    // Giả lập ảnh load lỗi (onError)
    fireEvent.error(img);

    // Ảnh phải được thay thế bằng fallback icon User thay vì hiển thị ảnh vỡ
    expect(screen.queryByRole("img", { name: "Nguyễn Văn Test" })).not.toBeInTheDocument();
  });

  it("hiển thị badge cảnh báo chưa cập nhật email trên avatar ngoài overflow-hidden", () => {
    mockUser = {
      userId: "user-2",
      username: "noemailuser",
      fullName: "Trần Không Email",
      email: "",
      roleCode: "VT-03",
      avatarUrl: null,
    };

    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    );

    // Badge phải hiển thị với title "Chưa cập nhật email"
    const badges = screen.getAllByTitle("Chưa cập nhật email");
    const badge = badges[0];
    expect(badge).toBeInTheDocument();
    expect(badge.className).toContain("bg-red-500");
    // Badge nằm trên wrapper có relative inline-flex để không bị overflow-hidden cắt xén
    expect(badge.parentElement?.className).toContain("relative inline-flex");
  });
});
