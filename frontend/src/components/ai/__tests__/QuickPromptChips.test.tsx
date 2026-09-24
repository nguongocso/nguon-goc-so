/**
 * Kiểm thử component QuickPromptChips:
 * Xác minh hiển thị danh sách câu hỏi gợi ý nhanh, kích hoạt sự kiện khi bấm chip,
 * và xử lý đúng khi ở trạng thái vô hiệu hóa (disabled) hoặc danh sách rỗng.
 */
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { QuickPromptChips } from "../QuickPromptChips";

describe("QuickPromptChips", () => {
  const samplePrompts = [
    "Làm sao để tạo lô sản xuất mới?",
    "Quy trình cấp mã số vùng trồng?",
    "Cách kích hoạt lô hàng vận chuyển?",
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("không hiển thị gì nếu danh sách gợi ý rỗng hoặc undefined", () => {
    const { container: emptyContainer } = render(
      <QuickPromptChips prompts={[]} onSelectPrompt={vi.fn()} />
    );
    expect(emptyContainer.firstChild).toBeNull();

    const { container: nullContainer } = render(
      <QuickPromptChips prompts={null as unknown as string[]} onSelectPrompt={vi.fn()} />
    );
    expect(nullContainer.firstChild).toBeNull();
  });

  it("hiển thị tiêu đề và đầy đủ danh sách các câu hỏi gợi ý", () => {
    render(
      <QuickPromptChips
        prompts={samplePrompts}
        onSelectPrompt={vi.fn()}
      />
    );

    // Xác minh tiêu đề gợi ý
    expect(screen.getByText("Gợi ý câu hỏi nhanh:")).toBeInTheDocument();

    // Xác minh các chip câu hỏi
    samplePrompts.forEach((prompt) => {
      expect(screen.getByRole("button", { name: prompt })).toBeInTheDocument();
    });
  });

  it("gọi callback onSelectPrompt với đúng câu hỏi khi bấm vào chip", () => {
    const handleSelectPrompt = vi.fn();

    render(
      <QuickPromptChips
        prompts={samplePrompts}
        onSelectPrompt={handleSelectPrompt}
      />
    );

    const targetButton = screen.getByRole("button", {
      name: samplePrompts[1],
    });
    fireEvent.click(targetButton);

    expect(handleSelectPrompt).toHaveBeenCalledTimes(1);
    expect(handleSelectPrompt).toHaveBeenCalledWith(samplePrompts[1]);
  });

  it("vô hiệu hóa tất cả các chip khi prop disabled là true", () => {
    const handleSelectPrompt = vi.fn();

    render(
      <QuickPromptChips
        prompts={samplePrompts}
        onSelectPrompt={handleSelectPrompt}
        disabled={true}
      />
    );

    samplePrompts.forEach((prompt) => {
      const chip = screen.getByRole("button", { name: prompt });
      expect(chip).toBeDisabled();
      fireEvent.click(chip);
    });

    // Không có sự kiện nào được kích hoạt khi nút bị disabled
    expect(handleSelectPrompt).not.toHaveBeenCalled();
  });
});
