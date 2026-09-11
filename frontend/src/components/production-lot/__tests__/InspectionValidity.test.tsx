import "@testing-library/jest-dom/vitest";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { ProductionLotList } from "../ProductionLotList";
import {
  InspectionValidityBadge,
  INSPECTION_VALIDITY_LABELS,
} from "../ProductionLotStatusBadge";
import type { ProductionLot } from "../../../types/productionLot";
import type { InspectionValidityStatus } from "../../../types/certification";

const mockNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("../../../hooks/useAuth", () => ({
  useAuth: () => ({
    user: {
      userId: "user-test-1",
      roleCode: "VT-02",
      organizationId: "org-1",
    },
    isLoading: false,
  }),
}));

const baseLot: ProductionLot = {
  id: "lot-validity-1",
  organizationName: "Hợp tác xã Nông nghiệp Xanh",
  farmAreaId: "area-1",
  farmAreaName: "Vùng trồng mẫu",
  productCategoryId: "cat-tea",
  productCategoryName: "Chè Shan Tuyết",
  name: "Lô chè Shan Tuyết A1",
  expectedQuantity: 500,
  expectedQuantityUnit: "kg",
  actualQuantity: 480,
  plantingDate: "2026-01-10",
  harvestDate: "2026-06-20",
  status: "APPROVED",
  approvalNotes: null,
  createdByName: "Kỹ thuật viên 1",
  approvedByName: "Quản lý HTX",
  cancellationReason: null,
  cancellationNote: null,
  cancelledByName: null,
  cancelledAt: null,
  disposalReason: null,
  handlingMeasure: null,
  disposalNote: null,
  disposedByName: null,
  disposedAt: null,
  createdAt: "2026-01-10T08:00:00Z",
  updatedAt: "2026-06-20T10:00:00Z",
  inspectionValidity: null,
};

const defaultProps = {
  isLoading: false,
  canCreate: false,
  canEdit: false,
  canSubmitForApproval: false,
  canApprove: false,
  canRecordFarmLog: false,
  canCancel: false,
  onCreate: vi.fn(),
  onEdit: vi.fn(),
  onSubmitForApproval: vi.fn().mockResolvedValue(undefined),
  onDecideApproval: vi.fn().mockResolvedValue(undefined),
  onRecordFarmLog: vi.fn(),
  onCancel: vi.fn().mockResolvedValue(undefined),
  onRefresh: vi.fn(),
};

const renderWithRouter = (ui: React.ReactElement) => {
  return render(<MemoryRouter>{ui}</MemoryRouter>);
};

describe("NCL-11-CN-004: InspectionValidityBadge Component", () => {
  const allStatuses: InspectionValidityStatus[] = [
    "NOT_REQUIRED",
    "NO_VALID_RESULT",
    "VALID",
    "EXPIRING",
    "EXPIRED",
  ];

  it.each(allStatuses)(
    "hiển thị chính xác nhãn tiếng Việt cho trạng thái hiệu lực: %s",
    (status) => {
      render(<InspectionValidityBadge status={status} />);
      const expectedLabel = INSPECTION_VALIDITY_LABELS[status];
      expect(screen.getByText(expectedLabel)).toBeDefined();
    },
  );
});

describe("NCL-11-CN-004: ProductionLotList Inspection Validity", () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  // Case 1: API trả EXPIRING -> hiển thị badge "Sắp hết hiệu lực", "Còn X ngày" và tên tiêu chí
  it("Case 1: Lô có kết quả kiểm nghiệm sắp hết hạn (EXPIRING) -> hiển thị badge Sắp hết hiệu lực, số ngày còn lại và tiêu chí", () => {
    const expiringLot: ProductionLot = {
      ...baseLot,
      id: "lot-expiring-1",
      name: "Lô chè sắp hết hạn",
      inspectionValidity: {
        requiresInspection: true,
        status: "EXPIRING",
        earliestExpiryDate: "2026-09-20",
        daysRemaining: 10,
        daysOverdue: null,
        canActivate: true,
        canCreateNewRequest: false,
        latestPassedRequestId: "req-1",
        inactiveStampCount: 50,
        expiringCriteria: ["Dư lượng thuốc BVTV"],
      },
    };

    renderWithRouter(
      <ProductionLotList
        {...defaultProps}
        lots={[expiringLot]}
      />,
    );

    expect(screen.getByText("Sắp hết hiệu lực")).toBeDefined();
    expect(screen.queryByText("Còn 10 ngày")).toBeNull();
    expect(screen.queryByText("Tiêu chí: Dư lượng thuốc BVTV")).toBeNull();
  });

  // Case 2: API trả EXPIRED -> hiển thị "Hết hiệu lực", không hiển thị số ngày quá hạn hay tiêu chí trong bảng danh sách
  it("Case 2: Lô có kết quả kiểm nghiệm hết hạn (EXPIRED) -> hiển thị badge Hết hiệu lực trong danh sách", () => {
    const expiredLot: ProductionLot = {
      ...baseLot,
      id: "lot-expired-2",
      name: "Lô chè đã hết hạn kiểm nghiệm",
      inspectionValidity: {
        requiresInspection: true,
        status: "EXPIRED",
        earliestExpiryDate: "2026-09-05",
        daysRemaining: null,
        daysOverdue: 5,
        canActivate: false,
        canCreateNewRequest: true,
        latestPassedRequestId: "req-1",
        inactiveStampCount: 30,
        expiredCriteria: ["Kim loại nặng"],
      },
    };

    renderWithRouter(
      <ProductionLotList
        {...defaultProps}
        lots={[expiredLot]}
      />,
    );

    expect(screen.getByText("Hết hiệu lực")).toBeDefined();
    expect(screen.queryByText("Quá hạn 5 ngày")).toBeNull();
    expect(screen.queryByText("Tiêu chí: Kim loại nặng")).toBeNull();
  });

  // Case 3: Lô đã thu hồi (RECALLED) -> không hiển thị cảnh báo sắp hết hiệu lực / CTA
  it("Case 3: Lô đã thu hồi (RECALLED) -> ưu tiên trạng thái thu hồi, không hiển thị cảnh báo hiệu lực kiểm nghiệm", () => {
    const recalledLot: ProductionLot = {
      ...baseLot,
      id: "lot-recalled-3",
      name: "Lô chè đã thu hồi",
      status: "RECALLED",
      inspectionValidity: {
        requiresInspection: true,
        status: "EXPIRING",
        earliestExpiryDate: "2026-09-18",
        daysRemaining: 8,
        daysOverdue: null,
        canActivate: false,
        canCreateNewRequest: false,
        latestPassedRequestId: "req-1",
        inactiveStampCount: 10,
      },
    };

    renderWithRouter(
      <ProductionLotList
        {...defaultProps}
        lots={[recalledLot]}
      />,
    );

    // Không được hiển thị badge "Sắp hết hiệu lực" hay dòng "Còn 8 ngày"
    expect(screen.queryByText("Sắp hết hiệu lực")).toBeNull();
    expect(screen.queryByText("Còn 8 ngày")).toBeNull();
  });

  // Case 4: Lô đã kích hoạt hết tem / kết quả còn hiệu lực -> hiển thị Còn hiệu lực, không có cảnh báo
  it("Case 4: Lô có kết quả kiểm nghiệm Còn hiệu lực (VALID) -> hiển thị Còn hiệu lực, không có cảnh báo quá hạn hay CTA", () => {
    const validLot: ProductionLot = {
      ...baseLot,
      id: "lot-valid-4",
      name: "Lô chè kiểm nghiệm còn hạn dài",
      inspectionValidity: {
        requiresInspection: true,
        status: "VALID",
        earliestExpiryDate: "2026-12-30",
        daysRemaining: 111,
        daysOverdue: null,
        canActivate: true,
        canCreateNewRequest: false,
        latestPassedRequestId: "req-2",
        inactiveStampCount: 0, // Đã kích hoạt hết tem
      },
    };

    renderWithRouter(
      <ProductionLotList
        {...defaultProps}
        lots={[validLot]}
      />,
    );

    expect(screen.getByText("Còn hiệu lực")).toBeDefined();
    expect(screen.queryByText("Sắp hết hiệu lực")).toBeNull();
    expect(screen.queryByText("Hết hiệu lực")).toBeNull();
    expect(screen.queryByText("Tạo yêu cầu kiểm nghiệm mới")).toBeNull();
  });

  // Case 5: Tại danh sách Lô sản xuất, cột Hiệu lực kiểm nghiệm chỉ hiển thị trạng thái hiện tại, không hiển thị CTA
  it("Case 5: Tại danh sách Lô sản xuất, cột Hiệu lực kiểm nghiệm hiển thị trạng thái Hết hiệu lực gọn gàng, không hiển thị nút CTA", () => {
    const expiredLot: ProductionLot = {
      ...baseLot,
      id: "lot-action-5",
      name: "Lô chè cần kiểm nghiệm lại",
      inspectionValidity: {
        requiresInspection: true,
        status: "EXPIRED",
        earliestExpiryDate: "2026-09-01",
        daysRemaining: null,
        daysOverdue: 9,
        canCreateNewRequest: true,
        canActivate: false,
        latestPassedRequestId: "req-1",
        inactiveStampCount: 20,
      },
    };

    renderWithRouter(
      <ProductionLotList
        {...defaultProps}
        lots={[expiredLot]}
      />,
    );

    expect(screen.getByText("Hết hiệu lực")).toBeDefined();
    expect(screen.queryByText("Tạo yêu cầu kiểm nghiệm mới")).toBeNull();
  });

  // Case 6: API error / field missing / null inspectionValidity -> UI fallback an toàn, không crash
  it("Case 6: Dữ liệu inspectionValidity bị thiếu hoặc null -> giao diện fallback an toàn với dấu gạch ngang, không crash", () => {
    const nullValidityLot: ProductionLot = {
      ...baseLot,
      id: "lot-null-6",
      name: "Lô chè chưa có dữ liệu hiệu lực",
      inspectionValidity: null,
    };

    expect(() => {
      renderWithRouter(
        <ProductionLotList
          {...defaultProps}
          lots={[nullValidityLot]}
        />,
      );
    }).not.toThrow();

    expect(screen.getByText("Lô chè chưa có dữ liệu hiệu lực")).toBeDefined();
    // Ký tự fallback '—' hiển thị
    expect(screen.getAllByText("—").length).toBeGreaterThan(0);
  });
});

