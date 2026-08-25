import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

interface DetailFieldProps {
  /** Field label, rendered small and muted above the value. */
  label: string;
  /**
   * Field value. Nullish/empty values fall back to an em dash ("—").
   * Rich values are supported by passing any ReactNode; compose secondary
   * lines inside the node instead of adding more props.
   */
  value?: ReactNode | null;
  /** Optional leading icon (pass a pre-sized lucide icon). */
  icon?: ReactNode;
  /** Render the value in monospace — IDs, trace codes, IPs, coordinates. */
  mono?: boolean;
  className?: string;
}

/**
 * Shared label/value primitive extracted from the identical patterns in
 * ShipmentDetailDialog, ScanAnomalyAlertDetailsDialog, CertificationDetailDialog
 * and the Product Feedback sheet.
 *
 * Designed to sit inside a responsive grid:
 *   <div className="grid gap-3 sm:grid-cols-2">…</div>
 */
export function DetailField({
  label,
  value,
  icon,
  mono = false,
  className,
}: DetailFieldProps) {
  const isEmpty =
    value === null ||
    value === undefined ||
    (typeof value === "string" && value.trim() === "");

  return (
    <div className={cn("flex min-w-0 items-start gap-3", className)}>
      {icon && <div className="mt-0.5 shrink-0">{icon}</div>}

      <div className="min-w-0 flex-1">
        <p className="text-xs text-muted-foreground">{label}</p>
        <div
          className={cn(
            "mt-0.5 break-words text-sm font-medium",
            mono && "font-mono",
          )}
        >
          {isEmpty ? "—" : value}
        </div>
      </div>
    </div>
  );
}
