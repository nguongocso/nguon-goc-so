import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

interface DetailSectionProps {
  /** Section heading, rendered as a small uppercase label. */
  title: string;
  /** Optional secondary line rendered under the title. */
  description?: ReactNode;
  /**
   * Optional leading icon rendered next to the title.
   * Callers pass a sized lucide icon (e.g. <MapPin className="size-4" />).
   */
  icon?: ReactNode;
  /** Section content. */
  children: ReactNode;
  /** Wrapper classes (spacing between sections is owned by the parent). */
  className?: string;
  /**
   * Classes for the content box. Defaults to the project's muted panel:
   * rounded-lg border bg-muted/30 p-4.
   */
  contentClassName?: string;
}

/**
 * Shared building block for detail interfaces: an uppercase section heading
 * with the standard tinted content panel used across existing detail views
 * (shipment header panel, product feedback sections).
 *
 * Pure presentation only — it owns spacing/typography, not layout or data.
 */
export function DetailSection({
  title,
  description,
  icon,
  children,
  className,
  contentClassName,
}: DetailSectionProps) {
  return (
    <section className={cn("min-w-0", className)}>
      <div className="mb-2 flex items-center gap-1.5">
        {icon}
        <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          {title}
        </h3>
      </div>

      {description && (
        <p className="mb-2 text-xs text-muted-foreground">{description}</p>
      )}

      <div className={cn("rounded-lg border bg-muted/30 p-4", contentClassName)}>
        {children}
      </div>
    </section>
  );
}
