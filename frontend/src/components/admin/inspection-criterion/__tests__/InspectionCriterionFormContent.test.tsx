import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { InspectionCriterionFormContent } from "../InspectionCriterionFormContent";
import * as standardApi from "@/api/standardApi";
import * as inspectionCriterionApi from "@/api/inspectionCriterionApi";
import type { Standard } from "@/types/standard";
import type { InspectionCriterion } from "@/types/inspectionCriterion";

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

vi.mock("@/api/standardApi", () => ({
  getStandards: vi.fn(),
}));

vi.mock("@/api/inspectionCriterionApi", () => ({
  createInspectionCriterion: vi.fn(),
  updateInspectionCriterion: vi.fn(),
}));

const buildStandard = (id: string, name: string): Standard => ({
  id,
  name,
  nameEn: null,
  description: null,
  issuingBody: null,
  isActive: true,
  createdAt: "2026-01-01T00:00:00",
  updatedAt: "2026-01-01T00:00:00",
});

const STANDARDS = [
  buildStandard("std-1", "SQF"),
  buildStandard("std-2", "Codex Alimentarius (rau quả tươi)"),
  buildStandard("std-3", "TCVN 11041-2:2017"),
  buildStandard("std-4", "Rainforest Alliance"),
  buildStandard("std-5", "HACCP (TCVN 5603)"),
];

const mockStandards = (items: Standard[]) => {
  vi.mocked(standardApi.getStandards).mockResolvedValue({
    items,
    page: 0,
    size: 100,
    totalElements: items.length,
  });
};

const renderForm = (criterion: InspectionCriterion | null = null) => {
  const onSuccess = vi.fn();
  const onCancel = vi.fn();
  render(
    <InspectionCriterionFormContent
      criterion={criterion}
      onSuccess={onSuccess}
      onCancel={onCancel}
    />
  );
  return { onSuccess, onCancel };
};

/** Chọn một tiêu chuẩn trong dropdown có ô tìm kiếm. */
const selectStandard = async (
  user: ReturnType<typeof userEvent.setup>,
  label: string
) => {
  await user.click(screen.getByLabelText(/Tiêu chuẩn chất lượng/));
  const listbox = await screen.findByRole("listbox");
  await user.click(within(listbox).getByText(label));
};

describe("InspectionCriterionFormContent", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockStandards(STANDARDS);
  });

  it("bố cục 3 hàng: Tên chỉ tiêu | Tên tiếng Anh, Tiêu chuẩn, Ngưỡng tối đa | Đơn vị", async () => {
    const { container } = render(
      <InspectionCriterionFormContent
        criterion={null}
        onSuccess={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    // Thứ tự nhãn trong DOM đúng bố cục 3 hàng mới
    const labelTexts = Array.from(
      container.querySelectorAll('label[data-slot="label"]')
    ).map((label) => label.textContent?.replace(/\s+/g, " ").trim());
    expect(labelTexts).toEqual([
      "Tên chỉ tiêu *",
      "Tên tiếng Anh (English Name)",
      "Tiêu chuẩn chất lượng *",
      "Ngưỡng tối đa *",
      "Đơn vị đo *",
    ]);

    // Lưới 1 cột ở mobile, 2 cột từ md
    const grid = container.querySelector("form > div");
    expect(grid?.className).toContain("grid");
    expect(grid?.className).toContain("grid-cols-1");
    expect(grid?.className).toContain("md:grid-cols-2");

    // Các ô nằm trong lưới: Tiêu chuẩn chiếm 2 cột, 4 ô còn lại mỗi ô 1 cột
    const cells = Array.from(grid?.children ?? []) as HTMLElement[];
    expect(cells).toHaveLength(5);
    expect(cells[2].className).toContain("md:col-span-2");
    cells.forEach((cell, index) => {
      if (index !== 2) expect(cell.className).not.toContain("col-span");
    });

    await waitFor(() => {
      expect(standardApi.getStandards).toHaveBeenCalledWith({
        isActive: true,
        page: 0,
        size: 100,
      });
    });
  });

  it("bắt buộc nhập Tên chỉ tiêu, Đơn vị đo, Ngưỡng tối đa và chọn Tiêu chuẩn", async () => {
    const user = userEvent.setup();
    const { onSuccess } = renderForm();

    await user.click(screen.getByRole("button", { name: /Thêm mới/ }));

    expect(
      await screen.findByText("Tên chỉ tiêu không được để trống")
    ).toBeInTheDocument();
    expect(
      screen.getByText("Đơn vị tính không được để trống")
    ).toBeInTheDocument();
    expect(
      screen.getByText("Ngưỡng tối đa phải là số dương")
    ).toBeInTheDocument();
    expect(
      screen.getByText("Vui lòng chọn Tiêu chuẩn chất lượng.")
    ).toBeInTheDocument();
    expect(inspectionCriterionApi.createInspectionCriterion).not.toHaveBeenCalled();
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it("tìm kiếm tiêu chuẩn không phân biệt dấu rồi chọn được", async () => {
    const user = userEvent.setup();
    const { onSuccess } = renderForm();

    await waitFor(() => {
      expect(screen.getByLabelText(/Tiêu chuẩn chất lượng/)).toBeEnabled();
    });

    const standardInput = screen.getByLabelText(/Tiêu chuẩn chất lượng/);
    await user.click(standardInput);
    await user.type(standardInput, "haccp");

    const listbox = await screen.findByRole("listbox");
    await waitFor(() => {
      expect(within(listbox).getAllByRole("option")).toHaveLength(1);
    });
    await user.click(within(listbox).getByText("HACCP (TCVN 5603)"));

    await user.type(screen.getByLabelText(/Tên chỉ tiêu/), "Dư lượng thuốc BVTV");
    await user.type(screen.getByLabelText(/Ngưỡng tối đa/), "0.5");
    await user.type(screen.getByLabelText(/Đơn vị đo/), "mg/kg");

    vi.mocked(inspectionCriterionApi.createInspectionCriterion).mockResolvedValue(
      {} as InspectionCriterion
    );

    await user.click(screen.getByRole("button", { name: /Thêm mới/ }));

    // standardId (ID tiêu chuẩn) được map thành referenceStandard (tên tiêu chuẩn)
    await waitFor(() => {
      expect(
        inspectionCriterionApi.createInspectionCriterion
      ).toHaveBeenCalledWith({
        name: "Dư lượng thuốc BVTV",
        nameEn: undefined,
        unit: "mg/kg",
        maxThreshold: 0.5,
        referenceStandard: "HACCP (TCVN 5603)",
      });
      expect(onSuccess).toHaveBeenCalled();
    });
  });

  it("gửi kèm tên tiếng Anh khi có nhập", async () => {
    const user = userEvent.setup();
    renderForm();

    await waitFor(() => {
      expect(screen.getByLabelText(/Tiêu chuẩn chất lượng/)).toBeEnabled();
    });
    await selectStandard(user, "SQF");

    await user.type(screen.getByLabelText(/Tên chỉ tiêu/), "Dư lượng thuốc BVTV");
    await user.type(
      screen.getByLabelText(/Tên tiếng Anh/),
      "Organophosphorus Pesticide Residue"
    );
    await user.type(screen.getByLabelText(/Ngưỡng tối đa/), "0.5");
    await user.type(screen.getByLabelText(/Đơn vị đo/), "mg/kg");

    vi.mocked(inspectionCriterionApi.createInspectionCriterion).mockResolvedValue(
      {} as InspectionCriterion
    );

    await user.click(screen.getByRole("button", { name: /Thêm mới/ }));

    await waitFor(() => {
      expect(
        inspectionCriterionApi.createInspectionCriterion
      ).toHaveBeenCalledWith({
        name: "Dư lượng thuốc BVTV",
        nameEn: "Organophosphorus Pesticide Residue",
        unit: "mg/kg",
        maxThreshold: 0.5,
        referenceStandard: "SQF",
      });
    });
  });

  it("khóa nút lưu và ô tiêu chuẩn khi danh mục tiêu chuẩn rỗng", async () => {
    mockStandards([]);

    renderForm();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Thêm mới/ })).toBeDisabled();
    });
    const standardInput = screen.getByLabelText(/Tiêu chuẩn chất lượng/);
    expect(standardInput).toBeDisabled();
    expect(standardInput).toHaveAttribute(
      "placeholder",
      "Chưa có tiêu chuẩn chất lượng"
    );
  });

  it("hiển thị lỗi tải danh mục kèm nút Thử lại", async () => {
    vi.mocked(standardApi.getStandards).mockRejectedValue({
      response: { data: { message: "Dịch vụ tiêu chuẩn không sẵn sàng" } },
    });

    renderForm();

    expect(
      await screen.findByText(/Dịch vụ tiêu chuẩn không sẵn sàng/)
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Thêm mới/ })).toBeDisabled();
  });

  it("tự chọn đúng tiêu chuẩn đã lưu khi chỉnh sửa", async () => {
    const criterion: InspectionCriterion = {
      id: 12,
      name: "Dư lượng thuốc BVTV nhóm Lân hữu cơ",
      nameEn: "Organophosphorus Pesticide Residue",
      unit: "mg/kg",
      maxThreshold: 0.5,
      referenceStandard: "TCVN 11041-2:2017",
      status: "ACTIVE",
      referenced: false,
      createdAt: "2026-01-01T00:00:00",
      updatedAt: null,
    };

    renderForm(criterion);

    await waitFor(() => {
      expect(
        (screen.getByLabelText(/Tiêu chuẩn chất lượng/) as HTMLInputElement).value
      ).toBe("TCVN 11041-2:2017");
    });
    expect(screen.getByLabelText(/Tên chỉ tiêu/)).toHaveValue(
      "Dư lượng thuốc BVTV nhóm Lân hữu cơ"
    );
    expect(screen.getByRole("button", { name: /Cập nhật/ })).toBeInTheDocument();
  });

  it("giữ lại tiêu chuẩn cũ không còn trong danh mục khi chỉnh sửa", async () => {
    const criterion: InspectionCriterion = {
      id: 13,
      name: "Chỉ tiêu cũ",
      nameEn: null,
      unit: "%",
      maxThreshold: 5,
      referenceStandard: "QCVN 01-2018",
      status: "ACTIVE",
      referenced: false,
      createdAt: "2026-01-01T00:00:00",
      updatedAt: null,
    };

    renderForm(criterion);

    await waitFor(() => {
      expect(
        (screen.getByLabelText(/Tiêu chuẩn chất lượng/) as HTMLInputElement).value
      ).toBe("QCVN 01-2018");
    });
  });
});
