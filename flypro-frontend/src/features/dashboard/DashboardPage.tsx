import { useState, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Activity,
  AlertTriangle,
  BadgeCheck,
  Building2,
  DoorOpen,
  Gauge,
  MapPin,
  Users,
} from "lucide-react";
import { PageHeader } from "@/components/shared/PageHeader";
import { AppAreaChart, AppBarChart, AppLineChart, AppPieChart } from "@/components/shared/Charts";
import { Card, CardBody, CardHeader, StatusBadge } from "@/components/ui/Card";
import { Input, Label } from "@/components/ui/FormControls";
import { PageLoader } from "@/components/ui/Feedback";
import { dashboardApi } from "@/api/services";
import { cn, formatNumber } from "@/utils/cn";
import type { ChartDto } from "@/types/api";

function pickChart(charts: ChartDto[] | undefined, key: string) {
  return charts?.find((c) => c.key === key);
}

function ChartCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Card>
      <CardHeader title={title} />
      <CardBody>{children}</CardBody>
    </Card>
  );
}

type KpiTone = "brand" | "success" | "warning" | "danger" | "neutral";

const TONE_CLASSES: Record<KpiTone, string> = {
  brand: "bg-brand-100 text-brand-700 dark:bg-brand-900/40 dark:text-brand-200",
  success: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-200",
  warning: "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-200",
  danger: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-200",
  neutral: "bg-[var(--color-ink-100)] text-[var(--color-ink-600)] dark:bg-white/10 dark:text-[var(--color-ink-300)]",
};

function kpiVisual(key: string): { icon: typeof Users; tone: KpiTone } {
  const k = key.toLowerCase();
  if (k.includes("employee")) {
    return { icon: Users, tone: k.includes("inactive") || k.includes("archived") ? "neutral" : "brand" };
  }
  if (k.includes("department")) return { icon: Building2, tone: "brand" };
  if (k.includes("badge")) {
    if (k.includes("active")) return { icon: BadgeCheck, tone: "success" };
    if (k.includes("expired") || k.includes("revoked") || k.includes("suspended")) {
      return { icon: BadgeCheck, tone: "danger" };
    }
    return { icon: BadgeCheck, tone: "brand" };
  }
  if (k.includes("accesspoint")) return { icon: DoorOpen, tone: "brand" };
  if (k.includes("zone")) return { icon: MapPin, tone: k.includes("sensitive") ? "warning" : "brand" };
  if (k.includes("alert")) return { icon: AlertTriangle, tone: k.includes("open") ? "danger" : "success" };
  if (k.includes("access")) {
    if (k.includes("granted")) return { icon: Activity, tone: "success" };
    if (k.includes("denied")) return { icon: Activity, tone: "danger" };
    return { icon: Activity, tone: "brand" };
  }
  return { icon: Gauge, tone: "brand" };
}

export function DashboardPage() {
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const params = {
    from: from || undefined,
    to: to || undefined,
  };

  const kpisQuery = useQuery({ queryKey: ["dashboard", "kpis", params], queryFn: () => dashboardApi.kpis(params) });
  const chartsQuery = useQuery({ queryKey: ["dashboard", "charts", params], queryFn: () => dashboardApi.charts(params) });
  const statsQuery = useQuery({ queryKey: ["dashboard", "statistics", params], queryFn: () => dashboardApi.statistics(params) });
  const trendsQuery = useQuery({ queryKey: ["dashboard", "trends", params], queryFn: () => dashboardApi.trends(params) });
  const peakQuery = useQuery({ queryKey: ["dashboard", "peak", params], queryFn: () => dashboardApi.peakHours(params) });

  if (kpisQuery.isLoading || chartsQuery.isLoading) return <PageLoader />;

  const charts = chartsQuery.data ?? [];
  const daily = pickChart(charts, "dailyAccessEvents");
  const granted = pickChart(charts, "grantedVsDenied");
  const badgeDist = pickChart(charts, "badgeStatusDistribution");
  const alertDist = pickChart(charts, "alertSeverityDistribution");
  const byDept = pickChart(charts, "accessesByDepartment");

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description="Vue d’ensemble opérationnelle des accès, badges et alertes"
        breadcrumbs={[{ label: "Accueil", to: "/dashboard" }, { label: "Dashboard" }]}
      />

      <div className="mb-6 overflow-hidden rounded-2xl bg-gradient-to-r from-brand-900 via-brand-700 to-brand-600 px-6 py-5 text-white shadow-lg shadow-brand-900/20">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs font-semibold tracking-[0.2em] text-white/70 uppercase">Centre opérationnel</p>
            <p className="ram-wordmark mt-1 text-xl text-white">Vue d'ensemble en temps réel</p>
          </div>
          <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-xs font-medium backdrop-blur-sm">
            <span className="size-2 animate-pulse rounded-full bg-emerald-400" aria-hidden />
            Données actualisées
          </div>
        </div>
      </div>

      <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <Label htmlFor="from">Du</Label>
          <Input id="from" type="datetime-local" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="to">Au</Label>
          <Input id="to" type="datetime-local" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {(kpisQuery.data ?? []).slice(0, 8).map((kpi) => {
          const { icon: Icon, tone } = kpiVisual(kpi.key);
          return (
            <Card key={kpi.key} className="overflow-hidden border-t-2 border-t-brand-600">
              <CardBody className="flex items-center gap-4">
                <span className={cn("flex size-11 shrink-0 items-center justify-center rounded-xl", TONE_CLASSES[tone])}>
                  <Icon className="size-5" />
                </span>
                <div className="min-w-0">
                  <p className="truncate text-xs font-semibold tracking-wide text-[var(--color-ink-500)] uppercase">
                    {kpi.label}
                  </p>
                  <p className="mt-1 font-display text-2xl font-bold tabular-nums text-[var(--color-ink-950)]">
                    {formatNumber(kpi.value)}
                  </p>
                </div>
              </CardBody>
            </Card>
          );
        })}
      </div>

      <div className="mt-6 grid gap-4 xl:grid-cols-2">
        {daily ? (
          <ChartCard title={daily.title}>
            <AppAreaChart chart={daily} />
          </ChartCard>
        ) : null}
        {byDept ? (
          <ChartCard title={byDept.title}>
            <AppBarChart chart={byDept} />
          </ChartCard>
        ) : null}
        {granted ? (
          <ChartCard title={granted.title}>
            <AppPieChart chart={granted} />
          </ChartCard>
        ) : null}
        {badgeDist ? (
          <ChartCard title={badgeDist.title}>
            <AppPieChart chart={badgeDist} />
          </ChartCard>
        ) : null}
        {alertDist ? (
          <ChartCard title={alertDist.title}>
            <AppBarChart chart={alertDist} />
          </ChartCard>
        ) : null}
        {pickChart(charts, "weeklyAccessEvents") ? (
          <ChartCard title="Tendance hebdomadaire">
            <AppLineChart chart={pickChart(charts, "weeklyAccessEvents")!} />
          </ChartCard>
        ) : null}
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <Card>
          <CardHeader title="Top employés" />
          <CardBody className="space-y-2">
            {(statsQuery.data?.mostActiveEmployees ?? []).map((item) => (
              <div key={`${item.id}-${item.name}`} className="flex justify-between text-sm">
                <span>{item.name}</span>
                <StatusBadge tone="brand">{item.count}</StatusBadge>
              </div>
            ))}
          </CardBody>
        </Card>
        <Card>
          <CardHeader title="Top points d’accès" />
          <CardBody className="space-y-2">
            {(statsQuery.data?.topAccessPoints ?? []).map((item) => (
              <div key={`${item.id}-${item.name}`} className="flex justify-between text-sm">
                <span>{item.name}</span>
                <StatusBadge>{item.count}</StatusBadge>
              </div>
            ))}
          </CardBody>
        </Card>
        <Card>
          <CardHeader title="Top zones" />
          <CardBody className="space-y-2">
            {(statsQuery.data?.topZones ?? []).map((item) => (
              <div key={`${item.id}-${item.name}`} className="flex justify-between text-sm">
                <span>{item.name}</span>
                <StatusBadge tone="warning">{item.count}</StatusBadge>
              </div>
            ))}
          </CardBody>
        </Card>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader title="Tendances" description="Comparaison période courante vs précédente" />
          <CardBody className="space-y-3">
            {(trendsQuery.data ?? []).map((t) => (
              <div key={t.key} className="flex items-center justify-between rounded-lg bg-[var(--color-surface-muted)] px-3 py-2 text-sm">
                <div>
                  <p className="font-semibold">{t.label}</p>
                  <p className="text-[var(--color-ink-500)]">
                    {t.currentValue} vs {t.previousValue}
                  </p>
                </div>
                <StatusBadge tone={t.changePercent >= 0 ? "warning" : "success"}>
                  {t.changePercent.toFixed(1)}%
                </StatusBadge>
              </div>
            ))}
          </CardBody>
        </Card>
        <Card>
          <CardHeader title="Heures de pointe (UTC)" />
          <CardBody>
            {peakQuery.data ? (
              <AppBarChart
                chart={{
                  key: "peak",
                  title: "Peak",
                  labels: peakQuery.data.map((p) => p.name),
                  series: [{ name: "accès", data: peakQuery.data.map((p) => p.count) }],
                }}
              />
            ) : (
              <p className="text-sm text-[var(--color-ink-500)]">Chargement…</p>
            )}
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
