import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import toast from "react-hot-toast";
import { Plus } from "lucide-react";
import { badgesApi, employeesApi } from "@/api/services";
import { DataTable, type Column } from "@/components/shared/DataTable";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, StatusBadge } from "@/components/ui/Card";
import { FieldError, Input, Label, Select } from "@/components/ui/FormControls";
import { ConfirmDialog, PageLoader } from "@/components/ui/Feedback";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { Badge, BadgeRequest, BadgeStatus, BadgeType } from "@/types/api";
import { formatDate } from "@/utils/cn";

const schema = z.object({
  badgeNumber: z.string().min(2),
  type: z.enum(["RFID", "NFC", "MAGNETIC", "QR_CODE", "OTHER"]),
  status: z.enum(["ACTIVE", "SUSPENDED", "EXPIRED", "REVOKED", "LOST", "PENDING_ACTIVATION"]),
  issueDate: z.string().min(1),
  expirationDate: z.string().optional(),
});
type FormValues = z.infer<typeof schema>;

export function BadgesListPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState("id,desc");
  const [toDelete, setToDelete] = useState<Badge | null>(null);
  const keyword = useDebouncedValue(search);
  const query = useQuery({ queryKey: ["badges", page, keyword, sort], queryFn: () => badgesApi.list({ page, size: 10, keyword, sort }) });
  const remove = useMutation({
    mutationFn: (id: number) => badgesApi.remove(id),
    onSuccess: async () => { toast.success("Badge supprimé"); setToDelete(null); await qc.invalidateQueries({ queryKey: ["badges"] }); },
  });
  const columns = useMemo<Column<Badge>[]>(() => [
    { key: "badgeNumber", header: "Numéro", sortable: true, render: (r) => r.badgeNumber, exportValue: (r) => r.badgeNumber },
    { key: "type", header: "Type", render: (r) => r.type, exportValue: (r) => r.type },
    { key: "status", header: "Statut", render: (r) => <StatusBadge tone={r.status === "ACTIVE" ? "success" : "warning"}>{r.status}</StatusBadge>, exportValue: (r) => r.status },
    { key: "employee", header: "Affecté à", render: (r) => r.currentAssignment ? `${r.currentAssignment.employeeFirstName} ${r.currentAssignment.employeeLastName}` : "—", exportValue: (r) => r.currentAssignment?.employeeNumber },
    { key: "actions", header: "Actions", render: (r) => (
      <div className="flex gap-2">
        <Link className="text-brand-700 hover:underline" to={`/badges/${r.id}`}>Détail</Link>
        <Link className="text-brand-700 hover:underline" to={`/badges/${r.id}/edit`}>Éditer</Link>
        <button type="button" className="text-[var(--color-danger)] hover:underline" onClick={() => setToDelete(r)}>Supprimer</button>
      </div>
    )},
  ], []);
  return (
    <div>
      <PageHeader title="Badges" breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Badges" }]} actions={<Link to="/badges/new"><Button><Plus className="size-4" /> Nouveau</Button></Link>} />
      <DataTable columns={columns} rows={query.data?.content ?? []} loading={query.isLoading} search={search} onSearchChange={setSearch} page={page} size={query.data?.size ?? 10} totalElements={query.data?.totalElements ?? 0} totalPages={query.data?.totalPages ?? 0} onPageChange={setPage} sort={sort} onSortChange={setSort} exportFileName="badges.csv" />
      <ConfirmDialog open={!!toDelete} title="Supprimer le badge ?" description={toDelete?.badgeNumber ?? ""} danger loading={remove.isPending} onClose={() => setToDelete(null)} onConfirm={() => toDelete && remove.mutate(toDelete.id)} />
    </div>
  );
}

export function BadgeFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const entityQuery = useQuery({ queryKey: ["badges", id], queryFn: () => badgesApi.get(Number(id)), enabled: isEdit });
  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { badgeNumber: "", type: "RFID", status: "ACTIVE", issueDate: new Date().toISOString().slice(0, 10), expirationDate: "" },
  });
  useEffect(() => {
    if (entityQuery.data) {
      const b = entityQuery.data;
      reset({ badgeNumber: b.badgeNumber, type: b.type, status: b.status, issueDate: b.issueDate, expirationDate: b.expirationDate ?? "" });
    }
  }, [entityQuery.data, reset]);
  const save = useMutation({
    mutationFn: (values: FormValues) => {
      const body: BadgeRequest = { ...values, expirationDate: values.expirationDate || undefined };
      return isEdit ? badgesApi.update(Number(id), body) : badgesApi.create(body);
    },
    onSuccess: async (saved) => { toast.success("Badge enregistré"); await qc.invalidateQueries({ queryKey: ["badges"] }); navigate(`/badges/${saved.id}`); },
  });
  if (isEdit && entityQuery.isLoading) return <PageLoader />;
  return (
    <div>
      <PageHeader title={isEdit ? "Modifier badge" : "Nouveau badge"} breadcrumbs={[{ label: "Badges", to: "/badges" }, { label: isEdit ? "Édition" : "Création" }]} />
      <Card><CardBody>
        <form className="grid max-w-2xl gap-4 md:grid-cols-2" onSubmit={handleSubmit((v) => save.mutate(v))}>
          <div><Label>Numéro</Label><Input {...register("badgeNumber")} /><FieldError message={errors.badgeNumber?.message} /></div>
          <div><Label>Type</Label><Select {...register("type")}>{(["RFID","NFC","MAGNETIC","QR_CODE","OTHER"] as BadgeType[]).map((t) => <option key={t} value={t}>{t}</option>)}</Select></div>
          <div><Label>Statut</Label><Select {...register("status")}>{(["ACTIVE","SUSPENDED","EXPIRED","REVOKED","LOST","PENDING_ACTIVATION"] as BadgeStatus[]).map((s) => <option key={s} value={s}>{s}</option>)}</Select></div>
          <div><Label>Date d&apos;émission</Label><Input type="date" {...register("issueDate")} /><FieldError message={errors.issueDate?.message} /></div>
          <div><Label>Expiration</Label><Input type="date" {...register("expirationDate")} /></div>
          <div className="flex gap-2 md:col-span-2"><Button type="submit">Enregistrer</Button><Button type="button" variant="secondary" onClick={() => navigate("/badges")}>Annuler</Button></div>
        </form>
      </CardBody></Card>
    </div>
  );
}

export function BadgeDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const [employeeId, setEmployeeId] = useState("");
  const query = useQuery({ queryKey: ["badges", id], queryFn: () => badgesApi.get(Number(id)) });
  const employees = useQuery({ queryKey: ["employees", "mini"], queryFn: () => employeesApi.list({ page: 0, size: 100 }) });
  const invalidate = async () => { await qc.invalidateQueries({ queryKey: ["badges"] }); await query.refetch(); };
  const action = useMutation({
    mutationFn: async (type: "assign" | "unassign" | "suspend" | "activate" | "revoke") => {
      const badgeId = Number(id);
      if (type === "assign") return badgesApi.assign(badgeId, Number(employeeId));
      if (type === "unassign") return badgesApi.unassign(badgeId);
      if (type === "suspend") return badgesApi.suspend(badgeId);
      if (type === "activate") return badgesApi.activate(badgeId);
      return badgesApi.revoke(badgeId);
    },
    onSuccess: async () => { toast.success("Action effectuée"); await invalidate(); },
  });
  const remove = useMutation({
    mutationFn: () => badgesApi.remove(Number(id)),
    onSuccess: async () => { toast.success("Supprimé"); await qc.invalidateQueries({ queryKey: ["badges"] }); navigate("/badges"); },
  });
  if (query.isLoading) return <PageLoader />;
  const b = query.data;
  if (!b) return <p>Introuvable</p>;
  return (
    <div>
      <PageHeader title={`Badge ${b.badgeNumber}`} breadcrumbs={[{ label: "Badges", to: "/badges" }, { label: b.badgeNumber }]} actions={<><Link to={`/badges/${b.id}/edit`}><Button variant="secondary">Éditer</Button></Link><Button variant="danger" onClick={() => setOpen(true)}>Supprimer</Button></>} />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card><CardBody className="space-y-2 text-sm">
          <p><strong>Type:</strong> {b.type}</p>
          <p><strong>Statut:</strong> <StatusBadge>{b.status}</StatusBadge></p>
          <p><strong>Émission:</strong> {b.issueDate}</p>
          <p><strong>Expiration:</strong> {b.expirationDate || "—"}</p>
          <p><strong>Affectation:</strong> {b.currentAssignment ? `${b.currentAssignment.employeeFirstName} ${b.currentAssignment.employeeLastName}` : "Aucune"}</p>
          <p><strong>Créé:</strong> {formatDate(b.createdAt)}</p>
        </CardBody></Card>
        <Card><CardBody className="space-y-3">
          <h3 className="font-semibold">Actions métier</h3>
          <div className="flex flex-wrap gap-2">
            <Select value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
              <option value="">Employé…</option>
              {(employees.data?.content ?? []).map((e) => <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>)}
            </Select>
            <Button disabled={!employeeId || action.isPending} onClick={() => action.mutate("assign")}>Assigner</Button>
            <Button variant="secondary" onClick={() => action.mutate("unassign")}>Désassigner</Button>
            <Button variant="secondary" onClick={() => action.mutate("suspend")}>Suspendre</Button>
            <Button variant="secondary" onClick={() => action.mutate("activate")}>Activer</Button>
            <Button variant="danger" onClick={() => action.mutate("revoke")}>Révoquer</Button>
          </div>
        </CardBody></Card>
      </div>
      <ConfirmDialog open={open} title="Supprimer ?" description="Action irréversible." danger loading={remove.isPending} onClose={() => setOpen(false)} onConfirm={() => remove.mutate()} />
    </div>
  );
}

export function BadgeAssignmentsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const keyword = useDebouncedValue(search);
  const query = useQuery({ queryKey: ["badge-assignments", page, keyword], queryFn: () => badgesApi.list({ page, size: 20, keyword }) });
  const rows = (query.data?.content ?? []).filter((b) => b.currentAssignment?.active);
  const columns = useMemo<Column<Badge>[]>(() => [
    { key: "badgeNumber", header: "Badge", render: (r) => r.badgeNumber, exportValue: (r) => r.badgeNumber },
    { key: "employee", header: "Employé", render: (r) => `${r.currentAssignment?.employeeFirstName} ${r.currentAssignment?.employeeLastName}`, exportValue: (r) => r.currentAssignment?.employeeNumber },
    { key: "assignedAt", header: "Affecté le", render: (r) => formatDate(r.currentAssignment?.assignedAt), exportValue: (r) => r.currentAssignment?.assignedAt },
    { key: "status", header: "Statut badge", render: (r) => <StatusBadge>{r.status}</StatusBadge>, exportValue: (r) => r.status },
    { key: "actions", header: "Actions", render: (r) => <Link className="text-brand-700 hover:underline" to={`/badges/${r.id}`}>Ouvrir</Link> },
  ], []);
  return (
    <div>
      <PageHeader title="Affectations de badges" breadcrumbs={[{ label: "Badges", to: "/badges" }, { label: "Affectations" }]} />
      <DataTable columns={columns} rows={rows} loading={query.isLoading} search={search} onSearchChange={setSearch} page={page} size={20} totalElements={rows.length} totalPages={query.data?.totalPages ?? 1} onPageChange={setPage} exportFileName="assignments.csv" />
    </div>
  );
}
