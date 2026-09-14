import "@testing-library/jest-dom/vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { ActivityLogExportDialog } from "../ActivityLogExportDialog";
import * as activityLogApi from "@/api/activityLogApi";
import { toast } from "sonner";

vi.mock("@/api/activityLogApi", () => ({
  previewExportActivityLogs: vi.fn(),
  requestActivityLogExport: vi.fn(),
  getActivityLogApiError: vi.fn((_error, fallback) => Promise.resolve(fallback)),
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe("ActivityLogExportDialog", () => {
  const defaultFilter = {
    action: "UPDATE_PRODUCTION_LOT",
    actorName: "Nguyễn Văn A",
    startDate: "2026-09-01",
    endDate: "2026-09-14",
    objectType: "PRODUCTION_LOT",
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("hiển thị hộp thoại, gọi API preview và hiển thị số lượng bản ghi kèm bộ lọc", async () => {
    vi.mocked(activityLogApi.previewExportActivityLogs).mockResolvedValue({
      count: 25,
      mode: "DIRECT",
    });

    render(
      <ActivityLogExportDialog
        open={true}
        onClose={vi.fn()}
        filter={defaultFilter}
      />
    );

    expect(screen.getByText("Xuất nhật ký hoạt động")).toBeInTheDocument();
    expect(screen.getByText(/Đang tính toán số lượng bản ghi/)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("25 bản ghi")).toBeInTheDocument();
    });

    expect(screen.getByText("Trực tiếp")).toBeInTheDocument();
    expect(screen.getByText(/2026-09-01 đến 2026-09-14/)).toBeInTheDocument();
    expect(screen.getByText("Nguyễn Văn A")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Tải tệp CSV/i })).toBeEnabled();
  });

  it("vô hiệu hóa nút tải tệp khi số lượng bản ghi bằng 0", async () => {
    vi.mocked(activityLogApi.previewExportActivityLogs).mockResolvedValue({
      count: 0,
      mode: "DIRECT",
    });

    render(
      <ActivityLogExportDialog
        open={true}
        onClose={vi.fn()}
        filter={defaultFilter}
      />
    );

    await waitFor(() => {
      expect(screen.getByText("Không có bản ghi phù hợp")).toBeInTheDocument();
    });

    const downloadBtn = screen.getByRole("button", { name: /Tải tệp CSV/i });
    expect(downloadBtn).toBeDisabled();
  });

  it("thực hiện tải tệp CSV thành công và gọi callback onExportSuccess", async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    const onExportSuccess = vi.fn();

    vi.mocked(activityLogApi.previewExportActivityLogs).mockResolvedValue({
      count: 10,
      mode: "DIRECT",
    });
    vi.mocked(activityLogApi.requestActivityLogExport).mockResolvedValue({ mode: "DIRECT" });

    render(
      <ActivityLogExportDialog
        open={true}
        onClose={onClose}
        filter={defaultFilter}
        onExportSuccess={onExportSuccess}
      />
    );

    await waitFor(() => {
      expect(screen.getByText("10 bản ghi")).toBeInTheDocument();
    });

    const downloadBtn = screen.getByRole("button", { name: /Tải tệp CSV/i });
    await user.click(downloadBtn);

    await waitFor(() => {
      expect(activityLogApi.requestActivityLogExport).toHaveBeenCalledWith(defaultFilter);
      expect(toast.success).toHaveBeenCalledWith("Xuất nhật ký hoạt động thành công.");
      expect(onExportSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it("hiển thị thông báo lỗi khi API preview thất bại", async () => {
    vi.mocked(activityLogApi.previewExportActivityLogs).mockRejectedValue({
      response: { data: { message: "Ngày bắt đầu không được sau ngày kết thúc." } },
    });
    vi.mocked(activityLogApi.getActivityLogApiError).mockResolvedValue(
      "Ngày bắt đầu không được sau ngày kết thúc.",
    );

    render(
      <ActivityLogExportDialog
        open={true}
        onClose={vi.fn()}
        filter={defaultFilter}
      />
    );

    await waitFor(() => {
      expect(screen.getByText("Lỗi xem trước số lượng")).toBeInTheDocument();
      expect(screen.getByText("Ngày bắt đầu không được sau ngày kết thúc.")).toBeInTheDocument();
    });

    const downloadBtn = screen.getByRole("button", { name: /Tải tệp CSV/i });
    expect(downloadBtn).toBeDisabled();
  });

  it("cho phép tạo job nền khi preview trả về chế độ ASYNC", async () => {
    const user = userEvent.setup();
    vi.mocked(activityLogApi.previewExportActivityLogs).mockResolvedValue({
      count: 15000,
      mode: "ASYNC",
    });
    vi.mocked(activityLogApi.requestActivityLogExport).mockResolvedValue({
      mode: "ASYNC",
      job: {
        exportId: "db5164c7-b7bd-4a0c-8ee4-3eb9d7f16e5d",
        mode: "ASYNC",
        status: "IN_PROGRESS",
        recordCount: 15000,
        createdAt: "2026-09-14T10:30:00",
      },
    });

    render(
      <ActivityLogExportDialog
        open={true}
        onClose={vi.fn()}
        filter={defaultFilter}
      />
    );

    await waitFor(() => {
      expect(screen.getByText("Sẽ xử lý trong nền")).toBeInTheDocument();
      expect(screen.getByText(/Hệ thống sẽ tạo snapshot/)).toBeInTheDocument();
    });

    const requestButton = screen.getByRole("button", { name: /Tạo yêu cầu xuất nền/i });
    expect(requestButton).toBeEnabled();
    await user.click(requestButton);

    await waitFor(() => {
      expect(activityLogApi.requestActivityLogExport).toHaveBeenCalledWith(defaultFilter);
      expect(toast.success).toHaveBeenCalledWith(
        "Đã tạo yêu cầu xuất nền. Hệ thống sẽ thông báo khi tệp sẵn sàng.",
      );
    });
  });
});
