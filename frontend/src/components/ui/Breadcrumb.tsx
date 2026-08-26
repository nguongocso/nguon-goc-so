import { Fragment } from "react";
import { Link } from "react-router-dom";
import { ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

export interface BreadcrumbItem {
  /** Nhãn hiển thị của mục. */
  label: string;
  /**
   * Đường dẫn đích. Nếu bỏ trống, mục được coi là trang hiện tại
   * (thường là phần tử cuối) và không thể nhấn.
   */
  href?: string;
}

interface BreadcrumbProps {
  items: BreadcrumbItem[];
  /** Phần tử phân cách giữa các mục. Mặc định: icon ChevronRight. */
  separator?: React.ReactNode;
  className?: string;
}

/**
 * Breadcrumb điều hướng thống nhất cho toàn ứng dụng.
 *
 * - Các mục có {@link BreadcrumbItem.href} là liên kết (hover gạch chân).
 * - Mục cuối (không có href) là trang hiện tại: đậm, màu foreground.
 * - Nhãn dài tự động cắt bằng dấu "..." trên màn hình nhỏ.
 */
export function Breadcrumb({ items, separator, className }: BreadcrumbProps) {
  if (!items || items.length === 0) {
    return null;
  }

  return (
    <nav aria-label="Breadcrumb" className={cn("min-w-0", className)}>
      <ol className="flex flex-wrap items-center gap-1 text-sm text-muted-foreground">
        {items.map((item, index) => {
          const isLast = index === items.length - 1;
          const clickable = Boolean(item.href) && !isLast;

          return (
            <Fragment key={`${item.label}-${index}`}>
              {index > 0 && (
                <li aria-hidden="true" className="flex shrink-0 items-center">
                  {separator ?? (
                    <ChevronRight className="size-3.5 text-muted-foreground/60" />
                  )}
                </li>
              )}

              <li className="min-w-0 max-w-[10rem] sm:max-w-[14rem]">
                {clickable ? (
                  <Link
                    to={item.href as string}
                    className="block truncate rounded px-0.5 transition-colors hover:text-foreground hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    {item.label}
                  </Link>
                ) : (
                  <span
                    aria-current={isLast ? "page" : undefined}
                    className={cn(
                      "block truncate px-0.5",
                      isLast &&
                        "font-semibold text-foreground",
                    )}
                  >
                    {item.label}
                  </span>
                )}
              </li>
            </Fragment>
          );
        })}
      </ol>
    </nav>
  );
}
