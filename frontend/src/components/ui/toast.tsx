/**
 * Centralized toast configuration (Sonner-powered)
 *
 * Every page automatically inherits these styles through the <AppToaster />
 * component rendered in App.tsx.  No page-level changes are required.
 *
 * Design tokens are consumed from docs/AI_DESIGN_SYSTEM.md and defined in
 * index.css as CSS custom properties (--success, --error, --warning, …).
 * Per-variant card colors are applied via CSS [data-type] selectors in index.css.
 */
import { Toaster, toast as sonnerToast } from "sonner"
import { CircleCheckIcon, InfoIcon, TriangleAlertIcon, OctagonXIcon } from "lucide-react"

// ── Re-export the raw toast API so existing `import { toast } from 'sonner'` ──
export { toast } from "sonner"
export { sonnerToast }

// ── Per-variant icons (Sonner maps these by type key) ─────────────────────────

const variantIcons: Record<string, React.ReactNode> = {
  success: <CircleCheckIcon className="text-success size-5" aria-hidden="true" />,
  error: <OctagonXIcon className="text-destructive size-5" aria-hidden="true" />,
  warning: <TriangleAlertIcon className="text-warning size-5" aria-hidden="true" />,
  info: <InfoIcon className="text-info size-5" aria-hidden="true" />,
}

// ── AppToaster – drop-in replacement for <Toaster /> in App.tsx ───────────────

export function AppToaster() {
  return (
    <Toaster
      position="top-right"
      offset={{ top: "4.5rem", right: "1rem" }}
      mobileOffset={{ top: "4.5rem", right: "1rem" }}
      gap={12}
      duration={5000}
      icons={variantIcons}
      toastOptions={{
        className:
          "rounded-lg border-l-4 shadow-card",
        classNames: {
          title: "text-[15px] font-semibold text-foreground",
          description: "text-sm font-normal text-label",
          closeButton: "text-muted-foreground hover:text-foreground",
        },
      }}
    />
  )
}

// ── Convenience helpers ───────────────────────────────────────────────────────

export const showSuccess = sonnerToast.success
export const showError = sonnerToast.error
export const showWarning = sonnerToast.warning
export const showInfo = sonnerToast.info
export const showLoading = sonnerToast.loading
export const showPromise = sonnerToast.promise
export const dismissToast = sonnerToast.dismiss

export default AppToaster