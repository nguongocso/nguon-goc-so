import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";
import { UserProfileForm } from "../UserProfileForm";
import * as userApi from "@/api/userApi";

const mockUpdateUser = vi.fn();
let mockUser: any = null;

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({
    user: mockUser,
    updateUser: mockUpdateUser,
  }),
}));

vi.mock("@/api/userApi", () => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  uploadAvatar: vi.fn(),
}));

describe("UserProfileForm component - Avatar rendering and fallback", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("hiển thị avatar khi có avatarUrl và tự động fallback về chữ cái đầu khi ảnh lỗi", async () => {
    mockUser = {
      userId: "user-1",
      username: "farmer01",
      fullName: "Nguyễn Văn Nông Dân",
      phone: "0987654321",
      email: "farmer@example.com",
      avatarUrl: "/uploads/avatars/user-1.png",
      roleCode: "VT-03",
    };

    vi.mocked(userApi.getProfile).mockResolvedValue({
      success: true,
      status: 200,
      message: "Thành công",
      data: {
        id: "user-1",
        userId: "user-1",
        username: "farmer01",
        fullName: "Nguyễn Văn Nông Dân",
        phone: "0987654321",
        email: "farmer@example.com",
        avatarUrl: "/uploads/avatars/user-1.png",
        roleCode: "VT-03",
      },
      timestamp: new Date().toISOString(),
    });

    render(<UserProfileForm />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("Nguyễn Văn Nông Dân")).toBeInTheDocument();
    });

    const img = screen.getByRole("img", { name: "Nguyễn Văn Nông Dân" });
    expect(img).toBeInTheDocument();
    expect(img.getAttribute("src")).toContain("/uploads/avatars/user-1.png");

    // Khi ảnh bị lỗi (404 hoặc response html từ proxy)
    fireEvent.error(img);

    // Không còn thẻ img, fallback hiển thị chữ cái đầu tiên của fullName ("N")
    expect(screen.queryByRole("img", { name: "Nguyễn Văn Nông Dân" })).not.toBeInTheDocument();
    expect(screen.getByText("N")).toBeInTheDocument();
  });
});
