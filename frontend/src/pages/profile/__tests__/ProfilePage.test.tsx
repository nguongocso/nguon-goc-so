import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ProfilePage } from "../ProfilePage";
import * as userApi from "@/api/userApi";

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({
    user: {
      userId: "user-123",
      username: "farmer01",
      fullName: "Nguyễn Văn Nông Dân",
      phone: "0987654321",
      email: "farmer01@example.com",
      roleCode: "VT-03",
      roleName: "Người ghi sự kiện",
      organizationId: "org-123",
      organizationName: "Hợp tác xã Nông nghiệp Xanh",
      organizationCode: "HTX_XANH",
      organizationType: "COOPERATIVE",
    },
    updateUser: vi.fn(),
  }),
}));

vi.mock("@/api/userApi", () => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  changePassword: vi.fn(),
  uploadAvatar: vi.fn(),
}));

describe("NCL-01-CN-010 - ProfilePage & Password Change UI", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("hiển thị thông tin hồ sơ và form đổi mật khẩu", async () => {
    vi.mocked(userApi.getProfile).mockResolvedValue({
      success: true,
      status: 200,
      message: "Thành công",
      data: {
        id: "user-123",
        userId: "user-123",
        username: "farmer01",
        fullName: "Nguyễn Văn Nông Dân",
        phone: "0987654321",
        email: "farmer01@example.com",
        roleCode: "VT-03",
        roleName: "Người ghi sự kiện",
        organizationId: "org-123",
        organizationName: "Hợp tác xã Nông nghiệp Xanh",
        organizationCode: "HTX_XANH",
        organizationType: "COOPERATIVE",
      },
      timestamp: new Date().toISOString(),
    });

    render(<ProfilePage />);

    expect(
      screen.getByRole("heading", { name: /Hồ sơ cá nhân & Bảo mật/i })
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByDisplayValue("farmer01")).toBeInTheDocument();
      expect(screen.getByDisplayValue("Nguyễn Văn Nông Dân")).toBeInTheDocument();
    });

    expect(screen.getByLabelText(/Mật khẩu hiện tại/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Mật khẩu mới/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Xác nhận mật khẩu mới/i)).toBeInTheDocument();
  });

  it("TC-01: cập nhật số điện thoại thành công khi bấm Chỉnh sửa và Lưu", async () => {
    const user = userEvent.setup();
    vi.mocked(userApi.getProfile).mockResolvedValue({
      success: true,
      status: 200,
      message: "Thành công",
      data: {
        id: "user-123",
        userId: "user-123",
        username: "farmer01",
        fullName: "Nguyễn Văn Nông Dân",
        phone: "0987654321",
        email: "farmer01@example.com",
        roleCode: "VT-03",
        roleName: "Người ghi sự kiện",
        organizationId: "org-123",
        organizationName: "Hợp tác xã Nông nghiệp Xanh",
        organizationCode: "HTX_XANH",
        organizationType: "COOPERATIVE",
      },
      timestamp: new Date().toISOString(),
    });

    vi.mocked(userApi.updateProfile).mockResolvedValue({
      success: true,
      status: 200,
      message: "Thành công",
      data: {
        id: "user-123",
        userId: "user-123",
        username: "farmer01",
        fullName: "Nguyễn Văn Nông Dân",
        phone: "0912345678",
        email: "farmer01@example.com",
        roleCode: "VT-03",
        roleName: "Người ghi sự kiện",
        organizationId: "org-123",
        organizationName: "Hợp tác xã Nông nghiệp Xanh",
        organizationCode: "HTX_XANH",
        organizationType: "COOPERATIVE",
      },
      timestamp: new Date().toISOString(),
    });

    render(<ProfilePage />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("0987654321")).toBeInTheDocument();
    });

    const editBtn = screen.getByRole("button", { name: /Chỉnh sửa/i });
    await user.click(editBtn);

    const saveBtn = await screen.findByRole("button", { name: /Lưu thay đổi/i });
    const phoneInput = screen.getByDisplayValue("0987654321");
    await user.clear(phoneInput);
    await user.type(phoneInput, "0912345678");

    await user.click(saveBtn);

    await waitFor(() => {
      expect(userApi.updateProfile).toHaveBeenCalledWith(
        expect.objectContaining({
          phone: "0912345678",
        })
      );
    });
  });

  it("TC-02: đổi mật khẩu gọi changePassword API", async () => {
    const user = userEvent.setup();
    vi.mocked(userApi.changePassword).mockResolvedValue({
      success: true,
      status: 200,
      message: "Đổi mật khẩu thành công",
      data: undefined as unknown as void,
      timestamp: new Date().toISOString(),
    });

    render(<ProfilePage />);

    const currentPwdInput = screen.getByLabelText(/Mật khẩu hiện tại/i);
    const newPwdInput = screen.getByLabelText(/^Mật khẩu mới/i);
    const confirmPwdInput = screen.getByLabelText(/Xác nhận mật khẩu mới/i);

    await user.type(currentPwdInput, "OldPassword@123");
    await user.type(newPwdInput, "NewPassword@456");
    await user.type(confirmPwdInput, "NewPassword@456");

    const submitBtn = screen.getByRole("button", { name: /Cập nhật mật khẩu/i });
    await user.click(submitBtn);

    await waitFor(() => {
      expect(userApi.changePassword).toHaveBeenCalledWith({
        currentPassword: "OldPassword@123",
        newPassword: "NewPassword@456",
        confirmNewPassword: "NewPassword@456",
      });
    });
  });
});
