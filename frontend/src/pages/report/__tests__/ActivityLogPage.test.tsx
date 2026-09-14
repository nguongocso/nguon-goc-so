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
  downloadActivityLogsCsv: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
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

    vi.mocked(activityLogApi.downloadActivityLogsCsv).mockResolvedValue();

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
    expect(await screen.findByText("Lịch sử hoạt động hệ thống")).toBeInTheDocument();
    expect(await screen.findByText("Cập nhật thông tin lô")).toBeInTheDocument();
    expect(screen.getByText("Tạo mới lô sản xuất")).toBeInTheDocument();

    // Bấm nút "Xuất nhật ký"
    const exportBtn = screen.getByRole("button", { name: /Xuất nhật ký/i });
    await user.click(exportBtn);

    // Kiểm tra dialog xuất hiện và gọi API preview
    expect(await screen.findByText("Xuất nhật ký hoạt động")).toBeInTheDocument();
    expect(await screen.findByText("2 bản ghi")).toBeInTheDocument();

    // Bấm "Tải tệp CSV"
    const downloadBtn = screen.getByRole("button", { name: /Tải tệp CSV/i });
    await user.click(downloadBtn);

    await waitFor(() => {
      expect(activityLogApi.downloadActivityLogsCsv).toHaveBeenCalledTimes(1);
      expect(toast.success).toHaveBeenCalledWith("Xuất nhật ký hoạt động thành công.");
    });
  });

  it("gọi lại API khi người dùng bấm nút 'Làm mới'", async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <ActivityLogPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("Lịch sử hoạt động hệ thống")).toBeInTheDocument();
    expect(activityLogApi.getActivityLogs).toHaveBeenCalledTimes(1);

    // Bấm nút "Làm mới"
    const refreshBtn = screen.getByRole("button", { name: /Làm mới/i });
    await user.click(refreshBtn);

    await waitFor(() => {
      expect(activityLogApi.getActivityLogs).toHaveBeenCalledTimes(2);
    });
  });

  it("hiển thị cảnh báo và vô hiệu hóa nút tải khi preview trả về vượt quá 10.000 bản ghi", async () => {
    vi.mocked(activityLogApi.previewExportActivityLogs).mockResolvedValue({
      count: 12500,
      mode: "DIRECT",
    });

    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <ActivityLogPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("Lịch sử hoạt động hệ thống")).toBeInTheDocument();

    const exportBtn = screen.getByRole("button", { name: /Xuất nhật ký/i });
    await user.click(exportBtn);

    expect(await screen.findByText("Vượt quá giới hạn xuất trực tiếp")).toBeInTheDocument();
    expect(screen.getByText(/Xuất trực tiếp hỗ trợ tối đa/)).toBeInTheDocument();

    const downloadBtn = screen.getByRole("button", { name: /Tải tệp CSV/i });
    expect(downloadBtn).toBeDisabled();
  });
});
