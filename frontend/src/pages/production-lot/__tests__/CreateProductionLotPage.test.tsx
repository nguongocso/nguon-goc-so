import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import "@testing-library/jest-dom/vitest";
import CreateProductionLotPage from "../CreateProductionLotPage";
import * as productionLotApi from "@/api/productionLotApi";

vi.mock("@/api/productionLotApi", () => ({
  getFarmAreaOptions: vi.fn(),
  getProductCategoryOptions: vi.fn(),
  getProductionLots: vi.fn(),
  getCloneProductionLotPreview: vi.fn(),
  cloneProductionLot: vi.fn(),
  createProductionLot: vi.fn(),
}));

const farmAreaOptions = [
  { id: "area-1", name: "Vùng trồng số 1", area: 2 },
  { id: "area-2", name: "Vùng trồng số 2", area: 3 },
];
const productCategoryOptions = [
  { id: "cat-1", name: "Lúa" },
  { id: "cat-2", name: "Chè" },
];
const lots = [
  {
    id: "lot-1",
    name: "Lô lúa vụ hè 2025",
    farmAreaId: "area-1",
    farmAreaName: "Vùng trồng số 1",
    productCategoryId: "cat-1",
    productCategoryName: "Lúa",
    expectedQuantity: 1000,
    expectedQuantityUnit: "kg",
    actualQuantity: 950,
    plantingDate: "2025-05-01",
    harvestDate: null,
    status: "APPROVED",
    startDate: null,
    endDate: null,
    createdAt: "2025-01-01T00:00:00",
    updatedAt: "2025-01-01T00:00:00",
  },
];

const preview = {
  sourceLotId: "lot-1",
  sourceLotName: "Lô lúa vụ hè 2025",
  farmAreaId: "area-1",
  farmAreaName: "Vùng trồng số 1",
  productCategoryId: "cat-1",
  productCategoryName: "Lúa",
  name: "Lô lúa vụ hè 2025",
  expectedQuantity: 1000,
  expectedQuantityUnit: "kg",
  plantingDate: "2025-05-01",
  activeCertifications: [
    { id: "cert-1", name: "VietGAP", code: "VG-001", expiryDate: "2027-01-01" },
  ],
  skippedCertifications: [],
  warnings: [],
};

const renderPage = () =>
  render(
    <MemoryRouter>
      <CreateProductionLotPage />
    </MemoryRouter>,
  );

describe("NCL-02-CN-007 - CreateProductionLotPage gộp tạo mới + tạo từ mẫu", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(productionLotApi.getFarmAreaOptions).mockResolvedValue(
      farmAreaOptions,
    );
    vi.mocked(productionLotApi.getProductCategoryOptions).mockResolvedValue(
      productCategoryOptions,
    );
    vi.mocked(productionLotApi.getProductionLots).mockResolvedValue(lots);
  });

  it("hiển thị dropdown sao chép từ vụ trước và form tạo mới khi không chọn lô mẫu", async () => {
    renderPage();

    const sourceSelect = await screen.findByLabelText(
      /Sao chép từ lô vụ trước/i,
    ) as HTMLSelectElement;
    expect(sourceSelect).toBeInTheDocument();

    await waitFor(() => {
      expect(
        screen.getByLabelText(/Tên lô sản xuất/i),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "Tạo lô sản xuất" }),
      ).toBeInTheDocument();
    });

    expect(sourceSelect.value).toBe("");
  });

  it("chọn lô mẫu gọi clone-preview, prefill form và khóa vùng trồng + loại nông sản", async () => {
    vi.mocked(productionLotApi.getCloneProductionLotPreview).mockResolvedValue(
      preview,
    );
    renderPage();

    const sourceSelect = await screen.findByLabelText(
      /Sao chép từ lô vụ trước/i,
    );
    fireEvent.change(sourceSelect, { target: { value: "lot-1" } });

    await waitFor(() => {
      expect(
        productionLotApi.getCloneProductionLotPreview,
      ).toHaveBeenCalledWith("lot-1");
    });

    expect(
      await screen.findByText(/Dữ liệu kế thừa từ lô/i),
    ).toBeInTheDocument();
    expect(
      screen.getByText("VietGAP (VG-001)"),
    ).toBeInTheDocument();

    expect(
      screen.getByDisplayValue("Lô lúa vụ hè 2025"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Vùng trồng/i)).toBeDisabled();
    expect(screen.getByLabelText(/Loại nông sản/i)).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Tạo lô từ mẫu" }),
    ).toBeInTheDocument();
  });

  it("submit khi đã chọn lô mẫu gọi cloneProductionLot với dữ liệu vụ mới", async () => {
    vi.mocked(productionLotApi.getCloneProductionLotPreview).mockResolvedValue(
      preview,
    );
    vi.mocked(productionLotApi.cloneProductionLot).mockResolvedValue({
      lot: {
        id: "new-lot-1",
        farmAreaId: "area-1",
        productCategoryId: "cat-1",
        organizationName: "DEMO_HTX",
        farmAreaName: "Vùng trồng số 1",
        productCategoryName: "Lúa",
        name: "Lô lúa vụ đông xuân 2026",
        expectedQuantity: 1200,
        expectedQuantityUnit: "kg",
        actualQuantity: null,
        plantingDate: "2026-01-10",
        harvestDate: null,
        status: "DRAFT",
        approvalNotes: null,
        createdByName: "Quản lý HTX Demo",
        approvedByName: null,
        createdAt: "2026-01-01T00:00:00",
        updatedAt: "2026-01-01T00:00:00",
      },
      copiedCertifications: [
        {
          id: "cert-1",
          name: "VietGAP",
          code: "VG-001",
          expiryDate: "2027-01-01",
        },
      ],
      skippedCertifications: [],
      warnings: [],
    });

    renderPage();

    const sourceSelect = await screen.findByLabelText(
      /Sao chép từ lô vụ trước/i,
    );
    fireEvent.change(sourceSelect, { target: { value: "lot-1" } });

    // Chờ form được remount + prefill từ lô mẫu trước khi chỉnh sửa.
    await screen.findByDisplayValue("Lô lúa vụ hè 2025");

    fireEvent.change(
      screen.getByLabelText(/Tên lô sản xuất/i),
      { target: { value: "Lô lúa vụ đông xuân 2026" } },
    );
    fireEvent.change(screen.getByLabelText(/Sản lượng dự kiến/i), {
      target: { value: "1200" },
    });

    fireEvent.click(screen.getByRole("button", { name: "Tạo lô từ mẫu" }));

    await waitFor(() => {
      expect(productionLotApi.cloneProductionLot).toHaveBeenCalledWith(
        "lot-1",
        expect.objectContaining({
          name: "Lô lúa vụ đông xuân 2026",
          expectedQuantity: 1200,
          expectedQuantityUnit: "kg",
          plantingDate: "2025-05-01",
        }),
      );
    });
  });

  it("bỏ chọn lô mẫu giữ lại giá trị đã prefill nhưng mở khóa form (về tạo lô thường)", async () => {
    vi.mocked(productionLotApi.getCloneProductionLotPreview).mockResolvedValue(
      preview,
    );
    renderPage();

    const sourceSelect = await screen.findByLabelText(
      /Sao chép từ lô vụ trước/i,
    );
    fireEvent.change(sourceSelect, { target: { value: "lot-1" } });

    await screen.findByText(/Dữ liệu kế thừa từ lô/i);

    fireEvent.change(sourceSelect, { target: { value: "" } });

    await waitFor(() => {
      expect(screen.queryByText(/Dữ liệu kế thừa từ lô/i)).not.toBeInTheDocument();
      expect(screen.getByLabelText(/Vùng trồng/i)).toBeEnabled();
    });

    expect(
      screen.getByDisplayValue("Lô lúa vụ hè 2025"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Tạo lô sản xuất" }),
    ).toBeInTheDocument();
  });
});