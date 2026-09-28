import { useState } from "react";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxInputGroup,
  ComboboxItem,
  ComboboxItemIndicator,
  ComboboxItemText,
  ComboboxList,
  ComboboxTrigger,
  vietnameseComboboxFilter,
  type ComboboxOption,
} from "@/components/ui/combobox";

const ITEMS: ComboboxOption[] = [
  { value: "std-1", label: "SQF" },
  { value: "std-2", label: "Codex Alimentarius (rau quả tươi)" },
  { value: "std-3", label: "TCVN 11041-2:2017" },
  { value: "std-4", label: "Rainforest Alliance" },
  { value: "std-5", label: "HACCP (TCVN 5603)" },
];

interface HarnessProps {
  initialValue?: string | null;
  onValueChange?: (value: string | null) => void;
}

function ComboboxHarness({ initialValue = null, onValueChange }: HarnessProps) {
  const [value, setValue] = useState<string | null>(initialValue);
  return (
    <Combobox
      items={ITEMS}
      value={value}
      onValueChange={(next) => {
        setValue(next);
        onValueChange?.(next);
      }}
    >
      <ComboboxInputGroup>
        <ComboboxInput aria-label="Tiêu chuẩn chất lượng" placeholder="Chọn tiêu chuẩn" />
        <ComboboxTrigger />
      </ComboboxInputGroup>
      <ComboboxContent>
        <ComboboxEmpty>Không tìm thấy tiêu chuẩn phù hợp</ComboboxEmpty>
        <ComboboxList>
          {(option: ComboboxOption) => (
            <ComboboxItem key={option.value} value={option}>
              <ComboboxItemText>{option.label}</ComboboxItemText>
              <ComboboxItemIndicator />
            </ComboboxItem>
          )}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  );
}

/** Mở popup rồi trả về listbox đang hiển thị. */
const openPopup = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(screen.getByLabelText("Tiêu chuẩn chất lượng"));
  return screen.findByRole("listbox");
};

describe("vietnameseComboboxFilter", () => {
  it("khớp không phân biệt dấu và hoa thường", () => {
    const item = { value: "1", label: "HACCP (TCVN 5603)" };
    const label = (value: ComboboxOption) => value.label;

    expect(vietnameseComboboxFilter(item, "haccp", label)).toBe(true);
    expect(vietnameseComboboxFilter(item, "HÀCCP", label)).toBe(true);
    expect(vietnameseComboboxFilter(item, "tcvn 5603", label)).toBe(true);
  });

  it("yêu cầu mọi từ trong từ khóa đều xuất hiện trong nhãn", () => {
    const item = { value: "1", label: "HACCP (TCVN 5603)" };
    const label = (value: ComboboxOption) => value.label;

    expect(vietnameseComboboxFilter(item, "tcvn haccp", label)).toBe(true);
    expect(vietnameseComboboxFilter(item, "tcvn iso", label)).toBe(false);
  });

  it("từ khóa rỗng thì mọi mục đều khớp", () => {
    const item = { value: "1", label: "SQF" };
    expect(vietnameseComboboxFilter(item, "   ", (value) => value.label)).toBe(true);
  });
});

describe("Combobox", () => {
  it("mở popup và hiển thị toàn bộ danh sách khi chưa gõ từ khóa", async () => {
    const user = userEvent.setup();
    render(<ComboboxHarness />);

    const listbox = await openPopup(user);
    await waitFor(() => {
      expect(within(listbox).getAllByRole("option")).toHaveLength(ITEMS.length);
    });
  });

  it("lọc danh sách theo từ khóa không dấu", async () => {
    const user = userEvent.setup();
    render(<ComboboxHarness />);

    await openPopup(user);
    const input = screen.getByLabelText("Tiêu chuẩn chất lượng");
    await user.type(input, "haccp");

    const listbox = await screen.findByRole("listbox");
    await waitFor(() => {
      const options = within(listbox).getAllByRole("option");
      expect(options).toHaveLength(1);
      expect(options[0]).toHaveTextContent("HACCP (TCVN 5603)");
    });
  });

  it("hiển thị empty state khi không có mục nào khớp", async () => {
    const user = userEvent.setup();
    render(<ComboboxHarness />);

    await openPopup(user);
    await user.type(screen.getByLabelText("Tiêu chuẩn chất lượng"), "khong-ton-tai");

    expect(
      await screen.findByText("Không tìm thấy tiêu chuẩn phù hợp")
    ).toBeInTheDocument();
  });

  it("chọn một mục thì đóng popup và đưa nhãn về ô chọn", async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<ComboboxHarness onValueChange={onValueChange} />);

    const listbox = await openPopup(user);
    const option = await within(listbox).findByText("TCVN 11041-2:2017");
    await user.click(option);

    expect(onValueChange).toHaveBeenCalledWith("std-3");
    await waitFor(() => {
      expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    });
    const input = screen.getByLabelText("Tiêu chuẩn chất lượng") as HTMLInputElement;
    expect(input.value).toBe("TCVN 11041-2:2017");
  });

  it("hiển thị giá trị đã chọn ban đầu trong ô chọn", () => {
    render(<ComboboxHarness initialValue="std-4" />);
    const input = screen.getByLabelText("Tiêu chuẩn chất lượng") as HTMLInputElement;
    expect(input.value).toBe("Rainforest Alliance");
  });

  it("đóng popup khi nhấn Escape", async () => {
    const user = userEvent.setup();
    render(<ComboboxHarness />);

    await openPopup(user);
    await user.keyboard("{Escape}");

    await waitFor(() => {
      expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    });
  });

  it("bề ngang popup bám theo bề ngang ô chọn thay vì khoảng cố định", async () => {
    const user = userEvent.setup();
    render(<ComboboxHarness />);

    const listbox = await openPopup(user);
    const popup = listbox.closest('[data-slot="combobox-content"]');
    expect(popup).not.toBeNull();
    // Popup khai báo bề ngang theo --anchor-width (bề ngang ô chọn),
    // tự co lại theo --available-width khi tràn khung nhìn.
    expect(popup?.className).toContain("w-[var(--anchor-width)]");
    expect(popup?.className).toContain("max-w-[var(--available-width)]");
    expect(popup?.className).not.toContain("min-w-[300px]");
  });
});
