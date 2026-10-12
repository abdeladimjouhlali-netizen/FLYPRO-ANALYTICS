import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import { TrendingDown, TrendingUp } from "lucide-react";
import { cn, formatNumber } from "@/utils/cn";

const toneStyles = {
  navy: "bg-brand-50 text-brand-700",
  blue: "bg-brand-100 text-brand-800",
  sky: "bg-brand-50 text-brand-600",
  success: "bg-emerald-50 text-emerald-700",
  warning: "bg-amber-50 text-amber-700",
  danger: "bg-red-50 text-red-700",
} as const;

export function StatCard({
  label,
  value,
  icon: Icon,
  tone = "navy",
  delta,
  hint,
}: {
  label: string;
  value: number | string;
  icon?: LucideIcon;
  tone?: keyof typeof toneStyles;
  delta?: number;
  hint?: string;
}) {
  return (
    <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)]/95 p-5 shadow-md backdrop-blur-sm transition hover:shadow-lg">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-semibold tracking-wide text-[var(--color-ink-500)] uppercase">{label}</p>
          <p className="ram-wordmark mt-2 text-3xl text-brand-600 tabular-nums">
            {typeof value === "number" ? formatNumber(value) : value}
          </p>
          {typeof delta === "number" ? (
            <p
              className={cn(
                "mt-2 inline-flex items-center gap-1 text-xs font-semibold",
                delta >= 0 ? "text-emerald-600" : "text-red-600",
              )}
            >
              {delta >= 0 ? <TrendingUp className="size-3.5" /> : <TrendingDown className="size-3.5" />}
              {delta >= 0 ? "+" : ""}
              {delta.toFixed(1)}%
            </p>
          ) : hint ? (
            <p className="mt-2 text-xs text-[var(--color-ink-500)]">{hint}</p>
          ) : null}
        </div>
        {Icon ? (
          <span className={cn("flex size-11 items-center justify-center rounded-xl", toneStyles[tone])}>
            <Icon className="size-5" aria-hidden />
          </span>
        ) : null}
      </div>
    </div>
  );
}

export function ChartCard({
  title,
  description,
  children,
  action,
}: {
  title: string;
  description?: string;
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="overflow-hidden rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)]/95 shadow-md backdrop-blur-sm">
      <div className="flex items-start justify-between gap-3 border-b border-[var(--color-border)] px-5 py-4">
        <div>
          <h3 className="ram-wordmark text-base text-brand-600">{title}</h3>
          {description ? <p className="mt-0.5 text-xs text-[var(--color-ink-500)]">{description}</p> : null}
        </div>
        {action}
      </div>
      <div className="p-5">{children}</div>
    </div>
  );
}

export function InfoCard({
  title,
  children,
  action,
}: {
  title: string;
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="flex h-full flex-col rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)]/95 shadow-md backdrop-blur-sm">
      <div className="flex items-center justify-between border-b border-[var(--color-border)] px-5 py-3.5">
        <h3 className="ram-wordmark text-sm text-brand-600">{title}</h3>
        {action}
      </div>
      <div className="flex-1 p-5">{children}</div>
    </div>
  );
}
