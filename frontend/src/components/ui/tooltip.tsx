import * as React from "react";
import { createPortal } from "react-dom";
import { cn } from "@/lib/utils";

type TooltipSide = "top" | "right";

interface TooltipContextValue {
  open: boolean;
  setOpen: (open: boolean) => void;
  side: TooltipSide;
  anchorRef: React.RefObject<HTMLDivElement | null>;
}

const TooltipContext = React.createContext<TooltipContextValue>({
  open: false,
  setOpen: () => {},
  side: "top",
  anchorRef: { current: null },
});

function TooltipProvider({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}

function Tooltip({
  children,
  side = "top",
  ...props
}: React.ComponentProps<"div"> & { side?: TooltipSide }) {
  const [open, setOpen] = React.useState(false);
  const anchorRef = React.useRef<HTMLDivElement>(null);
  return (
    <TooltipContext.Provider value={{ open, setOpen, side, anchorRef }}>
      <div ref={anchorRef} className="relative block" {...props}>
        {children}
      </div>
    </TooltipContext.Provider>
  );
}

function TooltipTrigger({
  children,
  asChild,
  ...props
}: React.ComponentProps<"div"> & { asChild?: boolean }) {
  const { setOpen } = React.useContext(TooltipContext);

  const handleMouseEnter = () => setOpen(true);
  const handleMouseLeave = () => setOpen(false);
  const handleFocus = () => setOpen(true);
  const handleBlur = () => setOpen(false);

  if (asChild && React.isValidElement(children)) {
    return React.cloneElement(children as React.ReactElement<any>, {
      onMouseEnter: handleMouseEnter,
      onMouseLeave: handleMouseLeave,
      onFocus: handleFocus,
      onBlur: handleBlur,
      ...props,
    });
  }

  return (
    <div
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onFocus={handleFocus}
      onBlur={handleBlur}
      className="inline-flex"
      {...props}
    >
      {children}
    </div>
  );
}

/**
 * Right-side tooltip rendered through a portal so it is never clipped by
 * scroll containers (e.g. the sidebar nav with overflow-y-auto).
 */
function RightSideTooltip({
  className,
  children,
  ...props
}: React.ComponentProps<"div">) {
  const { anchorRef } = React.useContext(TooltipContext);
  const [position, setPosition] = React.useState<{
    top: number;
    left: number;
  } | null>(null);

  const updatePosition = React.useCallback(() => {
    const el = anchorRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    setPosition({ top: rect.top + rect.height / 2, left: rect.right + 8 });
  }, [anchorRef]);

  React.useLayoutEffect(() => {
    updatePosition();
    window.addEventListener("scroll", updatePosition, true);
    window.addEventListener("resize", updatePosition);
    return () => {
      window.removeEventListener("scroll", updatePosition, true);
      window.removeEventListener("resize", updatePosition);
    };
  }, [updatePosition]);

  if (!position) return null;

  return createPortal(
    <div
      role="tooltip"
      className={cn(
        "pointer-events-none fixed z-[100] -translate-y-1/2 rounded-md bg-foreground px-3 py-1.5 text-xs text-background text-balance whitespace-nowrap shadow-lg animate-in fade-in-0 zoom-in-95",
        className,
      )}
      {...props}
      style={{ top: position.top, left: position.left }}
    >
      {children}
    </div>,
    document.body,
  );
}

function TooltipContent({
  className,
  children,
  side,
  ...props
}: React.ComponentProps<"div"> & { side?: TooltipSide }) {
  const { open, side: contextSide } = React.useContext(TooltipContext);
  const resolvedSide = side ?? contextSide;

  if (!open) return null;

  if (resolvedSide === "right") {
    return (
      <RightSideTooltip className={className} {...props}>
        {children}
      </RightSideTooltip>
    );
  }

  return (
    <div
      role="tooltip"
      className={cn(
        "bg-foreground text-background animate-in fade-in-0 zoom-in-95 z-50 absolute bottom-full left-1/2 -translate-x-1/2 mb-1.5 rounded-md px-3 py-1.5 text-xs text-balance whitespace-nowrap",
        className,
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export { Tooltip, TooltipTrigger, TooltipContent, TooltipProvider };