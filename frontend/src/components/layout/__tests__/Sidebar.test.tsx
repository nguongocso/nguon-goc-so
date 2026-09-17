import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import "@testing-library/jest-dom/vitest";
import { Sidebar } from "../Sidebar";

const mockLogout = vi.fn();
let mockUser: any = null;

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({
    user: mockUser,
    logout: mockLogout,
  }),
}));

describe("Sidebar - Highlight active menu Vùng trồng", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = {
      userId: "user-vt02",
      username: "quanlyhtx",
      fullName: "Quản lý Hợp tác xã",
      email: "htx@example.com",
      roleCode: "VT-02",
      organizationProvinceId: "01",
      organizationCommuneId: "00001",
    };
  });

  it("highlight menu Vùng trồng khi ở trang danh sách /farm-areas", () => {
    render(
      <MemoryRouter initialEntries={["/farm-areas"]}>
        <Sidebar collapsed={false} />
      </MemoryRouter>
    );

    const farmAreaLink = screen.getByRole("link", { name: "Vùng trồng" });
    expect(farmAreaLink).toBeInTheDocument();
    expect(farmAreaLink.className).toContain("bg-emerald-700");
    expect(farmAreaLink.className).toContain("text-white");
  });

  it("highlight menu Vùng trồng khi ở trang tạo mới /farm-areas/create", () => {
    render(
      <MemoryRouter initialEntries={["/farm-areas/create"]}>
        <Sidebar collapsed={false} />
      </MemoryRouter>
    );

    const farmAreaLink = screen.getByRole("link", { name: "Vùng trồng" });
    expect(farmAreaLink).toBeInTheDocument();
    expect(farmAreaLink.className).toContain("bg-emerald-700");
    expect(farmAreaLink.className).toContain("text-white");
  });

  it("highlight menu Vùng trồng khi ở trang chỉnh sửa /chinhsuavungtrong/:id", () => {
    render(
      <MemoryRouter initialEntries={["/chinhsuavungtrong/area-uuid-123"]}>
        <Sidebar collapsed={false} />
      </MemoryRouter>
    );

    const farmAreaLink = screen.getByRole("link", { name: "Vùng trồng" });
    expect(farmAreaLink).toBeInTheDocument();
    expect(farmAreaLink.className).toContain("bg-emerald-700");
    expect(farmAreaLink.className).toContain("text-white");
  });

  it("highlight menu Vùng trồng khi ở trang chỉnh sửa /farm-areas/:id/edit", () => {
    render(
      <MemoryRouter initialEntries={["/farm-areas/area-uuid-123/edit"]}>
        <Sidebar collapsed={false} />
      </MemoryRouter>
    );

    const farmAreaLink = screen.getByRole("link", { name: "Vùng trồng" });
    expect(farmAreaLink).toBeInTheDocument();
    expect(farmAreaLink.className).toContain("bg-emerald-700");
    expect(farmAreaLink.className).toContain("text-white");
  });
});
