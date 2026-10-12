import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { alertsApi } from "@/api/services";
import { DataTable, type Column } from "@/components/shared/DataTable";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, StatusBadge } from "@/components/ui/Card";
import { Label, Select } from "@/components/ui/FormControls";
import { PageLoader } from "@/components/ui/Feedback";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { Alert, AlertSeverity, AlertStatus, AlertType } from "@/types/api";
import { cn, formatDate } from "@/utils/cn";

function severityTone(s: AlertSeverity) {
  if (s === "CRITICAL" || s === "HIGH") return "danger" as const;
  if (s === "MEDIUM") return "warning" as const;
  return "neutral" as const;
}

function severityBar(s: AlertSeverity) {
  if (s === "CRITICAL") return "bg-red-600";
  if (s === "HIGH") return "bg-orange-500";
  if (s === "MEDIUM") return "bg-amber-400";
  return "bg-slate-400";
}

export function AlertsListPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [severity, setSeverity] = useState("");
  const [type, setType] = useState("");
  const keyword = useDebouncedValue(search);
  const query = useQuery({
    queryKey: ["alerts", page, keyword, status, severity, type],
    queryFn: () =>
      alertsApi.list({
        page,
        size: 10,
        keyword,
        status: status || undefined,
        severity: severity || undefined,
        type: type || undefined,
      }),
  });
  const columns = useMemo<Column<Alert>[]>(
    () => [
      {
        key: "severity",
        header: "Priorité",
        render: (r) => (
          <span className="inline-flex items-center gap-2">
            <span className={cn("size-2.5 rounded-full", severityBar(r.severity))} aria-hidden />
            <StatusBadge tone={severityTone(r.severity)}>{r.severity}</StatusBadge>
          </span>
        ),
        exportValue: (r) => r.severity,
      },
      { key: "createdDate", header: "Date", render: (r) => formatDate(r.createdDate), exportValue: (r) => r.createdDate },
      { key: "title", header: "Titre", render: (r) => r.title, exportValue: (r) => r.title },
      { key: "type", header: "Type", render: (r) => r.type, exportValue: (r) => r.type },
      { key: "status", header: "Statut", render: (r) => <StatusBadge>{r.status}</StatusBadge>, exportValue: (r) => r.status },
      {
        key: "actions",
        header: "Actions",
        render: (r) => (
          <Link className="font-semibold text-brand-600 hover:underline" to={`/alerts/${r.id}`}>
            Détail
          </Link>
        ),
      },
    ],
    [],
  );
  return (
    <div>
      <PageHeader
        title="Alertes"
        description="Surveillance opérationnelle et workflow de résolution"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Alertes" }]}
      />
      <div className="mb-4 grid gap-3 sm:grid-cols-3">
        <div>
          <Label>Statut</Label>
          <Select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Tous</option>
            {(["OPEN", "ACKNOWLEDGED", "RESOLVED", "CLOSED"] as AlertStatus[]).map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <Label>Sévérité</Label>
          <Select value={severity} onChange={(e) => setSeverity(e.target.value)}>
            <option value="">Toutes</option>
            {(["LOW", "MEDIUM", "HIGH", "CRITICAL"] as AlertSeverity[]).map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <Label>Type</Label>
          <Select value={type} onChange={(e) => setType(e.target.value)}>
            <option value="">Tous</option>
            {(
              [
                "ACCESS_DENIED",
                "MULTIPLE_ACCESS_DENIED",
                "BADGE_REVOKED",
                "UNAUTHORIZED_ZONE",
                "OUTSIDE_WORKING_HOURS",
              ] as AlertType[]
            ).map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </Select>
        </div>
      </div>
      <DataTable
        columns={columns}
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        search={search}
        onSearchChange={setSearch}
        page={page}
        size={10}
        totalElements={query.data?.totalElements ?? 0}
        totalPages={query.data?.totalPages ?? 0}
        onPageChange={setPage}
        exportFileName="alerts.csv"
      />
    </div>
  );
}

export function AlertDetailPage() {
  const { id } = useParams();
  const qc = useQueryClient();
  const query = useQuery({ queryKey: ["alerts", id], queryFn: () => alertsApi.get(Number(id)) });
  const mutate = useMutation({
    mutationFn: (action: "acknowledge" | "resolve" | "close") => {
      if (action === "acknowledge") return alertsApi.acknowledge(Number(id));
      if (action === "resolve") return alertsApi.resolve(Number(id));
      return alertsApi.close(Number(id));
    },
    onSuccess: async () => {
      toast.success("Alerte mise à jour");
      await qc.invalidateQueries({ queryKey: ["alerts"] });
      await query.refetch();
    },
  });
  if (query.isLoading) return <PageLoader />;
  const a = query.data;
  if (!a) return <p>Introuvable</p>;

  const steps = [
    { key: "OPEN", label: "Ouverte" },
    { key: "ACKNOWLEDGED", label: "Acquittée" },
    { key: "RESOLVED", label: "Résolue" },
    { key: "CLOSED", label: "Clôturée" },
  ];
  const stepIdx = Math.max(
    0,
    steps.findIndex((s) => s.key === a.status),
  );

  return (
    <div>
      <PageHeader
        title={a.title}
        breadcrumbs={[{ label: "Alertes", to: "/alerts" }, { label: `#${a.id}` }]}
        actions={
          <>
            <Button variant="secondary" disabled={mutate.isPending} onClick={() => mutate.mutate("acknowledge")}>
              Acquitter
            </Button>
            <Button disabled={mutate.isPending} onClick={() => mutate.mutate("resolve")}>
              Résoudre
            </Button>
            <Button variant="ghost" disabled={mutate.isPending} onClick={() => mutate.mutate("close")}>
              Clôturer
            </Button>
          </>
        }
      />

      <Card className="mb-4">
        <CardBody>
          <p className="mb-3 text-xs font-semibold tracking-wide text-ink-500 uppercase">Workflow de résolution</p>
          <ol className="flex flex-wrap gap-2">
            {steps.map((s, i) => (
              <li
                key={s.key}
                className={cn(
                  "rounded-xl px-3 py-1.5 text-xs font-semibold",
                  i <= stepIdx
                    ? "bg-brand-600 text-white"
                    : "bg-[var(--color-surface-muted)] text-ink-500",
                )}
              >
                {i + 1}. {s.label}
              </li>
            ))}
          </ol>
        </CardBody>
      </Card>

      <Card>
        <CardBody className="grid gap-3 text-sm md:grid-cols-2">
          <p>
            <strong>Type:</strong> {a.type}
          </p>
          <p>
            <strong>Sévérité:</strong>{" "}
            <StatusBadge tone={severityTone(a.severity)}>{a.severity}</StatusBadge>
          </p>
          <p>
            <strong>Statut:</strong> <StatusBadge>{a.status}</StatusBadge>
          </p>
          <p>
            <strong>Créée:</strong> {formatDate(a.createdDate)}
          </p>
          <p>
            <strong>Employé:</strong> {a.employeeFullName || "—"}
          </p>
          <p>
            <strong>Badge:</strong> {a.badgeNumber || "—"}
          </p>
          <p>
            <strong>Zone:</strong> {a.zoneName || "—"}
          </p>
          <p className="md:col-span-2">
            <strong>Description:</strong> {a.description || "—"}
          </p>
        </CardBody>
      </Card>
    </div>
  );
}
