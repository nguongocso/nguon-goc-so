import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import ActivityLogPage from "../ActivityLogPage";
import * as activityLogApi from "@/api/activityLogApi";
import { toast } from "sonner";

vi.mock("@/api/activityLogApi", () => ({
  getActivityLogs: vi.fn(),
  previewExportActivityLogs: vi.fn(),
  requestActivityLogExport: vi.fn(),
  getActivityLogExportJob: vi.fn(),
  downloadActivityLogExportJob: vi.fn(),
  getActivityLogApiError: vi.fn((_error, fallback) => Promise.resolve(fallback)),
}));

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}));

vi.mock("@/components/help/HelpButton", () => ({
  HelpButton: () => <button data-testid="help-button">Trợ giúp</button>,
}));

const mockLogs = [
  {
    id: "log-1",
    action: "UPDATE_PRODUCTION_LOT",
    username: "manager_a",
    fullName: "Nguyễn Văn An",
    actorName: "Nguyễn Văn An",
    entityType: "PRODUCTION_LOT",
    targetType: "PRODUCTION_LOT",
    entityId: "LOT-001",
    targetId: "LOT-001",
    description: "Cập nhật thông tin lô",
    createdAt: "2026-09-10T08:30:00",
  },
  {
    id: "log-2",
    action: "CREATE_PRODUCTION_LOT",
    username: "manager_a",
    fullName: "Nguyễn Văn An",
    actorName: "Nguyễn Văn An",
    entityType: "PRODUCTION_LOT",
    targetType: "PRODUCTION_LOT",
    entityId: "LOT-002",
    description: "Tạo mới lô sản xuất",
    createdAt: "2026-09-11T09:00:00",
  },
];

describe("ActivityLogPage E2E / Page Integration Tests", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(activityLogApi.getActivityLogs).mockResolvedValue({
      items: mockLogs,
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    vi.mocked(activityLogApi.previewExportActivityLogs).mockResolvedValue({
      count: 2,
      mode: "DIRECT",
    });

    vi.mocked(activityLogApi.requestActivityLogExport).mockResolvedValue({ mode: "DIRECT" });

    // Mock URL.createObjectURL & revokeObjectURL
    window.URL.createObjectURL = vi.fn(() => "blob:http://localhost/mock-url");
    window.URL.revokeObjectURL = vi.fn();
  });

  it("hiển thị danh sách nhật ký và mở dialog xuất nhật ký khi bấm nút 'Xuất nhật ký'", async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <ActivityLogPage />
      </MemoryRouter>
    );

    // Kiểm tra trang tải dữ liệu ban đầu
    expect(await screen.findByText("Lịch sử hoạt động")).toBeInTheDocument();
    expect(await screen.findByText("Cập nhật thông tin lô")).toBeInTheDocument();
    expect(screen.getByText("Tạo mới lô sản xuất")).toBeInTheDocument();

    // Bấm nút "Xuất nhật ký"
    const exportBtn = screen.getByRole("button", { name: /Xuất nhật ký/i });
    await user.click(exportBtn);

    // Kiểm tra dialog xuất hiện và gọi API preview
    expect(await screen.findByText("Xuất nhật ký hoạt động")).toBeInTheDocument();
    expect(await screen.findByText("2 bản ghi")).toBeInTheDocument();

    // Bấm "Xuất tệp CSV"
    const downloadBtn = screen.getByRole("button", { name: /Xuất tệp CSV/i });
    await user.click(downloadBtn);

    await waitFor(() => {
      expect(activityLogApi.requestActivityLogExport).toHaveBeenCalledTimes(1);
      expect(toast.success).toHaveBeenCalledWith("Xuất nhật ký hoạt động thành công.");
    });
  });

  it("giữ nguyên toàn bộ bộ lọc URL khi hiển thị và bấm nút 'Làm mới'", async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={[
        "/activity-logs?action=UPDATE_PRODUCTION_LOT&actorName=manager_a&startDate=2026-09-01&endDate=2026-09-14&objectType=PRODUCTION_LOT",
      ]}>
        <ActivityLogPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("Lịch sử hoạt động")).toBeInTheDocument();
    expect(activityLogApi.getActivityLogs).toHaveBeenCalledTimes(1);
    expect(screen.getByLabelText("Người thực hiện")).toHaveValue("manager_a");
    expect(screen.getByLabelText("Từ ngày")).toHaveValue("2026-09-01");

    // Bấm nút "Làm mới"
    const refreshBtn = screen.getByRole("button", { name: /Làm mới/i });
    await user.click(refreshBtn);

    await waitFor(() => {
      expect(activityLogApi.getActivityLogs).toHaveBeenCalledTimes(2);
      expect(activityLogApi.getActivityLogs).toHaveBeenLastCalledWith(expect.objectContaining({
        action: "UPDATE_PRODUCTION_LOT",
        actorName: "manager_a",
        startDate: "2026-09-01",
        endDate: "2026-09-14",
        objectType: "PRODUCTION_LOT",
      }));
    });
  });

  it("hiển thị chế độ nền và cho phép tạo export job khi dữ liệu lớn", async () => {
    vi.mocked(activityLogApi.previewExportActivityLogs).mockResolvedValue({
      count: 12500,
      mode: "ASYNC",
    });

    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <ActivityLogPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("Lịch sử hoạt động")).toBeInTheDocument();

    const exportBtn = screen.getByRole("button", { name: /Xuất nhật ký/i });
    await user.click(exportBtn);

    expect(await screen.findByText("Sẽ xử lý trong nền")).toBeInTheDocument();
    expect(screen.getByText(/Hệ thống sẽ tạo snapshot/)).toBeInTheDocument();

    const requestButton = screen.getByRole("button", { name: /Tạo yêu cầu xuất nền/i });
    expect(requestButton).toBeEnabled();
  });
});
