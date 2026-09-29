import * as React from "react"
import { Combobox as ComboboxPrimitive } from "@base-ui/react/combobox"

import { cn } from "@/lib/utils"
import { normalizeVietnamese } from "@/utils/string"
import { CheckIcon, ChevronDownIcon, SearchIcon, XIcon } from "lucide-react"

/** Một lựa chọn của Combobox: `value` là giá trị gửi đi, `label` là chữ hiển thị. */
export interface ComboboxOption {
  value: string
  label: string
}

/**
 * Bộ lọc mặc định: so khớp không phân biệt dấu tiếng Việt và không phân biệt
 * hoa/thường. Từ khóa được tách theo khoảng trắng, mọi từ đều phải xuất hiện
 * trong nhãn (ví dụ gõ "tcvn 5603" vẫn ra "TCVN 5603:2017").
 */
const vietnameseComboboxFilter: NonNullable<
  ComboboxPrimitive.Root.Props<ComboboxOption>["filter"]
> = (itemValue, query, itemToString) => {
  const label = itemToString ? itemToString(itemValue) : String(itemValue ?? "")
  const tokens = normalizeVietnamese(query).split(/\s+/).filter(Boolean)
  if (tokens.length === 0) return true
  const haystack = normalizeVietnamese(label)
  return tokens.every((token) => haystack.includes(token))
}

interface ComboboxProps
  extends Omit<ComboboxPrimitive.Root.Props<ComboboxOption>, "items" | "value" | "onValueChange" | "multiple"> {
  /** Danh sách lựa chọn; dùng để Base UI lọc và render danh sách trong popup. */
  items: ComboboxOption[]
  /** Giá trị đang chọn (null = chưa chọn). */
  value: string | null
  /** Được gọi khi người dùng chọn một lựa chọn, hoặc xoá giá trị hiện tại. */
  onValueChange: (value: string | null) => void
}

/**
 * Combobox chọn một giá trị kèm ô tìm kiếm tích hợp. Một ô input vừa hiển
 * thị giá trị đang chọn vừa làm từ khóa lọc danh sách, hỗ trợ điều hướng bằng
 * bàn phím (↑ ↓ Enter Escape) và đóng popup khi click ra ngoài.
 */
function Combobox({
  items,
  value,
  onValueChange,
  filter = vietnameseComboboxFilter,
  ...props
}: ComboboxProps) {
  return (
    <ComboboxPrimitive.Root<ComboboxOption>
      items={items}
      value={items.find((item) => item.value === value) ?? null}
      onValueChange={(item) => onValueChange(item ? item.value : null)}
      filter={filter}
      itemToStringLabel={(item) => item?.label ?? ""}
      itemToStringValue={(item) => item?.value ?? ""}
      isItemEqualToValue={(a, b) => a?.value === b?.value}
      {...props}
    />
  )
}

function ComboboxInputGroup({
  className,
  ...props
}: ComboboxPrimitive.InputGroup.Props) {
  return (
    <ComboboxPrimitive.InputGroup
      data-slot="combobox-input-group"
      className={cn("relative w-full", className)}
      {...props}
    />
  )
}

function ComboboxInput({ className, ...props }: ComboboxPrimitive.Input.Props) {
  return (
    <ComboboxPrimitive.Input
      data-slot="combobox-input"
      className={cn(
        // Base: 44px height, 10px radius, 16px horizontal padding, white bg, #D1D5DB border
        "h-11 w-full min-w-0 rounded-lg border border-input bg-white py-2 pl-4 pr-20 text-base text-foreground transition-colors outline-none md:text-sm",
        "placeholder:text-disabled",
        // Focus: border → Primary, ring → Primary Light
        "focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-primary-light",
        // Disabled: muted bg, disabled text
        "disabled:cursor-not-allowed disabled:bg-muted disabled:text-disabled",
        // Invalid state
        "aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20",
        // Icon sizing
        "[&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
        className
      )}
      {...props}
    />
  )
}

function ComboboxTrigger({
  className,
  children,
  ...props
}: ComboboxPrimitive.Trigger.Props) {
  return (
    <ComboboxPrimitive.Trigger
      data-slot="combobox-trigger"
      className={cn(
        // Nút mũi tên nằm đè trên input nên không viền, không nền
        "absolute right-1 top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground transition-colors outline-none",
        "hover:bg-muted focus-visible:bg-muted focus-visible:ring-[3px] focus-visible:ring-primary-light",
        "disabled:pointer-events-none disabled:opacity-50",
        "[&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
        className
      )}
      {...props}
    >
      {children ?? (
        <ComboboxPrimitive.Icon
          render={<ChevronDownIcon className="pointer-events-none size-4" />}
        />
      )}
    </ComboboxPrimitive.Trigger>
  )
}

function ComboboxClear({ className, ...props }: ComboboxPrimitive.Clear.Props) {
  return (
    <ComboboxPrimitive.Clear
      data-slot="combobox-clear"
      className={cn(
        "absolute right-9 top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground transition-colors outline-none",
        "hover:bg-muted focus-visible:bg-muted focus-visible:ring-[3px] focus-visible:ring-primary-light",
        "disabled:pointer-events-none disabled:opacity-50",
        "[&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
        className
      )}
      {...props}
    >
      <XIcon className="pointer-events-none size-4" />
    </ComboboxPrimitive.Clear>
  )
}

function ComboboxContent({
  className,
  children,
  side = "bottom",
  sideOffset = 4,
  align = "start",
  collisionPadding = 8,
  ...props
}: ComboboxPrimitive.Popup.Props &
  Pick<
    ComboboxPrimitive.Positioner.Props,
    "side" | "sideOffset" | "align" | "collisionPadding"
  >) {
  return (
    <ComboboxPrimitive.Portal>
      <ComboboxPrimitive.Positioner
        side={side}
        sideOffset={sideOffset}
        align={align}
        collisionPadding={collisionPadding}
        className="isolate z-[1100]"
      >
        <ComboboxPrimitive.Popup
          data-slot="combobox-content"
          className={cn(
            // Bề ngang luôn bằng ô chọn (--anchor-width), tự co lại khi tràn khung nhìn
            "relative isolate z-50 w-[var(--anchor-width)] max-w-[var(--available-width)] origin-(--transform-origin) overflow-hidden rounded-[12px] border border-border bg-popover text-popover-foreground shadow-xl transition-all duration-150",
            "data-[side=bottom]:slide-in-from-top-2 data-[side=inline-end]:slide-in-from-left-2 data-[side=inline-start]:slide-in-from-right-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2",
            "data-open:animate-in data-open:fade-in-0 data-open:zoom-in-95",
            "data-closed:animate-out data-closed:fade-out-0 data-closed:zoom-out-95",
            className
          )}
          {...props}
        >
          {children}
        </ComboboxPrimitive.Popup>
      </ComboboxPrimitive.Positioner>
    </ComboboxPrimitive.Portal>
  )
}

function ComboboxList({ className, ...props }: ComboboxPrimitive.List.Props) {
  return (
    <ComboboxPrimitive.List
      data-slot="combobox-list"
      className={cn(
        // Cuộn độc lập, chiều cao tối đa 260px theo chuẩn dropdown
        "max-h-[min(var(--available-height),260px)] scroll-py-1.5 overflow-y-auto overscroll-contain p-1.5",
        className
      )}
      {...props}
    />
  )
}

function ComboboxItem({ className, ...props }: ComboboxPrimitive.Item.Props) {
  return (
    <ComboboxPrimitive.Item
      data-slot="combobox-item"
      className={cn(
        // 40px chiều cao, bo góc 8px, chữ 14px — đồng bộ với SelectItem
        "relative flex w-full cursor-pointer items-center gap-2 rounded-md px-3.5 py-2 text-sm font-normal text-foreground outline-none select-none transition-colors duration-100",
        // Đường kẻ ngăn cách giữa các mục
        "not-last:border-b not-last:border-border",
        // Hover / điều hướng bàn phím: #F3F4F6
        "hover:bg-[#F3F4F6] data-[highlighted]:bg-[#F3F4F6]",
        // Đã chọn: Primary Light bg, Primary text, medium weight
        "data-[selected]:bg-primary-light data-[selected]:text-primary data-[selected]:font-medium",
        // Disabled
        "data-disabled:pointer-events-none data-disabled:opacity-50",
        // Icon sizing
        "[&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
        className
      )}
      {...props}
    />
  )
}

function ComboboxItemText({
  className,
  ...props
}: React.ComponentProps<"span">) {
  return (
    <span
      data-slot="combobox-item-text"
      className={cn("min-w-0 flex-1 truncate", className)}
      {...props}
    />
  )
}

function ComboboxItemIndicator({
  className,
  ...props
}: ComboboxPrimitive.ItemIndicator.Props) {
  return (
    <ComboboxPrimitive.ItemIndicator
      data-slot="combobox-item-indicator"
      className={cn("flex size-4 shrink-0 items-center justify-center text-primary", className)}
      {...props}
    >
      <CheckIcon className="pointer-events-none size-4" />
    </ComboboxPrimitive.ItemIndicator>
  )
}

function ComboboxEmpty({
  className,
  children,
  ...props
}: ComboboxPrimitive.Empty.Props) {
  return (
    <ComboboxPrimitive.Empty
      data-slot="combobox-empty"
      className={cn(
        "flex items-center gap-2 px-3.5 py-4 text-sm text-muted-foreground",
        className
      )}
      {...props}
    >
      <SearchIcon className="pointer-events-none size-4" />
      {children}
    </ComboboxPrimitive.Empty>
  )
}

function ComboboxStatus({ className, ...props }: ComboboxPrimitive.Status.Props) {
  return (
    <ComboboxPrimitive.Status
      data-slot="combobox-status"
      className={cn("px-3.5 py-2 text-xs text-muted-foreground", className)}
      {...props}
    />
  )
}

export {
  Combobox,
  ComboboxClear,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxInputGroup,
  ComboboxItem,
  ComboboxItemIndicator,
  ComboboxItemText,
  ComboboxList,
  ComboboxStatus,
  ComboboxTrigger,
  vietnameseComboboxFilter,
}
