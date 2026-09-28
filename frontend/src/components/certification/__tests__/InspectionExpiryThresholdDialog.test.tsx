import "@testing-library/jest-dom/vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { InspectionExpiryThresholdDialog } from "../InspectionExpiryThresholdDialog";
import * as inspectionCriterionApi from "../../../api/inspectionCriterionApi";

vi.mock("../../../api/inspectionCriterionApi", () => ({
  getInspectionExpiryThreshold: vi.fn(),
  updateInspectionExpiryThreshold: vi.fn(),
}));

describe("InspectionExpiryThresholdDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("hiển thị dialog và tải dữ liệu cấu hình ban đầu", async () => {
    vi.mocked(inspectionCriterionApi.getInspectionExpiryThreshold).mockResolvedValue({
      warningThresholdDays: 15,
      updatedAt: "2026-09-11T10:00:00",
      updatedByName: "Quản trị viên hệ thống",
    });

    render(
      <InspectionExpiryThresholdDialog
        open={true}
        onClose={vi.fn()}
      />
    );

    expect(screen.getByText("Cấu hình ngưỡng cảnh báo hết hiệu lực")).toBeInTheDocument();

    await waitFor(() => {
      const input = screen.getByLabelText(/Số ngày cảnh báo trước khi hết hạn/) as HTMLInputElement;
      expect(input.value).toBe("15");
    });

    expect(screen.getByText(/Quản trị viên hệ thống/)).toBeInTheDocument();
  });

  it("báo lỗi client-side khi nhập số ngày ngoài khoảng 1-365", async () => {
    const user = userEvent.setup();
    vi.mocked(inspectionCriterionApi.getInspectionExpiryThreshold).mockResolvedValue({
      warningThresholdDays: 15,
      updatedAt: null,
      updatedByName: null,
    });

    render(
      <InspectionExpiryThresholdDialog
        open={true}
        onClose={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/Số ngày cảnh báo trước khi hết hạn/)).toBeInTheDocument();
    });

    const input = screen.getByLabelText(/Số ngày cảnh báo trước khi hết hạn/);
    await user.clear(input);
    await user.type(input, "400");
    await user.click(screen.getByRole("button", { name: "Lưu cấu hình" }));

    expect(screen.getByText("Ngưỡng cảnh báo phải là số nguyên từ 1 đến 365 ngày.")).toBeInTheDocument();
    expect(inspectionCriterionApi.updateInspectionExpiryThreshold).not.toHaveBeenCalled();
  });

  it("gọi API cập nhật và callback onSuccess/onClose khi nhập hợp lệ", async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    const onSuccess = vi.fn();

    vi.mocked(inspectionCriterionApi.getInspectionExpiryThreshold).mockResolvedValue({
      warningThresholdDays: 15,
      updatedAt: null,
      updatedByName: null,
    });
    vi.mocked(inspectionCriterionApi.updateInspectionExpiryThreshold).mockResolvedValue({
      warningThresholdDays: 30,
      updatedAt: "2026-09-11T12:00:00",
      updatedByName: "Admin",
    });

    render(
      <InspectionExpiryThresholdDialog
        open={true}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/Số ngày cảnh báo trước khi hết hạn/)).toBeInTheDocument();
    });

    const input = screen.getByLabelText(/Số ngày cảnh báo trước khi hết hạn/);
    await user.clear(input);
    await user.type(input, "30");
    await user.click(screen.getByRole("button", { name: "Lưu cấu hình" }));

    await waitFor(() => {
      expect(inspectionCriterionApi.updateInspectionExpiryThreshold).toHaveBeenCalledWith({
        warningThresholdDays: 30,
      });
      expect(onSuccess).toHaveBeenCalledWith(30);
      expect(onClose).toHaveBeenCalled();
    });
  });

  it("hiển thị thông báo lỗi khi API cập nhật trả về lỗi", async () => {
    const user = userEvent.setup();
    vi.mocked(inspectionCriterionApi.getInspectionExpiryThreshold).mockResolvedValue({
      warningThresholdDays: 15,
      updatedAt: null,
      updatedByName: null,
    });
    vi.mocked(inspectionCriterionApi.updateInspectionExpiryThreshold).mockRejectedValue({
      response: {
        data: {
          message: "Số ngày cảnh báo không hợp lệ",
        },
      },
    });

    render(
      <InspectionExpiryThresholdDialog
        open={true}
        onClose={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/Số ngày cảnh báo trước khi hết hạn/)).toBeInTheDocument();
    });

    const input = screen.getByLabelText(/Số ngày cảnh báo trước khi hết hạn/);
    await user.clear(input);
    await user.type(input, "20");
    await user.click(screen.getByRole("button", { name: "Lưu cấu hình" }));

    await waitFor(() => {
      expect(screen.getByText("Số ngày cảnh báo không hợp lệ")).toBeInTheDocument();
    });
  });
});
