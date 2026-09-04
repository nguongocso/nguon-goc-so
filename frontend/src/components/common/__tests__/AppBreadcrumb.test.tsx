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
  it("ẩn hoàn toàn các tiền tố nhóm (group prefix) không phải route thật", () => {
    const items = buildAutoBreadcrumb("/transport-events/record");
    expect(items).toEqual([
      { label: "Tổng quan", href: "/dashboard" },
      { label: "Ghi vận chuyển" },
    ]);
  });

  it("ẩn hoàn toàn /farm-logs khi vào /farm-logs/create", () => {
    const items = buildAutoBreadcrumb("/farm-logs/create");
    expect(items).toEqual([
      { label: "Tổng quan", href: "/dashboard" },
      { label: "Ghi nhật ký canh tác" },
    ]);
  });

  it("ẩn hoàn toàn route không có quyền truy cập (/recall-requests cho VT-03)", () => {
    const items = buildAutoBreadcrumb("/recall-requests/create", "VT-03");
    expect(items).toEqual([
      { label: "Tổng quan", href: "/dashboard" },
      { label: "Tạo yêu cầu thu hồi" },
    ]);
  });

  it("giữ lại route khi người dùng có quyền truy cập (/recall-requests cho VT-02)", () => {
    const items = buildAutoBreadcrumb("/recall-requests/create", "VT-02");
    expect(items).toEqual([
      { label: "Tổng quan", href: "/dashboard" },
      { label: "Yêu cầu thu hồi", href: "/recall-requests" },
      { label: "Tạo yêu cầu thu hồi" },
    ]);
  });

  it("giữ liên kết danh sách khi vào chi tiết lô sản xuất", () => {
    const items = buildAutoBreadcrumb(
      "/production-lots/lot-uuid-123",
      "VT-02",
    );

    expect(items).toEqual([
      { label: "Tổng quan", href: "/dashboard" },
      { label: "Lô sản xuất", href: "/production-lots" },
      { label: "Chi tiết lô sản xuất" },
    ]);
  });

  it("giữ liên kết danh sách khi quản trị phạm vi đơn vị kiểm nghiệm", () => {
    const items = buildAutoBreadcrumb(
      "/admin/testing-units/unit-uuid-123/scopes",
      "VT-01",
    );

    expect(items).toEqual([
      { label: "Tổng quan", href: "/dashboard" },
      { label: "Đơn vị kiểm nghiệm", href: "/admin/testing-units" },
      { label: "Phạm vi công nhận" },
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
  it("ẩn hoàn toàn breadcrumb không có quyền truy cập khi dùng useSetBreadcrumb (VT-03 với /recall-requests)", () => {
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
              { label: "Tổng quan", href: "/dashboard" },
              { label: "Yêu cầu thu hồi", href: "/recall-requests" },
              { label: "Tạo yêu cầu" },
            ]}
          />
        </BreadcrumbOverrideProvider>
      </MemoryRouter>,
    );

    // Dashboard là link
    const dashboardLink = screen.getByRole("link", { name: "Tổng quan" });
    expect(dashboardLink).toHaveAttribute("href", "/dashboard");

    // Yêu cầu thu hồi bị ẨN HOÀN TOÀN vì VT-03 không có quyền truy cập /recall-requests
    expect(screen.queryByText("Yêu cầu thu hồi")).toBeNull();

    // Trang hiện tại luôn hiển thị
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
              { label: "Tổng quan", href: "/dashboard" },
              { label: "Yêu cầu thu hồi", href: "/recall-requests" },
              { label: "Tạo yêu cầu" },
            ]}
          />
        </BreadcrumbOverrideProvider>
      </MemoryRouter>,
    );

    // Cả Dashboard và Yêu cầu thu hồi đều hiển thị và là link
    const dashboardLink = screen.getByRole("link", { name: "Tổng quan" });
    expect(dashboardLink).toHaveAttribute("href", "/dashboard");

    const recallLink = screen.getByRole("link", { name: "Yêu cầu thu hồi" });
    expect(recallLink).toHaveAttribute("href", "/recall-requests");

    // Trang hiện tại là text
    expect(screen.getByText("Tạo yêu cầu")).toBeInTheDocument();
  });

  it("ẩn hoàn toàn breadcrumb có link không tồn tại như /transport-events", () => {
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
              { label: "Tổng quan", href: "/dashboard" },
              { label: "Vận chuyển", href: "/transport-events" },
              { label: "Ghi vận chuyển" },
            ]}
          />
        </BreadcrumbOverrideProvider>
      </MemoryRouter>,
    );

    // Dashboard là link
    expect(screen.getByRole("link", { name: "Tổng quan" })).toHaveAttribute(
      "href",
      "/dashboard",
    );

    // Vận chuyển bị ẩn hoàn toàn
    expect(screen.queryByText("Vận chuyển")).toBeNull();

    // Trang hiện tại hiển thị bình thường
    expect(screen.getByText("Ghi vận chuyển")).toBeInTheDocument();
  });

  it("ẩn hoàn toàn breadcrumb Lô sản xuất đối với VT-04 tại /shipments/:id", () => {
    mockUseAuth.mockReturnValue({
      user: {
        userId: "user-4",
        username: "vt04",
        fullName: "Doanh nghiệp thu mua",
        roleCode: "VT-04",
        roleName: "Doanh nghiệp thu mua",
        organizationId: "org-2",
        organizationCode: "DN-01",
        organizationName: "Doanh nghiệp 1",
        organizationType: "ENTERPRISE",
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
      <MemoryRouter initialEntries={["/shipments/shipment-123"]}>
        <BreadcrumbOverrideProvider>
          <TestBreadcrumbOverride
            items={[
              { label: "Tổng quan", href: "/dashboard" },
              { label: "Lô sản xuất", href: "/production-lots" },
              { label: "Chi tiết lô hàng" },
            ]}
          />
        </BreadcrumbOverrideProvider>
      </MemoryRouter>,
    );

    // Dashboard là link
    expect(screen.getByRole("link", { name: "Tổng quan" })).toHaveAttribute(
      "href",
      "/dashboard",
    );

    // Lô sản xuất bị ẩn hoàn toàn vì VT-04 không có quyền truy cập /production-lots
    expect(screen.queryByText("Lô sản xuất")).toBeNull();

    // Trang hiện tại
    expect(screen.getByText("Chi tiết lô hàng")).toBeInTheDocument();
  });
});
