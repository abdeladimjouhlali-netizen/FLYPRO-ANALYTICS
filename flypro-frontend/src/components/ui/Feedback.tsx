import type { ReactNode } from "react";
import { AlertCircle, Inbox } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { cn } from "@/utils/cn";

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Confirmer",
  danger,
  loading,
  onConfirm,
  onClose,
}: {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  danger?: boolean;
  loading?: boolean;
  onConfirm: () => void;
  onClose: () => void;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/45 p-4 backdrop-blur-sm">
      <div
        className="w-full max-w-md rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 shadow-2xl"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
      >
        <h3 id="confirm-title" className="text-lg font-semibold text-navy-900 dark:text-white">
          {title}
        </h3>
        <p className="mt-2 text-sm text-ink-500">{description}</p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={loading}>
            Annuler
          </Button>
          <Button variant={danger ? "danger" : "primary"} onClick={onConfirm} disabled={loading}>
            {loading ? "…" : confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-[var(--color-border)] bg-[var(--color-surface-muted)]/50 px-6 py-16 text-center">
      <span className="flex size-12 items-center justify-center rounded-xl bg-navy-900/5 text-navy-700 dark:bg-sky-400/10 dark:text-sky-300">
        <Inbox className="size-5" aria-hidden />
      </span>
      <h3 className="text-base font-semibold text-navy-900 dark:text-white">{title}</h3>
      {description ? <p className="max-w-md text-sm text-ink-500">{description}</p> : null}
      {action}
    </div>
  );
}

export function ErrorState({
  title = "Une erreur est survenue",
  description,
  onRetry,
}: {
  title?: string;
  description?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-red-200 bg-red-50 px-6 py-12 text-center dark:border-red-900 dark:bg-red-950/30">
      <AlertCircle className="size-8 text-danger" aria-hidden />
      <h3 className="text-base font-semibold">{title}</h3>
      {description ? <p className="max-w-md text-sm text-ink-500">{description}</p> : null}
      {onRetry ? (
        <Button variant="secondary" onClick={onRetry}>
          Réessayer
        </Button>
      ) : null}
    </div>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      className={cn("animate-pulse rounded-xl bg-slate-200/80 dark:bg-navy-700/50", className ?? "h-10 w-full")}
      aria-hidden
    />
  );
}

export function PageLoader() {
  return (
    <div className="grid gap-4" aria-busy="true" aria-label="Chargement">
      <Skeleton className="h-8 w-64" />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-28 w-full" />
        ))}
      </div>
      <Skeleton className="h-72 w-full" />
    </div>
  );
}
