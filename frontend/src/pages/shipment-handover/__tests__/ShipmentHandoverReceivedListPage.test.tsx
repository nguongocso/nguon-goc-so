import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes, useParams } from "react-router-dom";
import { toast } from "sonner";
import { ShipmentHandoverReceivedListPage } from "../ShipmentHandoverReceivedListPage";
import * as handoverApi from "@/api/handoverApi";

const toOrgId = "327a40dc-a396-11f1-aea2-32ec817c7ea4";
const fromOrgId = "327a3a0e-a396-11f1-aea2-32ec817c7ea4";

vi.mock("@/api/handoverApi", () => ({
  getReceivedHandovers: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

function buildHandover(data: Record<string, unknown> = {}) {
  return {
    id: "3dd95ecb-978f-42f7-8b09-cf1a966872d0",
    shipmentId: "00000000-0000-0000-0000-001000000001",
    shipmentName: "Lô hàng Nho đỏ 01 (demo)",
    fromOrganizationId: fromOrgId,
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

function DetailStub() {
  const { id } = useParams<{ id: string }>();
  return <div>detail:{id}</div>;
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/shipment-handovers/received"]}>
      <Routes>
        <Route
          path="/shipment-handovers/received"
          element={<ShipmentHandoverReceivedListPage />}
        />
        <Route path="/shipment-handovers/:id" element={<DetailStub />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("NCL-05-CN-008/009 - Danh sách phiếu bàn giao nhận (VT-04)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("hiển thị danh sách phiếu nhận được với trạng thái Việt hóa", async () => {
    vi.mocked(handoverApi.getReceivedHandovers).mockResolvedValue([
      buildHandover() as never,
      buildHandover({
        id: "b5c2463b-0007-4704-8277-a152a525a697",
        shipmentName: "Lô hàng Sầu riêng đột 02 (demo)",
        fromOrganizationId: "327a3a0e-a396-11f1-aea2-32ec817c7ea4",
        fromOrganizationName: "HTX Phúc Trách Demo",
        quantity: 50,
        status: "ACCEPTED",
      }) as never,
    ]);

    renderPage();

    expect(await screen.findByText("Lô hàng Nho đỏ 01 (demo)")).toBeInTheDocument();
    expect(screen.getByText("HTX Nông Sản Demo")).toBeInTheDocument();
    expect(screen.getByText("200 kg")).toBeInTheDocument();
    expect(screen.getAllByText("Chờ xác nhận")).toHaveLength(1);
    expect(screen.getByText("Đã xác nhận")).toBeInTheDocument();

    const detailButtons = screen.getAllByRole("button", { name: /Xem chi tiết/ });
    expect(detailButtons).toHaveLength(2);
  });

  it('click "Xem chi tiết" điều hướng tới trang chi tiết phiếu', async () => {
    vi.mocked(handoverApi.getReceivedHandovers).mockResolvedValue([
      buildHandover() as never,
    ]);

    renderPage();

    const user = userEvent.setup();
    await user.click(
      await screen.findByRole("button", { name: /Xem chi tiết/ }),
    );

    expect(
      await screen.findByText(
        "detail:3dd95ecb-978f-42f7-8b09-cf1a966872d0",
      ),
    ).toBeInTheDocument();
  });

  it("hiển thị thông báo rỗng khi chưa có phiếu nào", async () => {
    vi.mocked(handoverApi.getReceivedHandovers).mockResolvedValue([]);

    renderPage();

    expect(
      await screen.findByText(
        "Chưa có phiếu bàn giao nào gửi tới tổ chức của bạn.",
      ),
    ).toBeInTheDocument();
  });

  it("lọc theo từ khóa tìm kiếm tên lô hàng", async () => {
    vi.mocked(handoverApi.getReceivedHandovers).mockResolvedValue([
      buildHandover({ shipmentName: "Lô hàng Nho đỏ 01 (demo)" }) as never,
      buildHandover({
        id: "f1fa4b12-2a1b-4294-9c52-d99e36bc32d1",
        shipmentName: "Lô hàng Sầu riêng đột 02 (demo)",
        status: "ACCEPTED",
        quantity: 50,
      }) as never,
    ]);

    renderPage();

    const searchInput = await screen.findByPlaceholderText(
      "Tìm theo tên lô hàng hoặc tổ chức giao...",
    );
    const user = userEvent.setup();
    await user.type(searchInput, "Nho");

    expect(
      screen.getByText("Lô hàng Nho đỏ 01 (demo)"),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Lô hàng Sầu riêng đột 02 (demo)"),
    ).not.toBeInTheDocument();
  });

  it("hiển thị toast lỗi khi API thất bại", async () => {
    vi.mocked(handoverApi.getReceivedHandovers).mockRejectedValue(
      new Error("network"),
    );

    renderPage();

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        "Không thể tải danh sách phiếu bàn giao nhận.",
      );
    });
  });
});