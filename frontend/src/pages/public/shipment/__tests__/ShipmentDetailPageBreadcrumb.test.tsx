import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ShipmentDetailPage } from "../ShipmentDetailPage";
import { AppBreadcrumb, BreadcrumbOverrideProvider } from "@/components/common/AppBreadcrumb";
import * as shipmentApi from "@/api/shipmentApi";
import * as chainEventApi from "@/api/chainEventApi";
import * as handoverApi from "@/api/handoverApi";
import * as dossierApi from "@/api/dossierApi";

const shipmentId = "b21f26d1-ecc3-48fd-9149-46690a762755";
const handoverId = "h1111111-2222-3333-4444-555555555555";

let mockUser = {
  id: "u1",
  fullName: "Doanh Nghiệp A",
  roleCode: "VT-04",
  organizationId: "org-vt04",
};

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({
    user: mockUser,
    logout: vi.fn(),
  }),
}));

vi.mock("@/api/shipmentApi", () => ({
  getShipmentById: vi.fn(),
  activateShipmentStamps: vi.fn(),
}));

vi.mock("@/api/chainEventApi", () => ({
  getShipmentTimeline: vi.fn(),
}));

vi.mock("@/api/handoverApi", () => ({
  hasPendingHandover: vi.fn(),
  getReceivedHandovers: vi.fn(),
}));

vi.mock("@/api/dossierApi", () => ({
  checkDossierEligibility: vi.fn(),
  exportDossier: vi.fn(),
}));

describe("ShipmentDetailPage - Breadcrumb for VT-04", () => {
  it("hiển thị breadcrumb đúng phân cấp: Tổng quan > Phiếu bàn giao nhận > Chi tiết phiếu bàn giao > Lo hang 1", async () => {
    vi.mocked(shipmentApi.getShipmentById).mockResolvedValue({
      id: shipmentId,
      name: "Lo hang 1",
      status: "ACTIVATED",
      totalQuantity: 500,
      createdAt: "2026-09-10T10:00:00",
      traceCodes: [],
    } as never);

    vi.mocked(chainEventApi.getShipmentTimeline).mockResolvedValue([] as never);
    vi.mocked(handoverApi.hasPendingHandover).mockResolvedValue(false);
    vi.mocked(dossierApi.checkDossierEligibility).mockResolvedValue({ eligible: true } as never);

    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: `/shipments/${shipmentId}`,
            search: `?handoverId=${handoverId}`,
            state: {
              fromHandover: true,
              handoverId,
              handoverRoute: `/shipment-handovers/${handoverId}`,
            },
          },
        ]}
      >
        <BreadcrumbOverrideProvider>
          <AppBreadcrumb />
          <Routes>
            <Route path="/shipments/:id" element={<ShipmentDetailPage />} />
          </Routes>
        </BreadcrumbOverrideProvider>
      </MemoryRouter>,
    );

    // Kiểm tra breadcrumb navigation
    const breadcrumbNav = await screen.findByRole("navigation", { name: "Breadcrumb" });
    expect(breadcrumbNav).toBeInTheDocument();

    // 1. Tổng quan
    const homeLink = within(breadcrumbNav).getByRole("link", { name: "Tổng quan" });
    expect(homeLink).toBeInTheDocument();
    expect(homeLink).toHaveAttribute("href", "/dashboard");

    // 2. Phiếu bàn giao nhận
    const handoverListLink = within(breadcrumbNav).getByRole("link", { name: "Phiếu bàn giao nhận" });
    expect(handoverListLink).toBeInTheDocument();
    expect(handoverListLink).toHaveAttribute("href", "/handover");

    // 3. Chi tiết phiếu bàn giao
    const handoverDetailLink = within(breadcrumbNav).getByRole("link", { name: "Chi tiết phiếu bàn giao" });
    expect(handoverDetailLink).toBeInTheDocument();
    expect(handoverDetailLink).toHaveAttribute("href", `/shipment-handovers/${handoverId}`);

    // 4. Lo hang 1 (trang hiện tại)
    const currentItem = within(breadcrumbNav).getByText("Lo hang 1");
    expect(currentItem).toBeInTheDocument();
    expect(currentItem).toHaveAttribute("aria-current", "page");
  });
});
