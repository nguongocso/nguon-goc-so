import { CircleHelp } from "lucide-react";
import { Sheet, SheetTrigger } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { HelpDrawer } from "./HelpDrawer";
import type { HelpCustomContent } from "./localHelpContent";

/** Nội dung hướng dẫn tuỳ biến cho từng màn hình (giữ tương thích nhập cũ). */
export type { HelpCustomContent } from "./localHelpContent";

/** Thuộc tính của nút mở hướng dẫn sử dụng. */
interface HelpButtonProps {
  /** Mã định danh màn hình (ví dụ: "farm-log-create"). */
  screenKey: string;
  /** Nội dung hướng dẫn tuỳ biến (nếu có sẽ ưu tiên hiển thị trước). */
  customContent?: HelpCustomContent;
  /** Nhãn nút — mặc định "Hướng dẫn". */
  label?: string;
  /** Chỉ hiển thị icon, không hiện nhãn. */
  iconOnly?: boolean;
  /** Class ngoài tuỳ chỉnh. */
  className?: string;
}

/**
 * Nút mở drawer hướng dẫn sử dụng cho một màn hình (NCL-01-CN-006).
 * Chỉ hiển thị cho người dùng đã đăng nhập (mọi trang trong PrivateRoute).
 */
export function HelpButton({
  screenKey,
  customContent,
  label = "Hướng dẫn",
  iconOnly = false,
  className,
}: HelpButtonProps) {
  return (
    <Sheet>
      <SheetTrigger
        render={
          <Button
            variant="outline"
            size="sm"
            className={cn("gap-1.5", className)}
          >
            <CircleHelp className="size-4" />
            {!iconOnly && label}
          </Button>
        }
      />
      <HelpDrawer
        screenKey={screenKey}
        customContent={customContent}
      />
    </Sheet>
  );
}
