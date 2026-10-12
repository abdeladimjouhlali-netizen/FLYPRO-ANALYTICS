import { useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import toast from "react-hot-toast";
import { Plus } from "lucide-react";
import { accessEventsApi, accessPointsApi, badgesApi } from "@/api/services";
import { DataTable, type Column } from "@/components/shared/DataTable";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, StatusBadge } from "@/components/ui/Card";
import { FieldError, Input, Label, Select, Textarea } from "@/components/ui/FormControls";
import { PageLoader } from "@/components/ui/Feedback";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { AccessEvent, AccessEventRequest } from "@/types/api";
import { formatDate } from "@/utils/cn";

const schema = z.object({
  eventTimestamp: z.string().min(1),
  direction: z.enum(["ENTRY", "EXIT", "NEUTRAL"]),
  result: z.enum(["AUTHORIZED", "DENIED", "EXPIRED", "UNKNOWN", "TECHNICAL_ERROR"]),
  refusalReason: z.string().optional(),
  source: z.enum(["API", "CSV", "JSON", "EXCEL", "SIMULATOR", "MANUAL"]),
  badgeId: z.number().min(1),
  accessPointId: z.number().min(1),
});
type FormValues = z.infer<typeof schema>;

export function AccessEventsListPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const keyword = useDebouncedValue(search);
  const query = useQuery({
    queryKey: ["access-events", page, keyword],
    queryFn: () => accessEventsApi.list({ page, size: 10, keyword }),
  });
  const columns = useMemo<Column<AccessEvent>[]>(() => [
    { key: "eventTimestamp", header: "Horodatage", render: (r) => formatDate(r.eventTimestamp), exportValue: (r) => r.eventTimestamp },
    { key: "badge", header: "Badge", render: (r) => r.badgeNumber ?? r.badgeId, exportValue: (r) => r.badgeNumber },
    { key: "employee", header: "Employé", render: (r) => r.employeeFullName ?? "—", exportValue: (r) => r.employeeFullName },
    { key: "point", header: "Point", render: (r) => r.accessPointName ?? r.accessPointId, exportValue: (r) => r.accessPointName },
    { key: "result", header: "Résultat", render: (r) => <StatusBadge tone={r.result === "AUTHORIZED" ? "success" : "danger"}>{r.result}</StatusBadge>, exportValue: (r) => r.result },
    { key: "actions", header: "Actions", render: (r) => <Link className="text-brand-700 hover:underline" to={`/access-events/${r.id}`}>Détail</Link> },
  ], []);
  return (
    <div>
      <PageHeader title="Événements d'accès" breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Événements" }]} actions={<Link to="/access-events/new"><Button><Plus className="size-4" /> Nouvel événement</Button></Link>} />
      <DataTable columns={columns} rows={query.data?.content ?? []} loading={query.isLoading} search={search} onSearchChange={setSearch} page={page} size={10} totalElements={query.data?.totalElements ?? 0} totalPages={query.data?.totalPages ?? 0} onPageChange={setPage} exportFileName="access-events.csv" />
    </div>
  );
}

export function AccessEventFormPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const badges = useQuery({ queryKey: ["badges", "mini"], queryFn: () => badgesApi.list({ page: 0, size: 100 }) });
  const points = useQuery({ queryKey: ["access-points", "mini"], queryFn: () => accessPointsApi.list({ page: 0, size: 100 }) });
  const { register, handleSubmit, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      eventTimestamp: new Date().toISOString().slice(0, 16),
      direction: "ENTRY",
      result: "AUTHORIZED",
      source: "API",
      badgeId: 0,
      accessPointId: 0,
    },
  });
  const save = useMutation({
    mutationFn: (values: FormValues) => {
      const body: AccessEventRequest = {
        ...values,
        eventTimestamp: new Date(values.eventTimestamp).toISOString(),
      };
      return accessEventsApi.create(body);
    },
    onSuccess: async (saved) => {
      toast.success("Événement créé");
      await qc.invalidateQueries({ queryKey: ["access-events"] });
      navigate(`/access-events/${saved.id}`);
    },
  });
  return (
    <div>
      <PageHeader title="Nouvel événement" breadcrumbs={[{ label: "Événements", to: "/access-events" }, { label: "Création" }]} />
      <Card><CardBody>
        <form className="grid max-w-3xl gap-4 md:grid-cols-2" onSubmit={handleSubmit((v) => save.mutate(v))}>
          <div><Label>Horodatage</Label><Input type="datetime-local" {...register("eventTimestamp")} /><FieldError message={errors.eventTimestamp?.message} /></div>
          <div><Label>Direction</Label><Select {...register("direction")}><option value="ENTRY">ENTRY</option><option value="EXIT">EXIT</option><option value="NEUTRAL">NEUTRAL</option></Select></div>
          <div><Label>Résultat</Label><Select {...register("result")}><option value="AUTHORIZED">AUTHORIZED</option><option value="DENIED">DENIED</option><option value="EXPIRED">EXPIRED</option><option value="UNKNOWN">UNKNOWN</option><option value="TECHNICAL_ERROR">TECHNICAL_ERROR</option></Select></div>
          <div><Label>Source</Label><Select {...register("source")}><option value="API">API</option><option value="MANUAL">MANUAL</option><option value="SIMULATOR">SIMULATOR</option></Select></div>
          <div><Label>Badge</Label><Select {...register("badgeId", { valueAsNumber: true })}><option value={0}>Sélectionner…</option>{(badges.data?.content ?? []).map((b) => <option key={b.id} value={b.id}>{b.badgeNumber}</option>)}</Select><FieldError message={errors.badgeId?.message} /></div>
          <div><Label>Point d&apos;accès</Label><Select {...register("accessPointId", { valueAsNumber: true })}><option value={0}>Sélectionner…</option>{(points.data?.content ?? []).map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</Select><FieldError message={errors.accessPointId?.message} /></div>
          <div className="md:col-span-2"><Label>Motif de refus</Label><Textarea {...register("refusalReason")} /></div>
          <div className="flex gap-2 md:col-span-2"><Button type="submit">Créer</Button><Button type="button" variant="secondary" onClick={() => navigate("/access-events")}>Annuler</Button></div>
        </form>
      </CardBody></Card>
    </div>
  );
}

export function AccessEventDetailPage() {
  const { id } = useParams();
  const query = useQuery({ queryKey: ["access-events", id], queryFn: () => accessEventsApi.get(Number(id)) });
  if (query.isLoading) return <PageLoader />;
  const e = query.data;
  if (!e) return <p>Introuvable</p>;
  return (
    <div>
      <PageHeader title={`Événement #${e.id}`} breadcrumbs={[{ label: "Événements", to: "/access-events" }, { label: `#${e.id}` }]} />
      <Card><CardBody className="grid gap-2 text-sm md:grid-cols-2">
        <p><strong>Horodatage:</strong> {formatDate(e.eventTimestamp)}</p>
        <p><strong>Résultat:</strong> <StatusBadge tone={e.result === "AUTHORIZED" ? "success" : "danger"}>{e.result}</StatusBadge></p>
        <p><strong>Direction:</strong> {e.direction}</p>
        <p><strong>Source:</strong> {e.source}</p>
        <p><strong>Badge:</strong> {e.badgeNumber ?? e.badgeId}</p>
        <p><strong>Employé:</strong> {e.employeeFullName ?? "—"}</p>
        <p><strong>Point:</strong> {e.accessPointName ?? e.accessPointId}</p>
        <p><strong>Zone:</strong> {e.zone?.name ?? "—"}</p>
        <p className="md:col-span-2"><strong>Motif:</strong> {e.refusalReason || "—"}</p>
      </CardBody></Card>
    </div>
  );
}
