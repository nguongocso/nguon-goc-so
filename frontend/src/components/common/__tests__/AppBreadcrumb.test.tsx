import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import {
  AppBreadcrumb,
  buildAutoBreadcrumb,
  isRouteAccessible,
  BreadcrumbOverrideProvider,
  useSetBreadcrumb,
} from "../AppBreadcrumb";

// Mock useAuth
vi.mock("@/hooks/useAuth", () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from "@/hooks/useAuth";

const mockUseAuth = vi.mocked(useAuth);

describe("AppBreadcrumb - isRouteAccessible", () => {
  it("trả về false khi href rỗng hoặc undefined", () => {
    expect(isRouteAccessible(undefined, "VT-01")).toBe(false);
    expect(isRouteAccessible("", "VT-01")).toBe(false);
  });

  it("trả về false đối với các route không tồn tại trong hệ thống", () => {
    expect(isRouteAccessible("/transport-events", "VT-03")).toBe(false);
    expect(isRouteAccessible("/chain-events", "VT-03")).toBe(false);
    expect(isRouteAccessible("/preprocessing-events", "VT-02")).toBe(false);
    expect(isRouteAccessible("/packaging-events", "VT-02")).toBe(false);
    expect(isRouteAccessible("/farm-logs", "VT-02")).toBe(false);
    expect(isRouteAccessible("/random-non-existent-route", "VT-01")).toBe(false);
  });

  it("trả về false khi vai trò người dùng không có quyền truy cập route", () => {
    // /recall-requests chỉ dành cho VT-02 (Quản lý HTX)
    expect(isRouteAccessible("/recall-requests", "VT-03")).toBe(false);
    expect(isRouteAccessible("/recall-requests", "VT-04")).toBe(false);
    expect(isRouteAccessible("/recall-requests", "VT-05")).toBe(false);

    // /admin/product-categories chỉ dành cho VT-01 (Admin)
    expect(isRouteAccessible("/admin/product-categories", "VT-02")).toBe(false);
    expect(isRouteAccessible("/admin/product-categories", "VT-03")).toBe(false);

    // /export/open-data chỉ dành cho VT-05
    expect(isRouteAccessible("/export/open-data", "VT-03")).toBe(false);
  });

  it("trả về true khi người dùng có vai trò được phép truy cập", () => {
    // /recall-requests cho VT-02
    expect(isRouteAccessible("/recall-requests", "VT-02")).toBe(true);

    // /dashboard cho tất cả các vai trò
    expect(isRouteAccessible("/dashboard", "VT-01")).toBe(true);
    expect(isRouteAccessible("/dashboard", "VT-02")).toBe(true);
    expect(isRouteAccessible("/dashboard", "VT-03")).toBe(true);
    expect(isRouteAccessible("/dashboard", "VT-04")).toBe(true);
    expect(isRouteAccessible("/dashboard", "VT-05")).toBe(true);

    // /profile cho tất cả các vai trò
    expect(isRouteAccessible("/profile", "VT-03")).toBe(true);
  });

  it("khớp chính xác các route động có tham số (:id, :lotId)", () => {
    // /production-lots/:id cho VT-01, VT-02, VT-03
    expect(isRouteAccessible("/production-lots/lot-uuid-123", "VT-03")).toBe(true);
    expect(isRouteAccessible("/production-lots/lot-uuid-123", "VT-02")).toBe(true);
    expect(isRouteAccessible("/production-lots/lot-uuid-123", "VT-04")).toBe(false);

    // /production-lots/:lotId/shipments/:shipmentId cho VT-01, VT-02, VT-03, VT-04
    expect(
      isRouteAccessible(
        "/production-lots/lot-123/shipments/shipment-456",
        "VT-04",
      ),
    ).toBe(true);
  });
});



describe("AppBreadcrumb - buildAutoBreadcrumb", () => {
  it("không gán href cho các tiền tố nhóm (group prefix) không phải route thật", () => {
    const items = buildAutoBreadcrumb("/transport-events/record");
    expect(items).toEqual([
      { label: "Dashboard", href: "/dashboard" },
      { label: "Vận chuyển" },
      { label: "Ghi vận chuyển" },
    ]);
  });

  it("không gán href cho /farm-logs khi vào /farm-logs/create", () => {
    const items = buildAutoBreadcrumb("/farm-logs/create");
    expect(items).toEqual([
      { label: "Dashboard", href: "/dashboard" },
      { label: "Nhật ký canh tác" },
      { label: "Ghi nhật ký canh tác" },
    ]);
  });

  it("gán href cho các route trung gian tồn tại thật", () => {
    const items = buildAutoBreadcrumb("/recall-requests/create");
    expect(items).toEqual([
      { label: "Dashboard", href: "/dashboard" },
      { label: "Yêu cầu thu hồi", href: "/recall-requests" },
      { label: "Tạo yêu cầu thu hồi" },
    ]);
  });
});

// Component hỗ trợ test useSetBreadcrumb
function TestBreadcrumbOverride({
  items,
}: {
  items: Array<{ label: string; href?: string }> | null;
}) {
  useSetBreadcrumb(items);
  return <AppBreadcrumb />;
}

describe("AppBreadcrumb Component Rendering", () => {
  it("render plain text cho breadcrumb không có quyền truy cập khi dùng useSetBreadcrumb (VT-03 với /recall-requests)", () => {
    mockUseAuth.mockReturnValue({
      user: {
        userId: "user-1",
        username: "vt03",
        fullName: "Người ghi sự kiện",
        roleCode: "VT-03",
        roleName: "Người ghi sự kiện",
        organizationId: "org-1",
        organizationCode: "HTX-01",
        organizationName: "Hợp tác xã 1",
        organizationType: "COOPERATIVE",
      },
      token: "token",
      selectionToken: null,
      isLoading: false,
      loginWithSelection: vi.fn(),
      completeLogin: vi.fn(),
      updateUser: vi.fn(),
      logout: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={["/recall-requests/create"]}>
        <BreadcrumbOverrideProvider>
          <TestBreadcrumbOverride
            items={[
              { label: "Dashboard", href: "/dashboard" },
              { label: "Yêu cầu thu hồi", href: "/recall-requests" },
              { label: "Tạo yêu cầu" },
            ]}
          />
        </BreadcrumbOverrideProvider>
      </MemoryRouter>,
    );

    // Dashboard là link
    const dashboardLink = screen.getByRole("link", { name: "Dashboard" });
    expect(dashboardLink).toHaveAttribute("href", "/dashboard");

    // Yêu cầu thu hồi KHÔNG phải link vì VT-03 không có quyền truy cập /recall-requests
    expect(screen.queryByRole("link", { name: "Yêu cầu thu hồi" })).toBeNull();
    expect(screen.getByText("Yêu cầu thu hồi")).toBeInTheDocument();

    // Trang hiện tại là text
    expect(screen.getByText("Tạo yêu cầu")).toBeInTheDocument();
  });

  it("render link cho breadcrumb khi user có quyền truy cập (VT-02 với /recall-requests)", () => {
    mockUseAuth.mockReturnValue({
      user: {
        userId: "user-2",
        username: "vt02",
        fullName: "Quản lý HTX",
        roleCode: "VT-02",
        roleName: "Quản lý HTX",
        organizationId: "org-1",
        organizationCode: "HTX-01",
        organizationName: "Hợp tác xã 1",
        organizationType: "COOPERATIVE",
      },
      token: "token",
      selectionToken: null,
      isLoading: false,
      loginWithSelection: vi.fn(),
      completeLogin: vi.fn(),
      updateUser: vi.fn(),
      logout: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={["/recall-requests/create"]}>
        <BreadcrumbOverrideProvider>
          <TestBreadcrumbOverride
            items={[
              { label: "Dashboard", href: "/dashboard" },
              { label: "Yêu cầu thu hồi", href: "/recall-requests" },
              { label: "Tạo yêu cầu" },
            ]}
          />
        </BreadcrumbOverrideProvider>
      </MemoryRouter>,
    );

    // Cả Dashboard và Yêu cầu thu hồi đều là link
    const dashboardLink = screen.getByRole("link", { name: "Dashboard" });
    expect(dashboardLink).toHaveAttribute("href", "/dashboard");

    const recallLink = screen.getByRole("link", { name: "Yêu cầu thu hồi" });
    expect(recallLink).toHaveAttribute("href", "/recall-requests");

    // Trang hiện tại là text
    expect(screen.getByText("Tạo yêu cầu")).toBeInTheDocument();
  });

  it("render link không tồn tại như /transport-events dưới dạng plain text thay vì Link", () => {
    mockUseAuth.mockReturnValue({
      user: {
        userId: "user-1",
        username: "vt03",
        fullName: "Người ghi sự kiện",
        roleCode: "VT-03",
        roleName: "Người ghi sự kiện",
        organizationId: "org-1",
        organizationCode: "HTX-01",
        organizationName: "Hợp tác xã 1",
        organizationType: "COOPERATIVE",
      },
      token: "token",
      selectionToken: null,
      isLoading: false,
      loginWithSelection: vi.fn(),
      completeLogin: vi.fn(),
      updateUser: vi.fn(),
      logout: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={["/transport-events/record"]}>
        <BreadcrumbOverrideProvider>
          <TestBreadcrumbOverride
            items={[
              { label: "Dashboard", href: "/dashboard" },
              { label: "Vận chuyển", href: "/transport-events" },
              { label: "Ghi vận chuyển" },
            ]}
          />
        </BreadcrumbOverrideProvider>
      </MemoryRouter>,
    );

    // Dashboard là link
    expect(screen.getByRole("link", { name: "Dashboard" })).toHaveAttribute(
      "href",
      "/dashboard",
    );

    // Vận chuyển không phải là link
    expect(screen.queryByRole("link", { name: "Vận chuyển" })).toBeNull();
    expect(screen.getByText("Vận chuyển")).toBeInTheDocument();
  });
});
