import { useEffect } from "react";
import { CloudOff, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useOfflineSync } from "@/hooks/useOfflineSync";

export function SyncBadge() {
  const { isOnline, pendingCount, isSyncing, sync } = useOfflineSync();

  // Tự động đồng bộ khi trở lại online
  useEffect(() => {
    if (isOnline && pendingCount > 0 && !isSyncing) {
      void sync();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOnline, pendingCount, isSyncing]);

  const eventCount = pendingCount;
  const hasPending = eventCount > 0;

  return (
    <Button
      type="button"
      variant={hasPending ? "outline" : "ghost"}
      size="icon"
      onClick={() => {
        if (hasPending && !isSyncing) {
          void sync();
        }
      }}
      disabled={isSyncing}
      title={
        isSyncing
          ? "Đang đồng bộ..."
          : !isOnline
            ? "Mất kết nối mạng"
            : hasPending
              ? `${eventCount} sự kiện chờ đồng bộ — bấm để đồng bộ`
              : "Không có sự kiện chờ đồng bộ"
      }
      className={cn(
        "relative",
        isSyncing && "animate-pulse",
      )}
    >
      {isSyncing ? (
        <RefreshCw className="h-5 w-5 animate-spin" />
      ) : !isOnline ? (
        <CloudOff className="h-5 w-5 text-warning" />
      ) : (
        <RefreshCw className="h-5 w-5" />
      )}

      {hasPending && (
        <span
          className={cn(
            "absolute -right-1 -top-1 flex h-4 min-w-[16px] items-center justify-center",
            "rounded-full bg-warning px-0.5 text-[10px] font-bold leading-none text-white",
          )}
        >
          {eventCount > 99 ? "99+" : eventCount}
        </span>
      )}
    </Button>
  );
}