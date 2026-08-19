import type { FarmActivityType } from "@/types/farmLog";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import {
  CheckCircle2,
  ClipboardCheck,
  LoaderCircle,
  RefreshCw,
  TriangleAlert,
  XCircle,
} from "lucide-react";

export type FarmLogEligibilityStatus =
  | "unselected"
  | "idle"
  | "checking"
  | "eligible"
  | "ineligible"
  | "error";

interface FarmLogEligibilityAlertProps {
  status: FarmLogEligibilityStatus;
  productionLotName?: string;
  missingActivities?: FarmActivityType[];
  message?: string;
  actionLabel?: string;
  onAction?: () => void;
  onRetry?: () => void;
}

const activityLabels: Record<FarmActivityType, string> = {
  PLANTING: "Gieo trồng",
  WATERING: "Tưới nước",
  FERTILIZING: "Bón phân",
  PESTICIDE: "Phun thuốc bảo vệ thực vật",
  WEEDING: "Làm cỏ",
  HARVESTING: "Thu hoạch",
  OTHER: "Hoạt động khác",
};

const statusStyles: Record<
  FarmLogEligibilityStatus,
  {
    container: string;
    icon: string;
    title: string;
  }
> = {
  unselected: {
    container: "border-slate-200 bg-slate-50",
    icon: "bg-white text-slate-500 ring-slate-200",
    title: "text-slate-900",
  },
  idle: {
    container: "border-blue-200 bg-blue-50/70",
    icon: "bg-white text-blue-700 ring-blue-200",
    title: "text-blue-950",
  },
  checking: {
    container: "border-blue-200 bg-blue-50/70",
    icon: "bg-white text-blue-700 ring-blue-200",
    title: "text-blue-950",
  },
  eligible: {
    container: "border-emerald-200 bg-emerald-50/70",
    icon: "bg-white text-emerald-700 ring-emerald-200",
    title: "text-emerald-950",
  },
  ineligible: {
    container: "border-amber-300 bg-amber-50",
    icon: "bg-white text-amber-700 ring-amber-200",
    title: "text-amber-950",
  },
  error: {
    container: "border-red-200 bg-red-50/70",
    icon: "bg-white text-red-700 ring-red-200",
    title: "text-red-950",
  },
};

export function FarmLogEligibilityAlert({
  status,
  productionLotName,
  missingActivities = [],
  message,
  actionLabel,
  onAction,
  onRetry,
}: FarmLogEligibilityAlertProps) {
  const styles = statusStyles[status];

  const content = {
    unselected: {
      icon: ClipboardCheck,
      title: "Chọn lô để kiểm tra nhật ký",
      description:
        "Hệ thống sẽ kiểm tra hồ sơ canh tác trước khi cho phép ghi sự kiện đóng gói.",
    },
    idle: {
      icon: ClipboardCheck,
      title: "Sẵn sàng kiểm tra nhật ký",
      description: productionLotName
        ? `Nhật ký của lô “${productionLotName}” sẽ được kiểm tra khi bạn ghi sự kiện đóng gói.`
        : "Nhật ký của lô sẽ được kiểm tra khi bạn ghi sự kiện đóng gói.",
    },
    checking: {
      icon: LoaderCircle,
      title: "Đang kiểm tra nhật ký canh tác",
      description:
        "Vui lòng chờ trong khi hệ thống đối chiếu các hoạt động bắt buộc.",
    },
    eligible: {
      icon: CheckCircle2,
      title: "Nhật ký canh tác hợp lệ",
      description:
        message ?? "Lô sản xuất đủ điều kiện để ghi sự kiện đóng gói.",
    },
    ineligible: {
      icon: TriangleAlert,
      title: "Chưa đủ điều kiện đóng gói",
      description:
        message ??
        "Lô sản xuất còn thiếu nhật ký bắt buộc. Vui lòng bổ sung trước khi tiếp tục.",
    },
    error: {
      icon: XCircle,
      title: "Không thể kiểm tra nhật ký",
      description:
        message ?? "Đã xảy ra lỗi khi kiểm tra. Vui lòng thử lại.",
    },
  }[status];

  const Icon = content.icon;
  const isAssertive = status === "ineligible" || status === "error";

  return (
    <section
      className={cn(
        "rounded-xl border p-4 sm:p-5",
        styles.container,
      )}
      aria-live={isAssertive ? "assertive" : "polite"}
      role={isAssertive ? "alert" : "status"}
    >
      <div className="flex items-start gap-3">
        <span
          className={cn(
            "grid size-9 shrink-0 place-items-center rounded-full ring-1",
            styles.icon,
          )}
        >
          <Icon
            className={cn(
              "size-5",
              status === "checking" && "animate-spin",
            )}
            aria-hidden="true"
          />
        </span>

        <div className="min-w-0 flex-1">
          <h3 className={cn("text-sm font-bold", styles.title)}>
            {content.title}
          </h3>
          <p className="mt-1 text-sm leading-6 text-slate-600">
            {content.description}
          </p>

          {status === "ineligible" && missingActivities.length > 0 && (
            <div className="mt-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-amber-800">
                Nhật ký còn thiếu
              </p>
              <ul className="mt-2 flex flex-wrap gap-2">
                {missingActivities.map((activity) => (
                  <li
                    className="rounded-full border border-amber-200 bg-white px-3 py-1 text-xs font-semibold text-amber-800"
                    key={activity}
                  >
                    {activityLabels[activity]}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {(onAction || onRetry) && (
            <div className="mt-4 flex flex-wrap gap-2">
              {onAction && actionLabel && (
                <Button type="button" size="sm" onClick={onAction}>
                  {actionLabel}
                </Button>
              )}
              {onRetry && (
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={onRetry}
                >
                  <RefreshCw className="size-4" />
                  Kiểm tra lại
                </Button>
              )}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}