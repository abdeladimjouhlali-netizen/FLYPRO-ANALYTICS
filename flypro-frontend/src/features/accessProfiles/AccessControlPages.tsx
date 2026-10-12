import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import toast from "react-hot-toast";
import { Plus } from "lucide-react";
import { accessPointsApi, accessProfilesApi, zonesApi } from "@/api/services";
import { DataTable, type Column } from "@/components/shared/DataTable";
import { PageHeader } from "@/components/shared/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, StatusBadge } from "@/components/ui/Card";
import { Checkbox, FieldError, Input, Label, Select, Textarea } from "@/components/ui/FormControls";
import { ConfirmDialog, PageLoader } from "@/components/ui/Feedback";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { AccessPoint, AccessPointRequest, AccessProfile, AccessProfileRequest, SecurityLevel } from "@/types/api";
import { formatDate } from "@/utils/cn";

const profileSchema = z.object({
  name: z.string().min(2),
  description: z.string().optional(),
  securityLevel: z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"]),
  startTime: z.string().min(1),
  endTime: z.string().min(1),
  zoneIds: z.array(z.number()).min(1, "Sélectionnez au moins une zone"),
});
type ProfileForm = z.infer<typeof profileSchema>;

export function AccessProfilesListPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [toDelete, setToDelete] = useState<AccessProfile | null>(null);
  const keyword = useDebouncedValue(search);
  const query = useQuery({ queryKey: ["access-profiles", page, keyword], queryFn: () => accessProfilesApi.list({ page, size: 10, keyword }) });
  const remove = useMutation({ mutationFn: (id: number) => accessProfilesApi.remove(id), onSuccess: async () => { toast.success("Profil supprimé"); setToDelete(null); await qc.invalidateQueries({ queryKey: ["access-profiles"] }); } });
  const columns = useMemo<Column<AccessProfile>[]>(() => [
    { key: "name", header: "Nom", render: (r) => r.name, exportValue: (r) => r.name },
    { key: "securityLevel", header: "Niveau", render: (r) => <StatusBadge tone="brand">{r.securityLevel}</StatusBadge>, exportValue: (r) => r.securityLevel },
    { key: "hours", header: "Horaires", render: (r) => `${r.startTime} - ${r.endTime}`, exportValue: (r) => `${r.startTime}-${r.endTime}` },
    { key: "zones", header: "Zones", render: (r) => r.zones?.length ?? 0, exportValue: (r) => r.zones?.length ?? 0 },
    { key: "actions", header: "Actions", render: (r) => (
      <div className="flex gap-2">
        <Link className="text-brand-700 hover:underline" to={`/access-profiles/${r.id}`}>Détail</Link>
        <Link className="text-brand-700 hover:underline" to={`/access-profiles/${r.id}/edit`}>Éditer</Link>
        <button type="button" className="text-[var(--color-danger)] hover:underline" onClick={() => setToDelete(r)}>Supprimer</button>
      </div>
    )},
  ], []);
  return (
    <div>
      <PageHeader title="Profils d'accès" breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Profils d'accès" }]} actions={<Link to="/access-profiles/new"><Button><Plus className="size-4" /> Nouveau</Button></Link>} />
      <DataTable columns={columns} rows={query.data?.content ?? []} loading={query.isLoading} search={search} onSearchChange={setSearch} page={page} size={10} totalElements={query.data?.totalElements ?? 0} totalPages={query.data?.totalPages ?? 0} onPageChange={setPage} exportFileName="access-profiles.csv" />
      <ConfirmDialog open={!!toDelete} title="Supprimer ?" description={toDelete?.name ?? ""} danger loading={remove.isPending} onClose={() => setToDelete(null)} onConfirm={() => toDelete && remove.mutate(toDelete.id)} />
    </div>
  );
}

export function AccessProfileFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const entityQuery = useQuery({ queryKey: ["access-profiles", id], queryFn: () => accessProfilesApi.get(Number(id)), enabled: isEdit });
  const zones = useQuery({ queryKey: ["zones", "all"], queryFn: () => zonesApi.list({ page: 0, size: 100 }) });
  const { register, handleSubmit, reset, setValue, watch, formState: { errors } } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: { name: "", description: "", securityLevel: "MEDIUM", startTime: "08:00:00", endTime: "18:00:00", zoneIds: [] },
  });
  const selected = watch("zoneIds") ?? [];
  useEffect(() => {
    if (entityQuery.data) {
      const p = entityQuery.data;
      reset({ name: p.name, description: p.description ?? "", securityLevel: p.securityLevel, startTime: p.startTime, endTime: p.endTime, zoneIds: p.zones.map((z) => z.id) });
    }
  }, [entityQuery.data, reset]);
  const save = useMutation({
    mutationFn: (values: ProfileForm) => {
      const body: AccessProfileRequest = values;
      return isEdit ? accessProfilesApi.update(Number(id), body) : accessProfilesApi.create(body);
    },
    onSuccess: async (saved) => { toast.success("Profil enregistré"); await qc.invalidateQueries({ queryKey: ["access-profiles"] }); navigate(`/access-profiles/${saved.id}`); },
  });
  if (isEdit && entityQuery.isLoading) return <PageLoader />;
  return (
    <div>
      <PageHeader title={isEdit ? "Modifier profil" : "Nouveau profil"} breadcrumbs={[{ label: "Profils d'accès", to: "/access-profiles" }, { label: isEdit ? "Édition" : "Création" }]} />
      <Card><CardBody>
        <form className="grid max-w-3xl gap-4" onSubmit={handleSubmit((v) => save.mutate(v))}>
          <div><Label>Nom</Label><Input {...register("name")} /><FieldError message={errors.name?.message} /></div>
          <div><Label>Description</Label><Textarea {...register("description")} /></div>
          <div><Label>Niveau</Label><Select {...register("securityLevel")}>{(["LOW","MEDIUM","HIGH","CRITICAL"] as SecurityLevel[]).map((s) => <option key={s} value={s}>{s}</option>)}</Select></div>
          <div className="grid gap-4 md:grid-cols-2">
            <div><Label>Début</Label><Input type="time" step={1} {...register("startTime")} /></div>
            <div><Label>Fin</Label><Input type="time" step={1} {...register("endTime")} /></div>
          </div>
          <div className="space-y-2">
            <Label>Zones autorisées</Label>
            <div className="grid gap-2 sm:grid-cols-2">
              {(zones.data?.content ?? []).map((z) => (
                <Checkbox key={z.id} label={z.name} checked={selected.includes(z.id)} onChange={(e) => {
                  const next = e.target.checked ? [...selected, z.id] : selected.filter((id) => id !== z.id);
                  setValue("zoneIds", next, { shouldValidate: true });
                }} />
              ))}
            </div>
            <FieldError message={errors.zoneIds?.message} />
          </div>
          <div className="flex gap-2"><Button type="submit">Enregistrer</Button><Button type="button" variant="secondary" onClick={() => navigate("/access-profiles")}>Annuler</Button></div>
        </form>
      </CardBody></Card>
    </div>
  );
}

export function AccessProfileDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const query = useQuery({ queryKey: ["access-profiles", id], queryFn: () => accessProfilesApi.get(Number(id)) });
  const remove = useMutation({ mutationFn: () => accessProfilesApi.remove(Number(id)), onSuccess: async () => { toast.success("Supprimé"); await qc.invalidateQueries({ queryKey: ["access-profiles"] }); navigate("/access-profiles"); } });
  if (query.isLoading) return <PageLoader />;
  const p = query.data;
  if (!p) return <p>Introuvable</p>;
  return (
    <div>
      <PageHeader title={p.name} breadcrumbs={[{ label: "Profils d'accès", to: "/access-profiles" }, { label: p.name }]} actions={<><Link to={`/access-profiles/${p.id}/edit`}><Button variant="secondary">Éditer</Button></Link><Button variant="danger" onClick={() => setOpen(true)}>Supprimer</Button></>} />
      <Card><CardBody className="space-y-2 text-sm">
        <p><strong>Niveau:</strong> {p.securityLevel}</p>
        <p><strong>Horaires:</strong> {p.startTime} — {p.endTime}</p>
        <p><strong>Description:</strong> {p.description || "—"}</p>
        <p><strong>Zones:</strong> {p.zones.map((z) => z.name).join(", ") || "—"}</p>
        <p><strong>Créé:</strong> {formatDate(p.createdAt)}</p>
      </CardBody></Card>
      <ConfirmDialog open={open} title="Supprimer ?" description="Action irréversible." danger loading={remove.isPending} onClose={() => setOpen(false)} onConfirm={() => remove.mutate()} />
    </div>
  );
}

const pointSchema = z.object({
  name: z.string().min(2),
  code: z.string().min(2),
  type: z.enum(["DOOR", "TURNSTILE", "GATE", "MOBILE_READER", "VIRTUAL"]),
  description: z.string().optional(),
  status: z.enum(["ACTIVE", "INACTIVE", "MAINTENANCE", "OUT_OF_SERVICE"]),
  ipAddress: z.string().optional(),
  location: z.string().optional(),
  zoneId: z.number().min(1),
});
type PointForm = z.infer<typeof pointSchema>;

export function AccessPointsListPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [toDelete, setToDelete] = useState<AccessPoint | null>(null);
  const keyword = useDebouncedValue(search);
  const query = useQuery({ queryKey: ["access-points", page, keyword], queryFn: () => accessPointsApi.list({ page, size: 10, keyword }) });
  const remove = useMutation({ mutationFn: (id: number) => accessPointsApi.remove(id), onSuccess: async () => { toast.success("Point d'accès supprimé"); setToDelete(null); await qc.invalidateQueries({ queryKey: ["access-points"] }); } });
  const columns = useMemo<Column<AccessPoint>[]>(() => [
    { key: "code", header: "Code", render: (r) => r.code, exportValue: (r) => r.code },
    { key: "name", header: "Nom", render: (r) => r.name, exportValue: (r) => r.name },
    { key: "type", header: "Type", render: (r) => r.type, exportValue: (r) => r.type },
    { key: "zone", header: "Zone", render: (r) => r.zone?.name, exportValue: (r) => r.zone?.name },
    { key: "status", header: "Statut", render: (r) => <StatusBadge>{r.status}</StatusBadge>, exportValue: (r) => r.status },
    { key: "actions", header: "Actions", render: (r) => (
      <div className="flex gap-2">
        <Link className="text-brand-700 hover:underline" to={`/access-points/${r.id}`}>Détail</Link>
        <Link className="text-brand-700 hover:underline" to={`/access-points/${r.id}/edit`}>Éditer</Link>
        <button type="button" className="text-[var(--color-danger)] hover:underline" onClick={() => setToDelete(r)}>Supprimer</button>
      </div>
    )},
  ], []);
  return (
    <div>
      <PageHeader title="Points d'accès" breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Points d'accès" }]} actions={<Link to="/access-points/new"><Button><Plus className="size-4" /> Nouveau</Button></Link>} />
      <DataTable columns={columns} rows={query.data?.content ?? []} loading={query.isLoading} search={search} onSearchChange={setSearch} page={page} size={10} totalElements={query.data?.totalElements ?? 0} totalPages={query.data?.totalPages ?? 0} onPageChange={setPage} exportFileName="access-points.csv" />
      <ConfirmDialog open={!!toDelete} title="Supprimer ?" description={toDelete?.name ?? ""} danger loading={remove.isPending} onClose={() => setToDelete(null)} onConfirm={() => toDelete && remove.mutate(toDelete.id)} />
    </div>
  );
}

export function AccessPointFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const entityQuery = useQuery({ queryKey: ["access-points", id], queryFn: () => accessPointsApi.get(Number(id)), enabled: isEdit });
  const zones = useQuery({ queryKey: ["zones", "all"], queryFn: () => zonesApi.list({ page: 0, size: 100 }) });
  const { register, handleSubmit, reset, formState: { errors } } = useForm<PointForm>({
    resolver: zodResolver(pointSchema),
    defaultValues: { name: "", code: "", type: "DOOR", description: "", status: "ACTIVE", ipAddress: "", location: "", zoneId: 0 },
  });
  useEffect(() => {
    if (entityQuery.data) {
      const p = entityQuery.data;
      reset({ name: p.name, code: p.code, type: p.type, description: p.description ?? "", status: p.status, ipAddress: p.ipAddress ?? "", location: p.location ?? "", zoneId: p.zone.id });
    }
  }, [entityQuery.data, reset]);
  const save = useMutation({
    mutationFn: (values: PointForm) => {
      const body: AccessPointRequest = values;
      return isEdit ? accessPointsApi.update(Number(id), body) : accessPointsApi.create(body);
    },
    onSuccess: async (saved) => { toast.success("Enregistré"); await qc.invalidateQueries({ queryKey: ["access-points"] }); navigate(`/access-points/${saved.id}`); },
  });
  if (isEdit && entityQuery.isLoading) return <PageLoader />;
  return (
    <div>
      <PageHeader title={isEdit ? "Modifier point d'accès" : "Nouveau point d'accès"} breadcrumbs={[{ label: "Points d'accès", to: "/access-points" }, { label: isEdit ? "Édition" : "Création" }]} />
      <Card><CardBody>
        <form className="grid max-w-3xl gap-4 md:grid-cols-2" onSubmit={handleSubmit((v) => save.mutate(v))}>
          <div><Label>Nom</Label><Input {...register("name")} /><FieldError message={errors.name?.message} /></div>
          <div><Label>Code</Label><Input {...register("code")} /><FieldError message={errors.code?.message} /></div>
          <div><Label>Type</Label><Select {...register("type")}><option value="DOOR">DOOR</option><option value="TURNSTILE">TURNSTILE</option><option value="GATE">GATE</option><option value="MOBILE_READER">MOBILE_READER</option><option value="VIRTUAL">VIRTUAL</option></Select></div>
          <div><Label>Statut</Label><Select {...register("status")}><option value="ACTIVE">ACTIVE</option><option value="INACTIVE">INACTIVE</option><option value="MAINTENANCE">MAINTENANCE</option><option value="OUT_OF_SERVICE">OUT_OF_SERVICE</option></Select></div>
          <div><Label>Zone</Label><Select {...register("zoneId", { valueAsNumber: true })}><option value={0}>Sélectionner…</option>{(zones.data?.content ?? []).map((z) => <option key={z.id} value={z.id}>{z.name}</option>)}</Select><FieldError message={errors.zoneId?.message} /></div>
          <div><Label>IP</Label><Input {...register("ipAddress")} /></div>
          <div className="md:col-span-2"><Label>Emplacement</Label><Input {...register("location")} /></div>
          <div className="md:col-span-2"><Label>Description</Label><Textarea {...register("description")} /></div>
          <div className="flex gap-2 md:col-span-2"><Button type="submit">Enregistrer</Button><Button type="button" variant="secondary" onClick={() => navigate("/access-points")}>Annuler</Button></div>
        </form>
      </CardBody></Card>
    </div>
  );
}

export function AccessPointDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const query = useQuery({ queryKey: ["access-points", id], queryFn: () => accessPointsApi.get(Number(id)) });
  const remove = useMutation({ mutationFn: () => accessPointsApi.remove(Number(id)), onSuccess: async () => { toast.success("Supprimé"); await qc.invalidateQueries({ queryKey: ["access-points"] }); navigate("/access-points"); } });
  if (query.isLoading) return <PageLoader />;
  const p = query.data;
  if (!p) return <p>Introuvable</p>;
  return (
    <div>
      <PageHeader title={p.name} breadcrumbs={[{ label: "Points d'accès", to: "/access-points" }, { label: p.code }]} actions={<><Link to={`/access-points/${p.id}/edit`}><Button variant="secondary">Éditer</Button></Link><Button variant="danger" onClick={() => setOpen(true)}>Supprimer</Button></>} />
      <Card><CardBody className="space-y-2 text-sm">
        <p><strong>Code:</strong> {p.code}</p>
        <p><strong>Type:</strong> {p.type}</p>
        <p><strong>Statut:</strong> <StatusBadge>{p.status}</StatusBadge></p>
        <p><strong>Zone:</strong> {p.zone?.name}</p>
        <p><strong>IP:</strong> {p.ipAddress || "—"}</p>
        <p><strong>Emplacement:</strong> {p.location || "—"}</p>
        <p><strong>Créé:</strong> {formatDate(p.createdAt)}</p>
      </CardBody></Card>
      <ConfirmDialog open={open} title="Supprimer ?" description="Action irréversible." danger loading={remove.isPending} onClose={() => setOpen(false)} onConfirm={() => remove.mutate()} />
    </div>
  );
}
