import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { DisposeLotDialog } from "../DisposeLotDialog";
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

describe("DisposeLotDialog", () => {
  const mockOnClose = vi.fn();
  const mockOnDispose = vi.fn();

  beforeEach(() => {
    mockOnClose.mockClear();
    mockOnDispose.mockClear();
  });

  it("renders dialog with lot information when open", () => {
    render(
      <DisposeLotDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onDispose={mockOnDispose}
      />,
    );

    expect(screen.getByText("Loại bỏ lô sản xuất")).toBeInTheDocument();
    // Lot name appears in description and info box
    expect(screen.getAllByText("Lô chè Test").length).toBeGreaterThan(0);
  });

  it("does not render when lot is null", () => {
    render(
      <DisposeLotDialog
        open={true}
        lot={null}
        onClose={mockOnClose}
        onDispose={mockOnDispose}
      />,
    );

    expect(screen.queryByText("Loại bỏ lô sản xuất")).not.toBeInTheDocument();
  });

  it("shows validation error when submitting without reason (TC-03)", async () => {
    mockOnDispose.mockResolvedValue(undefined);

    render(
      <DisposeLotDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onDispose={mockOnDispose}
      />,
    );

    // Fill only handling measure, leave reason empty
    const handlingMeasureInput = screen.getByLabelText(/Biện pháp xử lý/i);
    fireEvent.change(handlingMeasureInput, {
      target: { value: "Tiêu hủy lô" },
    });

    // Submit
    const submitButton = screen.getByRole("button", { name: /Xác nhận loại bỏ/i });
    fireEvent.click(submitButton);

    // Should show validation error
    expect(await screen.findByText("Vui lòng nhập lý do loại bỏ")).toBeInTheDocument();
    expect(mockOnDispose).not.toHaveBeenCalled();
  });

  it("shows validation error when submitting without handling measure (TC-03)", async () => {
    mockOnDispose.mockResolvedValue(undefined);

    render(
      <DisposeLotDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onDispose={mockOnDispose}
      />,
    );

    // Fill only reason, leave handling measure empty
    const reasonInput = screen.getByLabelText(/Lý do loại bỏ/i);
    fireEvent.change(reasonInput, {
      target: { value: "Nông sản không đạt chuẩn" },
    });

    // Submit
    const submitButton = screen.getByRole("button", { name: /Xác nhận loại bỏ/i });
    fireEvent.click(submitButton);

    // Should show validation error
    expect(await screen.findByText("Vui lòng nhập biện pháp xử lý")).toBeInTheDocument();
    expect(mockOnDispose).not.toHaveBeenCalled();
  });

  it("calls onDispose with correct payload when form is valid", async () => {
    mockOnDispose.mockResolvedValue(undefined);

    render(
      <DisposeLotDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onDispose={mockOnDispose}
      />,
    );

    // Fill form
    const reasonInput = screen.getByLabelText(/Lý do loại bỏ/i);
    fireEvent.change(reasonInput, {
      target: { value: "Nông sản không đạt chuẩn" },
    });

    const handlingMeasureInput = screen.getByLabelText(/Biện pháp xử lý/i);
    fireEvent.change(handlingMeasureInput, {
      target: { value: "Tiêu hủy lô" },
    });

    // Submit
    const submitButton = screen.getByRole("button", { name: /Xác nhận loại bỏ/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockOnDispose).toHaveBeenCalledWith("lot-123", {
        reason: "Nông sản không đạt chuẩn",
        handlingMeasure: "Tiêu hủy lô",
        note: undefined,
      });
    });
  });

  it("calls onClose after successful dispose", async () => {
    mockOnDispose.mockResolvedValue(undefined);

    render(
      <DisposeLotDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onDispose={mockOnDispose}
      />,
    );

    // Fill form
    const reasonInput = screen.getByLabelText(/Lý do loại bỏ/i);
    fireEvent.change(reasonInput, {
      target: { value: "Nông sản không đạt chuẩn" },
    });

    const handlingMeasureInput = screen.getByLabelText(/Biện pháp xử lý/i);
    fireEvent.change(handlingMeasureInput, {
      target: { value: "Tiêu hủy lô" },
    });

    // Submit
    const submitButton = screen.getByRole("button", { name: /Xác nhận loại bỏ/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockOnClose).toHaveBeenCalled();
    });
  });

  it("does not call onClose when dispose fails", async () => {
    // The error propagates from handleConfirm (try/finally without catch)
    // We need to handle this expected unhandled rejection
    const rejectionHandler = vi.fn();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any, no-undef
    (globalThis as any).process?.on("unhandledRejection", rejectionHandler);

    mockOnDispose.mockRejectedValue(new Error("API Error"));

    render(
      <DisposeLotDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onDispose={mockOnDispose}
      />,
    );

    // Fill form
    const reasonInput = screen.getByLabelText(/Lý do loại bỏ/i);
    fireEvent.change(reasonInput, {
      target: { value: "Nông sản không đạt chuẩn" },
    });

    const handlingMeasureInput = screen.getByLabelText(/Biện pháp xử lý/i);
    fireEvent.change(handlingMeasureInput, {
      target: { value: "Tiêu hủy lô" },
    });

    // Submit
    const submitButton = screen.getByRole("button", { name: /Xác nhận loại bỏ/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockOnDispose).toHaveBeenCalled();
    });
    expect(mockOnClose).not.toHaveBeenCalled();

    // Clean up
    // eslint-disable-next-line @typescript-eslint/no-explicit-any, no-undef
    (globalThis as any).process?.off("unhandledRejection", rejectionHandler);
  });
});
