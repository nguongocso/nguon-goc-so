import { LoaderCircle } from "lucide-react";
import { useHelp } from "@/hooks/useHelp";
import {
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { cn } from "@/lib/utils";
import { LOCAL_HELP_CONTENT, formatHelpStep } from "./localHelpContent";
import type { HelpCustomContent } from "./localHelpContent";

/** Thuộc tính của ngăn hiển thị hướng dẫn chi tiết. */
interface HelpDrawerProps {
  screenKey: string;
  customContent?: HelpCustomContent;
}

/** Ngăn hiển thị nội dung hướng dẫn cho một màn hình. */
export function HelpDrawer({ screenKey, customContent }: HelpDrawerProps) {
  // Hooks
  const { data, isLoading, error } = useHelp(screenKey);

  // Dữ liệu hiển thị: ưu tiên tuỳ biến, sau đó API, cuối cùng dự phòng cục bộ
  const helpData = customContent || data || LOCAL_HELP_CONTENT[screenKey] || null;

  // Giao diện drawer hướng dẫn
  return (
    <SheetContent side="right">
      <SheetHeader className="pr-6">
        <SheetTitle>
          {isLoading && !helpData ? "Đang tải hướng dẫn..." : helpData?.title ?? "Hướng dẫn sử dụng"}
        </SheetTitle>
        <SheetDescription className="sr-only">
          {helpData?.title ?? "Hướng dẫn sử dụng"}
        </SheetDescription>
      </SheetHeader>
      <div className="flex-1 overflow-y-auto pr-1">
        {isLoading && !helpData ? (
          <div className="flex items-center gap-2 py-8 text-muted-foreground">
            <LoaderCircle className="size-4 animate-spin" />
            Đang tải...
          </div>
        ) : error && !helpData ? (
          <p className="py-8 text-center text-sm text-muted-foreground">{error}</p>
        ) : !helpData ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Chưa có hướng dẫn cho màn hình này.
          </p>
        ) : (
          <div className="space-y-4">
            <ol className="list-none space-y-3">
              {helpData.steps.map((step, index) => (
                <li
                  key={index}
                  className="flex gap-3"
                >
                  <span
                    className={cn(
                      "flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold",
                      "bg-primary/10 text-primary"
                    )}
                  >
                    {index + 1}
                  </span>
                  <span className="text-sm leading-relaxed text-foreground">
                    {formatHelpStep(step)}
                  </span>
                </li>
              ))}
            </ol>
          </div>
        )}
      </div>
    </SheetContent>
  );
}
