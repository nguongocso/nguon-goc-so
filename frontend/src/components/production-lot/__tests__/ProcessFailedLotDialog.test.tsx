import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ProcessFailedLotDialog } from "../ProcessFailedLotDialog";
import type { ProductionLot } from "@/types/productionLot";

const mockLot: ProductionLot = {
  id: "lot-123",
  organizationName: "HTX Test",
  farmAreaId: "area-1",
  farmAreaName: "Vùng trồng 1",
  productCategoryId: "cat-1",
  productCategoryName: "Chè",
  name: "Lô chè Test",
  expectedQuantity: 100,
  expectedQuantityUnit: "kg",
  actualQuantity: null,
  plantingDate: "2026-01-01",
  harvestDate: "2026-06-01",
  status: "PACKAGED",
  approvalNotes: null,
  createdByName: "Test User",
  approvedByName: "Manager",
  cancellationReason: null,
  cancellationNote: null,
  cancelledByName: null,
  cancelledAt: null,
  disposalReason: null,
  handlingMeasure: null,
  disposalNote: null,
  disposedByName: null,
  disposedAt: null,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-06-01T00:00:00Z",
};

describe("ProcessFailedLotDialog", () => {
  const mockOnClose = vi.fn();
  const mockOnSelectDispose = vi.fn();
  const mockOnSelectReInspection = vi.fn();

  beforeEach(() => {
    mockOnClose.mockClear();
    mockOnSelectDispose.mockClear();
    mockOnSelectReInspection.mockClear();
  });

  it("renders dialog with two options when open", () => {
    render(
      <ProcessFailedLotDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onSelectDispose={mockOnSelectDispose}
        onSelectReInspection={mockOnSelectReInspection}
      />,
    );

    expect(screen.getByText("Xử lý lô không đạt")).toBeInTheDocument();
    expect(screen.getByText("Loại bỏ lô")).toBeInTheDocument();
    expect(screen.getByText("Kiểm nghiệm lại")).toBeInTheDocument();
  });

  it("does not render when lot is null", () => {
    render(
      <ProcessFailedLotDialog
        open={true}
        lot={null}
        onClose={mockOnClose}
        onSelectDispose={mockOnSelectDispose}
        onSelectReInspection={mockOnSelectReInspection}
      />,
    );

    expect(screen.queryByText("Xử lý lô không đạt")).not.toBeInTheDocument();
  });

  it("calls onSelectDispose when dispose option is clicked", () => {
    render(
      <ProcessFailedLotDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onSelectDispose={mockOnSelectDispose}
        onSelectReInspection={mockOnSelectReInspection}
      />,
    );

    const disposeButton = screen.getByText("Loại bỏ lô").closest("button");
    fireEvent.click(disposeButton!);

    expect(mockOnSelectDispose).toHaveBeenCalled();
  });

  it("calls onSelectReInspection when re-inspection option is clicked", () => {
    render(
      <ProcessFailedLotDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onSelectDispose={mockOnSelectDispose}
        onSelectReInspection={mockOnSelectReInspection}
      />,
    );

    const reInspectionButton = screen.getByText("Kiểm nghiệm lại").closest("button");
    fireEvent.click(reInspectionButton!);

    expect(mockOnSelectReInspection).toHaveBeenCalled();
  });

  it("shows warning message about failed inspection", () => {
    render(
      <ProcessFailedLotDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onSelectDispose={mockOnSelectDispose}
        onSelectReInspection={mockOnSelectReInspection}
      />,
    );

    expect(screen.getByText(/Lô này chưa đủ điều kiện để tạo Lô hàng/i)).toBeInTheDocument();
  });
});
