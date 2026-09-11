import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { HandoverDetailPage } from "../HandoverDetailPage";
import * as handoverApi from "@/api/handoverApi";
import { toast } from "sonner";
import { AppBreadcrumb, BreadcrumbOverrideProvider } from "@/components/common/AppBreadcrumb";
import { Sidebar } from "@/components/layout/Sidebar";

const handoverId = "3dd95ecb-978f-42f7-8b09-cf1a966872d0";
const toOrgId = "327a40dc-a396-11f1-aea2-32ec817c7ea4";
const otherOrgId = "327a3a0e-a396-11f1-aea2-32ec817c7ea4";

let mockOrgId: string = toOrgId;

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({
    user: {
      userId: "user-123",
      username: "procurement",
      fullName: "Nhân viên thu mua Demo",
      roleCode: "VT-04",
      roleName: "Doanh nghiệp thu mua",
      organizationId: mockOrgId,
      organizationName: "Công ty Nông Sản Việt Demo",
      organizationCode: "DEMO_NSV",
      organizationType: "ENTERPRISE",
    },
  }),
}));

vi.mock("@/api/handoverApi", () => ({
  getHandoverById: vi.fn(),
  acceptHandover: vi.fn(),
  rejectHandover: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

function buildHandover(data: Record<string, unknown> = {}) {
  return {
    id: handoverId,
    shipmentId: "00000000-0000-0000-0000-001000000001",
    shipmentName: "Lô hàng Nho đột 01 (demo)",
    fromOrganizationId: otherOrgId,
    fromOrganizationName: "HTX Nông Sản Demo",
    toOrganizationId: toOrgId,
    toOrganizationName: "Công ty Nông Sản Việt Demo",
    quantity: 200,
    status: "PENDING_CONFIRMATION",
    plannedAt: "2026-09-10T09:00:00",
    vehicleInfo: null,
    carrierName: null,
    note: null,
    attachmentPath: null,
    expiresAt: "2026-09-11T05:33:15",
    createdAt: "2026-09-09T05:33:15",
    confirmedBy: null,
    confirmedAt: null,
    rejectedBy: null,
    rejectedAt: null,
    cancelReason: null,
    cancelledBy: null,
    cancelledAt: null,
    ...data,
  };
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={[`/shipment-handovers/${handoverId}`]}>
      <Routes>
        <Route path="/shipment-handovers/:id" element={<HandoverDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("NCL-05-CN-009 - HandoverDetailPage Xác nhận/Từ chối", () => {
  beforeEach(() => {
    mockOrgId = toOrgId;
    vi.clearAllMocks();
  });

  it("hiện nút Xác nhận/Từ chối khi user thuộc tổ chức nhận và phiếu PENDING", async () => {
    vi.mocked(handoverApi.getHandoverById).mockResolvedValue(
      buildHandover() as never,
    );

    renderPage();

    expect(
      await screen.findByRole("button", { name: "Xác nhận nhận hàng" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Từ chối nhận hàng" }),
    ).toBeInTheDocument();
  });

  it("KHÔNG hiện nút khi user không thuộc tổ chức nhận (bên giao)", async () => {
    mockOrgId = otherOrgId;
    vi.mocked(handoverApi.getHandoverById).mockResolvedValue(
      buildHandover() as never,
    );

    renderPage();

    expect(await screen.findByText("Phiếu bàn giao")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Xác nhận nhận hàng" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Từ chối nhận hàng" }),
    ).not.toBeInTheDocument();
  });

  it("KHÔNG hiện nút khi phiếu đã xác nhận (ACCEPTED)", async () => {
    vi.mocked(handoverApi.getHandoverById).mockResolvedValue(
      buildHandover({
        status: "ACCEPTED",
        confirmedAt: "2026-09-09T05:43:01",
        confirmedBy: "user-123",
      }) as never,
    );

    renderPage();

    expect(await screen.findByText("Đã xác nhận")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Xác nhận nhận hàng" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Từ chối nhận hàng" }),
    ).not.toBeInTheDocument();
  });

  it("click Xác nhận nhận hàng gọi acceptHandover và reload", async () => {
    vi.mocked(handoverApi.getHandoverById).mockResolvedValueOnce(
      buildHandover() as never,
    );
    vi.mocked(handoverApi.getHandoverById).mockResolvedValueOnce(
      buildHandover({ status: "ACCEPTED", confirmedAt: "2026-09-09T05:43:01" }) as never,
    );
    vi.mocked(handoverApi.acceptHandover).mockResolvedValue(
      buildHandover({ status: "ACCEPTED" }) as never,
    );

    renderPage();

    const user = userEvent.setup();
    await user.click(
      await screen.findByRole("button", { name: "Xác nhận nhận hàng" }),
    );

    await waitFor(() => {
      expect(handoverApi.acceptHandover).toHaveBeenCalledWith(handoverId);
    });
    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith(
        "Đã xác nhận nhận hàng lô bàn giao.",
      );
    });
    expect(await screen.findByText("Đã xác nhận")).toBeInTheDocument();
  });

  it("click Từ chối mở dialog, yêu cầu lý do, gọi rejectHandover", async () => {
    vi.mocked(handoverApi.getHandoverById).mockResolvedValueOnce(
      buildHandover() as never,
    );
    vi.mocked(handoverApi.getHandoverById).mockResolvedValueOnce(
      buildHandover({ status: "REJECTED", rejectedAt: "2026-09-09T05:49:56" }) as never,
    );
    vi.mocked(handoverApi.rejectHandover).mockResolvedValue(
      buildHandover({ status: "REJECTED" }) as never,
    );

    renderPage();

    const user = userEvent.setup();
    await user.click(
      await screen.findByRole("button", { name: "Từ chối nhận hàng" }),
    );

    const reasonInput = await screen.findByLabelText("Lý do từ chối");
    expect(reasonInput).toBeInTheDocument();

    expect(screen.getByRole("button", { name: "Xác nhận từ chối" })).toBeDisabled();

    await user.type(reasonInput, "Chứng từ kiểm dịch chưa đầy đủ");
    await user.click(screen.getByRole("button", { name: "Xác nhận từ chối" }));

    await waitFor(() => {
      expect(handoverApi.rejectHandover).toHaveBeenCalledWith(
        handoverId,
        "Chứng từ kiểm dịch chưa đầy đủ",
      );
    });
    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith(
        "Đã từ chối nhận hàng lô bàn giao.",
      );
    });
    expect(await screen.findByText("Đã từ chối")).toBeInTheDocument();
  });

  it("hiển thị breadcrumb đúng phân cấp: Tổng quan > Phiếu bàn giao nhận > Chi tiết phiếu bàn giao", async () => {
    vi.mocked(handoverApi.getHandoverById).mockResolvedValue(
      buildHandover() as never,
    );

    render(
      <MemoryRouter initialEntries={[`/shipment-handovers/${handoverId}`]}>
        <BreadcrumbOverrideProvider>
          <AppBreadcrumb />
          <Routes>
            <Route
              path="/shipment-handovers/:id"
              element={<HandoverDetailPage />}
            />
          </Routes>
        </BreadcrumbOverrideProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Chi tiết phiếu bàn giao")).toBeInTheDocument();
    const handoverLink = screen.getByRole("link", { name: "Phiếu bàn giao nhận" });
    expect(handoverLink).toBeInTheDocument();
    expect(handoverLink).toHaveAttribute("href", "/shipment-handovers/received");
  });

  it("Sidebar giữ active menu item 'Phiếu bàn giao nhận' khi ở trang chi tiết /shipment-handovers/:id", async () => {
    vi.mocked(handoverApi.getHandoverById).mockResolvedValue(
      buildHandover() as never,
    );

    render(
      <MemoryRouter initialEntries={[`/shipment-handovers/${handoverId}`]}>
        <Sidebar
          collapsed={false}
          setCollapsed={vi.fn()}
          mobileOpen={false}
          setMobileOpen={vi.fn()}
        />
        <Routes>
          <Route
            path="/shipment-handovers/:id"
            element={<HandoverDetailPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Phiếu bàn giao")).toBeInTheDocument();
    const menuLink = screen.getByRole("link", { name: /Phiếu bàn giao nhận/ });
    expect(menuLink).toBeInTheDocument();
    // Class khi active có bg-emerald-700 text-white
    expect(menuLink.className).toContain("bg-emerald-700");
    expect(menuLink.className).toContain("text-white");
  });
});