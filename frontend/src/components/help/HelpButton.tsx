import { CircleHelp, LoaderCircle } from "lucide-react";
import { useHelp } from "@/hooks/useHelp";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface HelpButtonProps {
  /** Mã định danh màn hình (ví dụ: "farm-log-create"). */
  screenKey: string;
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
      <HelpDrawer screenKey={screenKey} />
    </Sheet>
  );
}

function HelpDrawer({ screenKey }: { screenKey: string }) {
  const { data, isLoading, error } = useHelp(screenKey);

  return (
    <SheetContent side="right">
      <SheetHeader className="pr-6">
        <SheetTitle>
          {isLoading ? "Đang tải hướng dẫn..." : data?.title ?? "Hướng dẫn sử dụng"}
        </SheetTitle>
        <SheetDescription className="sr-only">
          {data?.title ?? "Hướng dẫn sử dụng"}
        </SheetDescription>
      </SheetHeader>

      <div className="flex-1 overflow-y-auto pr-1">
        {isLoading ? (
          <div className="flex items-center gap-2 py-8 text-muted-foreground">
            <LoaderCircle className="size-4 animate-spin" />
            Đang tải...
          </div>
        ) : error ? (
          <p className="py-8 text-center text-sm text-muted-foreground">{error}</p>
        ) : !data ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Chưa có hướng dẫn cho màn hình này.
          </p>
        ) : (
          <div className="space-y-4">
            <ol className="list-none space-y-3">
              {data.steps.map((step, index) => (
                <li key={index} className="flex gap-3">
                  <span
                    className={cn(
                      "flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold",
                      "bg-primary/10 text-primary"
                    )}
                  >
                    {index + 1}
                  </span>
                  <span className="text-sm leading-relaxed text-foreground">
                    {step}
                  </span>
                </li>
              ))}
            </ol>

            {data.exampleData && (
              <div className="rounded-lg border bg-muted/50 p-3">
                <p className="mb-1 text-xs font-medium text-muted-foreground">
                  Ví dụ
                </p>
                <p className="text-sm text-foreground">{data.exampleData}</p>
              </div>
            )}
          </div>
        )}
      </div>
    </SheetContent>
  );
}