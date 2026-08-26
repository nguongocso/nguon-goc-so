import { useState } from "react";
import { LoaderCircle } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  /** Optional explanation rendered under the title. */
  description?: React.ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  /** "destructive" styles the confirm button with the destructive variant. */
  variant?: "default" | "destructive";
  /**
   * External pending state (e.g. parent-driven mutation). When omitted,
   * loading is derived automatically if onConfirm returns a Promise.
   */
  loading?: boolean;
  onConfirm: () => void | Promise<void>;
}

/**
 * Shared confirmation primitive built on the project's Base UI Dialog, so it
 * keeps the existing focus trap, Escape handling and title/description
 * semantics for free.
 *
 * Intended first consumers are the native window.confirm() call sites
 * (delete draft, detach certification, resolve anomalies). Existing bespoke
 * confirmation dialogs are NOT migrated by this change.
 *
 * Note: a required-reason workflow exists today only in RecallShipmentDialog;
 * until a second consumer appears, reason input stays out of this API.
 */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel = "Xác nhận",
  cancelLabel = "Hủy",
  variant = "default",
  loading = false,
  onConfirm,
}: ConfirmDialogProps) {
  const [internalPending, setInternalPending] = useState(false);
  const busy = loading || internalPending;

  const handleConfirm = async () => {
    try {
      setInternalPending(true);
      await onConfirm();
    } finally {
      setInternalPending(false);
    }
  };

  // Block Escape/backdrop closes while the action is running.
  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen && busy) return;
    onOpenChange(nextOpen);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          {description && (
            <DialogDescription>{description}</DialogDescription>
          )}
        </DialogHeader>

        <DialogFooter>
          <Button variant="outline" disabled={busy} onClick={() => onOpenChange(false)}>
            {cancelLabel}
          </Button>

          <Button
            variant={variant === "destructive" ? "destructive" : "default"}
            disabled={busy}
            onClick={handleConfirm}
            className={cn(variant === "destructive" && "text-white")}
          >
            {busy && <LoaderCircle className="animate-spin" />}
            {confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
