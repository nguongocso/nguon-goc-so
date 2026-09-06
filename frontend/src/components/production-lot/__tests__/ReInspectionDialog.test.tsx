import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ReInspectionDialog } from "../ReInspectionDialog";
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

describe("ReInspectionDialog", () => {
  const mockOnClose = vi.fn();
  const mockOnNavigate = vi.fn();

  beforeEach(() => {
    mockOnClose.mockClear();
    mockOnNavigate.mockClear();
  });

  it("renders dialog with lot information when open", () => {
    render(
      <ReInspectionDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onNavigateToCreateInspection={mockOnNavigate}
      />,
    );

    expect(screen.getByText("Kiểm nghiệm lại")).toBeInTheDocument();
    // Lot name appears in description and info box
    expect(screen.getAllByText("Lô chè Test").length).toBeGreaterThan(0);
  });

  it("does not render when lot is null", () => {
    render(
      <ReInspectionDialog
        open={true}
        lot={null}
        onClose={mockOnClose}
        onNavigateToCreateInspection={mockOnNavigate}
      />,
    );

    expect(screen.queryByText("Kiểm nghiệm lại")).not.toBeInTheDocument();
  });

  it("shows validation error when submitting without corrective action (TC-03)", async () => {
    render(
      <ReInspectionDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onNavigateToCreateInspection={mockOnNavigate}
      />,
    );

    // Submit without filling corrective action
    const submitButton = screen.getByRole("button", { name: /Tạo yêu cầu kiểm nghiệm lại/i });
    fireEvent.click(submitButton);

    // Should show validation error
    expect(await screen.findByText("Vui lòng nhập biện pháp khắc phục")).toBeInTheDocument();
    expect(mockOnNavigate).not.toHaveBeenCalled();
  });

  it("calls onNavigateToCreateInspection when form is valid", async () => {
    render(
      <ReInspectionDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onNavigateToCreateInspection={mockOnNavigate}
      />,
    );

    // Fill corrective action
    const actionInput = screen.getByLabelText(/Biện pháp khắc phục/i);
    fireEvent.change(actionInput, {
      target: { value: "Đã thay đổi phân bón" },
    });

    // Submit
    const submitButton = screen.getByRole("button", { name: /Tạo yêu cầu kiểm nghiệm lại/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockOnNavigate).toHaveBeenCalledWith("lot-123");
    });
  });

  it("calls onClose after successful submission", async () => {
    render(
      <ReInspectionDialog
        open={true}
        lot={mockLot}
        onClose={mockOnClose}
        onNavigateToCreateInspection={mockOnNavigate}
      />,
    );

    // Fill corrective action
    const actionInput = screen.getByLabelText(/Biện pháp khắc phục/i);
    fireEvent.change(actionInput, {
      target: { value: "Đã thay đổi phân bón" },
    });

    // Submit
    const submitButton = screen.getByRole("button", { name: /Tạo yêu cầu kiểm nghiệm lại/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockOnClose).toHaveBeenCalled();
    });
  });
});
