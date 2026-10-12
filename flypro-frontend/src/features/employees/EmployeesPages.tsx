import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import toast from "react-hot-toast";
import { IdCard, Plus, ScanFace } from "lucide-react";
import { departmentsApi, employeesApi, accessProfilesApi, faceApi } from "@/api/services";
import { BadgeCard3D } from "@/components/badges/BadgeCard3D";
import { DataTable, type Column } from "@/components/shared/DataTable";
import { PageHeader } from "@/components/shared/PageHeader";
import { WebcamCapture } from "@/components/shared/WebcamCapture";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader, StatusBadge } from "@/components/ui/Card";
import { FieldError, Input, Label, Select } from "@/components/ui/FormControls";
import { ConfirmDialog, PageLoader } from "@/components/ui/Feedback";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { Employee, EmployeeRequest, EmployeeStatus } from "@/types/api";
import { formatDate } from "@/utils/cn";

const schema = z.object({
  employeeNumber: z.string().min(2),
  firstName: z.string().min(2),
  lastName: z.string().min(2),
  email: z.string().email(),
  phone: z.string().optional(),
  position: z.string().min(2),
  status: z.enum(["ACTIVE", "INACTIVE", "SUSPENDED", "ARCHIVED"]),
  departmentId: z.number().min(1),
  accessProfileId: z.number().optional().nullable(),
});

type FormValues = z.infer<typeof schema>;

export function EmployeesListPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState("id,desc");
  const [toDelete, setToDelete] = useState<Employee | null>(null);
  const keyword = useDebouncedValue(search);
  const query = useQuery({
    queryKey: ["employees", page, keyword, sort],
    queryFn: () => employeesApi.list({ page, size: 10, keyword, sort }),
  });
  const remove = useMutation({
    mutationFn: (id: number) => employeesApi.remove(id),
    onSuccess: async () => {
      toast.success("Employé supprimé");
      setToDelete(null);
      await qc.invalidateQueries({ queryKey: ["employees"] });
    },
  });

  const columns = useMemo<Column<Employee>[]>(
    () => [
      { key: "employeeNumber", header: "Matricule", sortable: true, render: (r) => r.employeeNumber, exportValue: (r) => r.employeeNumber },
      { key: "name", header: "Nom", render: (r) => `${r.firstName} ${r.lastName}`, exportValue: (r) => `${r.firstName} ${r.lastName}` },
      { key: "email", header: "Email", render: (r) => r.email, exportValue: (r) => r.email },
      { key: "department", header: "Département", render: (r) => r.department?.name, exportValue: (r) => r.department?.name },
      {
        key: "status",
        header: "Statut",
        render: (r) => (
          <StatusBadge tone={r.status === "ACTIVE" ? "success" : r.status === "ARCHIVED" ? "neutral" : "warning"}>
            {r.status}
          </StatusBadge>
        ),
        exportValue: (r) => r.status,
      },
      {
        key: "actions",
        header: "Actions",
        render: (r) => (
          <div className="flex gap-2">
            <Link className="text-brand-700 hover:underline" to={`/employees/${r.id}`}>Détail</Link>
            <Link className="text-brand-700 hover:underline" to={`/employees/${r.id}/edit`}>Éditer</Link>
            <button type="button" className="text-[var(--color-danger)] hover:underline" onClick={() => setToDelete(r)}>Supprimer</button>
          </div>
        ),
      },
    ],
    [],
  );

  return (
    <div>
      <PageHeader
        title="Employés"
        description="Annuaire et cycle de vie des collaborateurs"
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Employés" }]}
        actions={<Link to="/employees/new"><Button><Plus className="size-4" /> Nouveau</Button></Link>}
      />
      <DataTable
        columns={columns}
        rows={query.data?.content ?? []}
        loading={query.isLoading}
        search={search}
        onSearchChange={setSearch}
        page={page}
        size={query.data?.size ?? 10}
        totalElements={query.data?.totalElements ?? 0}
        totalPages={query.data?.totalPages ?? 0}
        onPageChange={setPage}
        sort={sort}
        onSortChange={setSort}
        exportFileName="employees.csv"
      />
      <ConfirmDialog
        open={!!toDelete}
        title="Supprimer l'employé ?"
        description={toDelete ? `${toDelete.firstName} ${toDelete.lastName}` : ""}
        danger
        loading={remove.isPending}
        onClose={() => setToDelete(null)}
        onConfirm={() => toDelete && remove.mutate(toDelete.id)}
      />
    </div>
  );
}

export function EmployeeFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const entityQuery = useQuery({ queryKey: ["employees", id], queryFn: () => employeesApi.get(Number(id)), enabled: isEdit });
  const departments = useQuery({ queryKey: ["departments", "all"], queryFn: () => departmentsApi.list({ page: 0, size: 100 }) });
  const profiles = useQuery({ queryKey: ["access-profiles", "all"], queryFn: () => accessProfilesApi.list({ page: 0, size: 100 }) });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      employeeNumber: "",
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      position: "",
      status: "ACTIVE",
      departmentId: 0,
      accessProfileId: null,
    },
  });

  useEffect(() => {
    if (entityQuery.data) {
      const e = entityQuery.data;
      reset({
        employeeNumber: e.employeeNumber,
        firstName: e.firstName,
        lastName: e.lastName,
        email: e.email,
        phone: e.phone ?? "",
        position: e.position,
        status: e.status,
        departmentId: e.department.id,
        accessProfileId: e.accessProfileId ?? null,
      });
    }
  }, [entityQuery.data, reset]);

  const save = useMutation({
    mutationFn: (values: FormValues) => {
      const body: EmployeeRequest = {
        ...values,
        accessProfileId: values.accessProfileId && values.accessProfileId > 0 ? values.accessProfileId : null,
      };
      return isEdit ? employeesApi.update(Number(id), body) : employeesApi.create(body);
    },
    onSuccess: async (saved) => {
      if (!isEdit && saved.portalUsername && saved.temporaryPassword) {
        toast.success(
          `Employé créé. Portail : ${saved.portalUsername} / ${saved.temporaryPassword}`,
          { duration: 10000 },
        );
      } else {
        toast.success(isEdit ? "Employé mis à jour" : "Employé créé");
      }
      await qc.invalidateQueries({ queryKey: ["employees"] });
      navigate(`/employees/${saved.id}`);
    },
  });

  if (isEdit && entityQuery.isLoading) return <PageLoader />;

  return (
    <div>
      <PageHeader
        title={isEdit ? "Modifier employé" : "Nouvel employé"}
        breadcrumbs={[{ label: "Employés", to: "/employees" }, { label: isEdit ? "Édition" : "Création" }]}
      />
      <Card>
        <CardBody>
          <form className="grid max-w-3xl gap-4 md:grid-cols-2" onSubmit={handleSubmit((v) => save.mutate(v))}>
            <div><Label>Matricule</Label><Input {...register("employeeNumber")} /><FieldError message={errors.employeeNumber?.message} /></div>
            <div><Label>Poste</Label><Input {...register("position")} /><FieldError message={errors.position?.message} /></div>
            <div><Label>Prénom</Label><Input {...register("firstName")} /><FieldError message={errors.firstName?.message} /></div>
            <div><Label>Nom</Label><Input {...register("lastName")} /><FieldError message={errors.lastName?.message} /></div>
            <div><Label>Email</Label><Input type="email" {...register("email")} /><FieldError message={errors.email?.message} /></div>
            <div><Label>Téléphone</Label><Input {...register("phone")} /></div>
            <div>
              <Label>Statut</Label>
              <Select {...register("status")}>
                {(["ACTIVE", "INACTIVE", "SUSPENDED", "ARCHIVED"] as EmployeeStatus[]).map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </Select>
            </div>
            <div>
              <Label>Département</Label>
              <Select {...register("departmentId", { valueAsNumber: true })} disabled={departments.isLoading}>
                <option value={0}>
                  {departments.isLoading
                    ? "Chargement…"
                    : departments.isError
                      ? "Erreur de chargement"
                      : "Sélectionner…"}
                </option>
                {(departments.data?.content ?? []).map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </Select>
              {departments.isError ? (
                <p className="mt-1 text-xs text-[var(--color-danger)]">
                  Impossible de charger les départements. Vérifiez que le backend tourne (profil dev).
                  <button
                    type="button"
                    className="ml-2 underline"
                    onClick={() => void departments.refetch()}
                  >
                    Réessayer
                  </button>
                </p>
              ) : null}
              {!departments.isLoading && !departments.isError && (departments.data?.content?.length ?? 0) === 0 ? (
                <p className="mt-1 text-xs text-[var(--color-ink-500)]">
                  Aucun département en base. Créez-en un d&apos;abord dans le menu Départements.
                </p>
              ) : null}
              <FieldError message={errors.departmentId?.message} />
            </div>
            <div className="md:col-span-2">
              <Label>Profil d&apos;accès</Label>
              <Select {...register("accessProfileId", { valueAsNumber: true })} disabled={profiles.isLoading}>
                <option value={0}>
                  {profiles.isLoading ? "Chargement…" : "Aucun"}
                </option>
                {(profiles.data?.content ?? []).map((p) => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </Select>
              {profiles.isError ? (
                <p className="mt-1 text-xs text-[var(--color-danger)]">
                  Impossible de charger les profils d&apos;accès.
                  <button type="button" className="ml-2 underline" onClick={() => void profiles.refetch()}>
                    Réessayer
                  </button>
                </p>
              ) : null}
            </div>
            <div className="flex gap-2 md:col-span-2">
              <Button type="submit" disabled={save.isPending}>Enregistrer</Button>
              <Button type="button" variant="secondary" onClick={() => navigate("/employees")}>Annuler</Button>
            </div>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}

export function EmployeeDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const query = useQuery({ queryKey: ["employees", id], queryFn: () => employeesApi.get(Number(id)) });
  const badgesQuery = useQuery({ queryKey: ["employees", id, "badges"], queryFn: () => employeesApi.badges(Number(id)) });
  const remove = useMutation({
    mutationFn: () => employeesApi.remove(Number(id)),
    onSuccess: async () => {
      toast.success("Supprimé");
      await qc.invalidateQueries({ queryKey: ["employees"] });
      navigate("/employees");
    },
  });
  const enrollFace = useMutation({
    mutationFn: (imageBase64: string) => faceApi.enroll(Number(id), imageBase64),
    onSuccess: (res) => toast.success(`Visage enrôlé (${res.samplesCount} échantillon(s))`),
    onError: () => toast.error("Enrôlement impossible — assurez-vous qu'un visage est bien visible dans le cadre."),
  });
  if (query.isLoading) return <PageLoader />;
  const e = query.data;
  if (!e) return <p>Introuvable</p>;
  return (
    <div>
      <PageHeader
        title={`${e.firstName} ${e.lastName}`}
        breadcrumbs={[{ label: "Employés", to: "/employees" }, { label: e.employeeNumber }]}
        actions={
          <>
            <Link to={`/employees/${e.id}/edit`}><Button variant="secondary">Éditer</Button></Link>
            <Button variant="danger" onClick={() => setOpen(true)}>Supprimer</Button>
          </>
        }
      />
      <Card>
        <CardBody className="grid gap-2 text-sm md:grid-cols-2">
          <p><strong>Matricule:</strong> {e.employeeNumber}</p>
          <p><strong>Email:</strong> {e.email}</p>
          <p><strong>Poste:</strong> {e.position}</p>
          <p><strong>Statut:</strong> <StatusBadge tone="brand">{e.status}</StatusBadge></p>
          <p><strong>Département:</strong> {e.department?.name}</p>
          <p><strong>Profil d&apos;accès:</strong> {e.accessProfileId ?? "—"}</p>
          <p><strong>Créé:</strong> {formatDate(e.createdAt)}</p>
          <p className="md:col-span-2 rounded-lg border border-dashed border-brand-200 bg-brand-50/60 px-3 py-2 text-xs text-brand-800">
            <strong>Portail employé :</strong> identifiant = matricule en minuscules (ex.{" "}
            {e.employeeNumber.toLowerCase()}) · mot de passe initial <code>Employee@123</code>
          </p>
        </CardBody>
      </Card>

      <Card className="mt-4">
        <CardHeader
          title="Badges"
          description="Badges physiques assignés à cet employé"
          action={<IdCard className="size-4 text-brand-600" />}
        />
        <CardBody>
          {badgesQuery.isLoading ? (
            <p className="text-sm text-[var(--color-ink-500)]">Chargement…</p>
          ) : (badgesQuery.data ?? []).length === 0 ? (
            <p className="text-sm text-[var(--color-ink-500)]">Aucun badge assigné à cet employé.</p>
          ) : (
            <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
              {(badgesQuery.data ?? []).map((b) => (
                <BadgeCard3D
                  key={b.id}
                  allowPhotoUpload={false}
                  data={{
                    badgeNumber: b.badgeNumber,
                    type: b.type,
                    status: b.status,
                    issueDate: b.issueDate,
                    expirationDate: b.expirationDate ?? undefined,
                    employeeId: e.id,
                    employeeFirstName: e.firstName,
                    employeeLastName: e.lastName,
                    employeeNumber: e.employeeNumber,
                  }}
                />
              ))}
            </div>
          )}
        </CardBody>
      </Card>

      <Card className="mt-4">
        <CardHeader
          title="Reconnaissance faciale"
          description="Enrôler le visage de cet employé pour la vérification d'identité (démonstration académique)"
          action={<ScanFace className="size-4 text-brand-600" />}
        />
        <CardBody className="flex flex-col items-center gap-3">
          <WebcamCapture onCapture={(img) => enrollFace.mutate(img)} />
          {enrollFace.isPending ? (
            <p className="text-xs text-[var(--color-ink-500)]">Enrôlement en cours…</p>
          ) : null}
        </CardBody>
      </Card>

      <ConfirmDialog open={open} title="Supprimer ?" description="Action irréversible." danger loading={remove.isPending} onClose={() => setOpen(false)} onConfirm={() => remove.mutate()} />
    </div>
  );
}
